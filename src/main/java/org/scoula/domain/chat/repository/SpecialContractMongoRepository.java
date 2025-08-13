package org.scoula.domain.chat.repository;

import java.util.List;
import java.util.Optional;

import org.scoula.domain.chat.document.FinalSpecialContractDocument;
import org.scoula.domain.chat.document.SpecialContractDocument;
import org.scoula.domain.chat.document.SpecialContractFixDocument;
import org.scoula.domain.chat.document.SpecialContractSelectionDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class SpecialContractMongoRepository {
      @Autowired private MongoTemplate mongoTemplate;

      public SpecialContractSelectionDocument saveSelectionStatus(
              SpecialContractSelectionDocument document) {
          return mongoTemplate.save(document);
      }

      public Optional<SpecialContractSelectionDocument> findSelectionByContractChatId(
              Long contractChatId) {
          Query query = new Query(Criteria.where("contractChatId").is(contractChatId));
          SpecialContractSelectionDocument result =
                  mongoTemplate.findOne(query, SpecialContractSelectionDocument.class);
          return Optional.ofNullable(result);
      }

      /** 특약 문서 생성 */
      public SpecialContractFixDocument createSpecialContract(SpecialContractFixDocument document) {
          return mongoTemplate.save(document);
      }

      /** contractChatId로 특약 문서 조회 */
      public Optional<SpecialContractFixDocument> findByContractChatId(Long contractChatId) {
          Query query = new Query(Criteria.where("contractChatId").is(contractChatId));
          SpecialContractFixDocument result =
                  mongoTemplate.findOne(query, SpecialContractFixDocument.class);
          return Optional.ofNullable(result);
      }

      /** 특약 문서 업데이트 */
      public SpecialContractFixDocument updateSpecialContract(SpecialContractFixDocument document) {
          return mongoTemplate.save(document);
      }

      /** 완료 여부로 특약 문서들 조회 */
      public List<SpecialContractFixDocument> findByIsPassed(Boolean isPassed) {
          Query query = new Query(Criteria.where("isPassed").is(isPassed));
          return mongoTemplate.find(query, SpecialContractFixDocument.class);
      }

      public Optional<SpecialContractFixDocument> findByContractChatIdAndOrder(
              Long contractChatId, Long order) {
          Query query =
                  new Query(
                          Criteria.where("contractChatId").is(contractChatId).and("order").is(order));
          SpecialContractFixDocument result =
                  mongoTemplate.findOne(query, SpecialContractFixDocument.class);
          return Optional.ofNullable(result);
      }

      public List<SpecialContractFixDocument> findByContractChatIdAndIsPassed(
              Long contractChatId, Boolean isPassed) {
          Query query =
                  new Query(
                          Criteria.where("contractChatId")
                                  .is(contractChatId)
                                  .and("isPassed")
                                  .is(isPassed));
          return mongoTemplate.find(query, SpecialContractFixDocument.class);
      }

      public List<SpecialContractFixDocument>
              findByContractChatIdAndIsPassedAndRecentDataMessagesEmpty(
                      Long contractChatId, Boolean isPassed) {
          Criteria criteria =
                  Criteria.where("contractChatId")
                          .is(contractChatId)
                          .and("isPassed")
                          .is(isPassed)
                          .orOperator(
                                  Criteria.where("recentData.messages").exists(false),
                                  Criteria.where("recentData.messages").regex("^\\s*$"));
          Query query = new Query(criteria);
          return mongoTemplate.find(query, SpecialContractFixDocument.class);
      }

      public Optional<SpecialContractDocument> findSpecialContractDocumentByContractChatIdAndRound(
              Long contractChatId, Long round) {
          Query query =
                  new Query(
                          Criteria.where("contractChatId").is(contractChatId).and("round").is(round));
          SpecialContractDocument result =
                  mongoTemplate.findOne(query, SpecialContractDocument.class);
          return Optional.ofNullable(result);
      }

      public SpecialContractDocument saveSpecialContractForNewRound(
              SpecialContractDocument document) {
          return mongoTemplate.save(document, "SPECIAL_CONTRACT");
      }

      public String updateSpecialContractForNewOrderAndRound(
              Long contractChatId, Long round, Integer order, SpecialContractDocument.Clause clause) {
          if (order == null || order <= 0) {
              throw new IllegalArgumentException(
                      "Order must be a positive integer. Provided: " + order);
          }
          long nextRound;
          if (round == null) {
              throw new IllegalArgumentException("round cannot be null");
          }
          if (round >= Long.MAX_VALUE) {
              throw new IllegalArgumentException("round is too large and would overflow");
          }
          nextRound = round + 1;
          Query query =
                  new Query(
                          Criteria.where("contractChatId")
                                  .is(contractChatId)
                                  .and("round")
                                  .is(nextRound));
          Update update = new Update().set("clauses." + (order - 1), clause);

          com.mongodb.client.result.UpdateResult result =
                  mongoTemplate.updateFirst(query, update, SpecialContractDocument.class);

          if (result.getModifiedCount() > 0) {
              return contractChatId.toString();
          }
          return null;
      }

      public FinalSpecialContractDocument saveFinalSpecialContract(
              FinalSpecialContractDocument document) {
          return mongoTemplate.save(document, "FINAL_SPECIAL_CONTRACT");
      }

      public Optional<FinalSpecialContractDocument> findFinalContractByContractChatId(
              Long contractChatId) {
          Query query = new Query(Criteria.where("contractChatId").is(contractChatId));
          FinalSpecialContractDocument result =
                  mongoTemplate.findOne(query, FinalSpecialContractDocument.class);
          return Optional.ofNullable(result);
      }
}
