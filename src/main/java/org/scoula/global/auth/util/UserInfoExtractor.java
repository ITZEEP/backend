package org.scoula.global.auth.util;

import org.scoula.domain.user.mapper.UserMapper;
import org.scoula.domain.user.vo.User;
import org.springframework.stereotype.Component;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * 사용자 정보 추출 유틸리티
 *
 * <p>username으로부터 사용자 정보를 추출하는 공통 로직을 제공합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class UserInfoExtractor {

      private final UserMapper userMapper;

      /**
       * username으로부터 사용자 정보 추출
       *
       * @param username 사용자명 (이메일 또는 user ID)
       * @return 추출된 사용자 정보
       */
      public UserInfo extractUserInfo(String username) {
          User user = null;

          // 먼저 이메일로 조회 시도
          if (username.contains("@")) {
              user = userMapper.selectByEmail(username).orElse(null);
              if (user != null) {
                  log.debug("사용자를 이메일로 찾음: {}", username);
              }
          }

          // 이메일로 찾지 못한 경우 user ID로 조회 시도
          if (user == null) {
              try {
                  Long userId = Long.parseLong(username);
                  user = userMapper.selectById(userId).orElse(null);
                  if (user != null) {
                      log.debug("사용자를 ID로 찾음: {}", userId);
                  }
              } catch (NumberFormatException e) {
                  log.warn("Username은 유효한 이메일이나 user ID가 아님: {}", username);
              }
          }

          String genderStr = null;
          if (user != null && user.getGender() != null) {
              genderStr = user.getGender().name();
          }

          return UserInfo.builder().user(user).genderStr(genderStr).build();
      }

      /** 사용자 정보 DTO */
      @Getter
      @Builder
      public static class UserInfo {
          private final User user;
          private final String genderStr;

          public String getEmail() {
              return user != null ? user.getEmail() : null;
          }

          public String getNickname() {
              return user != null ? user.getNickname() : null;
          }

          public String getProfileImageUrl() {
              return user != null ? user.getProfileImgUrl() : null;
          }
      }
}
