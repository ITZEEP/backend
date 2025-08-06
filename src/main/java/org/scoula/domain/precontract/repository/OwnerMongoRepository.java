package org.scoula.domain.precontract.repository;

import org.scoula.domain.precontract.document.OwnerMongoDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OwnerMongoRepository {
      @Autowired private MongoTemplate mongoTemplate;

      public OwnerMongoDocument insert(OwnerMongoDocument document) {
          return mongoTemplate.insert(document);
      }
}
