package org.scoula.domain.user.service;

import java.util.Optional;

import org.scoula.domain.user.vo.SocialAccount;
import org.scoula.domain.user.vo.User;

/**
 * 사용자 서비스 인터페이스
 *
 * <p>사용자 관련 비즈니스 로직의 계약을 정의합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
public interface UserServiceInterface {

      /**
       * OAuth2 사용자 등록 또는 업데이트
       *
       * @param socialId 소셜 ID
       * @param socialType 소셜 타입
       * @param email 이메일
       * @param nickname 닉네임
       * @param profileImageUrl 프로필 이미지 URL
       * @param genderStr 성별 문자열 (male/female)
       * @return 등록/업데이트된 사용자 정보
       */
      User registerOrUpdateOAuth2User(
              String socialId,
              SocialAccount.SocialType socialType,
              String email,
              String nickname,
              String profileImageUrl,
              String genderStr);

      /**
       * 이메일로 사용자 조회
       *
       * @param email 이메일
       * @return 사용자 정보
       */
      Optional<User> findByEmail(String email);

      /**
       * 사용자 ID로 조회
       *
       * @param userId 사용자 ID
       * @return 사용자 정보
       */
      User findById(Long userId);

      /**
       * 소셜 계정으로 사용자 조회
       *
       * @param socialId 소셜 ID
       * @param socialType 소셜 타입
       * @return 사용자 정보
       */
      Optional<User> findBySocialAccount(String socialId, SocialAccount.SocialType socialType);

      /**
       * 사용자 정보 업데이트
       *
       * @param userId 사용자 ID
       * @param nickname 닉네임
       * @param profileImageUrl 프로필 이미지 URL
       * @return 업데이트된 사용자 정보
       */
      User updateUser(Long userId, String nickname, String profileImageUrl);
}
