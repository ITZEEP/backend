package org.scoula.domain.user.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.user.vo.SocialAccount;

/**
 * 소셜 계정 매퍼
 *
 * <p>소셜 계정 데이터베이스 접근을 담당하는 매퍼입니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Mapper
public interface SocialAccountMapper {

      /**
       * 소셜 계정 등록
       *
       * @param socialAccount 소셜 계정 정보
       * @return 처리된 행 수
       */
      int insert(SocialAccount socialAccount);

      /**
       * 소셜 ID와 소셜 타입으로 계정 조회
       *
       * @param socialId 소셜 ID
       * @param socialType 소셜 타입
       * @return 소셜 계정 정보
       */
      Optional<SocialAccount> selectBySocialIdAndSocialType(
              @Param("socialId") String socialId,
              @Param("socialType") SocialAccount.SocialType socialType);

      /**
       * 사용자 ID로 소셜 계정 조회
       *
       * @param userId 사용자 ID
       * @return 소셜 계정 정보
       */
      Optional<SocialAccount> selectByUserId(@Param("userId") Long userId);

      /**
       * 사용자 ID와 소셜 타입으로 소셜 계정 조회
       *
       * @param userId 사용자 ID
       * @param socialType 소셜 타입
       * @return 소셜 계정 정보
       */
      Optional<SocialAccount> selectByUserIdAndSocialType(
              @Param("userId") Long userId, @Param("socialType") SocialAccount.SocialType socialType);
}
