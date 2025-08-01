package org.scoula.domain.fraud.controller;

import java.util.List;

import org.scoula.domain.fraud.dto.request.RiskAnalysisRequest;
import org.scoula.domain.fraud.dto.response.DocumentAnalysisResponse;
import org.scoula.domain.fraud.dto.response.LikedHomeResponse;
import org.scoula.domain.fraud.dto.response.RiskAnalysisResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckDetailResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckListResponse;
import org.scoula.domain.fraud.exception.FraudRiskException;
import org.scoula.domain.fraud.service.FraudRiskService;
import org.scoula.global.auth.dto.CustomUserDetails;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.dto.PageRequest;
import org.scoula.global.common.dto.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api/fraud-risk")
@RequiredArgsConstructor
@Log4j2
public class FraudRiskControllerImpl implements FraudRiskController {

      private final FraudRiskService fraudRiskService;

      @Override
      @GetMapping
      public ResponseEntity<PageResponse<RiskCheckListResponse>> getRiskCheckList(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestParam(defaultValue = "1") int page,
              @RequestParam(defaultValue = "10") int size) {

          if (userDetails == null) {
              log.error("인증되지 않은 사용자의 요청");
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
          }

          Long userId = userDetails.getUserId();
          log.info("사기 위험도 분석 기록 목록 조회 - userId: {}, page: {}, size: {}", userId, page, size);

          // PageRequest 생성
          PageRequest pageRequest =
                  PageRequest.builder()
                          .page(page)
                          .size(size)
                          .sort("checkedAt")
                          .direction("DESC")
                          .build();

          // 서비스 호출
          PageResponse<RiskCheckListResponse> pageResponse =
                  fraudRiskService.getRiskCheckList(userId, pageRequest);

          // PageResponse 직접 반환
          return ResponseEntity.ok(pageResponse);
      }

      @Override
      @PostMapping(value = "/documents", consumes = "multipart/form-data")
      public ResponseEntity<ApiResponse<DocumentAnalysisResponse>> analyzeDocuments(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestParam("registryFile") MultipartFile registryFile,
              @RequestParam("buildingFile") MultipartFile buildingFile,
              @RequestParam(required = false) Long homeId) {

          if (userDetails == null) {
              log.error("인증되지 않은 사용자의 요청");
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                      .body(ApiResponse.error("인증이 필요합니다."));
          }

          Long userId = userDetails.getUserId();
          log.info(
                  "PDF 문서 분석 (OCR) 요청 - userId: {}, 매물 ID: {}, 등기부등본: {}, 건축물대장: {}",
                  userId,
                  homeId,
                  registryFile.getOriginalFilename(),
                  buildingFile.getOriginalFilename());

          try {
              // 서비스 호출 (userId 추가)
              DocumentAnalysisResponse response =
                      fraudRiskService.analyzeDocuments(userId, registryFile, buildingFile, homeId);

              return ResponseEntity.ok(ApiResponse.success(response));
          } catch (FraudRiskException e) {
              log.error(
                      "문서 분석 실패 - errorCode: {}, message: {}",
                      e.getErrorCode().getCode(),
                      e.getMessage(),
                      e);
              // 문서 타입 검증 실패 등 사용자가 수정할 수 있는 오류
              return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                      .body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()));
          } catch (Exception e) {
              log.error("문서 분석 중 예상치 못한 오류", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("문서 분석 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
          }
      }

      @Override
      @PostMapping("/analyze")
      public ResponseEntity<ApiResponse<RiskAnalysisResponse>> analyzeRisk(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestBody RiskAnalysisRequest request) {

          if (userDetails == null) {
              log.error("인증되지 않은 사용자의 요청");
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                      .body(ApiResponse.error("인증이 필요합니다."));
          }

          Long userId = userDetails.getUserId();
          log.info("사기 위험도 종합 분석 요청 - userId: {}, 매물 ID: {}", userId, request.getHomeId());

          try {
              // 서비스 호출 (userId 추가)
              RiskAnalysisResponse response = fraudRiskService.analyzeRisk(userId, request);

              return ResponseEntity.ok(ApiResponse.success(response));
          } catch (Exception e) {
              log.error("위험도 분석 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("위험도 분석 중 오류가 발생했습니다: " + e.getMessage()));
          }
      }

      @Override
      @GetMapping("/{riskCheckId}")
      public ResponseEntity<ApiResponse<RiskCheckDetailResponse>> getRiskCheckDetail(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @PathVariable Long riskCheckId) {

          if (userDetails == null) {
              log.error("인증되지 않은 사용자의 요청");
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                      .body(ApiResponse.error("인증이 필요합니다."));
          }

          Long userId = userDetails.getUserId();
          log.info("사기 위험도 분석 결과 상세 조회 - userId: {}, riskCheckId: {}", userId, riskCheckId);

          try {
              // 서비스 호출 (userId 추가하여 권한 체크)
              RiskCheckDetailResponse response =
                      fraudRiskService.getRiskCheckDetail(userId, riskCheckId);

              return ResponseEntity.ok(ApiResponse.success(response));
          } catch (Exception e) {
              log.error("상세 조회 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("상세 조회 중 오류가 발생했습니다: " + e.getMessage()));
          }
      }

      @Override
      @DeleteMapping("/{riskCheckId}")
      public ResponseEntity<ApiResponse<Void>> deleteRiskCheck(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @PathVariable Long riskCheckId) {

          if (userDetails == null) {
              log.error("인증되지 않은 사용자의 요청");
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                      .body(ApiResponse.error("인증이 필요합니다."));
          }

          Long userId = userDetails.getUserId();
          log.info("사기 위험도 분석 결과 삭제 - userId: {}, riskCheckId: {}", userId, riskCheckId);

          try {
              // 서비스 호출 (userId 추가하여 권한 체크)
              fraudRiskService.deleteRiskCheck(userId, riskCheckId);

              return ResponseEntity.ok(ApiResponse.success(null));
          } catch (Exception e) {
              log.error("삭제 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("삭제 중 오류가 발생했습니다: " + e.getMessage()));
          }
      }

      @Override
      @GetMapping("/liked-homes")
      public ResponseEntity<ApiResponse<List<LikedHomeResponse>>> getLikedHomes(
              @AuthenticationPrincipal CustomUserDetails userDetails) {

          if (userDetails == null) {
              log.error("인증되지 않은 사용자의 요청");
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                      .body(ApiResponse.error("인증이 필요합니다."));
          }

          Long userId = userDetails.getUserId();
          log.info("찜한 매물 목록 조회 - userId: {}", userId);

          try {
              // 서비스 호출
              List<LikedHomeResponse> likedHomes = fraudRiskService.getLikedHomes(userId);

              return ResponseEntity.ok(ApiResponse.success(likedHomes));
          } catch (Exception e) {
              log.error("찜한 매물 조회 실패", e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(ApiResponse.error("찜한 매물 조회 중 오류가 발생했습니다: " + e.getMessage()));
          }
      }

      @Override
      @GetMapping("/chatting-homes")
      public ResponseEntity<PageResponse<LikedHomeResponse>> getChattingHomes(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestParam(defaultValue = "1") int page,
              @RequestParam(defaultValue = "10") int size) {

          if (userDetails == null) {
              log.error("인증되지 않은 사용자의 요청");
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
          }

          Long userId = userDetails.getUserId();
          log.info("채팅 중인 매물 목록 조회 - userId: {}, page: {}, size: {}", userId, page, size);

          // PageRequest 생성
          PageRequest pageRequest =
                  PageRequest.builder()
                          .page(page)
                          .size(size)
                          .sort("lastMessageAt")
                          .direction("DESC")
                          .build();

          // 서비스 호출
          PageResponse<LikedHomeResponse> pageResponse =
                  fraudRiskService.getChattingHomes(userId, pageRequest);

          // PageResponse 직접 반환
          return ResponseEntity.ok(pageResponse);
      }
}
