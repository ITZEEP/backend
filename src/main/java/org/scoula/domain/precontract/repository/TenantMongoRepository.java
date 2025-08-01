package org.scoula.domain.precontract.repository;

import org.scoula.domain.precontract.document.TenantMongoDocument;
import org.scoula.domain.precontract.dto.tenant.TenantMongoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TenantMongoRepository {

      @Autowired private MongoTemplate mongoTemplate;

      public TenantMongoDocument insert(TenantMongoDTO dto) {
          TenantMongoDocument document = TenantMongoDocument.toDocument(dto);
          return mongoTemplate.insert(document);
      }
}
