package org.scoula.domain.precontract.repository;

import org.scoula.domain.precontract.document.ContractDocumentMongoDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class ContractDocumentMongoRepository {

      @Autowired private MongoTemplate mongoTemplate;

      // 저장
      public ContractDocumentMongoDocument save(ContractDocumentMongoDocument document) {
          return mongoTemplate.save(document);
      }

      // 조회
      public ContractDocumentMongoDocument findByContractChatIdAndUserId(
              Long contractChatId, Long userId) {
          Query query = new Query();
          query.addCriteria(
                  Criteria.where("contractChatId").is(contractChatId).and("userId").is(userId));
          return mongoTemplate.findOne(query, ContractDocumentMongoDocument.class);
      }
}
