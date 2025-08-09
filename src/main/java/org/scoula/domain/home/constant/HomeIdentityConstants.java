package org.scoula.domain.home.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 매물 본인 인증 관련 상수
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HomeIdentityConstants {

      // 주민등록번호 관련 상수
      public static final int SSN_FRONT_LENGTH = 6;
      public static final int SSN_BACK_LENGTH = 7;
      public static final String SSN_FRONT_PATTERN = "^\\d{6}$";
      public static final String SSN_BACK_PATTERN = "^\\d{7}$";
      public static final String SSN_MASK = "*";
      public static final String SSN_FRONT_MASK = "******";
      public static final String SSN_BACK_MASK = "*******";

      // 발급일자 관련 상수
      public static final int ISSUED_DATE_LENGTH = 8;
      public static final String ISSUED_DATE_PATTERN = "^\\d{8}$";

      // 성별 코드로 세기 판별
      public static final class GenderCode {
          // 1900년대 출생
          public static final int MALE_1900 = 1;
          public static final int FEMALE_1900 = 2;
          public static final int MALE_1900_FOREIGN = 5;
          public static final int FEMALE_1900_FOREIGN = 6;

          // 2000년대 출생
          public static final int MALE_2000 = 3;
          public static final int FEMALE_2000 = 4;
          public static final int MALE_2000_FOREIGN = 7;
          public static final int FEMALE_2000_FOREIGN = 8;

          // 1800년대 출생
          public static final int MALE_1800 = 9;
          public static final int FEMALE_1800 = 0;
      }

      // 세기 값
      public static final int CENTURY_1800 = 1800;
      public static final int CENTURY_1900 = 1900;
      public static final int CENTURY_2000 = 2000;

      // 이름 마스킹 관련
      public static final int MIN_NAME_LENGTH = 2;

      // 날짜 포맷
      public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
      public static final String YEAR_FORMAT = "%02d";
      public static final String MONTH_FORMAT = "%02d";

      // 로그 메시지
      public static final String LOG_VERIFY_START = "매물 본인 인증 시작 - homeId: {}, userId: {}";
      public static final String LOG_VERIFY_SUCCESS = "신분증 진위 확인 성공";
      public static final String LOG_VERIFY_FAIL = "신분증 진위 확인 실패 - name: {}";
      public static final String LOG_UPDATE_SUCCESS = "매물 본인 인증 정보 업데이트 완료 - homeId: {}, userId: {}";
      public static final String LOG_INSERT_SUCCESS = "매물 본인 인증 정보 신규 저장 완료 - homeId: {}, userId: {}";
      public static final String LOG_GET_INFO = "매물 본인 인증 정보 조회 - homeId: {}";

      // API 응답 메시지
      public static final String MSG_VERIFY_SUCCESS = "매물 본인 인증이 성공적으로 완료되었습니다";
      public static final String MSG_GET_SUCCESS = "매물 본인 인증 정보 조회 성공";
}
