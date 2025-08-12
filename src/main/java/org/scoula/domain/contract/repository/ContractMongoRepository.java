package org.scoula.domain.contract.repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.scoula.domain.chat.document.FinalSpecialContractDocument;
import org.scoula.domain.contract.document.ContractMongoDocument;
import org.scoula.domain.contract.dto.ContractDTO;
import org.scoula.domain.contract.dto.PaymentDTO;
import org.scoula.domain.contract.dto.SpecialContractUpdateDTO;
import org.scoula.domain.contract.exception.ContractException;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class ContractMongoRepository {

      @Autowired private MongoTemplate mongoTemplate;

      public ContractMongoDocument saveContractMongo(ContractDTO dto, LocalDate contractEndDate) {
          ContractMongoDocument document = ContractMongoDocument.toDocument(dto, contractEndDate);
          return mongoTemplate.insert(document);
      }

      public ContractMongoDocument getContract(Long contractChatId) {
          Query contractQuery = new Query(Criteria.where("contractChatId").is(contractChatId));
          ContractMongoDocument document =
                  mongoTemplate.findOne(contractQuery, ContractMongoDocument.class);

          return document;
      }

      public ContractMongoDocument getDepositPrice(Long contractChatId) {
          //        ContractMongoDocument document = mongoTemplate.findById(contractChatId,
          // ContractMongoDocument.class);
          Query contractQuery = new Query(Criteria.where("contractChatId").is(contractChatId));
          ContractMongoDocument document =
                  mongoTemplate.findOne(contractQuery, ContractMongoDocument.class);
          return document;
      }

      public void updateDepositPrice(Long contractChatId, PaymentDTO dto) {
          Query contractQuery = new Query(Criteria.where("contractChatId").is(contractChatId));
          ContractMongoDocument document =
                  mongoTemplate.findOne(contractQuery, ContractMongoDocument.class);

          if (document == null) {
              throw new BusinessException(ContractException.CONTRACT_GET);
          }

          document.setDepositPrice(dto.getDepositPrice());
          document.setMonthlyRent(dto.getMonthlyRent());

          mongoTemplate.save(document);
      }

      public void saveSpecialContract(Long contractChatId) {
          // Step 1: 특약서 조회
          Query query = new Query(Criteria.where("contractChatId").is(contractChatId));
          FinalSpecialContractDocument specialDoc =
                  mongoTemplate.findOne(query, FinalSpecialContractDocument.class);

          if (specialDoc == null
                  || specialDoc.getFinalClauses() == null
                  || specialDoc.getFinalClauses().isEmpty()) {
              throw new BusinessException(ContractException.CONTRACT_GET);
          }

          // Step 2: 기존 계약서 가져오기
          Query contractQuery = new Query(Criteria.where("contractChatId").is(contractChatId));
          ContractMongoDocument contractDoc =
                  mongoTemplate.findOne(contractQuery, ContractMongoDocument.class);

          if (contractDoc == null) {
              throw new BusinessException(ContractException.CONTRACT_GET);
          }

          // Step 3: 특약사항 매핑
          List<FinalSpecialContractDocument.FinalClause> finalClauses = specialDoc.getFinalClauses();
          finalClauses.sort(Comparator.comparing(FinalSpecialContractDocument.FinalClause::getOrder));

          List<ContractMongoDocument.SpecialContract> specialClauses =
                  finalClauses.stream()
                          .map(
                                  fc ->
                                          ContractMongoDocument.SpecialContract.builder()
                                                  .order(fc.getOrder() + 1)
                                                  .title(fc.getTitle())
                                                  .content(fc.getContent())
                                                  .build())
                          .collect(Collectors.toList());

          contractDoc.setSpecialContracts(specialClauses);

          // Step 4: 저장
          mongoTemplate.save(contractDoc);
      }

      public void updateSpecialContract(Long contractChatId, SpecialContractUpdateDTO dto) {
          Query query = new Query(Criteria.where("contractChatId").is(contractChatId));
          ContractMongoDocument document = mongoTemplate.findOne(query, ContractMongoDocument.class);

          if (document == null) {
              throw new BusinessException(ContractException.CONTRACT_GET);
          }

          List<ContractMongoDocument.SpecialContract> existingClauses =
                  document.getSpecialContracts();
          if (existingClauses == null || existingClauses.isEmpty()) {
              throw new BusinessException(ContractException.CONTRACT_GET, "특약사항이 존재하지 않습니다.");
          }

          List<SpecialContractUpdateDTO.SpecialClauseDTO> newClauses = dto.getSpecialClauses();
          if (newClauses == null || newClauses.isEmpty()) return;

          for (SpecialContractUpdateDTO.SpecialClauseDTO newClause : newClauses) {
              Integer order = newClause.getOrder();
              if (order != null && order >= 0 && order < existingClauses.size()) {
                  ContractMongoDocument.SpecialContract target = existingClauses.get(order);
                  if (newClause.getContent() != null) {
                      target.setContent(newClause.getContent());
                  }
                  if (newClause.getTitle() != null) {
                      target.setTitle(newClause.getTitle());
                  }
              }
          }

          document.setSpecialContracts(existingClauses);
          mongoTemplate.save(document);
      }
}
