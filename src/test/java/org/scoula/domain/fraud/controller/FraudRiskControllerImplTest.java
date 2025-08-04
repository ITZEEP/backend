package org.scoula.domain.fraud.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.fraud.dto.common.BuildingDocumentDto;
import org.scoula.domain.fraud.dto.common.RegistryDocumentDto;
import org.scoula.domain.fraud.dto.request.RiskAnalysisRequest;
import org.scoula.domain.fraud.dto.response.DocumentAnalysisResponse;
import org.scoula.domain.fraud.dto.response.LikedHomeResponse;
import org.scoula.domain.fraud.dto.response.RiskAnalysisResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckDetailResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckListResponse;
import org.scoula.domain.fraud.enums.AnalysisStatus;
import org.scoula.domain.fraud.enums.RiskType;
import org.scoula.domain.fraud.exception.FraudErrorCode;
import org.scoula.domain.fraud.exception.FraudRiskException;
import org.scoula.domain.fraud.service.FraudRiskService;
import org.scoula.global.auth.dto.CustomUserDetails;
import org.scoula.global.common.dto.PageRequest;
import org.scoula.global.common.dto.PageResponse;
import org.scoula.global.common.exception.GlobalExceptionHandler;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudRiskControllerImpl 테스트")
class FraudRiskControllerImplTest {

      @Mock private FraudRiskService fraudRiskService;

      @InjectMocks private FraudRiskControllerImpl fraudRiskController;

      private MockMvc mockMvc;
      private ObjectMapper objectMapper;
      private CustomUserDetails userDetails;

      @BeforeEach
      void setUp() {
          // Mock 사용자 인증 정보 설정
          userDetails = new CustomUserDetails(1L, "testuser@example.com", "password", "ROLE_USER");

          // Custom ArgumentResolver to inject CustomUserDetails
          HandlerMethodArgumentResolver customUserDetailsResolver =
                  new HandlerMethodArgumentResolver() {
                      @Override
                      public boolean supportsParameter(MethodParameter parameter) {
                          return parameter.getParameterType().equals(CustomUserDetails.class);
                      }

                      @Override
                      public Object resolveArgument(
                              MethodParameter parameter,
                              ModelAndViewContainer mavContainer,
                              NativeWebRequest webRequest,
                              WebDataBinderFactory binderFactory)
                              throws Exception {
                          Principal principal = webRequest.getUserPrincipal();
                          return principal != null ? userDetails : null;
                      }
                  };

          mockMvc =
                  MockMvcBuilders.standaloneSetup(fraudRiskController)
                          .setControllerAdvice(new GlobalExceptionHandler())
                          .setCustomArgumentResolvers(customUserDetailsResolver)
                          .build();
          objectMapper = new ObjectMapper();
      }

      @Nested
      @DisplayName("GET /api/fraud-risk - 위험도 체크 목록 조회")
      class GetRiskCheckListTest {

          @Test
          @DisplayName("정상적인 목록 조회 요청 성공")
          void getRiskCheckList_Success() throws Exception {
              // given
              List<RiskCheckListResponse> content =
                      Arrays.asList(
                              RiskCheckListResponse.builder()
                                      .riskCheckId(1L)
                                      .address("서울시 강남구")
                                      .detailAddress("101동 1503호")
                                      .residenceType("아파트")
                                      .checkedAt(LocalDateTime.now())
                                      .build());

              PageResponse<RiskCheckListResponse> pageResponse =
                      PageResponse.of(content, PageRequest.builder().page(1).size(10).build(), 1L);

              when(fraudRiskService.getRiskCheckList(anyLong(), any(PageRequest.class)))
                      .thenReturn(pageResponse);

              // when & then
              mockMvc.perform(
                              get("/api/fraud-risk")
                                      .param("page", "1")
                                      .param("size", "10")
                                      .principal(() -> "testuser"))
                      .andDo(print())
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.content").isArray())
                      .andExpect(jsonPath("$.content[0].riskCheckId").value(1))
                      .andExpect(jsonPath("$.content[0].address").value("서울시 강남구"))
                      .andExpect(jsonPath("$.totalElements").value(1))
                      .andExpect(jsonPath("$.totalPages").value(1));
          }

          @Test
          @DisplayName("인증되지 않은 사용자 요청 시 500 반환")
          void getRiskCheckList_Unauthorized() throws Exception {
              // given - 인증되지 않은 요청이므로 service mock 설정 불필요

              // when & then
              mockMvc.perform(get("/api/fraud-risk").param("page", "1").param("size", "10"))
                      .andDo(print())
                      .andExpect(status().isInternalServerError());
          }
      }

      @Nested
      @DisplayName("POST /api/fraud-risk/documents - PDF 문서 분석")
      class AnalyzeDocumentsTest {

          @Test
          @DisplayName("정상적인 문서 분석 요청 성공")
          void analyzeDocuments_Success() throws Exception {
              // given
              MockMultipartFile registryFile =
                      new MockMultipartFile(
                              "registryFile",
                              "registry.pdf",
                              MediaType.APPLICATION_PDF_VALUE,
                              "registry content".getBytes());

              MockMultipartFile buildingFile =
                      new MockMultipartFile(
                              "buildingFile",
                              "building.pdf",
                              MediaType.APPLICATION_PDF_VALUE,
                              "building content".getBytes());

              DocumentAnalysisResponse response =
                      DocumentAnalysisResponse.builder()
                              .homeId(100L)
                              .registryAnalysisStatus(AnalysisStatus.SUCCESS.name())
                              .buildingAnalysisStatus(AnalysisStatus.SUCCESS.name())
                              .registryFileUrl("https://s3.url/registry.pdf")
                              .buildingFileUrl("https://s3.url/building.pdf")
                              .processingTime(2.5)
                              .build();

              when(fraudRiskService.analyzeDocuments(anyLong(), any(), any(), anyLong()))
                      .thenReturn(response);

              // when & then
              mockMvc.perform(
                              multipart("/api/fraud-risk/documents")
                                      .file(registryFile)
                                      .file(buildingFile)
                                      .param("homeId", "100")
                                      .with(
                                              request -> {
                                                  request.setMethod("POST");
                                                  return request;
                                              })
                                      .principal(() -> "testuser"))
                      .andDo(print())
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.success").value(true))
                      .andExpect(jsonPath("$.data.homeId").value(100))
                      .andExpect(jsonPath("$.data.registryAnalysisStatus").value("SUCCESS"))
                      .andExpect(jsonPath("$.data.buildingAnalysisStatus").value("SUCCESS"));
          }

          @Test
          @DisplayName("문서 분석 중 오류 발생 시 500 반환")
          void analyzeDocuments_Error() throws Exception {
              // given
              MockMultipartFile registryFile =
                      new MockMultipartFile(
                              "registryFile",
                              "registry.pdf",
                              MediaType.APPLICATION_PDF_VALUE,
                              "registry content".getBytes());

              MockMultipartFile buildingFile =
                      new MockMultipartFile(
                              "buildingFile",
                              "building.pdf",
                              MediaType.APPLICATION_PDF_VALUE,
                              "building content".getBytes());

              when(fraudRiskService.analyzeDocuments(anyLong(), any(), any(), anyLong()))
                      .thenThrow(new RuntimeException("S3 업로드 실패"));

              // when & then
              mockMvc.perform(
                              multipart("/api/fraud-risk/documents")
                                      .file(registryFile)
                                      .file(buildingFile)
                                      .param("homeId", "100")
                                      .with(
                                              request -> {
                                                  request.setMethod("POST");
                                                  return request;
                                              })
                                      .principal(() -> "testuser"))
                      .andDo(print())
                      .andExpect(status().isInternalServerError())
                      .andExpect(jsonPath("$.success").value(false))
                      .andExpect(jsonPath("$.message").value("내부 서버 오류"));
          }
      }

      @Nested
      @DisplayName("POST /api/fraud-risk/analyze - 위험도 분석")
      class AnalyzeRiskTest {

          @Test
          @DisplayName("정상적인 위험도 분석 요청 성공")
          void analyzeRisk_Success() throws Exception {
              // given
              RiskAnalysisRequest request =
                      RiskAnalysisRequest.builder()
                              .homeId(100L)
                              .address("서울시 강남구")
                              .propertyPrice(50000L)
                              .leaseType("JEONSE")
                              .residenceType("APARTMENT")
                              .registeredUserName("홍길동")
                              .registryDocument(
                                      RegistryDocumentDto.builder()
                                              .regionAddress("서울시 강남구")
                                              .ownerName("홍길동")
                                              .build())
                              .buildingDocument(
                                      BuildingDocumentDto.builder()
                                              .siteLocation("서울시 강남구")
                                              .purpose("아파트")
                                              .build())
                              .registryFileUrl("https://s3.url/registry.pdf")
                              .buildingFileUrl("https://s3.url/building.pdf")
                              .build();

              RiskAnalysisResponse response =
                      RiskAnalysisResponse.builder()
                              .riskCheckId(1L)
                              .riskType(RiskType.SAFE)
                              .analyzedAt(LocalDateTime.now())
                              .detailGroups(Arrays.asList())
                              .build();

              when(fraudRiskService.analyzeRisk(anyLong(), any(RiskAnalysisRequest.class)))
                      .thenReturn(response);

              // when & then
              mockMvc.perform(
                              post("/api/fraud-risk/analyze")
                                      .contentType(MediaType.APPLICATION_JSON)
                                      .content(objectMapper.writeValueAsString(request))
                                      .principal(() -> "testuser"))
                      .andDo(print())
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.success").value(true))
                      .andExpect(jsonPath("$.data.riskCheckId").value(1))
                      .andExpect(jsonPath("$.data.riskType").value("SAFE"));
          }
      }

      @Nested
      @DisplayName("GET /api/fraud-risk/{riskCheckId} - 상세 조회")
      class GetRiskCheckDetailTest {

          @Test
          @DisplayName("정상적인 상세 조회 요청 성공")
          void getRiskCheckDetail_Success() throws Exception {
              // given
              Long riskCheckId = 1L;
              RiskCheckDetailResponse response =
                      RiskCheckDetailResponse.builder()
                              .riskCheckId(riskCheckId)
                              .homeId(100L)
                              .address("서울시 강남구")
                              .riskType(RiskType.SAFE)
                              .checkedAt(LocalDateTime.now())
                              .build();

              when(fraudRiskService.getRiskCheckDetail(anyLong(), eq(riskCheckId)))
                      .thenReturn(response);

              // when & then
              mockMvc.perform(
                              get("/api/fraud-risk/{riskCheckId}", riskCheckId)
                                      .principal(() -> "testuser"))
                      .andDo(print())
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.success").value(true))
                      .andExpect(jsonPath("$.data.riskCheckId").value(riskCheckId))
                      .andExpect(jsonPath("$.data.address").value("서울시 강남구"));
          }

          @Test
          @DisplayName("존재하지 않는 위험도 체크 조회 시 500 반환")
          void getRiskCheckDetail_NotFound() throws Exception {
              // given
              Long riskCheckId = 999L;
              when(fraudRiskService.getRiskCheckDetail(anyLong(), eq(riskCheckId)))
                      .thenThrow(
                              new FraudRiskException(
                                      FraudErrorCode.FRAUD_ANALYSIS_FAILED, "위험도 체크 결과를 찾을 수 없습니다."));

              // when & then
              mockMvc.perform(
                              get("/api/fraud-risk/{riskCheckId}", riskCheckId)
                                      .principal(() -> "testuser"))
                      .andDo(print())
                      .andExpect(status().isInternalServerError())
                      .andExpect(jsonPath("$.success").value(false))
                      .andExpect(jsonPath("$.message").value("위험도 체크 결과를 찾을 수 없습니다."));
          }
      }

      @Nested
      @DisplayName("DELETE /api/fraud-risk/{riskCheckId} - 삭제")
      class DeleteRiskCheckTest {

          @Test
          @DisplayName("정상적인 삭제 요청 성공")
          void deleteRiskCheck_Success() throws Exception {
              // given
              Long riskCheckId = 1L;

              // when & then
              mockMvc.perform(
                              delete("/api/fraud-risk/{riskCheckId}", riskCheckId)
                                      .principal(() -> "testuser"))
                      .andDo(print())
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.success").value(true));
          }

          @Test
          @DisplayName("권한이 없는 삭제 요청 시 500 반환")
          void deleteRiskCheck_NoPermission() throws Exception {
              // given
              Long riskCheckId = 1L;
              doThrow(
                              new FraudRiskException(
                                      FraudErrorCode.FRAUD_ANALYSIS_FAILED,
                                      "해당 위험도 체크 결과에 대한 삭제 권한이 없습니다."))
                      .when(fraudRiskService)
                      .deleteRiskCheck(anyLong(), eq(riskCheckId));

              // when & then
              mockMvc.perform(
                              delete("/api/fraud-risk/{riskCheckId}", riskCheckId)
                                      .principal(() -> "testuser"))
                      .andDo(print())
                      .andExpect(status().isInternalServerError())
                      .andExpect(jsonPath("$.success").value(false))
                      .andExpect(jsonPath("$.message").value("해당 위험도 체크 결과에 대한 삭제 권한이 없습니다."));
          }
      }

      @Nested
      @DisplayName("GET /api/fraud-risk/liked-homes - 찜한 매물 목록 조회")
      class GetLikedHomesTest {

          @Test
          @DisplayName("정상적인 찜한 매물 목록 조회 성공")
          void getLikedHomes_Success() throws Exception {
              // given
              List<LikedHomeResponse> likedHomes =
                      Arrays.asList(
                              LikedHomeResponse.builder()
                                      .homeId(100L)
                                      .address("서울시 강남구")
                                      .residenceType("아파트")
                                      .leaseType("JEONSE")
                                      .depositPrice(300000000)
                                      .build());

              when(fraudRiskService.getLikedHomes(anyLong())).thenReturn(likedHomes);

              // when & then
              mockMvc.perform(get("/api/fraud-risk/liked-homes").principal(() -> "testuser"))
                      .andDo(print())
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.success").value(true))
                      .andExpect(jsonPath("$.data").isArray())
                      .andExpect(jsonPath("$.data[0].homeId").value(100))
                      .andExpect(jsonPath("$.data[0].address").value("서울시 강남구"));
          }
      }

      @Nested
      @DisplayName("GET /api/fraud-risk/chatting-homes - 채팅 중인 매물 목록 조회")
      class GetChattingHomesTest {

          @Test
          @DisplayName("정상적인 채팅 중인 매물 목록 조회 성공")
          void getChattingHomes_Success() throws Exception {
              // given
              List<LikedHomeResponse> content =
                      Arrays.asList(
                              LikedHomeResponse.builder()
                                      .homeId(200L)
                                      .address("서울시 송파구")
                                      .residenceType("오피스텔")
                                      .leaseType("WOLSE")
                                      .depositPrice(10000000)
                                      .monthlyRent(500000)
                                      .build());

              PageResponse<LikedHomeResponse> pageResponse =
                      PageResponse.of(content, PageRequest.builder().page(1).size(10).build(), 1L);

              when(fraudRiskService.getChattingHomes(anyLong(), any(PageRequest.class)))
                      .thenReturn(pageResponse);

              // when & then
              mockMvc.perform(
                              get("/api/fraud-risk/chatting-homes")
                                      .param("page", "1")
                                      .param("size", "10")
                                      .principal(() -> "testuser"))
                      .andDo(print())
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.content").isArray())
                      .andExpect(jsonPath("$.content[0].homeId").value(200))
                      .andExpect(jsonPath("$.content[0].address").value("서울시 송파구"))
                      .andExpect(jsonPath("$.totalElements").value(1));
          }

          @Test
          @DisplayName("인증되지 않은 사용자의 채팅 매물 조회 시 500 반환")
          void getChattingHomes_Unauthorized() throws Exception {
              // given - 인증되지 않은 요청이므로 service mock 설정 불필요

              // when & then
              mockMvc.perform(
                              get("/api/fraud-risk/chatting-homes")
                                      .param("page", "1")
                                      .param("size", "10"))
                      .andDo(print())
                      .andExpect(status().isInternalServerError());
          }
      }
}
