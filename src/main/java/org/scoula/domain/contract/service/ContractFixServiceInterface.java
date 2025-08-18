package org.scoula.domain.contract.service;

import org.scoula.domain.contract.dto.LegalityDTO;

public interface ContractFixServiceInterface {

      Void saveSpecialContract(Long contractChatId, Long userId);

      LegalityDTO getLegality(Long contractChatId, Long userId);
}
