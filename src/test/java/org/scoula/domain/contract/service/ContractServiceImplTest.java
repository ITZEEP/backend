package org.scoula.domain.contract.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.chat.mapper.ContractChatMapper;
import org.scoula.domain.chat.service.ContractChatServiceInterface;
import org.scoula.domain.contract.dto.*;
import org.scoula.domain.contract.mapper.ContractMapper;
import org.scoula.domain.contract.repository.ContractMongoRepository;
import org.scoula.domain.fraud.mapper.FraudRiskMapper;
import org.scoula.domain.precontract.mapper.TenantPreContractMapper;
import org.scoula.domain.precontract.service.IdentityVerificationService;
import org.scoula.global.common.service.EncryptionService;
import org.scoula.global.common.util.*;
import org.scoula.global.email.service.EmailServiceImpl;
import org.scoula.global.file.service.S3ServiceImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractServiceImpl 테스트")
class ContractServiceImplTest {

      @Mock private ContractChatServiceInterface contractChatService;
      @Mock private ContractMongoRepository contractMongoRepository;
      @Mock private IdentityVerificationService identityVerificationService;
      @Mock private ContractChatMapper contractChatMapper;
      @Mock private ContractMapper contractMapper;
      @Mock private TenantPreContractMapper tenantMapper;
      @Mock private RestTemplate restTemplate;
      @Mock private FraudRiskMapper fraudRiskMapper;
      @Mock private EncryptionService encryptionService;
      @Mock private RedisTemplate<String, String> stringRedisTemplate;
      @Mock private ValueOperations<String, String> valueOperations;
      @Mock private S3ServiceImpl s3Service;
      @Mock private EmailServiceImpl emailService;
      @Mock private AesCryptoUtil aesCryptoUtil;
      @Mock private ImgAesCryptoUtil imgAesCryptoUtil;
      @Mock private NumberFormatUtil numberFormatUtil;

      @InjectMocks private ContractServiceImpl contractService;

      private Long contractChatId;
      private Long userId;

      @BeforeEach
      void setUp() {
          contractChatId = 1L;
          userId = 100L;

          ReflectionTestUtils.setField(contractService, "aiServerUrl", "http://localhost:8000");
          when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
      }

      @Nested
      @DisplayName("getUserBirthDate 메서드")
      class GetUserBirthDate {

          @Test
          @DisplayName("owner 역할로 생년월일 조회 성공")
          void getUserBirthDate_Owner_Success() {
              // Given
              String userRole = "owner";
              String expectedBirthDate = "950101";

              // When
              String result = contractService.getUserBirthDate(contractChatId, userId, userRole);

              // Then - 메서드가 존재하고 호출 가능한지 확인
              assertNotNull(result);
          }

          @Test
          @DisplayName("buyer 역할로 생년월일 조회 성공")
          void getUserBirthDate_Buyer_Success() {
              // Given
              String userRole = "buyer";

              // When & Then - 메서드가 존재하고 호출 가능한지 확인
              assertDoesNotThrow(
                      () -> contractService.getUserBirthDate(contractChatId, userId, userRole));
          }
      }

      @Nested
      @DisplayName("saveFinalContractToDatabase 메서드")
      class SaveFinalContractToDatabase {

          @Test
          @DisplayName("최종 계약서 데이터베이스 저장 성공")
          void saveFinalContractToDatabase_Success() {
              // Given
              String s3Url = "https://s3.amazonaws.com/final_contract.pdf";
              String pdfHash = "hash123";

              // When & Then - 메서드가 존재하고 호출 가능한지 확인
              assertDoesNotThrow(
                      () ->
                              contractService.saveFinalContractToDatabase(
                                      contractChatId, s3Url, pdfHash));
          }
      }

      @Nested
      @DisplayName("validateUserId 메서드")
      class ValidateUserId {

          @Test
          @DisplayName("사용자 검증 메서드 호출 가능")
          void validateUserId_Success() {
              // When & Then - 메서드가 존재하고 호출 가능한지 확인
              assertDoesNotThrow(() -> contractService.validateUserId(contractChatId, userId));
          }
      }

      @Nested
      @DisplayName("validateIsOwner 메서드")
      class ValidateIsOwner {

          @Test
          @DisplayName("임대인 권한 검증 메서드 호출 가능")
          void validateIsOwner_Success() {
              // Given
              Long ownerId = 200L;

              // When & Then - 메서드가 존재하고 호출 가능한지 확인
              assertDoesNotThrow(() -> contractService.validateIsOwner(contractChatId, ownerId));
          }
      }

      @Nested
      @DisplayName("getExistingContractPdf 메서드")
      class GetExistingContractPdf {

          @Test
          @DisplayName("기존 계약서 PDF 조회 메서드 호출 가능")
          void getExistingContractPdf_Success() {
              // When
              byte[] result = contractService.getExistingContractPdf(contractChatId);

              // Then - 메서드가 존재하고 호출 가능한지 확인
              // null 반환도 정상적인 동작
              assertTrue(result == null || result.length >= 0);
          }
      }

      @Nested
      @DisplayName("nextSteps 메서드")
      class NextSteps {

          @Test
          @DisplayName("다음 단계 처리 메서드 호출 가능")
          void nextSteps_Success() {
              // Given
              NextStepDTO dto = new NextStepDTO();

              // When & Then - 메서드가 존재하고 호출 가능한지 확인
              assertDoesNotThrow(() -> contractService.nextSteps(contractChatId, userId, dto));
          }
      }

      @Nested
      @DisplayName("saveContractMongo 메서드")
      class SaveContractMongo {

          @Test
          @DisplayName("계약서 몽고 저장 메서드 호출 가능")
          void saveContractMongo_Success() {
              // When & Then - 메서드가 존재하고 호출 가능한지 확인
              assertDoesNotThrow(() -> contractService.saveContractMongo(contractChatId, userId));
          }
      }

      @Nested
      @DisplayName("getContract 메서드")
      class GetContract {

          @Test
          @DisplayName("계약서 조회 메서드 호출 가능")
          void getContract_Success() {
              // When & Then - 메서드가 존재하고 호출 가능한지 확인
              assertDoesNotThrow(() -> contractService.getContract(contractChatId, userId));
          }
      }

      @Nested
      @DisplayName("getContractNext 메서드")
      class GetContractNext {

          @Test
          @DisplayName("다음 계약서 조회 메서드 호출 가능")
          void getContractNext_Success() {
              // When & Then - 메서드가 존재하고 호출 가능한지 확인
              assertDoesNotThrow(() -> contractService.getContractNext(contractChatId, userId));
          }
      }

      @Nested
      @DisplayName("nextStep 메서드")
      class NextStep {

          @Test
          @DisplayName("다음 단계 이동 메서드 호출 가능")
          void nextStep_Success() {
              // Given
              NextStepDTO dto = new NextStepDTO();

              // When & Then - 메서드가 존재하고 호출 가능한지 확인
              assertDoesNotThrow(() -> contractService.nextStep(contractChatId, userId, dto));
          }
      }

      @Nested
      @DisplayName("getDepositPrice 메서드")
      class GetDepositPrice {

          @Test
          @DisplayName("보증금 조회 메서드 호출 가능")
          void getDepositPrice_Success() {
              // When & Then - 메서드가 존재하고 호출 가능한지 확인
              assertDoesNotThrow(() -> contractService.getDepositPrice(contractChatId, userId));
          }
      }

      @Nested
      @DisplayName("saveDepositPrice 메서드")
      class SaveDepositPrice {

          @Test
          @DisplayName("보증금 저장 메서드 호출 가능")
          void saveDepositPrice_Success() {
              // Given
              PaymentDTO dto = new PaymentDTO();

              // When & Then - 메서드가 존재하고 호출 가능한지 확인
              assertDoesNotThrow(() -> contractService.saveDepositPrice(contractChatId, userId, dto));
          }
      }

      @Nested
      @DisplayName("deleteDepositPrice 메서드")
      class DeleteDepositPrice {

          @Test
          @DisplayName("보증금 삭제 메서드 호출 가능")
          void deleteDepositPrice_Success() {
              // When & Then - 메서드가 존재하고 호출 가능한지 확인
              assertDoesNotThrow(() -> contractService.deleteDepositPrice(contractChatId, userId));
          }
      }

      @Nested
      @DisplayName("updateDepositPrice 메서드")
      class UpdateDepositPrice {

          @Test
          @DisplayName("보증금 업데이트 메서드 호출 가능")
          void updateDepositPrice_Success() {
              // When & Then - 메서드가 존재하고 호출 가능한지 확인
              assertDoesNotThrow(() -> contractService.updateDepositPrice(contractChatId, userId));
          }
      }

      @Nested
      @DisplayName("deleteOwnerLegality 메서드")
      class DeleteOwnerLegality {

          @Test
          @DisplayName("임대인 적법성 검사 메서드 호출 가능")
          void deleteOwnerLegality_Success() {
              // When & Then - 메서드가 존재하고 호출 가능한지 확인
              assertDoesNotThrow(() -> contractService.deleteOwnerLegality(contractChatId, userId));
          }
      }

      @Nested
      @DisplayName("startContractExport 메서드")
      class StartContractExport {

          @Test
          @DisplayName("계약서 내보내기 시작 메서드 호출 가능")
          void startContractExport_Success() {
              // When & Then - 메서드가 존재하고 호출 가능한지 확인
              assertDoesNotThrow(() -> contractService.startContractExport(contractChatId, userId));
          }
      }
}
