// src/main/java/org/scoula/domain/home/dto/response/FacilityResponseDto.java
package org.scoula.domain.home.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityResponseDto {
      private Long itemId;
      private String itemName;
      private Long categoryId;
      private String categoryType;
}
