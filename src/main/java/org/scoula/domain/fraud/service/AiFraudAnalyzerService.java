package org.scoula.domain.fraud.service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.scoula.domain.fraud.dto.ai.FraudRiskCheckDto;
import org.scoula.domain.fraud.dto.common.BuildingDocumentDto;
import org.scoula.domain.fraud.dto.common.MortgageeDto;
import org.scoula.domain.fraud.dto.common.RegistryDocumentDto;
import org.scoula.domain.fraud.dto.request.QuickRiskAnalysisRequest;
import org.scoula.domain.fraud.dto.request.RiskAnalysisRequest;
import org.scoula.domain.fraud.enums.RiskType;
import org.scoula.domain.fraud.exception.FraudErrorCode;
import org.scoula.domain.fraud.exception.FraudRiskException;
import org.scoula.global.common.util.LogSanitizerUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class AiFraudAnalyzerService {

      private final RestTemplate restTemplate;

      @Value("${ai.fraud.analyzer.url:http://localhost:8000}")
      private String aiServerUrl;

      /** FastAPI 서버에 사기 위험도 분석 요청 */
      public FraudRiskCheckDto.Response analyzeFraudRisk(Long userId, RiskAnalysisRequest request) {
          // 요청 데이터 구성
          FraudRiskCheckDto.Request aiRequest = buildAiRequest(userId, request);

          // AI 서버 호출
          return callAiServer(aiRequest);
      }

      /** AI 응답을 기반으로 위험도 타입 결정 */
      public RiskType determineRiskType(FraudRiskCheckDto.Response aiResponse) {
          if (aiResponse == null || aiResponse.getRiskLevel() == null) {
              return RiskType.WARN;
          }

          String riskLevel = aiResponse.getRiskLevel().toUpperCase();
          Double riskScore = aiResponse.getRiskScore();

          // AI 서버의 risk_level 기반 판단
          switch (riskLevel) {
              case "LOW":
              case "SAFE":
                  return RiskType.SAFE;
              case "MEDIUM":
              case "WARN":
              case "WARNING":
                  return RiskType.WARN;
              case "HIGH":
              case "DANGER":
              case "CRITICAL":
                  return RiskType.DANGER;
              default:
                  // risk_score 기반 판단
                  if (riskScore != null) {
                      if (riskScore < 30) {
                          return RiskType.SAFE;
                      } else if (riskScore < 70) {
                          return RiskType.WARN;
                      } else {
                          return RiskType.DANGER;
                      }
                  }
                  return RiskType.WARN;
          }
      }

      /** 등기부등본 OCR 요청 */
      public RegistryDocumentDto parseRegistryDocument(MultipartFile file) {
          try {
              // HTTP 헤더 설정
              HttpHeaders headers = new HttpHeaders();
              headers.setContentType(MediaType.MULTIPART_FORM_DATA);

              // MultiValueMap으로 요청 바디 구성
              MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
              // MultipartFile을 Resource로 변환하여 전송
              requestBody.add("file", new MultipartFileResource(file));

              // FastAPI 서버 호출
              String url = aiServerUrl + "/api/parse/register";
              HttpEntity<MultiValueMap<String, Object>> httpEntity =
                      new HttpEntity<>(requestBody, headers);

              log.info(
                      "등기부등본 OCR 요청 - URL: {}, fileName: {}, fileSize: {} bytes",
                      LogSanitizerUtil.sanitize(url),
                      LogSanitizerUtil.sanitize(file.getOriginalFilename()),
                      LogSanitizerUtil.sanitizeValue(file.getSize()));

              @SuppressWarnings("rawtypes")
              ResponseEntity<Map> response = restTemplate.postForEntity(url, httpEntity, Map.class);

              log.info(
                      "등기부등본 OCR 응답 상태: {}",
                      LogSanitizerUtil.sanitizeValue(response.getStatusCode()));

              if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                  log.info(
                          "등기부등본 OCR 완료 - AI 서버 전체 응답: {}",
                          LogSanitizerUtil.sanitizeValue(responseBody));

                  // success 필드 확인
                  Boolean success = (Boolean) responseBody.get("success");
                  if (success != null && success) {
                      // data.parsed_data 추출
                      @SuppressWarnings("unchecked")
                      Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                      if (data != null) {
                          @SuppressWarnings("unchecked")
                          Map<String, Object> parsedData =
                                  (Map<String, Object>) data.get("parsed_data");
                          if (parsedData != null) {
                              log.info(
                                      "등기부등본 OCR 파싱 데이터: {}",
                                      LogSanitizerUtil.sanitizeValue(parsedData));
                              return convertToRegistryDocument(parsedData);
                          }
                      }
                  } else {
                      // AI 서버 에러 코드 및 메시지 추출
                      String message = (String) responseBody.get("message");
                      String aiErrorCode = null;

                      // error 객체에서 code 추출
                      @SuppressWarnings("unchecked")
                      Map<String, Object> errorObj = (Map<String, Object>) responseBody.get("error");
                      if (errorObj != null) {
                          aiErrorCode = (String) errorObj.get("code");
                      }

                      // AI 서버 에러 코드 기준으로 분기
                      if ("INVALID_DOCUMENT_TYPE".equals(aiErrorCode)) {
                          // 400번대 - 사용자가 잘못된 파일을 업로드한 경우
                          if (message != null && message.contains("등기부등본")) {
                              throw new FraudRiskException(
                                      FraudErrorCode.INVALID_DOCUMENT_FORMAT, "등기부등본 PDF 파일이 아닙니다.");
                          } else if (message != null && message.contains("건축물대장")) {
                              throw new FraudRiskException(
                                      FraudErrorCode.INVALID_DOCUMENT_FORMAT, "건축물대장 PDF 파일이 아닙니다.");
                          } else {
                              throw new FraudRiskException(FraudErrorCode.UNSUPPORTED_DOCUMENT_TYPE);
                          }
                      } else {
                          // 500번대 - AI 서버 내부 오류
                          throw new FraudRiskException(
                                  FraudErrorCode.OCR_PROCESSING_FAILED,
                                  message != null ? message : "등기부등본 분석 중 오류가 발생했습니다.");
                      }
                  }

                  // 파싱 데이터가 없는 경우 빈 객체 반환
                  return RegistryDocumentDto.builder().build();
              } else {
                  throw new FraudRiskException(
                          FraudErrorCode.AI_SERVICE_UNAVAILABLE,
                          "AI 서버 등기부등본 OCR 응답 오류: " + response.getStatusCode());
              }

          } catch (FraudRiskException e) {
              // FraudRiskException은 그대로 다시 던지기 (에러 코드 보존)
              throw e;
          } catch (Exception e) {
              log.error("AI 서버 통신 실패: {}", LogSanitizerUtil.sanitize(e.getMessage()), e);
              throw new FraudRiskException(
                      FraudErrorCode.OCR_PROCESSING_FAILED,
                      "AI 서버 통신 중 오류가 발생했습니다: " + e.getMessage());
          }
      }

      /** 건축물대장 OCR 요청 */
      public BuildingDocumentDto parseBuildingDocument(MultipartFile file) {
          try {
              // HTTP 헤더 설정
              HttpHeaders headers = new HttpHeaders();
              headers.setContentType(MediaType.MULTIPART_FORM_DATA);

              // MultiValueMap으로 요청 바디 구성
              MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
              // MultipartFile을 Resource로 변환하여 전송
              requestBody.add("file", new MultipartFileResource(file));

              // FastAPI 서버 호출
              String url = aiServerUrl + "/api/parse/building";
              HttpEntity<MultiValueMap<String, Object>> httpEntity =
                      new HttpEntity<>(requestBody, headers);

              log.info(
                      "건축물대장 OCR 요청 - URL: {}, fileName: {}, fileSize: {} bytes",
                      LogSanitizerUtil.sanitize(url),
                      LogSanitizerUtil.sanitize(file.getOriginalFilename()),
                      LogSanitizerUtil.sanitizeValue(file.getSize()));

              @SuppressWarnings("rawtypes")
              ResponseEntity<Map> response = restTemplate.postForEntity(url, httpEntity, Map.class);

              log.info(
                      "건축물대장 OCR 응답 상태: {}",
                      LogSanitizerUtil.sanitizeValue(response.getStatusCode()));

              if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                  log.info(
                          "건축물대장 OCR 완료 - AI 서버 전체 응답: {}",
                          LogSanitizerUtil.sanitizeValue(responseBody));

                  // success 필드 확인
                  Boolean success = (Boolean) responseBody.get("success");
                  if (success != null && success) {
                      // data.parsed_data 추출
                      @SuppressWarnings("unchecked")
                      Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                      if (data != null) {
                          @SuppressWarnings("unchecked")
                          Map<String, Object> parsedData =
                                  (Map<String, Object>) data.get("parsed_data");
                          if (parsedData != null) {
                              log.info(
                                      "건축물대장 OCR 파싱 데이터: {}",
                                      LogSanitizerUtil.sanitizeValue(parsedData));
                              return convertToBuildingDocument(parsedData);
                          }
                      }
                  } else {
                      // AI 서버 에러 코드 및 메시지 추출
                      String message = (String) responseBody.get("message");
                      String aiErrorCode = null;

                      // error 객체에서 code 추출
                      @SuppressWarnings("unchecked")
                      Map<String, Object> errorObj = (Map<String, Object>) responseBody.get("error");
                      if (errorObj != null) {
                          aiErrorCode = (String) errorObj.get("code");
                      }

                      // AI 서버 에러 코드 기준으로 분기
                      if ("INVALID_DOCUMENT_TYPE".equals(aiErrorCode)) {
                          // 400번대 - 사용자가 잘못된 파일을 업로드한 경우
                          if (message != null && message.contains("등기부등본")) {
                              throw new FraudRiskException(
                                      FraudErrorCode.INVALID_DOCUMENT_FORMAT, "등기부등본 PDF 파일이 아닙니다.");
                          } else if (message != null && message.contains("건축물대장")) {
                              throw new FraudRiskException(
                                      FraudErrorCode.INVALID_DOCUMENT_FORMAT, "건축물대장 PDF 파일이 아닙니다.");
                          } else {
                              throw new FraudRiskException(FraudErrorCode.UNSUPPORTED_DOCUMENT_TYPE);
                          }
                      } else {
                          // 500번대 - AI 서버 내부 오류
                          throw new FraudRiskException(
                                  FraudErrorCode.OCR_PROCESSING_FAILED,
                                  message != null ? message : "건축물대장 분석 중 오류가 발생했습니다.");
                      }
                  }

                  // 파싱 데이터가 없는 경우 빈 객체 반환
                  return BuildingDocumentDto.builder().build();
              } else {
                  throw new FraudRiskException(
                          FraudErrorCode.AI_SERVICE_UNAVAILABLE,
                          "AI 서버 건축물대장 OCR 응답 오류: " + response.getStatusCode());
              }

          } catch (FraudRiskException e) {
              // FraudRiskException은 그대로 다시 던지기 (에러 코드 보존)
              throw e;
          } catch (Exception e) {
              log.error("AI 서버 통신 실패: {}", LogSanitizerUtil.sanitize(e.getMessage()), e);
              throw new FraudRiskException(
                      FraudErrorCode.OCR_PROCESSING_FAILED,
                      "AI 서버 통신 중 오류가 발생했습니다: " + e.getMessage());
          }
      }

      /** RiskAnalysisRequest를 AI 서버 요청 형식으로 변환 */
      private FraudRiskCheckDto.Request buildAiRequest(Long userId, RiskAnalysisRequest request) {
          FraudRiskCheckDto.Request.RequestBuilder builder =
                  FraudRiskCheckDto.Request.builder()
                          .userId(userId)
                          .userType("BUYER") // 기본값 설정
                          .homeId(request.getHomeId());

          // 주소 정보 추출 (등기부등본에서 추출)
          String address = null;
          String registeredUserName = null;

          if (request.getRegistryDocument() != null) {
              address =
                      request.getRegistryDocument().getRoadAddress() != null
                              ? request.getRegistryDocument().getRoadAddress()
                              : request.getRegistryDocument().getRegionAddress();
              registeredUserName = request.getRegistryDocument().getOwnerName();
          }

          // AI 서버가 요구하는 추가 필드들 설정
          // 요청에서 직접 받은 매물 정보 사용
          String finalAddress =
                  request.getAddress() != null
                          ? request.getAddress()
                          : (address != null ? address : "");
          log.error(
                  "[디버깅] 주소 결정 로직 - request.address: {}, registry.roadAddress: {}, final.address: {}",
                  request.getAddress(),
                  address,
                  finalAddress);

          builder.address(finalAddress)
                  .propertyPrice(request.getPropertyPrice() != null ? request.getPropertyPrice() : 0L)
                  .monthlyRent(request.getMonthlyRent() != null ? request.getMonthlyRent() : 0L)
                  .leaseType(request.getLeaseType() != null ? request.getLeaseType() : "")
                  .registeredUserName(
                          request.getRegisteredUserName() != null
                                  ? request.getRegisteredUserName()
                                  : (registeredUserName != null ? registeredUserName : ""))
                  .residenceType(
                          request.getResidenceType() != null ? request.getResidenceType() : "");

          // 등기부등본 정보 변환
          if (request.getRegistryDocument() != null) {
              RegistryDocumentDto registry = request.getRegistryDocument();

              // 근저당권 정보 처리
              List<FraudRiskCheckDto.MortgageeInfo> mortgageeList = new ArrayList<>();

              // 새로운 구조: mortgageeList 사용
              if (registry.getMortgageeList() != null && !registry.getMortgageeList().isEmpty()) {
                  for (MortgageeDto mortgageeDto : registry.getMortgageeList()) {
                      mortgageeList.add(
                              FraudRiskCheckDto.MortgageeInfo.builder()
                                      .priorityNumber(mortgageeDto.getPriorityNumber())
                                      .maxClaimAmount(mortgageeDto.getMaxClaimAmount())
                                      .debtor(mortgageeDto.getDebtor())
                                      .mortgagee(mortgageeDto.getMortgagee())
                                      .build());
                  }
              } else if (registry.getMaxClaimAmount() != null || registry.getMortgagee() != null) {
                  // 이전 버전 호환성: 단일 근저당권 정보가 있으면 리스트에 추가
                  mortgageeList.add(
                          FraudRiskCheckDto.MortgageeInfo.builder()
                                  .priorityNumber(1)
                                  .maxClaimAmount(registry.getMaxClaimAmount())
                                  .debtor(registry.getDebtor())
                                  .mortgagee(registry.getMortgagee())
                                  .build());
              }

              builder.registryDocument(
                      FraudRiskCheckDto.RegistryDocument.builder()
                              .regionAddress(registry.getRegionAddress())
                              .roadAddress(registry.getRoadAddress())
                              .ownerName(registry.getOwnerName())
                              .ownerBirthDate(
                                      registry.getOwnerBirthDate() != null
                                              ? registry.getOwnerBirthDate().toString()
                                              : null)
                              .debtor(registry.getDebtor())
                              .mortgageeList(mortgageeList.isEmpty() ? null : mortgageeList)
                              .hasSeizure(registry.getHasSeizure())
                              .hasAuction(registry.getHasAuction())
                              .hasLitigation(registry.getHasLitigation())
                              .hasAttachment(registry.getHasAttachment())
                              .build());
          }

          // 건축물대장 정보 변환
          if (request.getBuildingDocument() != null) {
              BuildingDocumentDto building = request.getBuildingDocument();
              builder.buildingDocument(
                      FraudRiskCheckDto.BuildingDocument.builder()
                              .siteLocation(building.getSiteLocation())
                              .roadAddress(building.getRoadAddress())
                              .totalFloorArea(building.getTotalFloorArea())
                              .purpose(building.getPurpose())
                              .floorNumber(building.getFloorNumber())
                              .approvalDate(
                                      building.getApprovalDate() != null
                                              ? building.getApprovalDate().toString()
                                              : null)
                              .isViolationBuilding(building.getIsViolationBuilding())
                              .build());
          }

          return builder.build();
      }

      /** QuickRiskAnalysisRequest를 처리하는 오버로드 메소드 */
      public FraudRiskCheckDto.Response analyzeFraudRisk(
              Long userId, QuickRiskAnalysisRequest request) {
          // QuickRiskAnalysisRequest를 RiskAnalysisRequest로 변환
          RiskAnalysisRequest analysisRequest =
                  RiskAnalysisRequest.builder()
                          .homeId(null) // 매물 ID 없음
                          .registryDocument(request.getRegistryDocument())
                          .buildingDocument(request.getBuildingDocument())
                          .registryFileUrl(request.getRegistryFileUrl())
                          .buildingFileUrl(request.getBuildingFileUrl())
                          .build();

          // 기존 analyzeFraudRisk 호출하되, QuickRiskAnalysisRequest의 정보로 보정
          FraudRiskCheckDto.Request aiRequest = buildAiRequestForQuick(userId, request);

          // AI 서버 호출
          return callAiServer(aiRequest);
      }

      /** QuickRiskAnalysisRequest를 AI 서버 요청 형식으로 변환 */
      private FraudRiskCheckDto.Request buildAiRequestForQuick(
              Long userId, QuickRiskAnalysisRequest request) {
          FraudRiskCheckDto.Request.RequestBuilder builder =
                  FraudRiskCheckDto.Request.builder()
                          .userId(userId)
                          .userType("BUYER") // 기본값 설정
                          .homeId(0L); // 매물 ID 없음 - AI 서버가 null을 허용하지 않으므로 0으로 설정

          // 주소 정보 추출 (등기부등본 우선, 없으면 직접 입력값 사용)
          String address = null;
          String registeredUserName = null;

          if (request.getRegistryDocument() != null) {
              address =
                      request.getRegistryDocument().getRoadAddress() != null
                              ? request.getRegistryDocument().getRoadAddress()
                              : request.getRegistryDocument().getRegionAddress();
              registeredUserName = request.getRegistryDocument().getOwnerName();
          }

          // 직접 입력된 매물 정보 우선 사용
          if (request.getAddress() != null) {
              address = request.getAddress();
          }
          // null이면 빈 문자열로
          if (address == null) {
              address = "";
          }

          if (registeredUserName == null && request.getRegisteredUserName() != null) {
              registeredUserName = request.getRegisteredUserName();
          }
          // null이면 빈 문자열로
          if (registeredUserName == null) {
              registeredUserName = "";
          }

          // AI 서버가 요구하는 추가 필드들 설정
          log.error(
                  "[디버깅] 서비스 외 매물 주소 결정 - request.address: {}, registry.address: {}, final.address:"
                          + " {}",
                  request.getAddress(),
                  address,
                  address != null ? address : "");

          builder.address(address) // 이미 위에서 null 처리됨
                  .propertyPrice(request.getPropertyPrice() != null ? request.getPropertyPrice() : 0L)
                  .monthlyRent(request.getMonthlyRent() != null ? request.getMonthlyRent() : 0L)
                  .leaseType(request.getLeaseType() != null ? request.getLeaseType() : "")
                  .registeredUserName(registeredUserName) // 이미 위에서 null 처리됨
                  .residenceType(
                          request.getResidenceType() != null ? request.getResidenceType() : "");

          // 등기부등본 정보 변환
          if (request.getRegistryDocument() != null) {
              RegistryDocumentDto registry = request.getRegistryDocument();

              // 근저당권 정보 처리
              List<FraudRiskCheckDto.MortgageeInfo> mortgageeList = new ArrayList<>();

              // 새로운 구조: mortgageeList 사용
              if (registry.getMortgageeList() != null && !registry.getMortgageeList().isEmpty()) {
                  for (MortgageeDto mortgageeDto : registry.getMortgageeList()) {
                      mortgageeList.add(
                              FraudRiskCheckDto.MortgageeInfo.builder()
                                      .priorityNumber(mortgageeDto.getPriorityNumber())
                                      .maxClaimAmount(mortgageeDto.getMaxClaimAmount())
                                      .debtor(mortgageeDto.getDebtor())
                                      .mortgagee(mortgageeDto.getMortgagee())
                                      .build());
                  }
              } else if (registry.getMaxClaimAmount() != null || registry.getMortgagee() != null) {
                  // 이전 버전 호환성: 단일 근저당권 정보가 있으면 리스트에 추가
                  mortgageeList.add(
                          FraudRiskCheckDto.MortgageeInfo.builder()
                                  .priorityNumber(1)
                                  .maxClaimAmount(registry.getMaxClaimAmount())
                                  .debtor(registry.getDebtor())
                                  .mortgagee(registry.getMortgagee())
                                  .build());
              }

              builder.registryDocument(
                      FraudRiskCheckDto.RegistryDocument.builder()
                              .regionAddress(registry.getRegionAddress())
                              .roadAddress(registry.getRoadAddress())
                              .ownerName(registry.getOwnerName())
                              .ownerBirthDate(
                                      registry.getOwnerBirthDate() != null
                                              ? registry.getOwnerBirthDate().toString()
                                              : null)
                              .debtor(registry.getDebtor())
                              .mortgageeList(mortgageeList.isEmpty() ? null : mortgageeList)
                              .hasSeizure(registry.getHasSeizure())
                              .hasAuction(registry.getHasAuction())
                              .hasLitigation(registry.getHasLitigation())
                              .hasAttachment(registry.getHasAttachment())
                              .build());
          }

          // 건축물대장 정보 변환
          if (request.getBuildingDocument() != null) {
              BuildingDocumentDto building = request.getBuildingDocument();
              builder.buildingDocument(
                      FraudRiskCheckDto.BuildingDocument.builder()
                              .siteLocation(building.getSiteLocation())
                              .roadAddress(building.getRoadAddress())
                              .totalFloorArea(building.getTotalFloorArea())
                              .purpose(building.getPurpose())
                              .floorNumber(building.getFloorNumber())
                              .approvalDate(
                                      building.getApprovalDate() != null
                                              ? building.getApprovalDate().toString()
                                              : null)
                              .isViolationBuilding(building.getIsViolationBuilding())
                              .build());
          }

          return builder.build();
      }

      /** AI 서버 호출 로직 추출 */
      private FraudRiskCheckDto.Response callAiServer(FraudRiskCheckDto.Request aiRequest) {
          try {
              // HTTP 헤더 설정
              HttpHeaders headers = new HttpHeaders();
              headers.setContentType(MediaType.APPLICATION_JSON);

              // FastAPI 서버 호출
              String url = aiServerUrl + "/api/analyze/risk";
              HttpEntity<FraudRiskCheckDto.Request> httpEntity = new HttpEntity<>(aiRequest, headers);

              log.error(
                      "[디버깅] AI 사기 위험도 분석 요청 - URL: {}, homeId: {}, address: {}",
                      LogSanitizerUtil.sanitize(url),
                      LogSanitizerUtil.sanitizeValue(aiRequest.getHomeId()),
                      aiRequest.getAddress());

              // 전체 요청 데이터 로깅
              try {
                  ObjectMapper mapper = new ObjectMapper();
                  String requestJson = mapper.writeValueAsString(aiRequest);
                  log.error("[디버깅] AI 서버 전송 데이터: {}", requestJson);
              } catch (Exception e) {
                  log.error("[디버깅] AI 요청 데이터 JSON 변환 실패", e);
              }

              @SuppressWarnings("rawtypes")
              ResponseEntity<Map> response = restTemplate.postForEntity(url, httpEntity, Map.class);

              log.info(
                      "AI 위험도 분석 응답 상태: {}",
                      LogSanitizerUtil.sanitizeValue(response.getStatusCode()));

              if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                  log.info("AI 위험도 분석 전체 응답: {}", LogSanitizerUtil.sanitizeValue(responseBody));

                  // 구조화된 응답인지 확인
                  Boolean success = (Boolean) responseBody.get("success");
                  if (success != null && success) {
                      // data 추출
                      @SuppressWarnings("unchecked")
                      Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                      if (data != null) {
                          log.info("AI 위험도 분석 데이터: {}", LogSanitizerUtil.sanitizeValue(data));
                          return convertToFraudRiskResponse(data);
                      }
                  } else {
                      // 오류 메시지 추출
                      String message = (String) responseBody.get("message");
                      throw new FraudRiskException(
                              FraudErrorCode.FRAUD_ANALYSIS_FAILED,
                              "위험도 분석 실패: " + (message != null ? message : "알 수 없는 오류"));
                  }

                  // 직접 FraudRiskCheckDto.Response 형태로 오는 경우 (대비용)
                  try {
                      ObjectMapper mapper = new ObjectMapper();
                      FraudRiskCheckDto.Response aiResponse =
                              mapper.convertValue(responseBody, FraudRiskCheckDto.Response.class);
                      log.info(
                              "AI 분석 완료 - analysisId: {}, riskLevel: {}, riskScore: {}",
                              LogSanitizerUtil.sanitize(String.valueOf(aiResponse.getAnalysisId())),
                              LogSanitizerUtil.sanitize(String.valueOf(aiResponse.getRiskLevel())),
                              LogSanitizerUtil.sanitize(String.valueOf(aiResponse.getRiskScore())));
                      return aiResponse;
                  } catch (Exception e) {
                      log.warn("직접 FraudRiskCheckDto.Response 변환 실패", e);
                      throw new FraudRiskException(FraudErrorCode.RISK_CALCULATION_ERROR);
                  }
              } else {
                  throw new FraudRiskException(
                          FraudErrorCode.AI_SERVICE_UNAVAILABLE,
                          "AI 서버 응답 오류: " + response.getStatusCode());
              }

          } catch (FraudRiskException e) {
              // FraudRiskException은 그대로 다시 던지기 (에러 코드 보존)
              throw e;
          } catch (Exception e) {
              log.error("AI 서버 통신 실패: {}", LogSanitizerUtil.sanitize(e.getMessage()), e);
              throw new FraudRiskException(
                      FraudErrorCode.AI_SERVICE_UNAVAILABLE,
                      "AI 서버 통신 중 오류가 발생했습니다: " + e.getMessage());
          }
      }

      /** OCR 결과를 RegistryDocumentDto로 변환 */
      private RegistryDocumentDto convertToRegistryDocument(Map<String, Object> ocrResult) {
          RegistryDocumentDto.RegistryDocumentDtoBuilder builder = RegistryDocumentDto.builder();

          // 주소 정보 (AI 서버 응답 키 이름에 맞춤)
          builder.regionAddress(getStringValue(ocrResult, "regionAddress", ""));
          builder.roadAddress(getStringValue(ocrResult, "roadAddress", ""));

          // 소유자 정보
          builder.ownerName(getStringValue(ocrResult, "ownerName", ""));
          String birthDateStr = getStringValue(ocrResult, "ownerBirthDate", null);
          if (birthDateStr != null && !birthDateStr.isEmpty()) {
              try {
                  LocalDate birthDate = LocalDate.parse(birthDateStr, DateTimeFormatter.ISO_DATE);
                  builder.ownerBirthDate(birthDate);
              } catch (Exception e) {
                  log.warn("소유자 생년월일 파싱 실패: {}", LogSanitizerUtil.sanitize(birthDateStr));
              }
          }

          // 채무자 정보
          builder.debtor(getStringValue(ocrResult, "debtor", ""));

          // 근저당권자 리스트 변환
          @SuppressWarnings("unchecked")
          List<Map<String, Object>> mortgageeListData =
                  (List<Map<String, Object>>) ocrResult.get("mortgageeList");
          List<MortgageeDto> mortgagees = new ArrayList<>();

          if (mortgageeListData != null && !mortgageeListData.isEmpty()) {
              for (Map<String, Object> mortgageeData : mortgageeListData) {
                  MortgageeDto mortgageeDto =
                          MortgageeDto.builder()
                                  .priorityNumber(getIntValue(mortgageeData, "priorityNumber", null))
                                  .maxClaimAmount(getLongValue(mortgageeData, "maxClaimAmount", 0L))
                                  .debtor(getStringValue(mortgageeData, "debtor", ""))
                                  .mortgagee(getStringValue(mortgageeData, "mortgagee", ""))
                                  .build();
                  mortgagees.add(mortgageeDto);
              }

              // 이전 버전 호환성을 위해 첫 번째 근저당권 정보를 단일 필드에도 설정
              if (!mortgagees.isEmpty()) {
                  MortgageeDto firstMortgagee = mortgagees.get(0);
                  builder.maxClaimAmount(firstMortgagee.getMaxClaimAmount());
                  builder.mortgagee(firstMortgagee.getMortgagee());
              }
          } else {
              // 근저당권 정보가 없는 경우 기본값 설정
              builder.maxClaimAmount(0L);
              builder.mortgagee("");
          }

          builder.mortgageeList(mortgagees);

          // 법적 제한사항 (AI 서버는 직접 boolean 값으로 반환)
          builder.hasSeizure(getBooleanValue(ocrResult, "hasSeizure", false));
          builder.hasAuction(getBooleanValue(ocrResult, "hasAuction", false));
          builder.hasLitigation(getBooleanValue(ocrResult, "hasLitigation", false));
          builder.hasAttachment(getBooleanValue(ocrResult, "hasAttachment", false));

          return builder.build();
      }

      /** OCR 결과를 BuildingDocumentDto로 변환 */
      private BuildingDocumentDto convertToBuildingDocument(Map<String, Object> ocrResult) {
          BuildingDocumentDto.BuildingDocumentDtoBuilder builder = BuildingDocumentDto.builder();

          // 주소 정보 (AI 서버 응답 키 이름에 맞춤)
          builder.siteLocation(getStringValue(ocrResult, "siteLocation", ""));
          builder.roadAddress(getStringValue(ocrResult, "roadAddress", ""));

          // 건물 정보
          builder.totalFloorArea(getDoubleValue(ocrResult, "totalFloorArea", 0.0));
          builder.purpose(getStringValue(ocrResult, "purpose", ""));
          builder.floorNumber(getIntValue(ocrResult, "floorNumber", 1));

          // 사용승인일
          String approvalDateStr = getStringValue(ocrResult, "approvalDate", null);
          if (approvalDateStr != null && !approvalDateStr.isEmpty()) {
              try {
                  LocalDate approvalDate =
                          LocalDate.parse(approvalDateStr, DateTimeFormatter.ISO_DATE);
                  builder.approvalDate(approvalDate);
              } catch (Exception e) {
                  log.warn("사용승인일 파싱 실패: {}", LogSanitizerUtil.sanitize(approvalDateStr));
              }
          }

          // 위반건축물 여부 (AI 서버 응답 키 이름에 맞춤)
          builder.isViolationBuilding(getBooleanValue(ocrResult, "isViolationBuilding", false));

          return builder.build();
      }

      /** AI 위험도 분석 응답을 FraudRiskCheckDto.Response로 변환 */
      private FraudRiskCheckDto.Response convertToFraudRiskResponse(Map<String, Object> data) {
          // AI 서버가 detailGroups 구조로 보내는 경우, 이를 analysis_results로 변환
          @SuppressWarnings("unchecked")
          List<Map<String, Object>> detailGroups =
                  (List<Map<String, Object>>) data.get("detailGroups");
          Map<String, Object> analysisResults = new LinkedHashMap<>();

          if (detailGroups != null) {
              for (Map<String, Object> group : detailGroups) {
                  String groupTitle = (String) group.get("title");
                  @SuppressWarnings("unchecked")
                  List<Map<String, Object>> items = (List<Map<String, Object>>) group.get("items");

                  if (items != null && !items.isEmpty()) {
                      // 각 그룹의 첫 번째 item을 사용하거나, 모든 item을 대표 값으로 사용
                      Map<String, Object> groupData = new LinkedHashMap<>();

                      for (int i = 0; i < items.size(); i++) {
                          Map<String, Object> item = items.get(i);
                          String itemTitle = (String) item.get("title");
                          String itemContent = (String) item.get("content");

                          // 각 item을 개별 키로 저장
                          groupData.put(
                                  itemTitle != null ? itemTitle : "item_" + i,
                                  Map.of(
                                          "title", itemTitle != null ? itemTitle : "",
                                          "content", itemContent != null ? itemContent : ""));
                      }

                      analysisResults.put(groupTitle, groupData);
                  }
              }
          }

          return FraudRiskCheckDto.Response.builder()
                  .status("SUCCESS")
                  .riskScore(getDoubleValue(data, "confidenceScore", 0.0))
                  .riskLevel(getStringValue(data, "riskType", "UNKNOWN"))
                  .analysisId("")
                  .analysisResults(analysisResults)
                  .recommendations(null)
                  .detailedAnalysis(null)
                  .timestamp(getStringValue(data, "analyzedAt", ""))
                  .build();
      }

      // Helper methods
      private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
          Object value = map.get(key);
          return value != null ? value.toString() : defaultValue;
      }

      private Long getLongValue(Map<String, Object> map, String key, Long defaultValue) {
          Object value = map.get(key);
          if (value == null) return defaultValue;

          try {
              if (value instanceof Number) {
                  return ((Number) value).longValue();
              }
              return Long.parseLong(value.toString());
          } catch (Exception e) {
              log.warn(
                      "Long 변환 실패 - key: {}, value: {}",
                      LogSanitizerUtil.sanitize(key),
                      LogSanitizerUtil.sanitizeValue(value));
              return defaultValue;
          }
      }

      private Double getDoubleValue(Map<String, Object> map, String key, Double defaultValue) {
          Object value = map.get(key);
          if (value == null) return defaultValue;

          try {
              if (value instanceof Number) {
                  return ((Number) value).doubleValue();
              }
              return Double.parseDouble(value.toString());
          } catch (Exception e) {
              log.warn(
                      "Double 변환 실패 - key: {}, value: {}",
                      LogSanitizerUtil.sanitize(key),
                      LogSanitizerUtil.sanitizeValue(value));
              return defaultValue;
          }
      }

      private Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
          Object value = map.get(key);
          if (value == null) return defaultValue;

          try {
              if (value instanceof Number) {
                  return ((Number) value).intValue();
              }
              return Integer.parseInt(value.toString());
          } catch (Exception e) {
              log.warn(
                      "Integer 변환 실패 - key: {}, value: {}",
                      LogSanitizerUtil.sanitize(key),
                      LogSanitizerUtil.sanitizeValue(value));
              return defaultValue;
          }
      }

      private Boolean getBooleanValue(Map<String, Object> map, String key, Boolean defaultValue) {
          Object value = map.get(key);
          if (value == null) return defaultValue;

          if (value instanceof Boolean) {
              return (Boolean) value;
          }

          String strValue = value.toString().toLowerCase();
          return "true".equals(strValue)
                  || "yes".equals(strValue)
                  || "y".equals(strValue)
                  || "1".equals(strValue);
      }

      @SuppressWarnings("unchecked")
      private Map<String, Object> getMapValue(Map<String, Object> map, String key) {
          Object value = map.get(key);
          if (value instanceof Map) {
              return (Map<String, Object>) value;
          }
          return null;
      }

      @SuppressWarnings("unchecked")
      private List<String> getListValue(Map<String, Object> map, String key) {
          Object value = map.get(key);
          if (value instanceof List) {
              return (List<String>) value;
          }
          return null;
      }

      /** MultipartFile을 Spring Resource로 변환하는 래퍼 클래스 */
      private static class MultipartFileResource implements org.springframework.core.io.Resource {
          private final MultipartFile multipartFile;

          public MultipartFileResource(MultipartFile multipartFile) {
              this.multipartFile = multipartFile;
          }

          @Override
          public InputStream getInputStream() throws java.io.IOException {
              return multipartFile.getInputStream();
          }

          @Override
          public String getFilename() {
              return multipartFile.getOriginalFilename();
          }

          @Override
          public long contentLength() throws java.io.IOException {
              return multipartFile.getSize();
          }

          @Override
          public String getDescription() {
              return "MultipartFile resource [" + multipartFile.getOriginalFilename() + "]";
          }

          // 나머지 Resource 인터페이스 메서드들은 기본 구현 또는 UnsupportedOperationException
          @Override
          public boolean exists() {
              return true;
          }

          @Override
          public boolean isReadable() {
              return true;
          }

          @Override
          public boolean isOpen() {
              return false;
          }

          @Override
          public boolean isFile() {
              return false;
          }

          @Override
          public java.net.URL getURL() throws java.io.IOException {
              throw new java.io.FileNotFoundException();
          }

          @Override
          public java.net.URI getURI() throws java.io.IOException {
              throw new java.io.FileNotFoundException();
          }

          @Override
          public java.io.File getFile() throws java.io.IOException {
              throw new java.io.FileNotFoundException();
          }

          @Override
          public long lastModified() throws java.io.IOException {
              return -1;
          }

          @Override
          public org.springframework.core.io.Resource createRelative(String relativePath)
                  throws java.io.IOException {
              throw new java.io.FileNotFoundException();
          }
      }
}
