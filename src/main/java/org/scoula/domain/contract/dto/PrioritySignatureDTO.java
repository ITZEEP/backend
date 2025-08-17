package org.scoula.domain.contract.dto;

import java.time.LocalDateTime;

import org.scoula.global.common.constant.Constants;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.annotations.ApiModelProperty;

public class PrioritySignatureDTO {

      @ApiModelProperty(value = "owner_Priority 서명 이미지 S3 URL", example = "url")
      private String ownerPrioritySignatureFileKey;

      @ApiModelProperty(value = "owner_Priority 서명 Hash key", example = "Hash key")
      private String ownerPriorityFileHash;

      @JsonFormat(
              shape = JsonFormat.Shape.STRING,
              pattern = Constants.DateTime.DEFAULT_DATETIME_FORMAT)
      @ApiModelProperty(value = "owner_Priority 서명 날짜/시간", example = "2024-08-08 14:23:00")
      private LocalDateTime ownerPrioritySignedAt;
}
