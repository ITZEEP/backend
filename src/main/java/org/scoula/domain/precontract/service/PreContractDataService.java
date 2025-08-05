package org.scoula.domain.precontract.service;

import org.scoula.domain.chat.dto.ai.ClauseImproveRequestDto;

public interface PreContractDataService {

      /**
       * contractChatId를 통해 Owner 데이터를 조회합니다.
       *
       * @param contractChatId 계약 채팅 ID
       * @return Owner 데이터
       */
      ClauseImproveRequestDto.OwnerData fetchOwnerData(Long contractChatId);

      /**
       * contractChatId를 통해 Tenant 데이터를 조회합니다.
       *
       * @param contractChatId 계약 채팅 ID
       * @return Tenant 데이터
       */
      ClauseImproveRequestDto.TenantData fetchTenantData(Long contractChatId);

      /**
       * contractChatId를 통해 OCR 데이터를 조회합니다.
       *
       * @param contractChatId 계약 채팅 ID
       * @return OCR 데이터
       */
      ClauseImproveRequestDto.OcrData fetchOcrData(Long contractChatId);
}
