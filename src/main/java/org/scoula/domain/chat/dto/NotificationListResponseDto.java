package org.scoula.domain.chat.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationListResponseDto {
      private List<NotificationDto> notifications;
      private int unreadCount;
      private int currentPage;
      private int pageSize;
      private boolean hasNext;
      private int totalCount;
}
