package org.scoula.global.common.util;

import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

// @Component, @Log4j2 는 유틸에선 보통 불필요해서 제거했습니다.
// 필요하면 남겨도 되지만, 주입받을 일 없으면 빼는 게 좋아요.
@Component
@Log4j2
public final class NumberFormatUtil {

      // === 상수 정의 ===
      private static final String[] KOREAN_DIGITS = {"", "일", "이", "삼", "사", "오", "육", "칠", "팔", "구"};
      private static final String[] KOREAN_UNITS = {"", "십", "백", "천"};
      private static final String[] KOREAN_BIG_UNITS = {"", "만", "억", "조", "경"};

      /** 1억 2천 3만 형태의 짧은 표기 */
      public String formatWonShort(int amount) {
          if (amount == 0) return "0원";
          long eok = amount / 100_000_000; // 억
          long man = (amount % 100_000_000) / 10_000; // 만원 단위

          StringBuilder sb = new StringBuilder();
          if (eok > 0) {
              sb.append(eok).append("억");
              long cheon = man / 1000; // 천만원 단위
              long remainMan = man % 1000;
              if (cheon > 0) sb.append(" ").append(cheon).append("천");
              if (cheon == 0 && remainMan > 0) sb.append(" ").append(remainMan).append("만");
              sb.append("원");
          } else {
              if (man >= 1000) {
                  long cheon = man / 1000;
                  long remainMan = man % 1000;
                  sb.append(cheon).append("천");
                  if (remainMan > 0) sb.append(" ").append(remainMan).append("만");
                  sb.append("원");
              } else {
                  sb.append(man).append("만원");
              }
          }
          return sb.toString().replaceAll("\\s+", " ");
      }

      // =======================
      // 숫자만 한글로 (예: 19091 -> "일만구천구십일")
      public String toKoreanNumber(int amount) {
          if (amount == 0) return "영";
          StringBuilder result = new StringBuilder();
          int unitPos = 0; // 만/억/조/경 단위 인덱스
          while (amount > 0) {
              int chunk = amount % 10000; // 4자리 묶음
              if (chunk > 0) {
                  String chunkText = convertChunkNatural(chunk);
                  // 큰 단위 붙이기
                  if (!KOREAN_BIG_UNITS[unitPos].isEmpty()) {
                      if (chunk == 1) {
                          // 정확히 1묶음이면 '일만', '일억'처럼 '일' 명시
                          chunkText = "일" + KOREAN_BIG_UNITS[unitPos];
                      } else {
                          chunkText += KOREAN_BIG_UNITS[unitPos];
                      }
                  }
                  result.insert(0, chunkText);
              }
              amount /= 10000;
              unitPos++;
          }
          return result.toString();
      }

      /** 0~9999를 자연스러운 한글로 변환 (십/백/천 자리에 '일'은 생략) */
      private String convertChunkNatural(int n) {
          StringBuilder sb = new StringBuilder();
          for (int i = 0; i < 4; i++) {
              int digit = n % 10;
              if (digit > 0) {
                  String digitText = KOREAN_DIGITS[digit];
                  // 십/백/천 자리에서는 '일' 생략 (예: 일십 -> 십, 일백 -> 백, 일천 -> 천)
                  if (i > 0 && digit == 1) digitText = "";
                  sb.insert(0, digitText + KOREAN_UNITS[i]);
              }
              n /= 10;
          }
          return sb.toString();
      }
}
