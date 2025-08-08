package org.scoula.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doAnswer;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.mypage.service.ProfileImageService;
import org.scoula.domain.user.mapper.SocialAccountMapper;
import org.scoula.domain.user.mapper.UserMapper;
import org.scoula.domain.user.vo.SocialAccount;
import org.scoula.domain.user.vo.User;
import org.scoula.global.common.exception.BusinessException;

/**
 * UserService 단위 테스트
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

      @Mock private UserMapper userMapper;

      @Mock private SocialAccountMapper socialAccountMapper;

      @Mock private ProfileImageService profileImageService;

      private UserServiceImpl userService;

      @BeforeEach
      void setUp() {
          userService = new UserServiceImpl(userMapper, socialAccountMapper, profileImageService);
      }

      @Nested
      @DisplayName("OAuth2 사용자 등록/업데이트")
      class RegisterOrUpdateOAuth2UserTest {

          @Test
          @DisplayName("신규 OAuth2 사용자 등록 성공")
          void registerNewOAuth2User_Success() {
              // given
              String socialId = "123456789";
              SocialAccount.SocialType socialType = SocialAccount.SocialType.KAKAO;
              String email = "test@example.com";
              String nickname = "테스트사용자";
              String profileImageUrl = "http://example.com/profile.jpg";
              String genderStr = "male";

              when(socialAccountMapper.selectBySocialIdAndSocialType(socialId, socialType))
                      .thenReturn(Optional.empty());
              when(userMapper.selectByEmail(email)).thenReturn(Optional.empty());
              when(userMapper.existsByNickname(nickname)).thenReturn(false);

              User newUser =
                      User.builder()
                              .userId(1L)
                              .email(email)
                              .nickname(nickname)
                              .profileImgUrl(profileImageUrl)
                              .gender(User.Gender.MALE)
                              .role(User.Role.ROLE_USER)
                              .build();

              // Mock userMapper.insert to set userId
              doAnswer(
                              invocation -> {
                                  User user = invocation.getArgument(0);
                                  // Simulate database setting the ID after insert
                                  user.setUserId(1L);
                                  return null;
                              })
                      .when(userMapper)
                      .insert(any(User.class));

              when(profileImageService.uploadProfileImageFromUrl(profileImageUrl, 1L))
                      .thenReturn("https://s3.amazonaws.com/bucket/profile/1.jpg");
              when(userMapper.selectById(1L)).thenReturn(Optional.of(newUser));

              // when
              User result =
                      userService.registerOrUpdateOAuth2User(
                              socialId, socialType, email, nickname, profileImageUrl, genderStr);

              // then
              assertThat(result).isNotNull();
              assertThat(result.getEmail()).isEqualTo(email);
              assertThat(result.getNickname()).isEqualTo(nickname);
              assertThat(result.getGender()).isEqualTo(User.Gender.MALE);

              verify(userMapper).insert(any(User.class));
              verify(socialAccountMapper).insert(any(SocialAccount.class));
              verify(profileImageService).uploadProfileImageFromUrl(profileImageUrl, 1L);
          }

          @Test
          @DisplayName("기존 OAuth2 사용자 업데이트 성공")
          void updateExistingOAuth2User_Success() {
              // given
              String socialId = "123456789";
              SocialAccount.SocialType socialType = SocialAccount.SocialType.KAKAO;
              String email = "test@example.com";
              String nickname = "새닉네임";
              String profileImageUrl = "http://example.com/new-profile.jpg";
              String genderStr = "female";
              Long userId = 1L;

              SocialAccount existingSocialAccount =
                      SocialAccount.builder()
                              .socialId(socialId)
                              .socialType(socialType)
                              .userId(userId)
                              .build();

              User existingUser =
                      User.builder()
                              .userId(userId)
                              .email(email)
                              .nickname("기존닉네임")
                              .profileImgUrl("http://example.com/old-profile.jpg")
                              .gender(User.Gender.MALE)
                              .role(User.Role.ROLE_USER)
                              .build();

              when(socialAccountMapper.selectBySocialIdAndSocialType(socialId, socialType))
                      .thenReturn(Optional.of(existingSocialAccount));
              when(userMapper.selectById(userId)).thenReturn(Optional.of(existingUser));
              when(profileImageService.uploadProfileImageFromUrl(profileImageUrl, userId))
                      .thenReturn("https://s3.amazonaws.com/bucket/profile/1-new.jpg");

              // when
              User result =
                      userService.registerOrUpdateOAuth2User(
                              socialId, socialType, email, nickname, profileImageUrl, genderStr);

              // then
              assertThat(result).isNotNull();
              assertThat(result.getUserId()).isEqualTo(userId);

              verify(userMapper).update(any(User.class));
              verify(profileImageService).uploadProfileImageFromUrl(profileImageUrl, userId);
              verify(userMapper, never()).insert(any(User.class));
              verify(socialAccountMapper, never()).insert(any(SocialAccount.class));
          }

          @Test
          @DisplayName("이메일로 기존 사용자 찾아 소셜 계정 연결")
          void linkSocialAccountToExistingUserByEmail_Success() {
              // given
              String socialId = "987654321";
              SocialAccount.SocialType socialType = SocialAccount.SocialType.KAKAO;
              String email = "existing@example.com";
              String nickname = "닉네임";
              String profileImageUrl = "http://example.com/profile.jpg";
              Long userId = 2L;

              User existingUser =
                      User.builder()
                              .userId(userId)
                              .email(email)
                              .nickname("기존닉네임")
                              .role(User.Role.ROLE_USER)
                              .build();

              when(socialAccountMapper.selectBySocialIdAndSocialType(socialId, socialType))
                      .thenReturn(Optional.empty());
              when(userMapper.selectByEmail(email)).thenReturn(Optional.of(existingUser));
              when(userMapper.selectById(userId)).thenReturn(Optional.of(existingUser));
              when(profileImageService.uploadProfileImageFromUrl(profileImageUrl, userId))
                      .thenReturn("https://s3.amazonaws.com/bucket/profile/2.jpg");

              // when
              User result =
                      userService.registerOrUpdateOAuth2User(
                              socialId, socialType, email, nickname, profileImageUrl, null);

              // then
              assertThat(result).isNotNull();
              assertThat(result.getUserId()).isEqualTo(userId);

              verify(socialAccountMapper).insert(any(SocialAccount.class));
              verify(userMapper).update(any(User.class));
              verify(userMapper, never()).insert(any(User.class));
          }

          @Test
          @DisplayName("닉네임 중복 시 유니크 닉네임 생성")
          void generateUniqueNickname_WhenDuplicated() {
              // given
              String socialId = "123456789";
              SocialAccount.SocialType socialType = SocialAccount.SocialType.KAKAO;
              String email = "test@example.com";
              String nickname = "중복닉네임";

              when(socialAccountMapper.selectBySocialIdAndSocialType(socialId, socialType))
                      .thenReturn(Optional.empty());
              when(userMapper.selectByEmail(email)).thenReturn(Optional.empty());
              when(userMapper.existsByNickname(nickname)).thenReturn(true);
              when(userMapper.existsByNickname(nickname + "1")).thenReturn(true);
              when(userMapper.existsByNickname(nickname + "2")).thenReturn(false);

              // when
              User result =
                      userService.registerOrUpdateOAuth2User(
                              socialId, socialType, email, nickname, null, null);

              // then
              verify(userMapper).insert(argThat(user -> user.getNickname().equals(nickname + "2")));
          }

          @Test
          @DisplayName("성별 변환 테스트 - male to MALE")
          void convertGender_MaleToMALE() {
              // given
              String genderStr = "male";
              when(socialAccountMapper.selectBySocialIdAndSocialType(anyString(), any()))
                      .thenReturn(Optional.empty());
              when(userMapper.selectByEmail(anyString())).thenReturn(Optional.empty());
              when(userMapper.existsByNickname(anyString())).thenReturn(false);

              // when
              userService.registerOrUpdateOAuth2User(
                      "123",
                      SocialAccount.SocialType.KAKAO,
                      "test@test.com",
                      "nick",
                      null,
                      genderStr);

              // then
              verify(userMapper).insert(argThat(user -> user.getGender() == User.Gender.MALE));
          }

          @Test
          @DisplayName("성별 변환 테스트 - female to FEMALE")
          void convertGender_FemaleToFEMALE() {
              // given
              String genderStr = "female";
              when(socialAccountMapper.selectBySocialIdAndSocialType(anyString(), any()))
                      .thenReturn(Optional.empty());
              when(userMapper.selectByEmail(anyString())).thenReturn(Optional.empty());
              when(userMapper.existsByNickname(anyString())).thenReturn(false);

              // when
              userService.registerOrUpdateOAuth2User(
                      "123",
                      SocialAccount.SocialType.KAKAO,
                      "test@test.com",
                      "nick",
                      null,
                      genderStr);

              // then
              verify(userMapper).insert(argThat(user -> user.getGender() == User.Gender.FEMALE));
          }

          @Test
          @DisplayName("프로필 이미지 업로드 실패 시에도 회원가입 진행")
          void continueRegistration_WhenProfileImageUploadFails() {
              // given
              String profileImageUrl = "http://example.com/profile.jpg";
              when(socialAccountMapper.selectBySocialIdAndSocialType(anyString(), any()))
                      .thenReturn(Optional.empty());
              when(userMapper.selectByEmail(anyString())).thenReturn(Optional.empty());
              when(userMapper.existsByNickname(anyString())).thenReturn(false);
              when(profileImageService.uploadProfileImageFromUrl(anyString(), anyLong()))
                      .thenThrow(new RuntimeException("S3 업로드 실패"));

              // when & then - Should not throw exception
              try {
                  userService.registerOrUpdateOAuth2User(
                          "123",
                          SocialAccount.SocialType.KAKAO,
                          "test@test.com",
                          "nick",
                          profileImageUrl,
                          null);
                  // If we reach here, no exception was thrown - this is good
              } catch (Exception e) {
                  // If an exception is thrown, fail the test
                  assertThat(e).isNull(); // This will fail and show the exception
              }

              verify(userMapper).insert(any(User.class));
              verify(socialAccountMapper).insert(any(SocialAccount.class));
          }
      }

      @Nested
      @DisplayName("사용자 조회")
      class FindUserTest {

          @Test
          @DisplayName("이메일로 사용자 조회 성공")
          void findByEmail_Success() {
              // given
              String email = "test@example.com";
              User user = User.builder().userId(1L).email(email).build();

              when(userMapper.selectByEmail(email)).thenReturn(Optional.of(user));

              // when
              Optional<User> result = userService.findByEmail(email);

              // then
              assertThat(result).isPresent();
              assertThat(result.get().getEmail()).isEqualTo(email);
          }

          @Test
          @DisplayName("이메일로 사용자 조회 실패")
          void findByEmail_NotFound() {
              // given
              String email = "notfound@example.com";
              when(userMapper.selectByEmail(email)).thenReturn(Optional.empty());

              // when
              Optional<User> result = userService.findByEmail(email);

              // then
              assertThat(result).isEmpty();
          }

          @Test
          @DisplayName("ID로 사용자 조회 성공")
          void findById_Success() {
              // given
              Long userId = 1L;
              User user = User.builder().userId(userId).email("test@example.com").build();

              when(userMapper.selectById(userId)).thenReturn(Optional.of(user));

              // when
              User result = userService.findById(userId);

              // then
              assertThat(result).isNotNull();
              assertThat(result.getUserId()).isEqualTo(userId);
          }

          @Test
          @DisplayName("ID로 사용자 조회 실패 시 예외 발생")
          void findById_ThrowsException_WhenNotFound() {
              // given
              Long userId = 999L;
              when(userMapper.selectById(userId)).thenReturn(Optional.empty());

              // when & then
              assertThatThrownBy(() -> userService.findById(userId))
                      .isInstanceOf(BusinessException.class)
                      .hasMessageContaining("사용자를 찾을 수 없습니다");
          }

          @Test
          @DisplayName("소셜 계정으로 사용자 조회 성공")
          void findBySocialAccount_Success() {
              // given
              String socialId = "123456789";
              SocialAccount.SocialType socialType = SocialAccount.SocialType.KAKAO;
              Long userId = 1L;

              SocialAccount socialAccount =
                      SocialAccount.builder()
                              .socialId(socialId)
                              .socialType(socialType)
                              .userId(userId)
                              .build();

              User user = User.builder().userId(userId).email("test@example.com").build();

              when(socialAccountMapper.selectBySocialIdAndSocialType(socialId, socialType))
                      .thenReturn(Optional.of(socialAccount));
              when(userMapper.selectById(userId)).thenReturn(Optional.of(user));

              // when
              Optional<User> result = userService.findBySocialAccount(socialId, socialType);

              // then
              assertThat(result).isPresent();
              assertThat(result.get().getUserId()).isEqualTo(userId);
          }

          @Test
          @DisplayName("소셜 계정으로 사용자 조회 실패")
          void findBySocialAccount_NotFound() {
              // given
              String socialId = "notfound";
              SocialAccount.SocialType socialType = SocialAccount.SocialType.KAKAO;

              when(socialAccountMapper.selectBySocialIdAndSocialType(socialId, socialType))
                      .thenReturn(Optional.empty());

              // when
              Optional<User> result = userService.findBySocialAccount(socialId, socialType);

              // then
              assertThat(result).isEmpty();
          }
      }

      @Nested
      @DisplayName("사용자 정보 업데이트")
      class UpdateUserTest {

          @Test
          @DisplayName("사용자 정보 업데이트 성공")
          void updateUser_Success() {
              // given
              Long userId = 1L;
              String newNickname = "새닉네임";
              String newProfileImageUrl = "http://example.com/new-profile.jpg";

              User existingUser =
                      User.builder()
                              .userId(userId)
                              .email("test@example.com")
                              .nickname("기존닉네임")
                              .build();

              when(userMapper.selectById(userId)).thenReturn(Optional.of(existingUser));
              when(userMapper.existsByNickname(newNickname)).thenReturn(false);

              // when
              User result = userService.updateUser(userId, newNickname, newProfileImageUrl);

              // then
              assertThat(result).isNotNull();
              verify(userMapper)
                      .update(
                              argThat(
                                      user ->
                                              user.getUserId().equals(userId)
                                                      && user.getNickname().equals(newNickname)
                                                      && user.getProfileImgUrl()
                                                              .equals(newProfileImageUrl)));
          }

          @Test
          @DisplayName("닉네임 중복 시 예외 발생")
          void updateUser_ThrowsException_WhenNicknameDuplicated() {
              // given
              Long userId = 1L;
              String duplicatedNickname = "중복닉네임";

              User existingUser = User.builder().userId(userId).nickname("현재닉네임").build();

              when(userMapper.selectById(userId)).thenReturn(Optional.of(existingUser));
              when(userMapper.existsByNickname(duplicatedNickname)).thenReturn(true);

              // when & then
              assertThatThrownBy(() -> userService.updateUser(userId, duplicatedNickname, null))
                      .isInstanceOf(BusinessException.class)
                      .hasMessageContaining("이미 사용 중인 닉네임입니다");
          }

          @Test
          @DisplayName("사용자를 찾을 수 없을 때 예외 발생")
          void updateUser_ThrowsException_WhenUserNotFound() {
              // given
              Long userId = 999L;
              when(userMapper.selectById(userId)).thenReturn(Optional.empty());

              // when & then
              assertThatThrownBy(() -> userService.updateUser(userId, "닉네임", null))
                      .isInstanceOf(BusinessException.class)
                      .hasMessageContaining("사용자를 찾을 수 없습니다");
          }
      }
}
