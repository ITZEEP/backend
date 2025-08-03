package org.scoula.domain.fraud.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.scoula.domain.fraud.dto.ai.AiParseResponse;
import org.scoula.domain.fraud.dto.common.MortgageeDto;
import org.scoula.domain.fraud.dto.common.ParsedRegistryDataDto;
import org.scoula.domain.fraud.dto.response.RegistryParseResponse;
import org.scoula.domain.fraud.exception.FraudErrorCode;
import org.scoula.domain.fraud.exception.FraudRiskException;
import org.scoula.global.common.util.LogSanitizerUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class AiDocumentAnalyzerService {

      private final RestTemplate restTemplate;
      private final ObjectMapper objectMapper;

      @Value("${ai.server.url:http://localhost:8000}")
      private String aiServerUrl;

      /** 등기부등본 파일을 AI 서버로 전송하여 파싱 */
      public AiParseResponse analyzeRegistryDocument(MultipartFile file) throws IOException {
          String url = aiServerUrl + "/api/parse/register";
          return sendFileToAiServer(file, url);
      }

      /** 등기부등본 파일을 AI 서버로 전송하여 파싱 */
      public RegistryParseResponse analyzeAndParseRegistryDocument(MultipartFile file)
              throws IOException {
          // AI 서버로 파일 전송하여 파싱
          AiParseResponse aiResponse = analyzeRegistryDocument(file);

          // AI 응답에서 파싱된 데이터 추출
          // AiParseResponse.AiParseData 구조에 맞게 접근
          AiParseResponse.AiParseData data = aiResponse.getData();
          if (data == null || data.getParsedData() == null) {
              throw new FraudRiskException(
                      FraudErrorCode.DOCUMENT_PROCESSING_FAILED, "AI 서버로부터 파싱된 데이터를 받지 못했습니다");
          }

          // parsedData 타입 안전 검증 후 변환
          Object parsedDataObj = data.getParsedData();
          if (!(parsedDataObj instanceof Map)) {
              throw new FraudRiskException(
                      FraudErrorCode.DOCUMENT_PROCESSING_FAILED, "파싱된 데이터 형식이 올바르지 않습니다");
          }
          @SuppressWarnings("unchecked")
          Map<String, Object> parsedData = (Map<String, Object>) parsedDataObj;

          // 응답 DTO 생성
          ParsedRegistryDataDto parsedDataDto = createParsedDataDto(parsedData);

          return RegistryParseResponse.builder()
                  .filename(
                          data.getFilename() != null
                                  ? data.getFilename()
                                  : file.getOriginalFilename())
                  .documentType(data.getDocumentType() != null ? data.getDocumentType() : "register")
                  .parsedData(parsedDataDto)
                  .build();
      }

      /** 파싱된 데이터를 DTO로 변환 */
      private ParsedRegistryDataDto createParsedDataDto(Map<String, Object> parsedData) {
          if (parsedData == null) {
              throw new FraudRiskException(
                      FraudErrorCode.DOCUMENT_PROCESSING_FAILED, "파싱된 데이터가 null입니다");
          }

          List<MortgageeDto> mortgageeList = extractMortgageeList(parsedData);

          // Boolean 값 안전하게 처리
          Boolean hasSeizure = getBooleanValue(parsedData.get("hasSeizure"));
          Boolean hasAuction = getBooleanValue(parsedData.get("hasAuction"));
          Boolean hasLitigation = getBooleanValue(parsedData.get("hasLitigation"));
          Boolean hasAttachment = getBooleanValue(parsedData.get("hasAttachment"));

          return ParsedRegistryDataDto.builder()
                  .regionAddress((String) parsedData.get("regionAddress"))
                  .roadAddress((String) parsedData.get("roadAddress"))
                  .ownerName((String) parsedData.get("ownerName"))
                  .ownerBirthDate((String) parsedData.get("ownerBirthDate"))
                  .debtor((String) parsedData.get("debtor"))
                  .mortgageeList(mortgageeList)
                  .hasSeizure(hasSeizure)
                  .hasAuction(hasAuction)
                  .hasLitigation(hasLitigation)
                  .hasAttachment(hasAttachment)
                  .build();
      }

      /** Boolean 값을 안전하게 변환하는 헬퍼 메소드 */
      private Boolean getBooleanValue(Object value) {
          if (value == null) {
              return Boolean.FALSE;
          }
          if (value instanceof Boolean) {
              return (Boolean) value;
          }
          if (value instanceof String) {
              return Boolean.parseBoolean((String) value);
          }
          return Boolean.FALSE;
      }

      /** 건축물대장 파일을 AI 서버로 전송하여 파싱 */
      public AiParseResponse analyzeBuildingDocument(MultipartFile file) throws IOException {
          String url = aiServerUrl + "/api/parse/building";
          return sendFileToAiServer(file, url);
      }

      /** 파일을 AI 서버로 전송하는 공통 메소드 */
      private AiParseResponse sendFileToAiServer(MultipartFile file, String url) throws IOException {
          log.info(
                  "AI 서버로 파일 전송 시작 - URL: {}, 파일명: {}",
                  LogSanitizerUtil.sanitize(url),
                  LogSanitizerUtil.sanitize(file.getOriginalFilename()));

          // HTTP 헤더 설정
          HttpHeaders headers = new HttpHeaders();
          headers.setContentType(MediaType.MULTIPART_FORM_DATA);

          // 멀티파트 폼 데이터 생성
          MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
          body.add(
                  "file",
                  new ByteArrayResource(file.getBytes()) {
                      @Override
                      public String getFilename() {
                          return file.getOriginalFilename();
                      }
                  });

          // HTTP 요청 엔티티 생성
          HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

          try {
              // AI 서버로 POST 요청 전송
              ResponseEntity<AiParseResponse> response =
                      restTemplate.exchange(
                              url, HttpMethod.POST, requestEntity, AiParseResponse.class);

              log.info(
                      "AI 서버 응답 성공 - 상태: {}",
                      LogSanitizerUtil.sanitizeValue(response.getStatusCode()));

              // AI 서버 응답 검증
              AiParseResponse aiResponse = response.getBody();
              if (aiResponse == null) {
                  throw new FraudRiskException(
                          FraudErrorCode.AI_SERVICE_UNAVAILABLE, "AI 서버로부터 빈 응답을 받았습니다");
              }

              if (!aiResponse.isSuccess()) {
                  throw new FraudRiskException(
                          FraudErrorCode.DOCUMENT_PROCESSING_FAILED,
                          "AI 파싱 실패: " + aiResponse.getMessage());
              }

              return aiResponse;

          } catch (HttpClientErrorException e) {
              // 4xx 클라이언트 오류
              log.error(
                      "AI 서버 클라이언트 오류 - URL: {}, 상태: {}, 메시지: {}",
                      LogSanitizerUtil.sanitize(url),
                      LogSanitizerUtil.sanitizeValue(e.getStatusCode()),
                      LogSanitizerUtil.sanitize(e.getMessage()));
              throw new FraudRiskException(
                      FraudErrorCode.INVALID_DOCUMENT_FORMAT, "AI 서버 요청 오류: " + e.getMessage(), e);
          } catch (HttpServerErrorException e) {
              // 5xx 서버 오류
              log.error(
                      "AI 서버 내부 오류 - URL: {}, 상태: {}, 메시지: {}",
                      LogSanitizerUtil.sanitize(url),
                      LogSanitizerUtil.sanitizeValue(e.getStatusCode()),
                      LogSanitizerUtil.sanitize(e.getMessage()));
              throw new FraudRiskException(
                      FraudErrorCode.AI_SERVICE_UNAVAILABLE, "AI 서버 내부 오류가 발생했습니다", e);
          } catch (ResourceAccessException e) {
              // 네트워크 연결 오류
              log.error(
                      "AI 서버 연결 실패 - URL: {}, 오류: {}",
                      LogSanitizerUtil.sanitize(url),
                      LogSanitizerUtil.sanitize(e.getMessage()));
              throw new FraudRiskException(
                      FraudErrorCode.AI_SERVICE_UNAVAILABLE, "AI 서버에 연결할 수 없습니다", e);
          } catch (FraudRiskException e) {
              // FraudRiskException은 그대로 다시 던지기
              throw e;
          } catch (Exception e) {
              // 기타 예상치 못한 오류
              log.error(
                      "AI 서버 통신 중 예상치 못한 오류 - URL: {}, 오류: {}",
                      LogSanitizerUtil.sanitize(url),
                      LogSanitizerUtil.sanitize(e.getMessage()),
                      e);
              throw new FraudRiskException(
                      FraudErrorCode.AI_SERVICE_UNAVAILABLE,
                      "AI 서버 통신 중 오류가 발생했습니다: " + e.getMessage(),
                      e);
          }
      }

      private List<MortgageeDto> extractMortgageeList(Map<String, Object> parsedData) {
          Object mortgageeListObj = parsedData.get("mortgageeList");
          if (!(mortgageeListObj instanceof List)) {
              return new ArrayList<>();
          }

          @SuppressWarnings("unchecked")
          List<Map<String, Object>> mortgageeListData = (List<Map<String, Object>>) mortgageeListObj;
          List<MortgageeDto> mortgageeList = new ArrayList<>();

          for (Map<String, Object> mortgageeData : mortgageeListData) {
              if (mortgageeData != null) {
                  mortgageeList.add(createMortgageeDto(mortgageeData));
              }
          }
          return mortgageeList;
      }

      private MortgageeDto createMortgageeDto(Map<String, Object> mortgageeData) {
          return MortgageeDto.builder()
                  .priorityNumber(extractIntegerValue(mortgageeData.get("priorityNumber")))
                  .maxClaimAmount(extractLongValue(mortgageeData.get("maxClaimAmount")))
                  .debtor((String) mortgageeData.get("debtor"))
                  .mortgagee((String) mortgageeData.get("mortgagee"))
                  .build();
      }

      private Integer extractIntegerValue(Object value) {
          return (value instanceof Number) ? ((Number) value).intValue() : null;
      }

      private Long extractLongValue(Object value) {
          return (value instanceof Number) ? ((Number) value).longValue() : null;
      }
}
