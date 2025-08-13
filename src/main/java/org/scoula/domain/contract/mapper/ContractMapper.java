package org.scoula.domain.contract.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.contract.dto.ContractDTO;

@Mapper
public interface ContractMapper {

      Long getOwnerId(@Param("contractChatId") Long contractChatId);
      Long getBuyerId (@Param("contractChatId") Long contractChatId);

      ContractDTO getContract(@Param("contractChatId") Long contractChatId);

      String getDuration(@Param("contractChatId") Long contractChatId);

      int updateStatus(@Param("contractChatId") Long contractChatId, @Param("step") int step);

      Long selectFinalContractId(@Param("contractChatId") Long contractChatId);

      int insertSignatureInit(@Param("contractChatId") Long contractChatId);

      int updateTaxSignature(
              @Param("finalContractId") Long finalContractId,
              @Param("url") String url,
              @Param("hashKey") String hashKey);

      int insertFinalContract(@Param("contractChatId") Long contractChatId);

      String selectOwnerTaxSignatureUrl(@Param("finalContractId") Long finalContractId);

      boolean getDepositAdjustment(@Param("contractChatId") Long contractChatId);
}
