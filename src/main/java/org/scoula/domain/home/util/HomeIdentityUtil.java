package org.scoula.domain.home.util;

import static org.scoula.domain.home.constant.HomeIdentityConstants.*;
import static org.scoula.domain.home.constant.HomeIdentityConstants.GenderCode.*;

import java.time.LocalDate;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 매물 본인 인증 관련 유틸리티 클래스
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HomeIdentityUtil {

      /**
       * 주민등록번호에서 생년월일 추출
       *
       * @param ssnFront 주민등록번호 앞자리 (6자리)
       * @param ssnBack 주민등록번호 뒷자리 (7자리)
       * @return 추출된 생년월일
       */
      public static LocalDate extractBirthDateFromSSN(String ssnFront, String ssnBack) {
          // 주민등록번호 뒷자리 첫번째 숫자로 세기 판단
          int genderDigit = Integer.parseInt(ssnBack.substring(0, 1));
          int century = determineCentury(genderDigit);

          int year = century + Integer.parseInt(ssnFront.substring(0, 2));
          int month = Integer.parseInt(ssnFront.substring(2, 4));
          int day = Integer.parseInt(ssnFront.substring(4, 6));

          return LocalDate.of(year, month, day);
      }

      /**
       * 성별 코드로 세기 판단
       *
       * @param genderDigit 주민등록번호 뒷자리 첫번째 숫자
       * @return 세기 (1800, 1900, 2000)
       */
      private static int determineCentury(int genderDigit) {
          switch (genderDigit) {
              case MALE_1900:
              case FEMALE_1900:
              case MALE_1900_FOREIGN:
              case FEMALE_1900_FOREIGN:
                  return CENTURY_1900;
              case MALE_2000:
              case FEMALE_2000:
              case MALE_2000_FOREIGN:
              case FEMALE_2000_FOREIGN:
                  return CENTURY_2000;
              default:
                  return CENTURY_1800;
          }
      }

      /**
       * 이름 마스킹 처리
       *
       * @param name 원본 이름
       * @return 마스킹된 이름
       */
      public static String maskName(String name) {
          if (name == null || name.length() < MIN_NAME_LENGTH) {
              return name;
          }
          if (name.length() == MIN_NAME_LENGTH) {
              return name.charAt(0) + SSN_MASK;
          }
          return name.charAt(0) + SSN_MASK.repeat(name.length() - 2) + name.charAt(name.length() - 1);
      }

      /**
       * 주민등록번호 앞자리 마스킹 (생년월일 기반)
       *
       * @param birthDate 생년월일
       * @return 마스킹된 주민등록번호 앞자리
       */
      public static String maskSSNFront(LocalDate birthDate) {
          if (birthDate == null) {
              return SSN_FRONT_MASK;
          }
          String yearStr = String.format(YEAR_FORMAT, birthDate.getYear() % 100);
          String monthStr = String.format(MONTH_FORMAT, birthDate.getMonthValue());
          return yearStr + monthStr + "**";
      }

      /**
       * 주민등록번호 뒷자리 마스킹
       *
       * @return 마스킹된 주민등록번호 뒷자리
       */
      public static String maskSSNBack() {
          return SSN_BACK_MASK;
      }
}
