package org.scoula.global.common.constant;

/** 애플리케이션 전역 상수 정의 */
public class Constants {

      private Constants() {
          // 상수 클래스는 인스턴스화 방지
      }

      /** JWT 관련 상수 */
      public static class Jwt {
          public static final long MAX_EXPIRATION_DAYS = 30L;
          public static final long DEFAULT_EXPIRATION_HOURS = 24L;
          public static final long REFRESH_EXPIRATION_DAYS = 7L;
          public static final String BEARER_PREFIX = "Bearer ";
          public static final String AUTHORIZATION_HEADER = "Authorization";
      }

      /** 파일 업로드 관련 상수 */
      public static class File {
          public static final long MAX_FILE_SIZE_MB = 50L;
          public static final long MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;
          public static final String[] ALLOWED_IMAGE_EXTENSIONS = {
              "jpg", "jpeg", "png", "gif", "bmp", "webp"
          };
          public static final String[] ALLOWED_DOCUMENT_EXTENSIONS = {"pdf"};
          public static final String UPLOAD_DIR = "uploads";
          public static final String TEMP_DIR = "temp";
      }

      /** 채팅 관련 상수 */
      public static class Chat {
          public static final int MAX_MESSAGE_LENGTH = 1000;
          public static final int MAX_USERNAME_LENGTH = 50;
          public static final String PUBLIC_TOPIC = "/topic/public";
          public static final String QUEUE_PREFIX = "/queue/";
          public static final String APP_PREFIX = "/app";
          public static final String WS_ENDPOINT = "/ws";
      }

      /** 이메일 관련 상수 */
      public static class Email {
          public static final int MAX_SUBJECT_LENGTH = 200;
          public static final int MAX_CONTENT_LENGTH = 10000;
          public static final int MAX_RECIPIENTS = 100;
          public static final String DEFAULT_CHARSET = "UTF-8";
      }

      /** 페이지네이션 관련 상수 */
      public static class Pagination {
          public static final int DEFAULT_PAGE_SIZE = 20;
          public static final int MAX_PAGE_SIZE = 100;
          public static final int DEFAULT_PAGE_NUMBER = 0;
      }

      /** 보안 관련 상수 */
      public static class Security {
          public static final int MIN_PASSWORD_LENGTH = 8;
          public static final int MAX_PASSWORD_LENGTH = 100;
          public static final int MAX_LOGIN_ATTEMPTS = 5;
          public static final long ACCOUNT_LOCK_DURATION_MINUTES = 30L;
          public static final String[] PUBLIC_URLS = {
              "/api/auth/login", "/api/auth/signup", "/api/auth/refresh", "/api/health", "/api"
          };
      }

      /** 캐시 관련 상수 */
      public static class Cache {
          public static final String USER_CACHE = "users";
          public static final String TOKEN_BLACKLIST_CACHE = "token-blacklist";
          public static final long DEFAULT_TTL_HOURS = 24L;
      }

      /** 날짜/시간 관련 상수 */
      public static class DateTime {
          public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
          public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
          public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
          public static final String TIMEZONE = "Asia/Seoul";
      }

      /** HTTP 관련 상수 */
      public static class Http {
          public static final int CONNECT_TIMEOUT_MS = 5000;
          public static final int READ_TIMEOUT_MS = 10000;
          public static final int MAX_CONNECTIONS = 100;
          public static final int MAX_CONNECTIONS_PER_ROUTE = 20;
      }

      /** 번호 형식 관련 상수 */
      public static class Number {
          public static final String PHONE = "^01[016789]-\\d{3,4}-\\d{4}$";
      }
}
