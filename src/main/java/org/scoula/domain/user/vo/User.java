package org.scoula.domain.user.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 VO
 *
 * <p>user 테이블과 매핑되는 VO입니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

      private Long userId;
      private String nickname;
      private String email;
      private String password;
      private LocalDate birthDate;
      private Gender gender;
      private String profileImgUrl;
      private LocalDateTime createdAt;
      private LocalDateTime updatedAt;
      private Role role;

      /** OAuth2 사용자 생성 (팩토리 메서드) */
      public static User createOAuth2User(
              String email, String nickname, String profileImgUrl, String password, Gender gender) {
          return User.builder()
                  .email(email)
                  .nickname(nickname)
                  .profileImgUrl(profileImgUrl)
                  .password(password)
                  .gender(gender)
                  .role(Role.ROLE_USER)
                  .build();
      }

      /** 성별 열거형 */
      public enum Gender {
          MALE,
          FEMALE
      }

      /** 사용자 권한 열거형 */
      public enum Role {
          ROLE_USER,
          ROLE_ADMIN
      }
}
