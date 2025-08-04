package org.scoula.domain.home.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeReportVO {
      private Long reportId;
      private Long userId;
      private Long homeId;
      private String reportReason;
      private String reportAt; // LocalDateTime 으로 바꿔도 OK
      private String reportStatus;
}
