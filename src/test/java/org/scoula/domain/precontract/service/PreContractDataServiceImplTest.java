package org.scoula.domain.precontract.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.chat.dto.ai.ClauseImproveRequestDto;
import org.scoula.domain.precontract.document.ContractDocumentMongoDocument;
import org.scoula.domain.precontract.dto.owner.OwnerPreContractMongoDTO;
import org.scoula.domain.precontract.dto.tenant.TenantMongoDTO;
import org.scoula.domain.precontract.enums.RentType;
import org.scoula.domain.precontract.mapper.OwnerPreContractMapper;
import org.scoula.domain.precontract.mapper.TenantPreContractMapper;
import org.scoula.domain.precontract.repository.ContractDocumentMongoRepository;
import org.scoula.domain.precontract.vo.RestoreCategoryVO;

@ExtendWith(MockitoExtension.class)
@DisplayName("PreContractDataServiceImpl 테스트")
class PreContractDataServiceImplTest {

      @Mock private OwnerPreContractMapper ownerMapper;
      @Mock private TenantPreContractMapper tenantMapper;
      @Mock private ContractDocumentMongoRepository contractDocumentMongoRepository;

      @InjectMocks private PreContractDataServiceImpl preContractDataService;

      private Long contractChatId;
      private Long ownerId;
      private Long buyerId;
      private OwnerPreContractMongoDTO ownerDto;
      private TenantMongoDTO tenantDto;
      private ContractDocumentMongoDocument contractDocument;

      @BeforeEach
      void setUp() {
          contractChatId = 1L;
          ownerId = 100L;
          buyerId = 200L;

          ownerDto = new OwnerPreContractMongoDTO();
          ownerDto.setRentType(RentType.JEONSE);
          ownerDto.setAllowJeonseRightRegistration(true);

          tenantDto = new TenantMongoDTO();
          tenantDto.setRentType("JEONSE");
          tenantDto.setOccupation("직장인");
          tenantDto.setResidentCount(2);

          contractDocument = new ContractDocumentMongoDocument();
          contractDocument.setContractChatId(contractChatId);
          contractDocument.setUserId(ownerId);
          contractDocument.setFilename("contract.pdf");
          contractDocument.setRawText("계약서 내용");
          contractDocument.setExtractedAt("2024-01-01T10:00:00");
      }

      @Nested
      @DisplayName("fetchOwnerData 메서드")
      class FetchOwnerData {

          @Test
          @DisplayName("Owner 데이터 조회 성공")
          void fetchOwnerData_Success() {
              // Given
              when(ownerMapper.selectContractOwnerId(contractChatId))
                      .thenReturn(Optional.of(ownerId));
              when(ownerMapper.selectMongo(contractChatId, ownerId)).thenReturn(ownerDto);
              when(ownerMapper.selectIdentityId(contractChatId)).thenReturn(Optional.of(1L));
              when(ownerMapper.selectOwnerPrecheckId(contractChatId, ownerId))
                      .thenReturn(Optional.of(1L));
              when(ownerMapper.selectRestoreScope(contractChatId, ownerId))
                      .thenReturn(Arrays.asList(new RestoreCategoryVO()));

              // When
              ClauseImproveRequestDto.OwnerData result =
                      preContractDataService.fetchOwnerData(contractChatId);

              // Then
              assertNotNull(result);
              assertEquals(contractChatId, result.getContractChatId());
              verify(ownerMapper).selectContractOwnerId(contractChatId);
              verify(ownerMapper).selectMongo(contractChatId, ownerId);
          }

          @Test
          @DisplayName("Owner ID를 찾을 수 없는 경우 예외 발생")
          void fetchOwnerData_OwnerNotFound() {
              // Given
              when(ownerMapper.selectContractOwnerId(contractChatId)).thenReturn(Optional.empty());

              // When & Then
              assertThrows(
                      IllegalArgumentException.class,
                      () -> preContractDataService.fetchOwnerData(contractChatId));
          }

          @Test
          @DisplayName("Owner 데이터를 찾을 수 없는 경우 예외 발생")
          void fetchOwnerData_OwnerDataNotFound() {
              // Given
              when(ownerMapper.selectContractOwnerId(contractChatId))
                      .thenReturn(Optional.of(ownerId));
              when(ownerMapper.selectMongo(contractChatId, ownerId)).thenReturn(null);

              // When & Then
              assertThrows(
                      IllegalArgumentException.class,
                      () -> preContractDataService.fetchOwnerData(contractChatId));
          }
      }

      @Nested
      @DisplayName("fetchTenantData 메서드")
      class FetchTenantData {

          @Test
          @DisplayName("Tenant 데이터 조회 성공")
          void fetchTenantData_Success() {
              // Given
              when(tenantMapper.selectContractBuyerId(contractChatId))
                      .thenReturn(Optional.of(buyerId));
              when(tenantMapper.selectMongo(buyerId, contractChatId)).thenReturn(tenantDto);
              when(tenantMapper.selectIdentityId(contractChatId, buyerId))
                      .thenReturn(Optional.of(1L));

              // When
              ClauseImproveRequestDto.TenantData result =
                      preContractDataService.fetchTenantData(contractChatId);

              // Then
              assertNotNull(result);
              assertEquals(contractChatId, result.getContractChatId());
              assertEquals("직장인", result.getOccupation());
              assertEquals(2, result.getResidentCount());
              verify(tenantMapper).selectContractBuyerId(contractChatId);
              verify(tenantMapper).selectMongo(buyerId, contractChatId);
          }

          @Test
          @DisplayName("Buyer ID를 찾을 수 없는 경우 예외 발생")
          void fetchTenantData_BuyerNotFound() {
              // Given
              when(tenantMapper.selectContractBuyerId(contractChatId)).thenReturn(Optional.empty());

              // When & Then
              assertThrows(
                      IllegalArgumentException.class,
                      () -> preContractDataService.fetchTenantData(contractChatId));
          }

          @Test
          @DisplayName("Tenant 데이터를 찾을 수 없는 경우 예외 발생")
          void fetchTenantData_TenantDataNotFound() {
              // Given
              when(tenantMapper.selectContractBuyerId(contractChatId))
                      .thenReturn(Optional.of(buyerId));
              when(tenantMapper.selectMongo(buyerId, contractChatId)).thenReturn(null);

              // When & Then
              assertThrows(
                      IllegalArgumentException.class,
                      () -> preContractDataService.fetchTenantData(contractChatId));
          }
      }

      @Nested
      @DisplayName("fetchOcrData 메서드")
      class FetchOcrData {

          @Test
          @DisplayName("OCR 데이터 조회 성공")
          void fetchOcrData_Success() {
              // Given
              when(ownerMapper.selectContractOwnerId(contractChatId))
                      .thenReturn(Optional.of(ownerId));
              when(contractDocumentMongoRepository.findByContractChatIdAndUserId(
                              contractChatId, ownerId))
                      .thenReturn(contractDocument);

              // When
              ClauseImproveRequestDto.OcrData result =
                      preContractDataService.fetchOcrData(contractChatId);

              // Then
              assertNotNull(result);
              assertEquals("contract.pdf", result.getFileName());
              assertEquals("계약서 내용", result.getRawText());
              assertNotNull(result.getExtractedAt());
              verify(contractDocumentMongoRepository)
                      .findByContractChatIdAndUserId(contractChatId, ownerId);
          }

          @Test
          @DisplayName("OCR 데이터가 없는 경우 null 반환")
          void fetchOcrData_NoData() {
              // Given
              when(ownerMapper.selectContractOwnerId(contractChatId))
                      .thenReturn(Optional.of(ownerId));
              when(contractDocumentMongoRepository.findByContractChatIdAndUserId(
                              contractChatId, ownerId))
                      .thenReturn(null);

              // When
              ClauseImproveRequestDto.OcrData result =
                      preContractDataService.fetchOcrData(contractChatId);

              // Then
              assertNull(result);
          }

          @Test
          @DisplayName("Owner ID를 찾을 수 없는 경우 예외 발생")
          void fetchOcrData_OwnerNotFound() {
              // Given
              when(ownerMapper.selectContractOwnerId(contractChatId)).thenReturn(Optional.empty());

              // When & Then
              assertThrows(
                      IllegalArgumentException.class,
                      () -> preContractDataService.fetchOcrData(contractChatId));
          }
      }
}
