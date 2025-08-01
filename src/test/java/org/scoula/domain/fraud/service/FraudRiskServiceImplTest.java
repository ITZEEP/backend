package org.scoula.domain.fraud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
import org.scoula.domain.fraud.dto.ai.FraudRiskCheckDto;
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
import org.scoula.domain.fraud.exception.FraudRiskException;
import org.scoula.domain.fraud.mapper.FraudRiskMapper;
import org.scoula.domain.fraud.mapper.HomeLikeMapper;
import org.scoula.domain.fraud.vo.RiskCheckDetailVO;
import org.scoula.domain.fraud.vo.RiskCheckVO;
import org.scoula.global.common.dto.PageRequest;
import org.scoula.global.common.dto.PageResponse;
import org.scoula.global.file.service.S3ServiceInterface;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudRiskServiceImpl 테스트")
class FraudRiskServiceImplTest {

      @Mock private FraudRiskMapper fraudRiskMapper;

      @Mock private HomeLikeMapper homeLikeMapper;

      @Mock private S3ServiceInterface s3Service;

      @Mock private ObjectMapper objectMapper;

      @Mock private AiFraudAnalyzerService aiFraudAnalyzerService;

      @InjectMocks private FraudRiskServiceImpl fraudRiskService;

      @Nested
      @DisplayName("analyzeDocuments 메서드 테스트")
      class AnalyzeDocumentsTest {

          private MultipartFile validRegistryFile;
          private MultipartFile validBuildingFile;
          private Long userId;
          private Long homeId;

          @BeforeEach
          void setUp() {
              userId = 1L;
              homeId = 100L;
              validRegistryFile =
                      new MockMultipartFile(
                              "registryFile",
                              "registry.pdf",
                              "application/pdf",
                              "registry content".getBytes());
              validBuildingFile =
                      new MockMultipartFile(
                              "buildingFile",
                              "building.pdf",
                              "application/pdf",
                              "building content".getBytes());
          }

          @Test
          @DisplayName("정상적인 문서 분석 요청 시 성공")
          void analyzeDocuments_Success() throws Exception {
              // given
              when(fraudRiskMapper.existsHome(homeId)).thenReturn(true);
              when(s3Service.uploadFile(any(MultipartFile.class), anyString()))
                      .thenReturn("file-key-1", "file-key-2");
              when(s3Service.getFileUrl(anyString()))
                      .thenReturn("https://s3.url/file1", "https://s3.url/file2");

              // AI 서비스 모킹
              RegistryDocumentDto mockRegistryDoc =
                      RegistryDocumentDto.builder()
                              .regionAddress("서울시 강남구")
                              .roadAddress("테헤란로 123")
                              .ownerName("홍길동")
                              .build();
              BuildingDocumentDto mockBuildingDoc =
                      BuildingDocumentDto.builder()
                              .siteLocation("서울시 강남구")
                              .roadAddress("테헤란로 123")
                              .totalFloorArea(100.0)
                              .build();

              when(aiFraudAnalyzerService.parseRegistryDocument(any(MultipartFile.class)))
                      .thenReturn(mockRegistryDoc);
              when(aiFraudAnalyzerService.parseBuildingDocument(any(MultipartFile.class)))
                      .thenReturn(mockBuildingDoc);

              // when
              DocumentAnalysisResponse response =
                      fraudRiskService.analyzeDocuments(
                              userId, validRegistryFile, validBuildingFile, homeId);

              // then
              assertThat(response).isNotNull();
              assertThat(response.getHomeId()).isEqualTo(homeId);
              assertThat(response.getRegistryAnalysisStatus())
                      .isEqualTo(AnalysisStatus.SUCCESS.name());
              assertThat(response.getBuildingAnalysisStatus())
                      .isEqualTo(AnalysisStatus.SUCCESS.name());
              assertThat(response.getRegistryDocument()).isNotNull();
              assertThat(response.getBuildingDocument()).isNotNull();
              assertThat(response.getRegistryFileUrl()).isEqualTo("https://s3.url/file1");
              assertThat(response.getBuildingFileUrl()).isEqualTo("https://s3.url/file2");
              assertThat(response.getProcessingTime()).isGreaterThanOrEqualTo(0);
          }

          @Test
          @DisplayName("파일이 null인 경우 예외 발생")
          void analyzeDocuments_NullFile_ThrowsException() {
              // given - existsHome 호출은 validateFile 이후에 일어나므로 stubbing 불필요

              // when & then
              assertThatThrownBy(
                              () ->
                                      fraudRiskService.analyzeDocuments(
                                              userId, null, validBuildingFile, homeId))
                      .isInstanceOf(FraudRiskException.class)
                      .hasMessageContaining("등기부등본 파일이 없습니다");
          }

          @Test
          @DisplayName("파일이 비어있는 경우 예외 발생")
          void analyzeDocuments_EmptyFile_ThrowsException() {
              // given
              MultipartFile emptyFile =
                      new MockMultipartFile(
                              "registryFile", "registry.pdf", "application/pdf", new byte[0]);
              // existsHome 호출은 validateFile 이후에 일어나므로 stubbing 불필요

              // when & then
              assertThatThrownBy(
                              () ->
                                      fraudRiskService.analyzeDocuments(
                                              userId, emptyFile, validBuildingFile, homeId))
                      .isInstanceOf(FraudRiskException.class)
                      .hasMessageContaining("등기부등본 파일이 없습니다");
          }

          @Test
          @DisplayName("파일 크기가 제한을 초과하는 경우 예외 발생")
          void analyzeDocuments_FileSizeExceeded_ThrowsException() {
              // given
              byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
              MultipartFile largeFile =
                      new MockMultipartFile(
                              "registryFile", "registry.pdf", "application/pdf", largeContent);
              // existsHome 호출은 validateFile 이후에 일어나므로 stubbing 불필요

              // when & then
              assertThatThrownBy(
                              () ->
                                      fraudRiskService.analyzeDocuments(
                                              userId, largeFile, validBuildingFile, homeId))
                      .isInstanceOf(FraudRiskException.class)
                      .hasMessageContaining("파일 크기가 10MB를 초과합니다");
          }

          @Test
          @DisplayName("PDF가 아닌 파일인 경우 예외 발생")
          void analyzeDocuments_InvalidFileExtension_ThrowsException() {
              // given
              MultipartFile invalidFile =
                      new MockMultipartFile(
                              "registryFile",
                              "registry.txt",
                              "text/plain",
                              "text content".getBytes());
              // existsHome 호출은 validateFile 이후에 일어나므로 stubbing 불필요

              // when & then
              assertThatThrownBy(
                              () ->
                                      fraudRiskService.analyzeDocuments(
                                              userId, invalidFile, validBuildingFile, homeId))
                      .isInstanceOf(FraudRiskException.class)
                      .hasMessageContaining("PDF 파일만 업로드 가능합니다");
          }

          @Test
          @DisplayName("존재하지 않는 매물인 경우 예외 발생")
          void analyzeDocuments_HomeNotExists_ThrowsException() {
              // given
              when(fraudRiskMapper.existsHome(homeId)).thenReturn(false);

              // when & then
              assertThatThrownBy(
                              () ->
                                      fraudRiskService.analyzeDocuments(
                                              userId, validRegistryFile, validBuildingFile, homeId))
                      .isInstanceOf(FraudRiskException.class)
                      .hasMessageContaining("존재하지 않는 매물입니다");
          }

          @Test
          @DisplayName("S3 업로드 중 오류 발생 시 실패 응답 반환")
          void analyzeDocuments_S3UploadError_ReturnsFailedResponse() throws Exception {
              // given
              when(fraudRiskMapper.existsHome(homeId)).thenReturn(true);
              when(s3Service.uploadFile(any(MultipartFile.class), anyString()))
                      .thenThrow(new RuntimeException("S3 업로드 실패"));

              // when & then
              assertThatThrownBy(
                              () ->
                                      fraudRiskService.analyzeDocuments(
                                              userId, validRegistryFile, validBuildingFile, homeId))
                      .isInstanceOf(FraudRiskException.class)
                      .hasMessageContaining("문서 분석 중 오류가 발생했습니다: S3 업로드 실패");
          }
      }

      @Nested
      @DisplayName("analyzeRisk 메서드 테스트")
      class AnalyzeRiskTest {

          private Long userId;
          private RiskAnalysisRequest request;

          @BeforeEach
          void setUp() {
              userId = 1L;
              request =
                      RiskAnalysisRequest.builder()
                              .homeId(100L)
                              .registryFileUrl("https://s3.url/registry.pdf")
                              .buildingFileUrl("https://s3.url/building.pdf")
                              .build();
          }

          @Test
          @DisplayName("정상적인 위험도 분석 요청 시 성공")
          void analyzeRisk_Success() {
              // given
              RiskCheckVO mockRiskCheck =
                      RiskCheckVO.builder()
                              .riskckId(1L)
                              .userId(userId)
                              .homeId(request.getHomeId())
                              .riskType(RiskType.SAFE)
                              .build();

              doAnswer(
                              invocation -> {
                                  RiskCheckVO arg = invocation.getArgument(0);
                                  arg.setRiskckId(1L);
                                  return null;
                              })
                      .when(fraudRiskMapper)
                      .insertRiskCheck(any(RiskCheckVO.class));

              // AI 서비스 모킹
              FraudRiskCheckDto.Response aiResponse =
                      FraudRiskCheckDto.Response.builder()
                              .status("SUCCESS")
                              .riskScore(20.0)
                              .riskLevel("LOW")
                              .analysisId("test-analysis-id")
                              .build();

              when(aiFraudAnalyzerService.analyzeFraudRisk(anyLong(), any(RiskAnalysisRequest.class)))
                      .thenReturn(aiResponse);
              when(aiFraudAnalyzerService.determineRiskType(any(FraudRiskCheckDto.Response.class)))
                      .thenReturn(RiskType.SAFE);

              List<RiskCheckDetailVO> mockDetails =
                      Arrays.asList(
                              RiskCheckDetailVO.builder()
                                      .title1("갑기본정보")
                                      .title2("소유 및 주소")
                                      .content("등기부등본의 소유자가 임대인 정보와 일치하며, 주소가 정확히 일치합니다.")
                                      .build());
              when(fraudRiskMapper.selectRiskCheckDetailByRiskCheckId(anyLong()))
                      .thenReturn(mockDetails);

              // when
              RiskAnalysisResponse response = fraudRiskService.analyzeRisk(userId, request);

              // then
              assertThat(response).isNotNull();
              assertThat(response.getRiskCheckId()).isEqualTo(1L);
              assertThat(response.getRiskType()).isEqualTo(RiskType.SAFE);
              assertThat(response.getAnalyzedAt()).isNotNull();
              assertThat(response.getDetailGroups()).isNotEmpty();

              verify(fraudRiskMapper).insertRiskCheck(any(RiskCheckVO.class));
              verify(fraudRiskMapper).updateRiskCheck(any(RiskCheckVO.class));
              verify(fraudRiskMapper).deleteRiskCheckDetail(1L);
          }

          @Test
          @DisplayName("위험도 분석 중 예외 발생 시 예외 전파")
          void analyzeRisk_Exception_ThrowsException() {
              // given
              doThrow(new RuntimeException("DB 오류"))
                      .when(fraudRiskMapper)
                      .insertRiskCheck(any(RiskCheckVO.class));

              // when & then
              assertThatThrownBy(() -> fraudRiskService.analyzeRisk(userId, request))
                      .isInstanceOf(FraudRiskException.class)
                      .hasMessageContaining("위험도 분석 중 오류가 발생했습니다");
          }
      }

      @Nested
      @DisplayName("getRiskCheckList 메서드 테스트")
      class GetRiskCheckListTest {

          @Test
          @DisplayName("위험도 체크 목록 조회 성공")
          void getRiskCheckList_Success() {
              // given
              Long userId = 1L;
              PageRequest pageRequest = PageRequest.builder().page(1).size(10).build();

              List<RiskCheckListResponse> mockList =
                      Arrays.asList(
                              RiskCheckListResponse.builder()
                                      .riskCheckId(1L)
                                      .address("서울시 강남구")
                                      .detailAddress("101동 1503호")
                                      .residenceType("아파트")
                                      .checkedAt(LocalDateTime.now())
                                      .build());

              when(fraudRiskMapper.selectRiskChecksByUserId(userId, pageRequest))
                      .thenReturn(mockList);
              when(fraudRiskMapper.countRiskChecksByUserId(userId)).thenReturn(1L);

              // when
              PageResponse<RiskCheckListResponse> response =
                      fraudRiskService.getRiskCheckList(userId, pageRequest);

              // then
              assertThat(response).isNotNull();
              assertThat(response.getContent()).hasSize(1);
              assertThat(response.getTotalElements()).isEqualTo(1);
              assertThat(response.getTotalPages()).isEqualTo(1);
          }
      }

      @Nested
      @DisplayName("getRiskCheckDetail 메서드 테스트")
      class GetRiskCheckDetailTest {

          @Test
          @DisplayName("권한이 있는 사용자의 상세 조회 성공")
          void getRiskCheckDetail_Success() {
              // given
              Long userId = 1L;
              Long riskCheckId = 100L;

              when(fraudRiskMapper.isOwnerOfRiskCheck(riskCheckId, userId)).thenReturn(true);

              RiskCheckDetailResponse mockResponse =
                      RiskCheckDetailResponse.builder()
                              .riskCheckId(riskCheckId)
                              .homeId(1000L)
                              .address("서울시 강남구")
                              .riskType(RiskType.SAFE)
                              .leaseType("JEONSE")
                              .build();

              when(fraudRiskMapper.selectRiskCheckDetailResponse(riskCheckId))
                      .thenReturn(mockResponse);

              List<RiskCheckDetailVO> mockDetails =
                      Arrays.asList(
                              RiskCheckDetailVO.builder()
                                      .title1("갑기본정보")
                                      .title2("소유 정보")
                                      .content("정상")
                                      .build());

              when(fraudRiskMapper.selectRiskCheckDetailByRiskCheckId(riskCheckId))
                      .thenReturn(mockDetails);

              // when
              RiskCheckDetailResponse response =
                      fraudRiskService.getRiskCheckDetail(userId, riskCheckId);

              // then
              assertThat(response).isNotNull();
              assertThat(response.getRiskCheckId()).isEqualTo(riskCheckId);
              assertThat(response.getTransactionType()).isEqualTo("전세");
              assertThat(response.getDetailGroups()).isNotEmpty();
          }

          @Test
          @DisplayName("권한이 없는 사용자의 상세 조회 시 예외 발생")
          void getRiskCheckDetail_NoPermission_ThrowsException() {
              // given
              Long userId = 1L;
              Long riskCheckId = 100L;

              when(fraudRiskMapper.isOwnerOfRiskCheck(riskCheckId, userId)).thenReturn(false);

              // when & then
              assertThatThrownBy(() -> fraudRiskService.getRiskCheckDetail(userId, riskCheckId))
                      .isInstanceOf(FraudRiskException.class)
                      .hasMessageContaining("해당 위험도 체크 결과에 대한 권한이 없습니다");
          }

          @Test
          @DisplayName("존재하지 않는 위험도 체크 조회 시 예외 발생")
          void getRiskCheckDetail_NotFound_ThrowsException() {
              // given
              Long userId = 1L;
              Long riskCheckId = 100L;

              when(fraudRiskMapper.isOwnerOfRiskCheck(riskCheckId, userId)).thenReturn(true);
              when(fraudRiskMapper.selectRiskCheckDetailResponse(riskCheckId)).thenReturn(null);

              // when & then
              assertThatThrownBy(() -> fraudRiskService.getRiskCheckDetail(userId, riskCheckId))
                      .isInstanceOf(FraudRiskException.class)
                      .hasMessageContaining("위험도 체크 결과를 찾을 수 없습니다");
          }
      }

      @Nested
      @DisplayName("deleteRiskCheck 메서드 테스트")
      class DeleteRiskCheckTest {

          @Test
          @DisplayName("권한이 있는 사용자의 삭제 요청 성공")
          void deleteRiskCheck_Success() {
              // given
              Long userId = 1L;
              Long riskCheckId = 100L;

              when(fraudRiskMapper.isOwnerOfRiskCheck(riskCheckId, userId)).thenReturn(true);
              when(fraudRiskMapper.deleteRiskCheck(riskCheckId)).thenReturn(1);

              // when
              fraudRiskService.deleteRiskCheck(userId, riskCheckId);

              // then
              verify(fraudRiskMapper).deleteRiskCheckDetail(riskCheckId);
              verify(fraudRiskMapper).deleteRiskCheck(riskCheckId);
          }

          @Test
          @DisplayName("권한이 없는 사용자의 삭제 요청 시 예외 발생")
          void deleteRiskCheck_NoPermission_ThrowsException() {
              // given
              Long userId = 1L;
              Long riskCheckId = 100L;

              when(fraudRiskMapper.isOwnerOfRiskCheck(riskCheckId, userId)).thenReturn(false);

              // when & then
              assertThatThrownBy(() -> fraudRiskService.deleteRiskCheck(userId, riskCheckId))
                      .isInstanceOf(FraudRiskException.class)
                      .hasMessageContaining("해당 위험도 체크 결과에 대한 삭제 권한이 없습니다");
          }

          @Test
          @DisplayName("삭제 실패 시 예외 발생")
          void deleteRiskCheck_DeleteFailed_ThrowsException() {
              // given
              Long userId = 1L;
              Long riskCheckId = 100L;

              when(fraudRiskMapper.isOwnerOfRiskCheck(riskCheckId, userId)).thenReturn(true);
              when(fraudRiskMapper.deleteRiskCheck(riskCheckId)).thenReturn(0);

              // when & then
              assertThatThrownBy(() -> fraudRiskService.deleteRiskCheck(userId, riskCheckId))
                      .isInstanceOf(FraudRiskException.class)
                      .hasMessageContaining("위험도 체크 결과 삭제에 실패했습니다");
          }
      }

      @Nested
      @DisplayName("getLikedHomes 메서드 테스트")
      class GetLikedHomesTest {

          @Test
          @DisplayName("찜한 매물 목록 조회 성공")
          void getLikedHomes_Success() {
              // given
              Long userId = 1L;
              List<LikedHomeResponse> mockHomes =
                      Arrays.asList(
                              LikedHomeResponse.builder()
                                      .homeId(100L)
                                      .address("서울시 강남구")
                                      .residenceType("아파트")
                                      .leaseType("JEONSE")
                                      .depositPrice(300000000)
                                      .build());

              when(homeLikeMapper.selectLikedHomesByUserId(userId)).thenReturn(mockHomes);

              // when
              List<LikedHomeResponse> response = fraudRiskService.getLikedHomes(userId);

              // then
              assertThat(response).isNotNull();
              assertThat(response).hasSize(1);
              assertThat(response.get(0).getHomeId()).isEqualTo(100L);
          }
      }

      @Nested
      @DisplayName("getChattingHomes 메서드 테스트")
      class GetChattingHomesTest {

          @Test
          @DisplayName("채팅 중인 매물 목록 조회 성공")
          void getChattingHomes_Success() {
              // given
              Long userId = 1L;
              PageRequest pageRequest = PageRequest.builder().page(1).size(10).build();

              List<LikedHomeResponse> mockHomes =
                      Arrays.asList(
                              LikedHomeResponse.builder()
                                      .homeId(200L)
                                      .address("서울시 송파구")
                                      .residenceType("오피스텔")
                                      .leaseType("WOLSE")
                                      .depositPrice(10000000)
                                      .monthlyRent(500000)
                                      .build());

              when(homeLikeMapper.selectChattingHomesByUserId(userId, pageRequest))
                      .thenReturn(mockHomes);
              when(homeLikeMapper.countChattingHomesByUserId(userId)).thenReturn(1L);

              // when
              PageResponse<LikedHomeResponse> response =
                      fraudRiskService.getChattingHomes(userId, pageRequest);

              // then
              assertThat(response).isNotNull();
              assertThat(response.getContent()).hasSize(1);
              assertThat(response.getTotalElements()).isEqualTo(1);
              assertThat(response.getContent().get(0).getHomeId()).isEqualTo(200L);
          }
      }
}
