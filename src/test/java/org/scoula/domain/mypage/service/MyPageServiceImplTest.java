package org.scoula.domain.mypage.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.mypage.dto.*;
import org.scoula.domain.mypage.mapper.MyPageMapper;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("MyPageServiceImpl 테스트")
class MyPageServiceImplTest {

      @Mock private MyPageMapper myPageMapper;
      @Mock private ProfileImageService profileImageService;

      @InjectMocks private MyPageServiceImpl myPageService;

      private Long userId;
      private MyPageUserInfoDto userInfoDto;
      private MultipartFile profileImage;

      @BeforeEach
      void setUp() {
          userId = 1L;

          userInfoDto = new MyPageUserInfoDto();
          userInfoDto.setUserId(userId);
          userInfoDto.setNickname("testUser");
          userInfoDto.setEmail("test@example.com");
          userInfoDto.setProfileImageUrl("http://example.com/profile.jpg");

          profileImage =
                  new MockMultipartFile(
                          "profileImage",
                          "profile.jpg",
                          "image/jpeg",
                          "profile image content".getBytes());
      }

      @Nested
      @DisplayName("getUserInfo 메서드")
      class GetUserInfo {

          @Test
          @DisplayName("사용자 정보 조회 성공")
          void getUserInfo_Success() {
              // Given
              when(myPageMapper.selectUserInfoByUserId(userId)).thenReturn(userInfoDto);

              // When
              MyPageUserInfoDto result = myPageService.getUserInfo(userId);

              // Then
              assertNotNull(result);
              assertEquals(userId, result.getUserId());
              assertEquals("testUser", result.getNickname());
              assertEquals("test@example.com", result.getEmail());
              verify(myPageMapper).selectUserInfoByUserId(userId);
          }

          @Test
          @DisplayName("존재하지 않는 사용자 정보 조회 시 예외 발생")
          void getUserInfo_UserNotFound() {
              // Given
              when(myPageMapper.selectUserInfoByUserId(userId)).thenReturn(null);

              // When & Then
              assertThrows(BusinessException.class, () -> myPageService.getUserInfo(userId));
              verify(myPageMapper).selectUserInfoByUserId(userId);
          }
      }

      @Nested
      @DisplayName("updateProfileImage 메서드")
      class UpdateProfileImage {

          @Test
          @DisplayName("프로필 이미지 업데이트 성공")
          void updateProfileImage_Success() {
              // Given
              String newImageUrl = "http://s3.amazonaws.com/new-profile.jpg";
              String previousImageUrl = "http://example.com/old-profile.jpg";

              when(myPageMapper.selectUserInfoByUserId(userId)).thenReturn(userInfoDto);
              when(profileImageService.uploadProfileImage(profileImage, userId, previousImageUrl))
                      .thenReturn(newImageUrl);

              // When
              String result = myPageService.updateProfileImage(userId, profileImage);

              // Then
              assertEquals(newImageUrl, result);
              verify(profileImageService).uploadProfileImage(profileImage, userId, previousImageUrl);
              verify(myPageMapper).updateProfileImage(userId, newImageUrl);
          }

          @Test
          @DisplayName("프로필 이미지 업로드 실패 시 예외 발생")
          void updateProfileImage_UploadFail() {
              // Given
              when(myPageMapper.selectUserInfoByUserId(userId)).thenReturn(userInfoDto);
              when(profileImageService.uploadProfileImage(eq(profileImage), eq(userId), anyString()))
                      .thenThrow(new RuntimeException("Upload failed"));

              // When & Then
              assertThrows(
                      BusinessException.class,
                      () -> myPageService.updateProfileImage(userId, profileImage));
          }
      }

      @Nested
      @DisplayName("updateNickname 메서드")
      class UpdateNickname {

          @Test
          @DisplayName("닉네임 업데이트 성공")
          void updateNickname_Success() {
              // Given
              String newNickname = "newNickname";

              when(myPageMapper.existsByNickname(newNickname, userId)).thenReturn(false);

              // When
              myPageService.updateNickname(userId, newNickname);

              // Then
              verify(myPageMapper).existsByNickname(newNickname, userId);
              verify(myPageMapper).updateNickname(userId, newNickname);
          }

          @Test
          @DisplayName("중복된 닉네임으로 업데이트 시 예외 발생")
          void updateNickname_DuplicateNickname() {
              // Given
              String newNickname = "existingNickname";

              when(myPageMapper.existsByNickname(newNickname, userId)).thenReturn(true);

              // When & Then
              assertThrows(
                      BusinessException.class,
                      () -> myPageService.updateNickname(userId, newNickname));
          }
      }

      @Nested
      @DisplayName("updateNotificationSetting 메서드")
      class UpdateNotificationSetting {

          @Test
          @DisplayName("알림 설정 업데이트 성공")
          void updateNotificationSetting_Success() {
              // Given
              Boolean enabled = false;

              // When
              myPageService.updateNotificationSetting(userId, enabled);

              // Then
              verify(myPageMapper).updateNotificationSetting(userId, enabled);
          }
      }

      @Nested
      @DisplayName("getMyContracts 메서드")
      class GetMyContracts {

          @Test
          @DisplayName("내 계약서 목록 조회 성공")
          void getMyContracts_Success() {
              // Given
              Pageable pageable = PageRequest.of(0, 10);
              List<MyPageContractDto> contracts =
                      Arrays.asList(new MyPageContractDto(), new MyPageContractDto());

              when(myPageMapper.selectContractsByUserId(userId, 0, 10)).thenReturn(contracts);
              when(myPageMapper.countContractsByUserId(userId)).thenReturn(2);

              // When
              Page<MyPageContractDto> result = myPageService.getMyContracts(userId, pageable);

              // Then
              assertNotNull(result);
              assertEquals(2, result.getContent().size());
              assertEquals(2, result.getTotalElements());
              verify(myPageMapper).selectContractsByUserId(userId, 0, 10);
          }
      }

      @Nested
      @DisplayName("getMyProperties 메서드")
      class GetMyProperties {

          @Test
          @DisplayName("내 매물 목록 조회 성공")
          void getMyProperties_Success() {
              // Given
              Pageable pageable = PageRequest.of(0, 10);
              List<MyPagePropertyDto> properties =
                      Arrays.asList(new MyPagePropertyDto(), new MyPagePropertyDto());

              when(myPageMapper.selectPropertiesByUserId(userId, 0, 10)).thenReturn(properties);
              when(myPageMapper.countPropertiesByUserId(userId)).thenReturn(2);

              // When
              Page<MyPagePropertyDto> result = myPageService.getMyProperties(userId, pageable);

              // Then
              assertNotNull(result);
              assertEquals(2, result.getContent().size());
              assertEquals(2, result.getTotalElements());
              verify(myPageMapper).selectPropertiesByUserId(userId, 0, 10);
          }
      }

      @Nested
      @DisplayName("getMyRiskAnalyses 메서드")
      class GetMyRiskAnalyses {

          @Test
          @DisplayName("내 사기위험도 분석 이력 조회 성공")
          void getMyRiskAnalyses_Success() {
              // Given
              Pageable pageable = PageRequest.of(0, 10);
              List<MyPageRiskAnalysisDto> analyses =
                      Arrays.asList(new MyPageRiskAnalysisDto(), new MyPageRiskAnalysisDto());

              when(myPageMapper.selectRiskAnalysesByUserId(userId, 0, 10)).thenReturn(analyses);
              when(myPageMapper.countRiskAnalysesByUserId(userId)).thenReturn(2);

              // When
              Page<MyPageRiskAnalysisDto> result = myPageService.getMyRiskAnalyses(userId, pageable);

              // Then
              assertNotNull(result);
              assertEquals(2, result.getContent().size());
              assertEquals(2, result.getTotalElements());
              verify(myPageMapper).selectRiskAnalysesByUserId(userId, 0, 10);
          }
      }
}
