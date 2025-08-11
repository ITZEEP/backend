package org.scoula.domain.verification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 주민등록증 진위 확인 응답 DTO
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@ApiModel(description = "주민등록증 진위 확인 응답")
public class IdCardVerificationResponse {

      @ApiModelProperty(value = "응답 데이터")
      @JsonProperty("data")
      private VerificationData data;

      @ApiModelProperty(value = "API 정보")
      @JsonProperty("api")
      private ApiInfo api;

      /** 검증 데이터 내부 클래스 */
      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @ToString
      public static class VerificationData {

          @ApiModelProperty(value = "신분증 진위 확인 요청 ID", example = "123456")
          @JsonProperty("ic_id")
          private Integer icId;

          @ApiModelProperty(value = "신분증 종류", example = "1")
          @JsonProperty("type")
          private Integer type;

          @ApiModelProperty(value = "조회 결과 (0: 실패, 1: 성공, 2: msg 확인, 3: 실패(timeout))", example = "1")
          @JsonProperty("result")
          private Integer result;

          @ApiModelProperty(value = "메시지", example = "")
          @JsonProperty("msg")
          private String msg;

          @ApiModelProperty(value = "과금 여부 (0: 실패, 1: 성공, 3: 실패(timeout))", example = "1")
          @JsonProperty("success")
          private Integer success;
      }

      /** API 정보 내부 클래스 */
      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      @ToString
      public static class ApiInfo {

          @ApiModelProperty(value = "API 성공 여부")
          @JsonProperty("success")
          private Boolean success;

          @ApiModelProperty(value = "API 비용", example = "40")
          @JsonProperty("cost")
          private Integer cost;

          @ApiModelProperty(value = "처리 시간(ms)", example = "4032")
          @JsonProperty("ms")
          private Long ms;

          @ApiModelProperty(value = "플랫폼 ID", example = "103894")
          @JsonProperty("pl_id")
          private Long plId;
      }

      /**
       * 검증 성공 여부 확인
       *
       * @return 검증 성공 시 true
       */
      public boolean isVerificationSuccessful() {
          return data != null
                  && data.getResult() != null
                  && data.getResult() == 1 // 1: 성공
                  && data.getSuccess() != null
                  && data.getSuccess() == 1; // 1: 성공
      }

      /**
       * API 호출 성공 여부 확인
       *
       * @return API 호출 성공 시 true
       */
      public boolean isApiSuccessful() {
          return api != null && api.getSuccess() != null && api.getSuccess();
      }
}
