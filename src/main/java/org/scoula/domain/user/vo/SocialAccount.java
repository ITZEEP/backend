package org.scoula.domain.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 소셜 계정 VO
 *
 * <p>social_account 테이블과 매핑되는 VO입니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialAccount {

      private String socialId;
      private SocialType socialType;
      private Long userId;

      /** 소셜 타입 열거형 */
      public enum SocialType {
          KAKAO,
          GOOGLE,
          NAVER
      }
}
