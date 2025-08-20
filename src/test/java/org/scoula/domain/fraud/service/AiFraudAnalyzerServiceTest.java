package org.scoula.domain.fraud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.fraud.dto.ai.FraudRiskCheckDto;
import org.scoula.domain.fraud.dto.common.BuildingDocumentDto;
import org.scoula.domain.fraud.dto.common.MortgageeDto;
import org.scoula.domain.fraud.dto.common.RegistryDocumentDto;
import org.scoula.domain.fraud.dto.request.ExternalRiskAnalysisRequest;
import org.scoula.domain.fraud.dto.request.RiskAnalysisRequest;
import org.scoula.domain.fraud.enums.RiskType;
import org.scoula.domain.fraud.exception.FraudErrorCode;
import org.scoula.domain.fraud.exception.FraudRiskException;
import org.scoula.domain.home.enums.LeaseType;
import org.scoula.domain.home.enums.ResidenceType;
import org.scoula.domain.home.mapper.HomeMapper;
import org.scoula.domain.home.vo.HomeVO;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiFraudAnalyzerService 테스트")
class AiFraudAnalyzerServiceTest {

      @Mock private RestTemplate restTemplate;
      @Mock private HomeMapper homeMapper;
      @Mock private MultipartFile multipartFile;

      @InjectMocks private AiFraudAnalyzerService aiFraudAnalyzerService;

      @BeforeEach
      void setUp() {
          ReflectionTestUtils.setField(
                  aiFraudAnalyzerService, "aiServerUrl", "http://test-ai-server:8000");
      }

      @Nested
      @DisplayName("analyzeFraudRisk 메서드 테스트 (RiskAnalysisRequest)")
      class AnalyzeFraudRiskTest {

          @Test
          @DisplayName("사기 위험도 분석 성공")
          void analyzeFraudRisk_Success() {
              // Given
              Long userId = 1L;
              RiskAnalysisRequest request = createMockRiskAnalysisRequest();
              HomeVO homeVO = createMockHomeVO();
              Map<String, Object> mockResponseBody = createMockSuccessResponse();

              when(homeMapper.findHomeById(1L)).thenReturn(homeVO);
              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.ok(mockResponseBody));

              // When
              FraudRiskCheckDto.Response result =
                      aiFraudAnalyzerService.analyzeFraudRisk(userId, request);

              // Then
              assertNotNull(result);
              assertEquals("SUCCESS", result.getStatus());
              assertEquals("HIGH", result.getRiskLevel());
              assertEquals(85.5, result.getRiskScore());
              verify(homeMapper).findHomeById(1L);
              verify(restTemplate).postForEntity(anyString(), any(), eq(Map.class));
          }

          @Test
          @DisplayName("홈 정보 없을 때 예외 발생")
          void analyzeFraudRisk_InvalidHomeId() {
              // Given
              Long userId = 1L;
              RiskAnalysisRequest request = createMockRiskAnalysisRequest();

              when(homeMapper.findHomeById(1L)).thenThrow(new RuntimeException("Home not found"));

              // When & Then
              BusinessException exception =
                      assertThrows(
                              BusinessException.class,
                              () -> aiFraudAnalyzerService.analyzeFraudRisk(userId, request));
              assertEquals(FraudErrorCode.INVALID_HOME_ID, exception.getErrorCode());
          }

          @Test
          @DisplayName("AI 서버 응답 실패")
          void analyzeFraudRisk_AiServerError() {
              // Given
              Long userId = 1L;
              RiskAnalysisRequest request = createMockRiskAnalysisRequest();
              HomeVO homeVO = createMockHomeVO();

              when(homeMapper.findHomeById(1L)).thenReturn(homeVO);
              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiFraudAnalyzerService.analyzeFraudRisk(userId, request));
              assertEquals(FraudErrorCode.AI_SERVICE_UNAVAILABLE, exception.getErrorCode());
          }

          @Test
          @DisplayName("AI 서버 분석 실패 응답")
          void analyzeFraudRisk_AiAnalysisFailed() {
              // Given
              Long userId = 1L;
              RiskAnalysisRequest request = createMockRiskAnalysisRequest();
              HomeVO homeVO = createMockHomeVO();
              Map<String, Object> failureResponse = createMockFailureResponse();

              when(homeMapper.findHomeById(1L)).thenReturn(homeVO);
              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.ok(failureResponse));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiFraudAnalyzerService.analyzeFraudRisk(userId, request));
              assertEquals(FraudErrorCode.FRAUD_ANALYSIS_FAILED, exception.getErrorCode());
          }

          @Test
          @DisplayName("네트워크 오류 발생")
          void analyzeFraudRisk_NetworkError() {
              // Given
              Long userId = 1L;
              RiskAnalysisRequest request = createMockRiskAnalysisRequest();
              HomeVO homeVO = createMockHomeVO();

              when(homeMapper.findHomeById(1L)).thenReturn(homeVO);
              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenThrow(new RuntimeException("Network error"));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiFraudAnalyzerService.analyzeFraudRisk(userId, request));
              assertEquals(FraudErrorCode.AI_SERVICE_UNAVAILABLE, exception.getErrorCode());
          }
      }

      @Nested
      @DisplayName("analyzeFraudRisk 메서드 테스트 (ExternalRiskAnalysisRequest)")
      class AnalyzeFraudRiskExternalTest {

          @Test
          @DisplayName("외부 요청으로 사기 위험도 분석 성공")
          void analyzeFraudRisk_ExternalRequest_Success() {
              // Given
              Long userId = 1L;
              ExternalRiskAnalysisRequest request = createMockExternalRiskAnalysisRequest();
              Map<String, Object> mockResponseBody = createMockSuccessResponse();

              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.ok(mockResponseBody));

              // When
              FraudRiskCheckDto.Response result =
                      aiFraudAnalyzerService.analyzeFraudRisk(userId, request);

              // Then
              assertNotNull(result);
              assertEquals("SUCCESS", result.getStatus());
              assertEquals("HIGH", result.getRiskLevel());
              assertEquals(85.5, result.getRiskScore());
              verify(restTemplate).postForEntity(anyString(), any(), eq(Map.class));
          }

          @Test
          @DisplayName("외부 요청 - 등기부등본만 있는 경우")
          void analyzeFraudRisk_ExternalRequest_RegistryOnly() {
              // Given
              Long userId = 1L;
              ExternalRiskAnalysisRequest request =
                      ExternalRiskAnalysisRequest.builder()
                              .address("서울특별시 강남구 역삼동")
                              .propertyPrice(500000000L)
                              .registryDocument(createMockRegistryDocument())
                              .build();

              Map<String, Object> mockResponseBody = createMockSuccessResponse();
              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.ok(mockResponseBody));

              // When
              FraudRiskCheckDto.Response result =
                      aiFraudAnalyzerService.analyzeFraudRisk(userId, request);

              // Then
              assertNotNull(result);
              verify(restTemplate).postForEntity(anyString(), any(), eq(Map.class));
          }

          @Test
          @DisplayName("외부 요청 - null 필드들 처리")
          void analyzeFraudRisk_ExternalRequest_NullFields() {
              // Given
              Long userId = 1L;
              ExternalRiskAnalysisRequest request =
                      ExternalRiskAnalysisRequest.builder()
                              .address(null)
                              .propertyPrice(null)
                              .monthlyRent(null)
                              .leaseType(null)
                              .registeredUserName(null)
                              .residenceType(null)
                              .build();

              Map<String, Object> mockResponseBody = createMockSuccessResponse();
              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.ok(mockResponseBody));

              // When
              FraudRiskCheckDto.Response result =
                      aiFraudAnalyzerService.analyzeFraudRisk(userId, request);

              // Then
              assertNotNull(result);
              verify(restTemplate).postForEntity(anyString(), any(), eq(Map.class));
          }
      }

      @Nested
      @DisplayName("determineRiskType 메서드 테스트")
      class DetermineRiskTypeTest {

          @Test
          @DisplayName("LOW 레벨 - SAFE 반환")
          void determineRiskType_LowLevel_ReturnsSafe() {
              // Given
              FraudRiskCheckDto.Response response =
                      FraudRiskCheckDto.Response.builder().riskLevel("LOW").riskScore(20.0).build();

              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(response);

              // Then
              assertEquals(RiskType.SAFE, result);
          }

          @Test
          @DisplayName("SAFE 레벨 - SAFE 반환")
          void determineRiskType_SafeLevel_ReturnsSafe() {
              // Given
              FraudRiskCheckDto.Response response =
                      FraudRiskCheckDto.Response.builder().riskLevel("SAFE").riskScore(15.0).build();

              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(response);

              // Then
              assertEquals(RiskType.SAFE, result);
          }

          @Test
          @DisplayName("MEDIUM 레벨 - WARN 반환")
          void determineRiskType_MediumLevel_ReturnsWarn() {
              // Given
              FraudRiskCheckDto.Response response =
                      FraudRiskCheckDto.Response.builder()
                              .riskLevel("MEDIUM")
                              .riskScore(50.0)
                              .build();

              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(response);

              // Then
              assertEquals(RiskType.WARN, result);
          }

          @Test
          @DisplayName("WARN 레벨 - WARN 반환")
          void determineRiskType_WarnLevel_ReturnsWarn() {
              // Given
              FraudRiskCheckDto.Response response =
                      FraudRiskCheckDto.Response.builder().riskLevel("WARN").riskScore(60.0).build();

              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(response);

              // Then
              assertEquals(RiskType.WARN, result);
          }

          @Test
          @DisplayName("WARNING 레벨 - WARN 반환")
          void determineRiskType_WarningLevel_ReturnsWarn() {
              // Given
              FraudRiskCheckDto.Response response =
                      FraudRiskCheckDto.Response.builder()
                              .riskLevel("WARNING")
                              .riskScore(55.0)
                              .build();

              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(response);

              // Then
              assertEquals(RiskType.WARN, result);
          }

          @Test
          @DisplayName("HIGH 레벨 - DANGER 반환")
          void determineRiskType_HighLevel_ReturnsDanger() {
              // Given
              FraudRiskCheckDto.Response response =
                      FraudRiskCheckDto.Response.builder().riskLevel("HIGH").riskScore(85.0).build();

              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(response);

              // Then
              assertEquals(RiskType.DANGER, result);
          }

          @Test
          @DisplayName("DANGER 레벨 - DANGER 반환")
          void determineRiskType_DangerLevel_ReturnsDanger() {
              // Given
              FraudRiskCheckDto.Response response =
                      FraudRiskCheckDto.Response.builder()
                              .riskLevel("DANGER")
                              .riskScore(90.0)
                              .build();

              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(response);

              // Then
              assertEquals(RiskType.DANGER, result);
          }

          @Test
          @DisplayName("CRITICAL 레벨 - DANGER 반환")
          void determineRiskType_CriticalLevel_ReturnsDanger() {
              // Given
              FraudRiskCheckDto.Response response =
                      FraudRiskCheckDto.Response.builder()
                              .riskLevel("CRITICAL")
                              .riskScore(95.0)
                              .build();

              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(response);

              // Then
              assertEquals(RiskType.DANGER, result);
          }

          @Test
          @DisplayName("알 수 없는 레벨 - 스코어로 판단 (낮은 스코어)")
          void determineRiskType_UnknownLevel_LowScore() {
              // Given
              FraudRiskCheckDto.Response response =
                      FraudRiskCheckDto.Response.builder()
                              .riskLevel("UNKNOWN")
                              .riskScore(20.0)
                              .build();

              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(response);

              // Then
              assertEquals(RiskType.SAFE, result);
          }

          @Test
          @DisplayName("알 수 없는 레벨 - 스코어로 판단 (중간 스코어)")
          void determineRiskType_UnknownLevel_MediumScore() {
              // Given
              FraudRiskCheckDto.Response response =
                      FraudRiskCheckDto.Response.builder()
                              .riskLevel("UNKNOWN")
                              .riskScore(50.0)
                              .build();

              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(response);

              // Then
              assertEquals(RiskType.WARN, result);
          }

          @Test
          @DisplayName("알 수 없는 레벨 - 스코어로 판단 (높은 스코어)")
          void determineRiskType_UnknownLevel_HighScore() {
              // Given
              FraudRiskCheckDto.Response response =
                      FraudRiskCheckDto.Response.builder()
                              .riskLevel("UNKNOWN")
                              .riskScore(80.0)
                              .build();

              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(response);

              // Then
              assertEquals(RiskType.DANGER, result);
          }

          @Test
          @DisplayName("null 응답 - WARN 반환")
          void determineRiskType_NullResponse_ReturnsWarn() {
              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(null);

              // Then
              assertEquals(RiskType.WARN, result);
          }

          @Test
          @DisplayName("null 위험 레벨 - WARN 반환")
          void determineRiskType_NullRiskLevel_ReturnsWarn() {
              // Given
              FraudRiskCheckDto.Response response =
                      FraudRiskCheckDto.Response.builder().riskLevel(null).riskScore(50.0).build();

              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(response);

              // Then
              assertEquals(RiskType.WARN, result);
          }

          @Test
          @DisplayName("스코어도 null인 경우 - WARN 반환")
          void determineRiskType_NullScore_ReturnsWarn() {
              // Given
              FraudRiskCheckDto.Response response =
                      FraudRiskCheckDto.Response.builder()
                              .riskLevel("UNKNOWN")
                              .riskScore(null)
                              .build();

              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(response);

              // Then
              assertEquals(RiskType.WARN, result);
          }

          @Test
          @DisplayName("경계값 테스트 - 스코어 29 (SAFE)")
          void determineRiskType_BoundaryScore_29() {
              // Given
              FraudRiskCheckDto.Response response =
                      FraudRiskCheckDto.Response.builder()
                              .riskLevel("UNKNOWN")
                              .riskScore(29.0)
                              .build();

              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(response);

              // Then
              assertEquals(RiskType.SAFE, result);
          }

          @Test
          @DisplayName("경계값 테스트 - 스코어 30 (WARN)")
          void determineRiskType_BoundaryScore_30() {
              // Given
              FraudRiskCheckDto.Response response =
                      FraudRiskCheckDto.Response.builder()
                              .riskLevel("UNKNOWN")
                              .riskScore(30.0)
                              .build();

              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(response);

              // Then
              assertEquals(RiskType.WARN, result);
          }

          @Test
          @DisplayName("경계값 테스트 - 스코어 69 (WARN)")
          void determineRiskType_BoundaryScore_69() {
              // Given
              FraudRiskCheckDto.Response response =
                      FraudRiskCheckDto.Response.builder()
                              .riskLevel("UNKNOWN")
                              .riskScore(69.0)
                              .build();

              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(response);

              // Then
              assertEquals(RiskType.WARN, result);
          }

          @Test
          @DisplayName("경계값 테스트 - 스코어 70 (DANGER)")
          void determineRiskType_BoundaryScore_70() {
              // Given
              FraudRiskCheckDto.Response response =
                      FraudRiskCheckDto.Response.builder()
                              .riskLevel("UNKNOWN")
                              .riskScore(70.0)
                              .build();

              // When
              RiskType result = aiFraudAnalyzerService.determineRiskType(response);

              // Then
              assertEquals(RiskType.DANGER, result);
          }
      }

      @Nested
      @DisplayName("parseRegistryDocument 메서드 테스트")
      class ParseRegistryDocumentTest {

          @Test
          @DisplayName("등기부등본 OCR 성공")
          void parseRegistryDocument_Success() throws IOException {
              // Given
              lenient().when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              lenient().when(multipartFile.getSize()).thenReturn(1024L);
              lenient()
                      .when(multipartFile.getInputStream())
                      .thenReturn(new ByteArrayInputStream("test".getBytes()));

              Map<String, Object> mockResponseBody = createMockRegistryOcrResponse();
              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.ok(mockResponseBody));

              // When
              RegistryDocumentDto result =
                      aiFraudAnalyzerService.parseRegistryDocument(multipartFile);

              // Then
              assertNotNull(result);
              assertEquals("서울특별시 강남구 역삼동", result.getRegionAddress());
              assertEquals("서울특별시 강남구 역삼로 123", result.getRoadAddress());
              assertEquals("홍길동", result.getOwnerName());
              verify(restTemplate).postForEntity(anyString(), any(), eq(Map.class));
          }

          @Test
          @DisplayName("등기부등본 OCR - AI 서버 오류")
          void parseRegistryDocument_AiServerError() throws IOException {
              // Given
              lenient().when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              lenient().when(multipartFile.getSize()).thenReturn(1024L);
              lenient()
                      .when(multipartFile.getInputStream())
                      .thenReturn(new ByteArrayInputStream("test".getBytes()));

              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiFraudAnalyzerService.parseRegistryDocument(multipartFile));
              assertEquals(FraudErrorCode.AI_SERVICE_UNAVAILABLE, exception.getErrorCode());
          }

          @Test
          @DisplayName("등기부등본 OCR - 잘못된 문서 형식")
          void parseRegistryDocument_InvalidDocumentType() throws IOException {
              // Given
              lenient().when(multipartFile.getOriginalFilename()).thenReturn("invalid.pdf");
              lenient().when(multipartFile.getSize()).thenReturn(1024L);
              lenient()
                      .when(multipartFile.getInputStream())
                      .thenReturn(new ByteArrayInputStream("test".getBytes()));

              Map<String, Object> errorResponse = createMockRegistryErrorResponse();
              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.ok(errorResponse));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiFraudAnalyzerService.parseRegistryDocument(multipartFile));
              assertEquals(FraudErrorCode.INVALID_DOCUMENT_FORMAT, exception.getErrorCode());
          }

          @Test
          @DisplayName("등기부등본 OCR - 네트워크 오류")
          void parseRegistryDocument_NetworkError() throws IOException {
              // Given
              lenient().when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              lenient().when(multipartFile.getSize()).thenReturn(1024L);
              lenient()
                      .when(multipartFile.getInputStream())
                      .thenReturn(new ByteArrayInputStream("test".getBytes()));

              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenThrow(new RuntimeException("Network error"));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiFraudAnalyzerService.parseRegistryDocument(multipartFile));
              assertEquals(FraudErrorCode.OCR_PROCESSING_FAILED, exception.getErrorCode());
          }

          @Test
          @DisplayName("등기부등본 OCR - 파싱 데이터 없음")
          void parseRegistryDocument_NoParsedData() throws IOException {
              // Given
              lenient().when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              lenient().when(multipartFile.getSize()).thenReturn(1024L);
              lenient()
                      .when(multipartFile.getInputStream())
                      .thenReturn(new ByteArrayInputStream("test".getBytes()));

              Map<String, Object> emptyResponse = Map.of("success", true, "data", Map.of());
              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.ok(emptyResponse));

              // When
              RegistryDocumentDto result =
                      aiFraudAnalyzerService.parseRegistryDocument(multipartFile);

              // Then
              assertNotNull(result);
          }

          @Test
          @DisplayName("등기부등본 OCR - 건축물대장 관련 오류")
          void parseRegistryDocument_BuildingDocumentError() throws IOException {
              // Given
              lenient().when(multipartFile.getOriginalFilename()).thenReturn("building.pdf");
              lenient().when(multipartFile.getSize()).thenReturn(1024L);
              lenient()
                      .when(multipartFile.getInputStream())
                      .thenReturn(new ByteArrayInputStream("test".getBytes()));

              Map<String, Object> errorResponse =
                      Map.of(
                              "success",
                              false,
                              "message",
                              "건축물대장 파일이 아닙니다",
                              "error",
                              Map.of("code", "INVALID_DOCUMENT_TYPE"));

              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.ok(errorResponse));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiFraudAnalyzerService.parseRegistryDocument(multipartFile));
              assertEquals(FraudErrorCode.INVALID_DOCUMENT_FORMAT, exception.getErrorCode());
          }

          @Test
          @DisplayName("등기부등본 OCR - 알 수 없는 문서 타입")
          void parseRegistryDocument_UnsupportedDocumentType() throws IOException {
              // Given
              lenient().when(multipartFile.getOriginalFilename()).thenReturn("unknown.pdf");
              lenient().when(multipartFile.getSize()).thenReturn(1024L);
              lenient()
                      .when(multipartFile.getInputStream())
                      .thenReturn(new ByteArrayInputStream("test".getBytes()));

              Map<String, Object> errorResponse =
                      Map.of(
                              "success",
                              false,
                              "message",
                              "알 수 없는 문서 형식입니다",
                              "error",
                              Map.of("code", "INVALID_DOCUMENT_TYPE"));

              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.ok(errorResponse));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiFraudAnalyzerService.parseRegistryDocument(multipartFile));
              assertEquals(FraudErrorCode.UNSUPPORTED_DOCUMENT_TYPE, exception.getErrorCode());
          }

          @Test
          @DisplayName("등기부등본 OCR - 서버 내부 오류")
          void parseRegistryDocument_ServerInternalError() throws IOException {
              // Given
              lenient().when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              lenient().when(multipartFile.getSize()).thenReturn(1024L);
              lenient()
                      .when(multipartFile.getInputStream())
                      .thenReturn(new ByteArrayInputStream("test".getBytes()));

              Map<String, Object> errorResponse =
                      Map.of(
                              "success",
                              false,
                              "message",
                              "서버 내부 오류가 발생했습니다",
                              "error",
                              Map.of("code", "INTERNAL_ERROR"));

              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.ok(errorResponse));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiFraudAnalyzerService.parseRegistryDocument(multipartFile));
              assertEquals(FraudErrorCode.OCR_PROCESSING_FAILED, exception.getErrorCode());
          }
      }

      @Nested
      @DisplayName("parseBuildingDocument 메서드 테스트")
      class ParseBuildingDocumentTest {

          @Test
          @DisplayName("건축물대장 OCR 성공")
          void parseBuildingDocument_Success() throws IOException {
              // Given
              lenient().when(multipartFile.getOriginalFilename()).thenReturn("building.pdf");
              lenient().when(multipartFile.getSize()).thenReturn(1024L);
              lenient()
                      .when(multipartFile.getInputStream())
                      .thenReturn(new ByteArrayInputStream("test".getBytes()));

              Map<String, Object> mockResponseBody = createMockBuildingOcrResponse();
              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.ok(mockResponseBody));

              // When
              BuildingDocumentDto result =
                      aiFraudAnalyzerService.parseBuildingDocument(multipartFile);

              // Then
              assertNotNull(result);
              assertEquals("서울특별시 강남구 역삼동 123", result.getSiteLocation());
              assertEquals("서울특별시 강남구 역삼로 123", result.getRoadAddress());
              assertEquals(85.5, result.getTotalFloorArea());
              assertEquals("아파트", result.getPurpose());
              verify(restTemplate).postForEntity(anyString(), any(), eq(Map.class));
          }

          @Test
          @DisplayName("건축물대장 OCR - AI 서버 오류")
          void parseBuildingDocument_AiServerError() throws IOException {
              // Given
              lenient().when(multipartFile.getOriginalFilename()).thenReturn("building.pdf");
              lenient().when(multipartFile.getSize()).thenReturn(1024L);
              lenient()
                      .when(multipartFile.getInputStream())
                      .thenReturn(new ByteArrayInputStream("test".getBytes()));

              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiFraudAnalyzerService.parseBuildingDocument(multipartFile));
              assertEquals(FraudErrorCode.AI_SERVICE_UNAVAILABLE, exception.getErrorCode());
          }

          @Test
          @DisplayName("건축물대장 OCR - 잘못된 문서 형식")
          void parseBuildingDocument_InvalidDocumentType() throws IOException {
              // Given
              lenient().when(multipartFile.getOriginalFilename()).thenReturn("invalid.pdf");
              lenient().when(multipartFile.getSize()).thenReturn(1024L);
              lenient()
                      .when(multipartFile.getInputStream())
                      .thenReturn(new ByteArrayInputStream("test".getBytes()));

              Map<String, Object> errorResponse = createMockBuildingErrorResponse();
              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.ok(errorResponse));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiFraudAnalyzerService.parseBuildingDocument(multipartFile));
              assertEquals(FraudErrorCode.INVALID_DOCUMENT_FORMAT, exception.getErrorCode());
          }

          @Test
          @DisplayName("건축물대장 OCR - 네트워크 오류")
          void parseBuildingDocument_NetworkError() throws IOException {
              // Given
              lenient().when(multipartFile.getOriginalFilename()).thenReturn("building.pdf");
              lenient().when(multipartFile.getSize()).thenReturn(1024L);
              lenient()
                      .when(multipartFile.getInputStream())
                      .thenReturn(new ByteArrayInputStream("test".getBytes()));

              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenThrow(new RuntimeException("Network error"));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiFraudAnalyzerService.parseBuildingDocument(multipartFile));
              assertEquals(FraudErrorCode.OCR_PROCESSING_FAILED, exception.getErrorCode());
          }

          @Test
          @DisplayName("건축물대장 OCR - 파싱 데이터 없음")
          void parseBuildingDocument_NoParsedData() throws IOException {
              // Given
              lenient().when(multipartFile.getOriginalFilename()).thenReturn("building.pdf");
              lenient().when(multipartFile.getSize()).thenReturn(1024L);
              lenient()
                      .when(multipartFile.getInputStream())
                      .thenReturn(new ByteArrayInputStream("test".getBytes()));

              Map<String, Object> emptyResponse = Map.of("success", true, "data", Map.of());
              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.ok(emptyResponse));

              // When
              BuildingDocumentDto result =
                      aiFraudAnalyzerService.parseBuildingDocument(multipartFile);

              // Then
              assertNotNull(result);
          }

          @Test
          @DisplayName("건축물대장 OCR - 등기부등본 관련 오류")
          void parseBuildingDocument_RegistryDocumentError() throws IOException {
              // Given
              lenient().when(multipartFile.getOriginalFilename()).thenReturn("registry.pdf");
              lenient().when(multipartFile.getSize()).thenReturn(1024L);
              lenient()
                      .when(multipartFile.getInputStream())
                      .thenReturn(new ByteArrayInputStream("test".getBytes()));

              Map<String, Object> errorResponse =
                      Map.of(
                              "success",
                              false,
                              "message",
                              "등기부등본 파일이 아닙니다",
                              "error",
                              Map.of("code", "INVALID_DOCUMENT_TYPE"));

              when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                      .thenReturn(ResponseEntity.ok(errorResponse));

              // When & Then
              FraudRiskException exception =
                      assertThrows(
                              FraudRiskException.class,
                              () -> aiFraudAnalyzerService.parseBuildingDocument(multipartFile));
              assertEquals(FraudErrorCode.INVALID_DOCUMENT_FORMAT, exception.getErrorCode());
          }
      }

      // Helper methods for creating mock objects
      private RiskAnalysisRequest createMockRiskAnalysisRequest() {
          return RiskAnalysisRequest.builder()
                  .homeId(1L)
                  .address("서울특별시 강남구 역삼동")
                  .propertyPrice(500000000)
                  .monthlyRent(1000000)
                  .leaseType("전세")
                  .registeredUserName("홍길동")
                  .residenceType("아파트")
                  .registryDocument(createMockRegistryDocument())
                  .buildingDocument(createMockBuildingDocument())
                  .build();
      }

      private ExternalRiskAnalysisRequest createMockExternalRiskAnalysisRequest() {
          return ExternalRiskAnalysisRequest.builder()
                  .address("서울특별시 강남구 역삼동")
                  .propertyPrice(500000000L)
                  .monthlyRent(1000000L)
                  .leaseType("전세")
                  .registeredUserName("홍길동")
                  .residenceType("아파트")
                  .registryDocument(createMockRegistryDocument())
                  .buildingDocument(createMockBuildingDocument())
                  .build();
      }

      private RegistryDocumentDto createMockRegistryDocument() {
          return RegistryDocumentDto.builder()
                  .regionAddress("서울특별시 강남구 역삼동")
                  .roadAddress("서울특별시 강남구 역삼로 123")
                  .ownerName("홍길동")
                  .ownerBirthDate(LocalDate.of(1980, 1, 1))
                  .debtor("김채무")
                  .mortgageeList(
                          List.of(
                                  MortgageeDto.builder()
                                          .priorityNumber(1)
                                          .maxClaimAmount(300000000L)
                                          .debtor("김채무")
                                          .mortgagee("국민은행")
                                          .build()))
                  .hasSeizure(false)
                  .hasAuction(false)
                  .hasLitigation(false)
                  .hasAttachment(false)
                  .build();
      }

      private BuildingDocumentDto createMockBuildingDocument() {
          return BuildingDocumentDto.builder()
                  .siteLocation("서울특별시 강남구 역삼동 123")
                  .roadAddress("서울특별시 강남구 역삼로 123")
                  .totalFloorArea(85.5)
                  .purpose("아파트")
                  .floorNumber(15)
                  .approvalDate(LocalDate.of(2020, 5, 1))
                  .isViolationBuilding(false)
                  .build();
      }

      private HomeVO createMockHomeVO() {
          return HomeVO.builder()
                  .homeId(1)
                  .addr1("서울특별시 강남구")
                  .addr2("역삼동 123-45")
                  .depositPrice(500000000)
                  .monthlyRent(1000000)
                  .leaseType(LeaseType.JEONSE)
                  .residenceType(ResidenceType.APARTMENT)
                  .userName("홍길동")
                  .build();
      }

      private Map<String, Object> createMockSuccessResponse() {
          Map<String, Object> data =
                  Map.of(
                          "confidenceScore",
                          85.5,
                          "riskType",
                          "HIGH",
                          "analyzedAt",
                          "2023-01-01T00:00:00Z",
                          "detailGroups",
                          List.of(
                                  Map.of(
                                          "title",
                                          "위험 요소",
                                          "items",
                                          List.of(
                                                  Map.of(
                                                          "title",
                                                          "근저당권",
                                                          "content",
                                                          "높은 근저당권 설정",
                                                          "riskLevel",
                                                          "HIGH")))));

          return Map.of("success", true, "data", data);
      }

      private Map<String, Object> createMockFailureResponse() {
          return Map.of("success", false, "message", "분석에 실패했습니다");
      }

      private Map<String, Object> createMockRegistryOcrResponse() {
          Map<String, Object> parsedData =
                  Map.of(
                          "regionAddress",
                          "서울특별시 강남구 역삼동",
                          "roadAddress",
                          "서울특별시 강남구 역삼로 123",
                          "ownerName",
                          "홍길동",
                          "ownerBirthDate",
                          "1980-01-01",
                          "debtor",
                          "김채무",
                          "mortgageeList",
                          List.of(
                                  Map.of(
                                          "priorityNumber",
                                          1,
                                          "maxClaimAmount",
                                          300000000L,
                                          "debtor",
                                          "김채무",
                                          "mortgagee",
                                          "국민은행")),
                          "hasSeizure",
                          false,
                          "hasAuction",
                          false,
                          "hasLitigation",
                          false,
                          "hasAttachment",
                          false);

          Map<String, Object> data = Map.of("parsed_data", parsedData);
          return Map.of("success", true, "data", data);
      }

      private Map<String, Object> createMockRegistryErrorResponse() {
          return Map.of(
                  "success",
                  false,
                  "message",
                  "등기부등본 파일이 아닙니다",
                  "error",
                  Map.of("code", "INVALID_DOCUMENT_TYPE"));
      }

      private Map<String, Object> createMockBuildingOcrResponse() {
          Map<String, Object> parsedData =
                  Map.of(
                          "siteLocation",
                          "서울특별시 강남구 역삼동 123",
                          "roadAddress",
                          "서울특별시 강남구 역삼로 123",
                          "totalFloorArea",
                          85.5,
                          "purpose",
                          "아파트",
                          "floorNumber",
                          15,
                          "approvalDate",
                          "2020-05-01",
                          "isViolationBuilding",
                          false);

          Map<String, Object> data = Map.of("parsed_data", parsedData);
          return Map.of("success", true, "data", data);
      }

      private Map<String, Object> createMockBuildingErrorResponse() {
          return Map.of(
                  "success",
                  false,
                  "message",
                  "건축물대장 파일이 아닙니다",
                  "error",
                  Map.of("code", "INVALID_DOCUMENT_TYPE"));
      }
}
