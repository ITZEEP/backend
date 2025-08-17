package org.scoula.domain.contract.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.contract.dto.ContractDTO;
import org.scoula.domain.contract.dto.DBFinalContractDTO;
import org.scoula.domain.contract.enums.SignedType;
import org.scoula.domain.contract.vo.ElectronicSignature;
import org.scoula.domain.contract.vo.FinalContract;

@Mapper
public interface ContractMapper {

      Long getOwnerId(@Param("contractChatId") Long contractChatId);

      Long getBuyerId(@Param("contractChatId") Long contractChatId);

      ContractDTO getContract(@Param("contractChatId") Long contractChatId);

      String getDuration(@Param("contractChatId") Long contractChatId);

      int updateStatus(@Param("contractChatId") Long contractChatId, @Param("step") int step);

      Long selectFinalContractId(@Param("contractChatId") Long contractChatId);

      // ==============

      int insertFinalContractInit(
              @Param("contractChatId") Long contractChatId,
              @Param("depositPrice") int depositPrice,
              @Param("monthlyRent") int monthlyRent,
              @Param("maintenanceFee") int maintenanceFee);

      int insertContract(@Param("contractChatId") Long contractChatId, @Param("s3Key") String s3Key);

      DBFinalContractDTO selectFinalContractPDF(@Param("contractChatId") Long contractChatId);

      FinalContract selectFinalContract(@Param("contractChatId") Long contractChatId);

      int insertSignature(
              @Param("contractChatId") Long contractChatId,
              @Param("s3Key") String s3Key,
              @Param("hashKey") String hashKey,
              @Param("signedType") SignedType signedType,
              @Param("userId") Long userId);

      List<ElectronicSignature> selectSignature(
              @Param("contractChatId") Long contractChatId, @Param("userId") Long userId);

      int updateFinalContract(
              @Param("contractChatId") Long contractChatId,
              @Param("contractPdfKey") String contractPdfKey,
              @Param("contractPdfHash") String contractPdfHash);

      String selectBirth(@Param("userId") Long userId);

      String selectMail(@Param("userId") Long userId);

      // ========

      String selectOwnerTaxSignatureUrl(@Param("finalContractId") Long finalContractId);

      boolean getDepositAdjustment(@Param("contractChatId") Long contractChatId);
}
