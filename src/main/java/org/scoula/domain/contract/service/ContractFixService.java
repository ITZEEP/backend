package org.scoula.domain.contract.service;

import org.scoula.domain.contract.document.ContractMongoDocument;
import org.scoula.domain.contract.dto.ContractDTO;
import org.scoula.domain.contract.dto.LegalityDTO;
import org.scoula.domain.contract.exception.ContractException;
import org.scoula.domain.contract.mapper.ContractMapper;
import org.scoula.domain.contract.repository.ContractMongoRepository;
import org.scoula.domain.precontract.exception.PreContractErrorCode;
import org.scoula.domain.precontract.mapper.TenantPreContractMapper;
import org.scoula.domain.precontract.service.IdentityVerificationService;
import org.scoula.domain.precontract.vo.IdentityVerificationInfoVO;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class ContractFixService implements ContractFixServiceInterface {
      private final ContractMongoRepository repository;
      private final RestTemplate restTemplate;
      private final IdentityVerificationService identityVerificationService;

      private final ContractMapper contractMapper;
      private final TenantPreContractMapper tenantMapper;

      @Value("${ai.server.url:http://localhost:8000}")
      private String aiServerUrl;

      @Override
      public LegalityDTO getLegality(Long contractChatId, Long userId) {
          // userId 검증
          validateUserId(contractChatId, userId);

          // MongoDB에서 전체 부분을 조회한다
          ContractMongoDocument document = repository.getContract(contractChatId);
          if (document == null) {
              throw new BusinessException(ContractException.CONTRACT_GET);
          }

          Long ownerContractId = contractMapper.getOwnerId(contractChatId);
          Long buyerContractId = contractMapper.getBuyerId(contractChatId);
          IdentityVerificationInfoVO ownerVO =
                  identityVerificationService.getDecryptedVerificationInfo(
                          contractChatId, ownerContractId);
          IdentityVerificationInfoVO buyerVO =
                  identityVerificationService.getDecryptedVerificationInfo(
                          contractChatId, buyerContractId);

          ContractDTO dto = ContractDTO.toDTO(document, ownerVO, buyerVO);

          // AI
          try {
              // AI로 해당 데이터를 넘긴다 (restTemplate 사용)
              String url = aiServerUrl + "/api/contract/validate";
              HttpHeaders headers = new HttpHeaders();
              headers.setContentType(MediaType.APPLICATION_JSON);

              HttpEntity<ContractDTO> requestEntity = new HttpEntity<>(dto, headers);

              // 반환값을 받아오고, 그 값을 프론트에 넘겨준다.
              ResponseEntity<LegalityDTO> response =
                      restTemplate.exchange(url, HttpMethod.POST, requestEntity, LegalityDTO.class);
              LegalityDTO res = response.getBody();
              assert res != null;
              log.warn("AI 응답 값 확인: {}", res.toString());

              log.warn("AI 응답 헤더 확인: {}", response.getStatusCode());
              if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                  return response.getBody();
              } else {
                  // Sanitize response body before logging to prevent log injection
                  String responseBodyStr;
                  try {
                      ObjectMapper objectMapper = new ObjectMapper();
                      responseBodyStr = objectMapper.writeValueAsString(response.getBody());
                  } catch (Exception ex) {
                      responseBodyStr = String.valueOf(response.getBody());
                  }
                  // Remove newlines and carriage returns
                  responseBodyStr = responseBodyStr.replaceAll("[\\r\\n]", " ");
                  log.error(responseBodyStr);
                  throw new BusinessException(ContractException.CONTRACT_AI_SERVER_ERROR);
              }

          } catch (Exception e) {
              log.error(e.getMessage());
              throw new BusinessException(ContractException.CONTRACT_AI_SERVER_ERROR, e);
          }
      }

      @Override
      public Void saveSpecialContract(Long contractChatId, Long userId) {
          // userId 검증
          validateUserId(contractChatId, userId);
          // 몽고 DB에서 특약부분을 받아서 저장한다.
          try {
              repository.saveSpecialContract(contractChatId);
          } catch (Exception e) {
              // 예외 로그 기록 및 사용자에게 전달할 메시지 등 처리
              log.error("특약사항 저장 실패 ❌", e);
              throw new BusinessException(ContractException.CONTRACT_INSERT, e);
          }
          return null;
      }

      public void validateUserId(Long contractChatId, Long userId) {

          if (userId == null) {
              throw new BusinessException(PreContractErrorCode.TENANT_USER);
          }

          Long ownerContractId = contractMapper.getOwnerId(contractChatId);
          Long buyerContractId = contractMapper.getBuyerId(contractChatId);

          if (userId.equals(ownerContractId)) {
              validateIsOwner(contractChatId, userId);
              return;
          }

          if (userId.equals(buyerContractId)) {
              Long buyerId =
                      tenantMapper
                              .selectContractBuyerId(contractChatId)
                              .orElseThrow(
                                      () -> new BusinessException(PreContractErrorCode.TENANT_USER));

              if (!userId.equals(buyerId)) {
                  throw new BusinessException(PreContractErrorCode.TENANT_USER);
              }
              return;
          }

          throw new BusinessException(PreContractErrorCode.TENANT_USER);
      }

      public void validateIsOwner(Long contractChatId, Long userId) {
          Long ownerId =
                  tenantMapper
                          .selectContractOwnerId(contractChatId)
                          .orElseThrow(() -> new BusinessException(PreContractErrorCode.TENANT_USER));
          if (!userId.equals(ownerId)) {
              throw new BusinessException(PreContractErrorCode.TENANT_USER);
          }
      }
}
