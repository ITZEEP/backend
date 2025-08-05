package org.scoula.domain.chat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.chat.vo.Notification;

@Mapper
public interface NotificationMapper {

      // 알림 생성
      void insertNotification(Notification notification);

      // 알림 ID로 조회
      Notification findByNotiId(@Param("notiId") Long notiId);

      // 사용자의 알림 목록 조회
      List<Notification> findByUserId(
              @Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);

      // 사용자의 전체 알림 수 조회
      int countByUserId(@Param("userId") Long userId);

      // 읽지 않은 알림 수 조회
      int countUnreadNotifications(@Param("userId") Long userId);

      // 알림 읽음 처리
      void markAsRead(@Param("notiId") Long notiId);

      // 여러 알림 읽음 처리
      void markAsReadByIds(@Param("notiIds") List<Long> notiIds);

      // 모든 알림 읽음 처리
      void markAllAsRead(@Param("userId") Long userId);

      // 알림 삭제
      void deleteNotification(@Param("notiId") Long notiId);

      // 여러 알림 삭제
      void deleteNotifications(@Param("notiIds") List<Long> notiIds);

      // 사용자의 모든 알림 삭제
      void deleteAllByUserId(@Param("userId") Long userId);

      // 오래된 알림 자동 삭제 (N일 이상)
      void deleteOldNotifications(@Param("days") int days);

      // 타입별 알림 조회
      List<Notification> findByUserIdAndType(
              @Param("userId") Long userId,
              @Param("type") String type,
              @Param("limit") int limit,
              @Param("offset") int offset);

      // 타입별 알림 수 조회
      int countByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

      // 최신 알림 N개 조회
      List<Notification> findLatestByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}
