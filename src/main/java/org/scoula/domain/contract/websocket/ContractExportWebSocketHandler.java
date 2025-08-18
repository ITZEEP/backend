package org.scoula.domain.contract.websocket;

import org.scoula.domain.contract.dto.ContractExportStatusDTO;
import org.scoula.domain.contract.dto.PasswordSubmitDTO;
import org.scoula.domain.contract.dto.SignatureSubmitDTO;
import org.scoula.domain.contract.service.ContractExportSyncService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/** 계약서 내보내기 WebSocket 핸들러 임대인과 임차인 간의 실시간 동기화를 처리 */
@Controller
@RequiredArgsConstructor
@Log4j2
public class ContractExportWebSocketHandler {

      private final SimpMessagingTemplate messagingTemplate;
      private final ContractExportSyncService syncService;
      private final ObjectMapper objectMapper;

      /** 계약서 내보내기 세션 참가 */
      @MessageMapping("/contract/{contractChatId}/export/join")
      @SendTo("/topic/contract/{contractChatId}/export/status")
      public ContractExportStatusDTO joinExportSession(
              @DestinationVariable Long contractChatId, @Payload String userId) {

          log.info("User {} joined contract export session for contract {}", userId, contractChatId);

          // 현재 상태 반환
          return syncService.getExportStatus(contractChatId);
      }

      /** 서명 제출 */
      @MessageMapping("/contract/{contractChatId}/export/signature")
      @SendTo("/topic/contract/{contractChatId}/export/status")
      public ContractExportStatusDTO submitSignature(
              @DestinationVariable Long contractChatId, @Payload SignatureSubmitDTO signatureData) {

          log.info(
                  "Signature submitted for contract {} by {}",
                  contractChatId,
                  signatureData.getUserRole());
          log.info("Signature data: {}", signatureData);

          // 서명 데이터 저장 및 상태 업데이트
          ContractExportStatusDTO updatedStatus =
                  syncService.updateSignature(contractChatId, signatureData);

          // 상태 업데이트는 이미 syncService에서 처리됨
          // (양측 서명 완료 시 자동으로 최종 PDF 생성)
          log.info(
                  "Signature status updated for contract {}: step={}, completed={}, ownerSigned={},"
                          + " buyerSigned={}",
                  contractChatId,
                  updatedStatus.getCurrentStep(),
                  updatedStatus.isCompleted(),
                  updatedStatus.isOwnerSignatureCompleted(),
                  updatedStatus.isBuyerSignatureCompleted());

          return updatedStatus;
      }

      /** 암호 설정 */
      @MessageMapping("/contract/{contractChatId}/export/password")
      @SendTo("/topic/contract/{contractChatId}/export/status")
      public ContractExportStatusDTO submitPassword(
              @DestinationVariable Long contractChatId, @Payload PasswordSubmitDTO passwordData) {

          log.info(
                  "Password submitted for contract {} by {}",
                  contractChatId,
                  passwordData.getUserRole());

          // 암호 저장 및 상태 업데이트
          ContractExportStatusDTO updatedStatus =
                  syncService.updatePassword(contractChatId, passwordData);

          // 양측 암호 설정 완료 확인
          if (updatedStatus.isBothPasswordsSet()) {
              log.info("Both parties have set passwords for contract {}", contractChatId);

              // 최종 PDF 생성
              try {
                  String finalPdfUrl = syncService.generateFinalPdf(contractChatId);
                  updatedStatus.setFinalPdfUrl(finalPdfUrl);
                  updatedStatus.setCurrentStep("complete");
                  updatedStatus.setCompleted(true);

                  log.info("Final PDF generated for contract {}: {}", contractChatId, finalPdfUrl);
              } catch (Exception e) {
                  log.error("Failed to generate final PDF for contract {}", contractChatId, e);
              }
          }

          return updatedStatus;
      }

      /** 진행 상태 조회 */
      @MessageMapping("/contract/{contractChatId}/export/status")
      @SendTo("/topic/contract/{contractChatId}/export/status")
      public ContractExportStatusDTO getStatus(@DestinationVariable Long contractChatId) {

          return syncService.getExportStatus(contractChatId);
      }

      /** 세션 나가기 */
      @MessageMapping("/contract/{contractChatId}/export/leave")
      public void leaveExportSession(
              @DestinationVariable Long contractChatId, @Payload String userId) {

          log.info("User {} left contract export session for contract {}", userId, contractChatId);
      }

      /** 특정 계약에 상태 업데이트 브로드캐스트 */
      public void broadcastStatusUpdate(Long contractChatId, ContractExportStatusDTO status) {
          messagingTemplate.convertAndSend(
                  "/topic/contract/" + contractChatId + "/export/status", status);
      }

      /** 에러 메시지 전송 */
      public void sendError(Long contractChatId, String errorMessage) {
          messagingTemplate.convertAndSend(
                  "/topic/contract/" + contractChatId + "/export/error", errorMessage);
      }
}
