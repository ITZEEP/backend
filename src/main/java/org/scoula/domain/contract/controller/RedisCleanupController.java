package org.scoula.domain.contract.controller;

import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/** Redis 데이터 정리용 임시 컨트롤러 개발 환경에서만 사용! 운영 환경에서는 제거해야 함 */
@RestController
@RequestMapping("/api/admin/redis")
@RequiredArgsConstructor
@Log4j2
public class RedisCleanupController {

      private final RedisTemplate<String, Object> redisTemplate;

      /** 특정 계약의 export 상태 삭제 */
      @DeleteMapping("/contract/{contractChatId}")
      public String deleteContractExportStatus(@PathVariable Long contractChatId) {
          String key = "contract:export:status:" + contractChatId;
          Boolean deleted = redisTemplate.delete(key);

          // 암호 키도 삭제
          String ownerPasswordKey = "contract:export:password:" + contractChatId + ":owner";
          String buyerPasswordKey = "contract:export:password:" + contractChatId + ":buyer";
          redisTemplate.delete(ownerPasswordKey);
          redisTemplate.delete(buyerPasswordKey);

          log.info("Deleted Redis keys for contract {}", contractChatId);
          return "Deleted: " + deleted;
      }

      /** 모든 contract export 관련 키 조회 */
      @GetMapping("/contract/export/keys")
      public Set<String> getContractExportKeys() {
          Set<String> keys = redisTemplate.keys("contract:export:*");
          log.info("Found {} export keys", keys != null ? keys.size() : 0);
          return keys;
      }

      /** 모든 contract export 관련 데이터 삭제 */
      @DeleteMapping("/contract/export/all")
      public String deleteAllContractExportData() {
          Set<String> keys = redisTemplate.keys("contract:export:*");
          if (keys != null && !keys.isEmpty()) {
              Long deleted = redisTemplate.delete(keys);
              log.info("Deleted {} export keys", deleted);
              return "Deleted " + deleted + " keys";
          }
          return "No keys found";
      }

      /** Redis의 모든 데이터 삭제 (위험!) 개발 환경에서만 사용할 것 */
      @DeleteMapping("/flush-all")
      public String flushAll() {
          log.warn("FLUSHING ALL REDIS DATA!");
          redisTemplate.getConnectionFactory().getConnection().flushAll();
          return "All Redis data has been deleted!";
      }

      /** 현재 데이터베이스의 모든 키 삭제 */
      @DeleteMapping("/flush-db")
      public String flushDb() {
          log.warn("FLUSHING CURRENT REDIS DATABASE!");
          redisTemplate.getConnectionFactory().getConnection().flushDb();
          return "Current Redis database has been cleared!";
      }
}
