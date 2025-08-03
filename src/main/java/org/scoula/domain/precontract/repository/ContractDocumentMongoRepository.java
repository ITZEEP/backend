package org.scoula.domain.precontract.repository;

import org.scoula.domain.precontract.document.ContractDocumentMongoDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ContractDocumentMongoRepository {

      @Autowired private MongoTemplate mongoTemplate;

      public ContractDocumentMongoDocument save(ContractDocumentMongoDocument document) {
          return mongoTemplate.save(document); // insert → save로 변경 (upsert)
      }
}
