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

      public void saveMessage(Long chatRoomId, ChatMessageDocument message) {
          String collectionName = getCollectionName(chatRoomId);
          mongoTemplate.save(message, collectionName);
      }

      public List<ChatMessageDocument> getMessages(Long chatRoomId) {
          String collectionName = getCollectionName(chatRoomId);

          Query query = new Query();
          query.addCriteria(Criteria.where("chatRoomId").is(chatRoomId.intValue()));

          query.with(Sort.by(Sort.Direction.ASC, "sendTime"));

          List<ChatMessageDocument> result =
                  mongoTemplate.find(query, ChatMessageDocument.class, collectionName);
          return result;
      }

      public int countUnreadMessages(Long chatRoomId, Long receiverId) {
          Query query = new Query();
          query.addCriteria(
                  Criteria.where("chatRoomId")
                          .is(chatRoomId)
                          .and("receiverId")
                          .is(receiverId)
                          .and("isRead")
                          .is(false));

          return (int) mongoTemplate.count(query, ChatMessageDocument.class);
      }

      public void markAsRead(Long chatroomId, Long userId) {
          mongoTemplate.updateMulti(
                  new Query()
                          .addCriteria(
                                  Criteria.where("chatRoomId")
                                          .is(chatroomId)
                                          .and("receiverId")
                                          .is(userId)
                                          .and("isRead")
                                          .is(false)),
                  new Update().set("isRead", true),
                  ChatMessageDocument.class,
                  getCollectionName(chatroomId));
      }

      private String getCollectionName(Long chatroomId) {
          return "MESSAGE";
      }
}
