package org.scoula.domain.fraud.enums;

/** 사기 위험도 분석 타입 */
public enum AnalysisType {
      FULL("전체 분석", "매물 정보와 함께 DB 저장"),
      QUICK("빠른 분석", "매물 정보 없이 임시 분석"),
      UPDATE("업데이트", "기존 분석 업데이트");

      private final String title;
      private final String description;

      AnalysisType(String title, String description) {
          this.title = title;
          this.description = description;
      }

      public String getTitle() {
          return title;
      }

      public String getDescription() {
          return description;
      }
}
