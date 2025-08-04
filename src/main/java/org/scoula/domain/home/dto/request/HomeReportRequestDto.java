package org.scoula.domain.home.dto.request;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.scoula.domain.home.vo.HomeReportVO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "매물 신고 요청 및 응답 DTO")
public class HomeReportRequestDto {

      @ApiModelProperty(value = "신고 ID", example = "1", readOnly = true)
      private Long reportId;

      @ApiModelProperty(value = "신고한 유저 ID", example = "3", readOnly = true)
      private Long userId;

      @ApiModelProperty(value = "신고 대상 매물 ID", example = "3", required = true)
      private Long homeId;

      @ApiModelProperty(value = "신고 사유", example = "사진과 실제 매물이 다릅니다.", required = true)
      private String reportReason;

      @ApiModelProperty(value = "신고 일시", example = "2025-07-23T00:47:37", readOnly = true)
      private LocalDateTime reportAt;

      @ApiModelProperty(value = "신고 처리 상태", example = "PROCESSING", readOnly = true)
      private String reportStatus;

      // VO -> DTO
      public static HomeReportRequestDto from(HomeReportVO vo) {
          return HomeReportRequestDto.builder()
                  .reportId(vo.getReportId())
                  .userId(vo.getUserId())
                  .homeId(vo.getHomeId())
                  .reportReason(vo.getReportReason())
                  .reportAt(
                          LocalDateTime.parse(
                                  vo.getReportAt(),
                                  DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                  .reportStatus(vo.getReportStatus())
                  .build();
      }

      // DTO -> VO
      public HomeReportVO toVO() {
          return new HomeReportVO(
                  this.reportId,
                  this.userId,
                  this.homeId,
                  this.reportReason,
                  this.reportAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                  this.reportStatus);
      }
}
