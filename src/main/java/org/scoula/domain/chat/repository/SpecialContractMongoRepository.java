package org.scoula.domain.chat.repository;

import java.util.List;
import java.util.Optional;

import org.scoula.domain.chat.document.SpecialContractDocument;
import org.scoula.domain.chat.document.SpecialContractFixDocument;
import org.scoula.domain.chat.document.SpecialContractSelectionDocument;
import org.scoula.domain.chat.dto.SpecialContractUserViewDto;
import org.scoula.domain.chat.mapper.ContractChatMapper;
import org.scoula.domain.chat.vo.ContractChat;
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

      /** contractChatId 존재 여부 확인 */
      public boolean existsByContractChatId(Long contractChatId) {
          Query query = new Query(Criteria.where("contractChatId").is(contractChatId));
          return mongoTemplate.exists(query, SpecialContractFixDocument.class);
      }

      /** 특약 문서 업데이트 */
      public SpecialContractFixDocument updateSpecialContract(SpecialContractFixDocument document) {
          return mongoTemplate.save(document);
      }

      /** 특정 라운드의 특약 문서들 조회 */
      public List<SpecialContractFixDocument> findByRound(Long round) {
          Query query = new Query(Criteria.where("round").is(round));
          return mongoTemplate.find(query, SpecialContractFixDocument.class);
      }

      /** 완료 여부로 특약 문서들 조회 */
      public List<SpecialContractFixDocument> findByIsPassed(Boolean isPassed) {
          Query query = new Query(Criteria.where("isPassed").is(isPassed));
          return mongoTemplate.find(query, SpecialContractFixDocument.class);
      }

      /** order 순으로 정렬하여 모든 특약 문서 조회 */
      public List<SpecialContractFixDocument> findAllByOrderByOrderAsc() {
          Query query = new Query();
          query.with(
                  org.springframework.data.domain.Sort.by(
                          org.springframework.data.domain.Sort.Direction.ASC, "order"));
          return mongoTemplate.find(query, SpecialContractFixDocument.class);
      }

      /** 특정 order의 특약 문서들 조회 */
      public List<SpecialContractFixDocument> findByOrder(Long order) {
          Query query = new Query(Criteria.where("order").is(order));
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

      /** 특약 문서 삭제 */
      public void deleteSpecialContract(SpecialContractFixDocument document) {
          mongoTemplate.remove(document);
      }

      /** contractChatId로 특약 문서 삭제 */
      public void deleteByContractChatId(Long contractChatId) {
          Query query = new Query(Criteria.where("contractChatId").is(contractChatId));
          mongoTemplate.remove(query, SpecialContractFixDocument.class);
      }

      /** contractChatId로 SpecialContractDocument (원본 특약 문서) 조회 */
      public Optional<SpecialContractDocument> findSpecialContractDocumentByContractChatId(
              Long contractChatId) {
          Query query = new Query(Criteria.where("contractChatId").is(contractChatId));
          SpecialContractDocument result =
                  mongoTemplate.findOne(query, SpecialContractDocument.class);
          return Optional.ofNullable(result);
      }

      /** 사용자 역할별 특약 문서 조회 (ID 포함) */
      public SpecialContractUserViewDto getSpecialContractForUserWithIds(
              Long contractChatId, Long userId, ContractChatMapper contractChatMapper) {
          // 1. Raw Document로 조회
          Query query = new Query(Criteria.where("contractChatId").is(contractChatId));
          org.bson.Document rawDocument =
                  mongoTemplate.findOne(query, org.bson.Document.class, "SPECIAL_CONTRACT");

          if (rawDocument == null) {
              throw new IllegalArgumentException("해당 특약 문서를 찾을 수 없습니다: " + contractChatId);
          }

          // 2. 사용자 역할 확인
          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new IllegalArgumentException("계약 채팅방을 찾을 수 없습니다.");
          }

          boolean isOwner = userId.equals(contractChat.getOwnerId());
          boolean isTenant = userId.equals(contractChat.getBuyerId());

          if (!isOwner && !isTenant) {
              throw new IllegalArgumentException("해당 계약 채팅방에 접근 권한이 없습니다.");
          }

          String userRole = isOwner ? "owner" : "tenant";

          // 3. Raw Document에서 데이터 추출
          Long docContractChatId = rawDocument.getLong("contractChatId");
          ContractChat contractChats = contractChatMapper.findByContractChatId(contractChatId);
          Long round = contractChats.getCurrentRound();
          Integer totalClauses = rawDocument.getInteger("totalClauses");

          @SuppressWarnings("unchecked")
          java.util.List<org.bson.Document> clausesDocs =
                  (java.util.List<org.bson.Document>) rawDocument.get("clauses");

          java.util.List<SpecialContractUserViewDto.ClauseUserView> userClauses =
                  new java.util.ArrayList<>();

          for (org.bson.Document clauseDoc : clausesDocs) {
              Integer clauseId = clauseDoc.getInteger("_id"); // MongoDB의 _id 필드
              String title = clauseDoc.getString("title");
              String content = clauseDoc.getString("content");

              org.bson.Document assessmentDoc = (org.bson.Document) clauseDoc.get("assessment");
              org.bson.Document userEvalDoc =
                      isOwner
                              ? (org.bson.Document) assessmentDoc.get("owner")
                              : (org.bson.Document) assessmentDoc.get("tenant");

              String level = userEvalDoc.getString("level");
              String reason = userEvalDoc.getString("reason");

              SpecialContractUserViewDto.ClauseUserView clauseView =
                      SpecialContractUserViewDto.ClauseUserView.builder()
                              .id(clauseId)
                              .title(title)
                              .content(content)
                              .level(level)
                              .reason(reason)
                              .build();

              userClauses.add(clauseView);
          }

          return SpecialContractUserViewDto.builder()
                  .contractChatId(docContractChatId)
                  .round(round)
                  .totalClauses(totalClauses)
                  .userRole(userRole)
                  .clauses(userClauses)
                  .build();
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
          Query query =
                  new Query(
                          Criteria.where("contractChatId").is(contractChatId).and("round").is(round+1));
          Update update = new Update().set("clauses." + (order - 1), clause);
          
          com.mongodb.client.result.UpdateResult result = 
                  mongoTemplate.updateFirst(query, update, SpecialContractDocument.class);
          
          if (result.getModifiedCount() > 0) {
              return contractChatId.toString();
          }
          return null;
      }
}
