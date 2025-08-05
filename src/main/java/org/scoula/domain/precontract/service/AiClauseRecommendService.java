package org.scoula.domain.precontract.service;

import org.scoula.domain.precontract.dto.ai.ClauseRecommendRequestDto;
import org.scoula.domain.precontract.dto.ai.ClauseRecommendResponseDto;

public interface AiClauseRecommendService {
      ClauseRecommendResponseDto recommendClauses(ClauseRecommendRequestDto request);
}
