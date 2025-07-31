package org.scoula.domain.fraud.dto.ai;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
      @JsonSubTypes.Type(value = ParsedData.class),
      @JsonSubTypes.Type(value = BuildingParsedData.class)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedData {
      private String regionAddress;
      private String roadAddress;
      private String ownerName;
      private Date ownerBirthDate;
      private String debtor;
      private List<MortgageeInfo> mortgageeList;
      private Boolean hasSeizure;
      private Boolean hasAuction;
      private Boolean hasLitigation;
      private Boolean hasAttachment;

      @Getter
      @Setter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class MortgageeInfo {
          private Integer priorityNumber;
          private Long maxClaimAmount;
          private String debtor;
          private String mortgagee;
      }
}
