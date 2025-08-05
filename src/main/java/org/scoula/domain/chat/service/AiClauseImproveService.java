package org.scoula.domain.chat.service;

import org.scoula.domain.chat.dto.ai.ClauseImproveRequestDto;
import org.scoula.domain.chat.dto.ai.ClauseImproveResponseDto;

public interface AiClauseImproveService {

      /**
       * AI 서버에 특약 개선을 요청합니다.
       *
       * @param request 특약 개선 요청 데이터
       * @return 개선된 특약 데이터
       */
      ClauseImproveResponseDto improveClause(ClauseImproveRequestDto request);
}
