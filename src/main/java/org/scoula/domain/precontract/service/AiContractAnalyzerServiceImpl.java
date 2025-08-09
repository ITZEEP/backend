package org.scoula.domain.precontract.service;

import java.io.InputStream;
import java.util.Map;

import org.scoula.domain.precontract.dto.ai.ContractParseResponseDto;
import org.scoula.domain.precontract.exception.OwnerPreContractErrorCode;
import org.scoula.global.common.exception.BusinessException;
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
public class AiContractAnalyzerServiceImpl implements AiContractAnalyzerService {

      private final RestTemplate restTemplate;
      private final ObjectMapper objectMapper;

      @Value("${ai.server.url:http://localhost:8000}")
      private String aiServerUrl;

      @Override
      public ContractParseResponseDto parseContractDocument(MultipartFile file) {
          try {
              // HTTP 헤더 설정
              HttpHeaders headers = new HttpHeaders();
              headers.setContentType(MediaType.MULTIPART_FORM_DATA);

              MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
              requestBody.add("file", new MultipartFileResource(file));

              String url = aiServerUrl + "/api/parse/contract";
              HttpEntity<MultiValueMap<String, Object>> httpEntity =
                      new HttpEntity<>(requestBody, headers);

              log.info(
                      "계약서 특약 OCR 요청 - URL: {}, fileName: {}, fileSize: {} bytes",
                      LogSanitizerUtil.sanitize(url),
                      LogSanitizerUtil.sanitize(file.getOriginalFilename()),
                      LogSanitizerUtil.sanitizeValue(file.getSize()));

              @SuppressWarnings("rawtypes")
              ResponseEntity<Map> response = restTemplate.postForEntity(url, httpEntity, Map.class);

              log.info(
                      "계약서 특약 OCR 응답 상태: {}",
                      LogSanitizerUtil.sanitizeValue(response.getStatusCode()));

              if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                  log.info(
                          "계약서 특약 OCR 완료 - AI 서버 전체 응답: {}",
                          LogSanitizerUtil.sanitizeValue(responseBody));

                  // 전체 응답을 DTO로 변환
                  ContractParseResponseDto parseResponse =
                          objectMapper.convertValue(responseBody, ContractParseResponseDto.class);

                  if (parseResponse.isSuccess() && parseResponse.getData() != null) {
                      log.info(
                              "계약서 특약 OCR 파싱 성공 - 특약 수: {}",
                              parseResponse.getData().getParsedData() != null
                                              && parseResponse
                                                              .getData()
                                                              .getParsedData()
                                                              .getSpecialTerms()
                                                      != null
                                      ? parseResponse
                                              .getData()
                                              .getParsedData()
                                              .getSpecialTerms()
                                              .size()
                                      : 0);

                      return parseResponse;
                  } else {
                      log.error(
                              "AI 서버 계약서 특약 OCR 실패 - error: {}, message: {}",
                              LogSanitizerUtil.sanitize(parseResponse.getError()),
                              LogSanitizerUtil.sanitize(parseResponse.getMessage()));

                      throw new BusinessException(
                              OwnerPreContractErrorCode.AI_SERVICE_ERROR,
                              parseResponse.getMessage() != null
                                      ? parseResponse.getMessage()
                                      : "계약서 특약 분석 중 오류가 발생했습니다.");
                  }
              } else {
                  throw new BusinessException(
                          OwnerPreContractErrorCode.AI_SERVICE_ERROR,
                          "AI 서버 계약서 특약 OCR 응답 오류: " + response.getStatusCode());
              }

          } catch (BusinessException e) {
              throw e;
          } catch (Exception e) {
              log.error("AI 서버 통신 실패: {}", LogSanitizerUtil.sanitize(e.getMessage()), e);
              throw new BusinessException(
                      OwnerPreContractErrorCode.AI_SERVICE_ERROR,
                      "AI 서버 통신 중 오류가 발생했습니다: " + e.getMessage());
          }
      }

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
