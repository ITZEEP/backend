package org.scoula.domain.chat.repository;

import java.util.List;

import org.scoula.domain.chat.dto.ChatMessageDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class ChatMessageMongoRepository {
      @Autowired private MongoTemplate mongoTemplate;

      /** 컬렉션 이름 결정 - chatRoomId를 5로 나눈 나머지 사용 */
      private String getCollectionName(Long chatRoomId) {
          int shardIndex = (int) (chatRoomId % 5);
          return "MESSAGE_" + shardIndex;
      }

      public void saveMessage(Long chatRoomId, ChatMessageDocument message) {
          String collectionName = getCollectionName(chatRoomId);
          mongoTemplate.save(message, collectionName);
      }

      public List<ChatMessageDocument> getMessages(Long chatRoomId) {
          String collectionName = getCollectionName(chatRoomId);

          Query query = new Query();
          query.addCriteria(Criteria.where("chatRoomId").is(chatRoomId));
          query.with(Sort.by(Sort.Direction.ASC, "sendTime"));

          return mongoTemplate.find(query, ChatMessageDocument.class, collectionName);
      }

      public int countUnreadMessages(Long chatRoomId, Long receiverId) {
          String collectionName = getCollectionName(chatRoomId);

          Query query = new Query();
          query.addCriteria(
                  Criteria.where("chatRoomId")
                          .is(chatRoomId)
                          .and("receiverId")
                          .is(receiverId)
                          .and("isRead")
                          .is(false));

          return (int) mongoTemplate.count(query, ChatMessageDocument.class, collectionName);
      }

      public void markAsRead(Long chatRoomId, Long userId) {
          String collectionName = getCollectionName(chatRoomId);

          mongoTemplate.updateMulti(
                  new Query()
                          .addCriteria(
                                  Criteria.where("chatRoomId")
                                          .is(chatRoomId)
                                          .and("receiverId")
                                          .is(userId)
                                          .and("isRead")
                                          .is(false)),
                  new Update().set("isRead", true),
                  ChatMessageDocument.class,
                  collectionName);
      }
}
