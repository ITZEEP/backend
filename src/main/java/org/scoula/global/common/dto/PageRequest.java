package org.scoula.global.common.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 페이징 요청 DTO */
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PageRequest {

      @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
      @Builder.Default
      private int page = 1;

      @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
      @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
      @Builder.Default
      private int size = 20;

      @Builder.Default private String sort = "id";

      @Builder.Default private String direction = "DESC";

      /** 오프셋 계산 (0-based) */
      public int getOffset() {
          return (page - 1) * size;
      }

      /** ORDER BY 절 문자열 생성 */
      public String getOrderBy() {
          return sort + " " + direction.toUpperCase();
      }

      /** Spring Data의 PageRequest로 변환 */
      public org.springframework.data.domain.PageRequest toSpringPageRequest() {
          org.springframework.data.domain.Sort.Direction sortDirection =
                  "ASC".equalsIgnoreCase(direction)
                          ? org.springframework.data.domain.Sort.Direction.ASC
                          : org.springframework.data.domain.Sort.Direction.DESC;

          return org.springframework.data.domain.PageRequest.of(
                  page - 1, // Spring Data는 0-based 페이징 사용
                  size,
                  org.springframework.data.domain.Sort.by(sortDirection, sort));
      }
}
