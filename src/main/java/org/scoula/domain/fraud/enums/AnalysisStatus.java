package org.scoula.domain.fraud.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnalysisStatus {
      PENDING("대기중", "분석을 시작하지 않았습니다"),
      IN_PROGRESS("진행중", "분석이 진행 중입니다"),
      SUCCESS("성공", "분석이 성공적으로 완료되었습니다"),
      FAILED("실패", "분석 중 오류가 발생했습니다");

      private final String displayName;
      private final String description;
}
