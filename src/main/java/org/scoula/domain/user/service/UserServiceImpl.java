package org.scoula.domain.user.service;

import java.util.Optional;

import org.scoula.domain.mypage.service.ProfileImageService;
import org.scoula.domain.user.mapper.SocialAccountMapper;
import org.scoula.domain.user.mapper.UserMapper;
import org.scoula.domain.user.vo.SocialAccount;
import org.scoula.domain.user.vo.User;
import org.scoula.global.auth.util.PasswordUtil;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.CommonErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * 사용자 서비스
 *
 * <p>사용자 관련 비즈니스 로직을 처리하는 서비스입니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Log4j2
@Transactional(readOnly = true)
public class UserServiceImpl implements UserServiceInterface {

      private final UserMapper userMapper;
      private final SocialAccountMapper socialAccountMapper;
      private final ProfileImageService profileImageService;

      /** {@inheritDoc} */
      @Override
      @Transactional
      public User registerOrUpdateOAuth2User(
              String socialId,
              SocialAccount.SocialType socialType,
              String email,
              String nickname,
              String profileImageUrl,
              String genderStr) {

          log.info(
                  "OAuth2 사용자 등록/업데이트 시작 - Provider: {}, ID: {}, Email: {}, ProfileImageUrl: {},"
                          + " Gender: {}",
                  socialType,
                  socialId,
                  email,
                  profileImageUrl,
                  genderStr);

          // Gender 변환 (카카오는 male/female로 제공, User enum은 MALE/FEMALE)
          User.Gender gender = null;
          if (genderStr != null) {
              if ("male".equalsIgnoreCase(genderStr)) {
                  gender = User.Gender.MALE;
              } else if ("female".equalsIgnoreCase(genderStr)) {
                  gender = User.Gender.FEMALE;
              }
          }

          // 1. 소셜 계정으로 기존 사용자 확인
          Optional<SocialAccount> existingSocialAccount =
                  socialAccountMapper.selectBySocialIdAndSocialType(socialId, socialType);

          if (existingSocialAccount.isPresent()) {
              // 기존 사용자는 정보 업데이트 없이 그대로 반환
              Long userId = existingSocialAccount.get().getUserId();
              User user = findById(userId);
              log.info("기존 사용자 로그인 - ID: {}, 정보 업데이트 하지 않음", user.getUserId());
              return user;
          }

          // 2. 이메일로 기존 사용자 확인 (다른 소셜 계정으로 가입된 경우)
          Optional<User> emailUser = userMapper.selectByEmail(email);
          if (emailUser.isPresent()) {
              // 기존 사용자에 새로운 소셜 계정 연결
              User user = emailUser.get();
              SocialAccount newSocialAccount =
                      SocialAccount.builder()
                              .socialId(socialId)
                              .socialType(socialType)
                              .userId(user.getUserId())
                              .build();
              socialAccountMapper.insert(newSocialAccount);

              // 프로필 이미지를 S3에 업로드
              String s3ProfileImageUrl = profileImageUrl;
              if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                  try {
                      String uploadedUrl =
                              profileImageService.uploadProfileImageFromUrl(
                                      profileImageUrl, user.getUserId());
                      if (uploadedUrl != null) {
                          s3ProfileImageUrl = uploadedUrl;
                          log.info("기존 사용자(이메일 연결) 프로필 이미지 S3 업로드 성공: {}", s3ProfileImageUrl);
                      } else {
                          log.warn("프로필 이미지 S3 업로드 실패, 기본 이미지 URL 사용: {}", profileImageUrl);
                      }
                  } catch (Exception e) {
                      log.error("프로필 이미지 업로드 중 오류 발생", e);
                  }
              }

              // 사용자 정보 업데이트
              User updateUser =
                      User.builder()
                              .userId(user.getUserId())
                              .nickname(nickname)
                              .profileImgUrl(s3ProfileImageUrl)
                              .gender(gender)
                              .build();
              userMapper.update(updateUser);
              log.info("사용자 정보 업데이트 - 닉네임: {}, 프로필: {}", nickname, s3ProfileImageUrl);

              // 업데이트된 사용자 정보 다시 조회
              user = findById(user.getUserId());
              log.info(
                      "기존 사용자에 새로운 소셜 계정 연결 완료 - User ID: {}, Social Type: {}",
                      user.getUserId(),
                      socialType);
              return user;
          }

          // 3. 닉네임 중복 확인 및 유니크 닉네임 생성
          String uniqueNickname = generateUniqueNickname(nickname);

          // 4. social_id를 기반으로 암호화된 비밀번호 생성
          String encryptedPassword = PasswordUtil.generateOAuth2Password(socialId);
          log.info("OAuth2 사용자 비밀번호 생성 완료");

          // 5. 프로필 이미지를 먼저 S3에 업로드 (사용자 ID가 필요하므로 임시로 원본 URL 사용)
          User newUser =
                  User.createOAuth2User(
                          email, uniqueNickname, profileImageUrl, encryptedPassword, gender);
          userMapper.insert(newUser);
          log.info("신규 사용자 생성 완료 - User ID: {}", newUser.getUserId());

          // 6. 프로필 이미지를 S3에 업로드하고 DB 업데이트
          if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
              try {
                  String s3ProfileImageUrl =
                          profileImageService.uploadProfileImageFromUrl(
                                  profileImageUrl, newUser.getUserId());

                  if (s3ProfileImageUrl != null) {
                      log.info("프로필 이미지 S3 업로드 성공: {}", s3ProfileImageUrl);

                      // S3 URL로 사용자 프로필 이미지 업데이트
                      User updateUser =
                              User.builder()
                                      .userId(newUser.getUserId())
                                      .profileImgUrl(s3ProfileImageUrl)
                                      .build();
                      userMapper.update(updateUser);

                      // 업데이트된 정보로 다시 조회
                      newUser = findById(newUser.getUserId());
                      log.info("프로필 이미지 URL 업데이트 완료 - URL: {}", newUser.getProfileImgUrl());
                  } else {
                      log.warn("프로필 이미지 S3 업로드 실패, 원본 URL 유지: {}", profileImageUrl);
                  }
              } catch (Exception e) {
                  log.error("프로필 이미지 처리 중 오류 발생", e);
                  // 프로필 이미지 업로드 실패해도 회원가입은 계속 진행
              }
          }

          // 6. 소셜 계정 정보 등록
          SocialAccount socialAccount =
                  SocialAccount.builder()
                          .socialId(socialId)
                          .socialType(socialType)
                          .userId(newUser.getUserId())
                          .build();
          socialAccountMapper.insert(socialAccount);

          log.info(
                  "신규 사용자 등록 완료 - ID: {}, Email: {}, Social Type: {}",
                  newUser.getUserId(),
                  newUser.getEmail(),
                  socialType);

          return newUser;
      }

      /** {@inheritDoc} */
      @Override
      public Optional<User> findByEmail(String email) {
          return userMapper.selectByEmail(email);
      }

      /** {@inheritDoc} */
      @Override
      public User findById(Long userId) {
          return userMapper
                  .selectById(userId)
                  .orElseThrow(
                          () ->
                                  new BusinessException(
                                          CommonErrorCode.ENTITY_NOT_FOUND,
                                          "사용자를 찾을 수 없습니다: " + userId));
      }

      /** {@inheritDoc} */
      @Override
      public Optional<User> findBySocialAccount(
              String socialId, SocialAccount.SocialType socialType) {
          Optional<SocialAccount> socialAccount =
                  socialAccountMapper.selectBySocialIdAndSocialType(socialId, socialType);

          if (socialAccount.isPresent()) {
              return Optional.of(findById(socialAccount.get().getUserId()));
          }
          return Optional.empty();
      }

      /** {@inheritDoc} */
      @Override
      @Transactional
      public User updateUser(Long userId, String nickname, String profileImageUrl) {
          User user = findById(userId);

          // 닉네임 변경 시 중복 확인
          if (nickname != null && !nickname.equals(user.getNickname())) {
              if (userMapper.existsByNickname(nickname)) {
                  throw new BusinessException(
                          CommonErrorCode.DUPLICATE_KEY_ERROR, "이미 사용 중인 닉네임입니다: " + nickname);
              }
          }

          User updateUser =
                  User.builder()
                          .userId(userId)
                          .nickname(nickname)
                          .profileImgUrl(profileImageUrl)
                          .build();
          userMapper.update(updateUser);

          log.info("사용자 정보 업데이트 완료 - ID: {}", userId);

          return findById(userId);
      }

      /**
       * 유니크한 닉네임 생성
       *
       * @param baseNickname 기본 닉네임
       * @return 유니크한 닉네임
       */
      private String generateUniqueNickname(String baseNickname) {
          if (baseNickname == null || baseNickname.isBlank()) {
              baseNickname = "사용자";
          }

          String nickname = baseNickname;
          int counter = 1;

          while (userMapper.existsByNickname(nickname)) {
              nickname = baseNickname + counter;
              counter++;
          }

          return nickname;
      }
}
