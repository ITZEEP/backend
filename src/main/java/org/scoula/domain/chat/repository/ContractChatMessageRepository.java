package org.scoula.domain.chat.repository;

import java.util.List;

import org.scoula.domain.chat.dto.ContractChatDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class ContractChatMessageRepository {

      @Autowired private MongoTemplate mongoTemplate;

      private String getCollectionName(Long contractChatId) {
          int shardIndex = (int) (contractChatId % 5);
          return "CONTRACT_MESSAGE_" + shardIndex;
      }

      /** 메시지 저장 - 일반 채팅과 동일하게 ObjectId 자동 생성 */
      public ContractChatDocument saveMessage(ContractChatDocument message) {
          Long contractChatId = Long.valueOf(message.getContractChatId());
          String collectionName = getCollectionName(contractChatId);

          return mongoTemplate.save(message, collectionName);
      }

      /** 전체 메시지 조회 - sendTime으로 정렬 */
      public List<ContractChatDocument> getMessages(Long contractChatId) {
          String collectionName = getCollectionName(contractChatId);

          Query query = new Query();
          query.addCriteria(Criteria.where("contractChatId").is(contractChatId.toString()));
          query.with(Sort.by(Sort.Direction.ASC, "sendTime"));

          return mongoTemplate.find(query, ContractChatDocument.class, collectionName);
      }

      /** 시간 범위로 메시지 조회 (특약 내보내기용) */
      public List<ContractChatDocument> getMessagesBetweenTime(
              Long contractChatId, String startTime, String endTime) {
          String collectionName = getCollectionName(contractChatId);

          Query query = new Query();
          query.addCriteria(
                  Criteria.where("contractChatId")
                          .is(contractChatId.toString())
                          .and("sendTime")
                          .gte(startTime)
                          .lte(endTime));
          query.with(Sort.by(Sort.Direction.ASC, "sendTime"));

          List<ContractChatDocument> messages =
                  mongoTemplate.find(query, ContractChatDocument.class, collectionName);

          log.debug(
                  "시간 범위 조회 결과: contractChatId={}, startTime={}, endTime={}, count={}",
                  contractChatId,
                  startTime,
                  endTime,
                  messages.size());

          return messages;
      }

      /** 페이지네이션 - sendTime 기반 */
      public List<ContractChatDocument> getMessagesPaged(Long contractChatId, int page, int size) {
          String collectionName = getCollectionName(contractChatId);

          Query query = new Query();
          query.addCriteria(Criteria.where("contractChatId").is(contractChatId.toString()));
          query.with(Sort.by(Sort.Direction.DESC, "sendTime"));
          query.skip((long) page * size).limit(size);

          List<ContractChatDocument> messages =
                  mongoTemplate.find(query, ContractChatDocument.class, collectionName);

          // 시간 순으로 다시 정렬
          messages.sort((a, b) -> a.getSendTime().compareTo(b.getSendTime()));

          return messages;
      }

      /** ID로 메시지 찾기 */
      public ContractChatDocument findMessageById(Long contractChatId, String messageId) {
          String collectionName = getCollectionName(contractChatId);

          Query query = new Query();
          query.addCriteria(
                  Criteria.where("contractChatId")
                          .is(contractChatId.toString())
                          .and("_id")
                          .is(messageId));

          return mongoTemplate.findOne(query, ContractChatDocument.class, collectionName);
      }
}
