package org.scoula.domain.fraud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.fraud.dto.ai.AiParseResponse;
import org.scoula.domain.fraud.dto.common.MortgageeDto;
import org.scoula.domain.fraud.dto.common.ParsedRegistryDataDto;
import org.scoula.domain.fraud.dto.response.RegistryParseResponse;
import org.scoula.domain.fraud.exception.FraudErrorCode;
import org.scoula.domain.fraud.exception.FraudRiskException;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiDocumentAnalyzerService 테스트")
class AiDocumentAnalyzerServiceTest {

      @Mock private RestTemplate restTemplate;
      @Mock private ObjectMapper objectMapper;
      @Mock private MultipartFile multipartFile;

      @InjectMocks private AiDocumentAnalyzerService aiDocumentAnalyzerService;

      @BeforeEach
      void setUp() {
          ReflectionTestUtils.setField(
                  aiDocumentAnalyzerService, "aiServerUrl", "http://test-ai-server:8000");
      }

      @Nested
      @DisplayName("analyzeRegistryDocument 메서드 테스트")
      class AnalyzeRegistryDocumentTest {

          @Test
          @DisplayName("등기부등본 분석 성공")
          void analyzeRegistryDocument_Success() throws IOException {
              // Given
              AiParseResponse mockResponse = createMockAiParseResponse();
              when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenReturn(ResponseEntity.ok(mockResponse));

              // When
              AiParseResponse result =
                      aiDocumentAnalyzerService.analyzeRegistryDocument(multipartFile);

              // Then
              assertNotNull(result);
              assertTrue(result.isSuccess());
              assertEquals("파싱 성공", result.getMessage());
              verify(restTemplate)
                      .exchange(anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class));
          }

          @Test
          @DisplayName("등기부등본 분석 - AI 서버 응답 null")
          void analyzeRegistryDocument_NullResponse() throws IOException {
              // Given
              when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenReturn(ResponseEntity.ok(null));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiDocumentAnalyzerService.analyzeRegistryDocument(multipartFile));
              assertEquals(FraudErrorCode.AI_SERVICE_UNAVAILABLE, exception.getErrorCode());
          }

          @Test
          @DisplayName("등기부등본 분석 - AI 서버 파싱 실패")
          void analyzeRegistryDocument_ParseFailed() throws IOException {
              // Given
              AiParseResponse mockResponse = createMockFailedAiParseResponse();
              when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenReturn(ResponseEntity.ok(mockResponse));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiDocumentAnalyzerService.analyzeRegistryDocument(multipartFile));
              assertEquals(FraudErrorCode.DOCUMENT_PROCESSING_FAILED, exception.getErrorCode());
          }

          @Test
          @DisplayName("등기부등본 분석 - 클라이언트 오류 (4xx)")
          void analyzeRegistryDocument_ClientError() throws IOException {
              // Given
              when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiDocumentAnalyzerService.analyzeRegistryDocument(multipartFile));
              assertEquals(FraudErrorCode.INVALID_DOCUMENT_FORMAT, exception.getErrorCode());
          }

          @Test
          @DisplayName("등기부등본 분석 - 서버 오류 (5xx)")
          void analyzeRegistryDocument_ServerError() throws IOException {
              // Given
              when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenThrow(
                              new HttpServerErrorException(
                                      HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiDocumentAnalyzerService.analyzeRegistryDocument(multipartFile));
              assertEquals(FraudErrorCode.AI_SERVICE_UNAVAILABLE, exception.getErrorCode());
          }

          @Test
          @DisplayName("등기부등본 분석 - 네트워크 연결 오류")
          void analyzeRegistryDocument_NetworkError() throws IOException {
              // Given
              when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenThrow(new ResourceAccessException("Connection failed"));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiDocumentAnalyzerService.analyzeRegistryDocument(multipartFile));
              assertEquals(FraudErrorCode.AI_SERVICE_UNAVAILABLE, exception.getErrorCode());
          }

          @Test
          @DisplayName("등기부등본 분석 - 예상치 못한 오류")
          void analyzeRegistryDocument_UnexpectedError() throws IOException {
              // Given
              when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenThrow(new RuntimeException("Unexpected error"));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiDocumentAnalyzerService.analyzeRegistryDocument(multipartFile));
              assertEquals(FraudErrorCode.AI_SERVICE_UNAVAILABLE, exception.getErrorCode());
          }

          @Test
          @DisplayName("등기부등본 분석 - IOException 처리")
          void analyzeRegistryDocument_IOException() throws IOException {
              // Given
              when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              when(multipartFile.getBytes()).thenThrow(new IOException("File read error"));

              // When & Then
              IOException exception =
                      assertThrows(
                              IOException.class,
                              () -> aiDocumentAnalyzerService.analyzeRegistryDocument(multipartFile));
              assertEquals("File read error", exception.getMessage());
          }
      }

      @Nested
      @DisplayName("analyzeAndParseRegistryDocument 메서드 테스트")
      class AnalyzeAndParseRegistryDocumentTest {

          @Test
          @DisplayName("등기부등본 분석 및 파싱 성공")
          void analyzeAndParseRegistryDocument_Success() throws IOException {
              // Given
              AiParseResponse mockAiResponse = createMockAiParseResponseWithData();
              when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenReturn(ResponseEntity.ok(mockAiResponse));

              // When
              RegistryParseResponse result =
                      aiDocumentAnalyzerService.analyzeAndParseRegistryDocument(multipartFile);

              // Then
              assertNotNull(result);
              assertEquals("registry.pdf", result.getFilename());
              assertEquals("register", result.getDocumentType());
              assertNotNull(result.getParsedData());
              assertEquals("서울특별시 강남구 역삼동", result.getParsedData().getRegionAddress());
              assertEquals("홍길동", result.getParsedData().getOwnerName());
          }

          @Test
          @DisplayName("등기부등본 분석 및 파싱 - 데이터 null")
          void analyzeAndParseRegistryDocument_NullData() throws IOException {
              // Given
              AiParseResponse mockAiResponse = createMockAiParseResponseWithNullData();
              when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenReturn(ResponseEntity.ok(mockAiResponse));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () ->
                                      aiDocumentAnalyzerService.analyzeAndParseRegistryDocument(
                                              multipartFile));
              assertEquals(FraudErrorCode.DOCUMENT_PROCESSING_FAILED, exception.getErrorCode());
          }

          @Test
          @DisplayName("등기부등본 분석 및 파싱 - 파싱 데이터 null")
          void analyzeAndParseRegistryDocument_NullParsedData() throws IOException {
              // Given
              AiParseResponse mockAiResponse = createMockAiParseResponseWithNullParsedData();
              when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenReturn(ResponseEntity.ok(mockAiResponse));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () ->
                                      aiDocumentAnalyzerService.analyzeAndParseRegistryDocument(
                                              multipartFile));
              assertEquals(FraudErrorCode.DOCUMENT_PROCESSING_FAILED, exception.getErrorCode());
          }

          @Test
          @DisplayName("등기부등본 분석 및 파싱 - 잘못된 파싱 데이터 타입")
          void analyzeAndParseRegistryDocument_InvalidParsedDataType() throws IOException {
              // Given
              AiParseResponse mockAiResponse = createMockAiParseResponseWithInvalidData();
              when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenReturn(ResponseEntity.ok(mockAiResponse));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () ->
                                      aiDocumentAnalyzerService.analyzeAndParseRegistryDocument(
                                              multipartFile));
              assertEquals(FraudErrorCode.DOCUMENT_PROCESSING_FAILED, exception.getErrorCode());
          }

          @Test
          @DisplayName("등기부등본 분석 및 파싱 - 파일명 null 처리")
          void analyzeAndParseRegistryDocument_NullFilename() throws IOException {
              // Given
              AiParseResponse mockAiResponse = createMockAiParseResponseWithNullFilename();
              when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenReturn(ResponseEntity.ok(mockAiResponse));

              // When
              RegistryParseResponse result =
                      aiDocumentAnalyzerService.analyzeAndParseRegistryDocument(multipartFile);

              // Then
              assertNotNull(result);
              assertEquals("registry.pdf", result.getFilename()); // multipartFile의 파일명 사용
          }

          @Test
          @DisplayName("등기부등본 분석 및 파싱 - 문서 타입 null 처리")
          void analyzeAndParseRegistryDocument_NullDocumentType() throws IOException {
              // Given
              AiParseResponse mockAiResponse = createMockAiParseResponseWithNullDocumentType();
              when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenReturn(ResponseEntity.ok(mockAiResponse));

              // When
              RegistryParseResponse result =
                      aiDocumentAnalyzerService.analyzeAndParseRegistryDocument(multipartFile);

              // Then
              assertNotNull(result);
              assertEquals("register", result.getDocumentType()); // 기본값 사용
          }

          @Test
          @DisplayName("등기부등본 분석 및 파싱 - 근저당권 리스트 포함")
          void analyzeAndParseRegistryDocument_WithMortgageeList() throws IOException {
              // Given
              AiParseResponse mockAiResponse = createMockAiParseResponseWithMortgagees();
              when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenReturn(ResponseEntity.ok(mockAiResponse));

              // When
              RegistryParseResponse result =
                      aiDocumentAnalyzerService.analyzeAndParseRegistryDocument(multipartFile);

              // Then
              assertNotNull(result);
              assertNotNull(result.getParsedData().getMortgageeList());
              assertEquals(2, result.getParsedData().getMortgageeList().size());

              MortgageeDto firstMortgagee = result.getParsedData().getMortgageeList().get(0);
              assertEquals(1, firstMortgagee.getPriorityNumber());
              assertEquals(300000000L, firstMortgagee.getMaxClaimAmount());
              assertEquals("김채무", firstMortgagee.getDebtor());
              assertEquals("국민은행", firstMortgagee.getMortgagee());
          }

          @Test
          @DisplayName("등기부등본 분석 및 파싱 - Boolean 값들 처리")
          void analyzeAndParseRegistryDocument_BooleanValues() throws IOException {
              // Given
              AiParseResponse mockAiResponse = createMockAiParseResponseWithBooleans();
              when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenReturn(ResponseEntity.ok(mockAiResponse));

              // When
              RegistryParseResponse result =
                      aiDocumentAnalyzerService.analyzeAndParseRegistryDocument(multipartFile);

              // Then
              assertNotNull(result);
              ParsedRegistryDataDto parsedData = result.getParsedData();
              assertTrue(parsedData.getHasSeizure());
              assertFalse(parsedData.getHasAuction());
              assertTrue(parsedData.getHasLitigation());
              assertFalse(parsedData.getHasAttachment());
          }
      }

      @Nested
      @DisplayName("analyzeBuildingDocument 메서드 테스트")
      class AnalyzeBuildingDocumentTest {

          @Test
          @DisplayName("건축물대장 분석 성공")
          void analyzeBuildingDocument_Success() throws IOException {
              // Given
              AiParseResponse mockResponse = createMockAiParseResponse();
              when(multipartFile.getOriginalFilename()).thenReturn("building.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenReturn(ResponseEntity.ok(mockResponse));

              // When
              AiParseResponse result =
                      aiDocumentAnalyzerService.analyzeBuildingDocument(multipartFile);

              // Then
              assertNotNull(result);
              assertTrue(result.isSuccess());
              assertEquals("파싱 성공", result.getMessage());
              verify(restTemplate)
                      .exchange(anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class));
          }

          @Test
          @DisplayName("건축물대장 분석 - AI 서버 응답 null")
          void analyzeBuildingDocument_NullResponse() throws IOException {
              // Given
              when(multipartFile.getOriginalFilename()).thenReturn("building.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenReturn(ResponseEntity.ok(null));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiDocumentAnalyzerService.analyzeBuildingDocument(multipartFile));
              assertEquals(FraudErrorCode.AI_SERVICE_UNAVAILABLE, exception.getErrorCode());
          }

          @Test
          @DisplayName("건축물대장 분석 - AI 서버 파싱 실패")
          void analyzeBuildingDocument_ParseFailed() throws IOException {
              // Given
              AiParseResponse mockResponse = createMockFailedAiParseResponse();
              when(multipartFile.getOriginalFilename()).thenReturn("building.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenReturn(ResponseEntity.ok(mockResponse));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiDocumentAnalyzerService.analyzeBuildingDocument(multipartFile));
              assertEquals(FraudErrorCode.DOCUMENT_PROCESSING_FAILED, exception.getErrorCode());
          }

          @Test
          @DisplayName("건축물대장 분석 - 클라이언트 오류 (4xx)")
          void analyzeBuildingDocument_ClientError() throws IOException {
              // Given
              when(multipartFile.getOriginalFilename()).thenReturn("building.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiDocumentAnalyzerService.analyzeBuildingDocument(multipartFile));
              assertEquals(FraudErrorCode.INVALID_DOCUMENT_FORMAT, exception.getErrorCode());
          }

          @Test
          @DisplayName("건축물대장 분석 - 서버 오류 (5xx)")
          void analyzeBuildingDocument_ServerError() throws IOException {
              // Given
              when(multipartFile.getOriginalFilename()).thenReturn("building.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenThrow(
                              new HttpServerErrorException(
                                      HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiDocumentAnalyzerService.analyzeBuildingDocument(multipartFile));
              assertEquals(FraudErrorCode.AI_SERVICE_UNAVAILABLE, exception.getErrorCode());
          }

          @Test
          @DisplayName("건축물대장 분석 - 네트워크 연결 오류")
          void analyzeBuildingDocument_NetworkError() throws IOException {
              // Given
              when(multipartFile.getOriginalFilename()).thenReturn("building.pdf");
              when(multipartFile.getBytes()).thenReturn("test content".getBytes());
              when(restTemplate.exchange(
                              anyString(), eq(HttpMethod.POST), any(), eq(AiParseResponse.class)))
                      .thenThrow(new ResourceAccessException("Connection failed"));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiDocumentAnalyzerService.analyzeBuildingDocument(multipartFile));
              assertEquals(FraudErrorCode.AI_SERVICE_UNAVAILABLE, exception.getErrorCode());
          }
      }

      // Helper methods for creating mock objects
      private AiParseResponse createMockAiParseResponse() {
          return AiParseResponse.builder().success(true).message("파싱 성공").data(null).build();
      }

      private AiParseResponse createMockFailedAiParseResponse() {
          return AiParseResponse.builder().success(false).message("파싱 실패").data(null).build();
      }

      private AiParseResponse createMockAiParseResponseWithData() {
          Map<String, Object> parsedData = new HashMap<>();
          parsedData.put("regionAddress", "서울특별시 강남구 역삼동");
          parsedData.put("roadAddress", "서울특별시 강남구 역삼로 123");
          parsedData.put("ownerName", "홍길동");
          parsedData.put("ownerBirthDate", "1980-01-01");
          parsedData.put("debtor", "김채무");
          parsedData.put("mortgageeList", new ArrayList<>());
          parsedData.put("hasSeizure", false);
          parsedData.put("hasAuction", false);
          parsedData.put("hasLitigation", false);
          parsedData.put("hasAttachment", false);

          AiParseResponse.AiParseData data =
                  AiParseResponse.AiParseData.builder()
                          .filename("registry.pdf")
                          .documentType("register")
                          .parsedData(parsedData)
                          .build();

          return AiParseResponse.builder().success(true).message("파싱 성공").data(data).build();
      }

      private AiParseResponse createMockAiParseResponseWithNullData() {
          return AiParseResponse.builder().success(true).message("파싱 성공").data(null).build();
      }

      private AiParseResponse createMockAiParseResponseWithNullParsedData() {
          AiParseResponse.AiParseData data =
                  AiParseResponse.AiParseData.builder()
                          .filename("registry.pdf")
                          .documentType("register")
                          .parsedData(null)
                          .build();

          return AiParseResponse.builder().success(true).message("파싱 성공").data(data).build();
      }

      private AiParseResponse createMockAiParseResponseWithInvalidData() {
          AiParseResponse.AiParseData data =
                  AiParseResponse.AiParseData.builder()
                          .filename("registry.pdf")
                          .documentType("register")
                          .parsedData("invalid_data_type") // Map이 아닌 String 타입
                          .build();

          return AiParseResponse.builder().success(true).message("파싱 성공").data(data).build();
      }

      private AiParseResponse createMockAiParseResponseWithNullFilename() {
          Map<String, Object> parsedData = new HashMap<>();
          parsedData.put("regionAddress", "서울특별시 강남구 역삼동");
          parsedData.put("ownerName", "홍길동");

          AiParseResponse.AiParseData data =
                  AiParseResponse.AiParseData.builder()
                          .filename(null) // filename null
                          .documentType("register")
                          .parsedData(parsedData)
                          .build();

          return AiParseResponse.builder().success(true).message("파싱 성공").data(data).build();
      }

      private AiParseResponse createMockAiParseResponseWithNullDocumentType() {
          Map<String, Object> parsedData = new HashMap<>();
          parsedData.put("regionAddress", "서울특별시 강남구 역삼동");
          parsedData.put("ownerName", "홍길동");

          AiParseResponse.AiParseData data =
                  AiParseResponse.AiParseData.builder()
                          .filename("registry.pdf")
                          .documentType(null) // documentType null
                          .parsedData(parsedData)
                          .build();

          return AiParseResponse.builder().success(true).message("파싱 성공").data(data).build();
      }

      private AiParseResponse createMockAiParseResponseWithMortgagees() {
          List<Map<String, Object>> mortgageeList = new ArrayList<>();

          Map<String, Object> mortgagee1 = new HashMap<>();
          mortgagee1.put("priorityNumber", 1);
          mortgagee1.put("maxClaimAmount", 300000000L);
          mortgagee1.put("debtor", "김채무");
          mortgagee1.put("mortgagee", "국민은행");
          mortgageeList.add(mortgagee1);

          Map<String, Object> mortgagee2 = new HashMap<>();
          mortgagee2.put("priorityNumber", 2);
          mortgagee2.put("maxClaimAmount", 200000000L);
          mortgagee2.put("debtor", "이채무");
          mortgagee2.put("mortgagee", "신한은행");
          mortgageeList.add(mortgagee2);

          Map<String, Object> parsedData = new HashMap<>();
          parsedData.put("regionAddress", "서울특별시 강남구 역삼동");
          parsedData.put("roadAddress", "서울특별시 강남구 역삼로 123");
          parsedData.put("ownerName", "홍길동");
          parsedData.put("ownerBirthDate", "1980-01-01");
          parsedData.put("debtor", "김채무");
          parsedData.put("mortgageeList", mortgageeList);
          parsedData.put("hasSeizure", false);
          parsedData.put("hasAuction", false);
          parsedData.put("hasLitigation", false);
          parsedData.put("hasAttachment", false);

          AiParseResponse.AiParseData data =
                  AiParseResponse.AiParseData.builder()
                          .filename("registry.pdf")
                          .documentType("register")
                          .parsedData(parsedData)
                          .build();

          return AiParseResponse.builder().success(true).message("파싱 성공").data(data).build();
      }

      private AiParseResponse createMockAiParseResponseWithBooleans() {
          Map<String, Object> parsedData = new HashMap<>();
          parsedData.put("regionAddress", "서울특별시 강남구 역삼동");
          parsedData.put("ownerName", "홍길동");
          parsedData.put("mortgageeList", new ArrayList<>());
          parsedData.put("hasSeizure", true); // Boolean true
          parsedData.put("hasAuction", "false"); // String false
          parsedData.put("hasLitigation", "true"); // String true
          parsedData.put("hasAttachment", null); // null (should default to false)

          AiParseResponse.AiParseData data =
                  AiParseResponse.AiParseData.builder()
                          .filename("registry.pdf")
                          .documentType("register")
                          .parsedData(parsedData)
                          .build();

          return AiParseResponse.builder().success(true).message("파싱 성공").data(data).build();
      }
}
