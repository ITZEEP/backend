package org.scoula.domain.fraud.dto.ai;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class FraudRiskCheckDto {

      @Data
      @NoArgsConstructor
      @AllArgsConstructor
      @Builder
      public static class Request {
          @JsonProperty("userId")
          private Long userId;

          @JsonProperty("userType")
          private String userType;

          @JsonProperty("homeId")
          private Long homeId;

          @JsonProperty("address")
          private String address;

          @JsonProperty("propertyPrice")
          private Long propertyPrice;

          @JsonProperty("leaseType")
          private String leaseType;

          @JsonProperty("registryDocument")
          private RegistryDocument registryDocument;

          @JsonProperty("buildingDocument")
          private BuildingDocument buildingDocument;

          @JsonProperty("registeredUserName")
          private String registeredUserName;

          @JsonProperty("residenceType")
          private String residenceType;
      }

      @Data
      @NoArgsConstructor
      @AllArgsConstructor
      @Builder
      public static class RegistryDocument {
          @JsonProperty("regionAddress")
          private String regionAddress;

          @JsonProperty("roadAddress")
          private String roadAddress;

          @JsonProperty("ownerName")
          private String ownerName;

          @JsonProperty("ownerBirthDate")
          private String ownerBirthDate;

          @JsonProperty("debtor")
          private String debtor;

          @JsonProperty("mortgageeList")
          private List<MortgageeInfo> mortgageeList;

          @JsonProperty("hasSeizure")
          private Boolean hasSeizure;

          @JsonProperty("hasAuction")
          private Boolean hasAuction;

          @JsonProperty("hasLitigation")
          private Boolean hasLitigation;

          @JsonProperty("hasAttachment")
          private Boolean hasAttachment;
      }

      @Data
      @NoArgsConstructor
      @AllArgsConstructor
      @Builder
      public static class MortgageeInfo {
          @JsonProperty("priorityNumber")
          private Integer priorityNumber;

          @JsonProperty("maxClaimAmount")
          private Long maxClaimAmount;

          @JsonProperty("debtor")
          private String debtor;

          @JsonProperty("mortgagee")
          private String mortgagee;
      }

      @Data
      @NoArgsConstructor
      @AllArgsConstructor
      @Builder
      public static class BuildingDocument {
          @JsonProperty("siteLocation")
          private String siteLocation;

          @JsonProperty("roadAddress")
          private String roadAddress;

          @JsonProperty("totalFloorArea")
          private Double totalFloorArea;

          @JsonProperty("purpose")
          private String purpose;

          @JsonProperty("floorNumber")
          private Integer floorNumber;

          @JsonProperty("approvalDate")
          private String approvalDate;

          @JsonProperty("isViolationBuilding")
          private Boolean isViolationBuilding;
      }

      @Data
      @NoArgsConstructor
      @AllArgsConstructor
      @Builder
      public static class Response {
          @JsonProperty("status")
          private String status;

          @JsonProperty("risk_score")
          private Double riskScore;

          @JsonProperty("risk_level")
          private String riskLevel;

          @JsonProperty("analysis_id")
          private String analysisId;

          @JsonProperty("analysis_results")
          private Map<String, Object> analysisResults;

          @JsonProperty("recommendations")
          private List<String> recommendations;

          @JsonProperty("detailed_analysis")
          private Map<String, Object> detailedAnalysis;

          @JsonProperty("timestamp")
          private String timestamp;
      }

      @Data
      @NoArgsConstructor
      @AllArgsConstructor
      @Builder
      public static class OcrRequest {
          @JsonProperty("registryFileUrl")
          private String registryFileUrl;

          @JsonProperty("buildingFileUrl")
          private String buildingFileUrl;
      }

      @Data
      @NoArgsConstructor
      @AllArgsConstructor
      @Builder
      public static class OcrResponse {
          @JsonProperty("status")
          private String status;

          @JsonProperty("registryDocument")
          private org.scoula.domain.fraud.dto.common.RegistryDocumentDto registryDocument;

          @JsonProperty("buildingDocument")
          private org.scoula.domain.fraud.dto.common.BuildingDocumentDto buildingDocument;

          @JsonProperty("processingTime")
          private Double processingTime;

          @JsonProperty("errorMessage")
          private String errorMessage;
      }
}
