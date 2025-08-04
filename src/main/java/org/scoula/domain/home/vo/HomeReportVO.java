package org.scoula.domain.home.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeReportVO {
      private Long reportId;
      private Long userId;
      private Long homeId;
      private String reportReason;
      private LocalDateTime reportAt; // LocalDateTime 으로 바꿔도 OK
      private String reportStatus;
}
