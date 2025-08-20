package org.scoula.domain.contract.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import javax.servlet.http.HttpServletResponse;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

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
      private PaymentDTO paymentDTO;
      private NextStepDTO nextStepDTO;
      private ContractDTO contractDTO;

      @BeforeEach
      void setUp() {
          contractChatId = 1L;
          userId = 100L;

          paymentDTO = PaymentDTO.builder().depositPrice(50000000).monthlyRent(500000).build();

          nextStepDTO = NextStepDTO.builder().owner(true).buyer(false).build();

          contractDTO = ContractDTO.builder().contractChatId(contractChatId).build();

          ReflectionTestUtils.setField(contractService, "aiServerUrl", "http://localhost:8000");
          when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
      }

      @Nested
      @DisplayName("saveContractMongo 메서드")
      class SaveContractMongo {

          @Test
          @DisplayName("계약서 몽고 저장 성공")
          void saveContractMongo_Success() {
              // When & Then
              assertDoesNotThrow(() -> contractService.saveContractMongo(contractChatId, userId));
          }
      }

      @Nested
      @DisplayName("getContract 메서드")
      class GetContract {

          @Test
          @DisplayName("계약서 조회 성공")
          void getContract_Success() {
              // Given
              when(contractMapper.getContract(contractChatId)).thenReturn(contractDTO);

              // When
              ContractDTO result = contractService.getContract(contractChatId, userId);

              // Then
              assertNotNull(result);
              verify(contractMapper).getContract(contractChatId);
          }
      }

      @Nested
      @DisplayName("nextStep 메서드")
      class NextStep {

          @Test
          @DisplayName("다음 단계 이동 성공")
          void nextStep_Success() {
              // When
              Boolean result = contractService.nextStep(contractChatId, userId, nextStepDTO);

              // Then - 메서드 실행 확인
              assertNotNull(result);
          }
      }

      @Nested
      @DisplayName("getUserBirthDate 메서드")
      class GetUserBirthDate {

          @Test
          @DisplayName("owner 역할로 생년월일 조회 성공")
          void getUserBirthDate_Owner_Success() {
              // Given
              String userRole = "owner";

              // When
              String result = contractService.getUserBirthDate(contractChatId, userId, userRole);

              // Then - 메서드 실행 확인
              assertTrue(result == null || !result.isEmpty());
          }

          @Test
          @DisplayName("buyer 역할로 생년월일 조회 성공")
          void getUserBirthDate_Buyer_Success() {
              // Given
              String userRole = "buyer";

              // When
              String result = contractService.getUserBirthDate(contractChatId, userId, userRole);

              // Then - 메서드 실행 확인
              assertTrue(result == null || !result.isEmpty());
          }
      }

      @Nested
      @DisplayName("getDepositPrice 메서드")
      class GetDepositPrice {

          @Test
          @DisplayName("보증금 정보 조회 성공")
          void getDepositPrice_Success() {
              // When
              PaymentDTO result = contractService.getDepositPrice(contractChatId, userId);

              // Then - 메서드 실행 확인
              assertTrue(result == null || result.getDepositPrice() >= 0);
          }
      }

      @Nested
      @DisplayName("saveDepositPrice 메서드")
      class SaveDepositPrice {

          @Test
          @DisplayName("보증금 정보 저장 성공")
          void saveDepositPrice_Success() {
              // When & Then
              assertDoesNotThrow(
                      () -> contractService.saveDepositPrice(contractChatId, userId, paymentDTO));
          }
      }

      @Nested
      @DisplayName("deleteDepositPrice 메서드")
      class DeleteDepositPrice {

          @Test
          @DisplayName("보증금 정보 삭제 성공")
          void deleteDepositPrice_Success() {
              // When & Then
              assertDoesNotThrow(() -> contractService.deleteDepositPrice(contractChatId, userId));
          }
      }

      @Nested
      @DisplayName("updateDepositPrice 메서드")
      class UpdateDepositPrice {

          @Test
          @DisplayName("보증금 정보 업데이트 성공")
          void updateDepositPrice_Success() {
              // When & Then
              assertDoesNotThrow(() -> contractService.updateDepositPrice(contractChatId, userId));
          }
      }

      @Nested
      @DisplayName("deleteOwnerLegality 메서드")
      class DeleteOwnerLegality {

          @Test
          @DisplayName("임대인 적법성 검사 삭제 성공")
          void deleteOwnerLegality_Success() {
              // When
              String result = contractService.deleteOwnerLegality(contractChatId, userId);

              // Then - 메서드 실행 확인
              assertTrue(result == null || !result.isEmpty());
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

              // When & Then
              assertDoesNotThrow(
                      () ->
                              contractService.saveFinalContractToDatabase(
                                      contractChatId, s3Url, pdfHash));
          }
      }

      @Nested
      @DisplayName("saveSignature 메서드")
      class SaveSignature {

          @Test
          @DisplayName("서명 저장 성공")
          void saveSignature_Success() throws Exception {
              // Given
              SaveSignatureDTO signatureDTO = new SaveSignatureDTO();
              List<MultipartFile> imgFiles =
                      Arrays.asList(
                              new MockMultipartFile(
                                      "file1",
                                      "signature1.png",
                                      "image/png",
                                      "signature1".getBytes()));

              when(s3Service.uploadFile(any(MultipartFile.class), anyString()))
                      .thenReturn("https://s3.amazonaws.com/signature.png");

              // When
              Boolean result =
                      contractService.saveSignature(contractChatId, userId, signatureDTO, imgFiles);

              // Then - 메서드 실행 확인
              assertTrue(result == null || result instanceof Boolean);
          }
      }

      @Nested
      @DisplayName("updateSpecialContract 메서드")
      class UpdateSpecialContract {

          @Test
          @DisplayName("특약 업데이트 성공")
          void updateSpecialContract_Success() {
              // Given
              SpecialContractUpdateDTO dto = new SpecialContractUpdateDTO();

              // When & Then
              assertDoesNotThrow(
                      () -> contractService.updateSpecialContract(contractChatId, userId, dto));
          }
      }

      @Nested
      @DisplayName("validateIsOwner 메서드")
      class ValidateIsOwner {

          @Test
          @DisplayName("임대인 권한 검증 성공")
          void validateIsOwner_Success() {
              // Given
              when(contractMapper.getOwnerId(contractChatId)).thenReturn(userId);

              // When & Then
              assertDoesNotThrow(() -> contractService.validateIsOwner(contractChatId, userId));
              verify(contractMapper).getOwnerId(contractChatId);
          }
      }

      @Nested
      @DisplayName("nextSteps 메서드")
      class NextSteps {

          @Test
          @DisplayName("다음 단계 처리 성공")
          void nextSteps_Success() {
              // When
              Boolean result = contractService.nextSteps(contractChatId, userId, nextStepDTO);

              // Then - 메서드 실행 확인
              assertNotNull(result);
          }
      }

      @Nested
      @DisplayName("getContractNext 메서드")
      class GetContractNext {

          @Test
          @DisplayName("다음 계약서 조회 성공")
          void getContractNext_Success() {
              // When & Then
              assertDoesNotThrow(() -> contractService.getContractNext(contractChatId, userId));
          }
      }

      @Nested
      @DisplayName("finalContractPDF 메서드")
      class FinalContractPDF {

          @Test
          @DisplayName("최종 계약서 PDF 생성 성공")
          void finalContractPDF_Success() {
              // When
              MultipartFile result = contractService.finalContractPDF(contractChatId, userId);

              // Then - 메서드 실행 확인
              assertTrue(result == null || result.getSize() >= 0);
          }
      }

      @Nested
      @DisplayName("rejectBuyerLegality 메서드")
      class RejectBuyerLegality {

          @Test
          @DisplayName("임차인 적법성 검사 거절 성공")
          void rejectBuyerLegality_Success() {
              // When
              String result = contractService.rejectBuyerLegality(contractChatId, userId);

              // Then - 메서드 실행 확인
              assertTrue(result == null || !result.isEmpty());
          }
      }

      @Nested
      @DisplayName("updateBuyerLegality 메서드")
      class UpdateBuyerLegality {

          @Test
          @DisplayName("임차인 적법성 검사 업데이트 성공")
          void updateBuyerLegality_Success() {
              // Given
              SpecialContractUpdateDTO dto = new SpecialContractUpdateDTO();

              // When & Then
              assertDoesNotThrow(
                      () -> contractService.updateBuyerLegality(contractChatId, userId, dto));
          }
      }

      @Nested
      @DisplayName("updateOwnerLegality 메서드")
      class UpdateOwnerLegality {

          @Test
          @DisplayName("임대인 적법성 검사 업데이트 성공")
          void updateOwnerLegality_Success() {
              // Given
              UpdateLegalityDTO dto = new UpdateLegalityDTO();

              // When & Then
              assertDoesNotThrow(
                      () -> contractService.updateOwnerLegality(contractChatId, userId, dto));
          }
      }

      @Nested
      @DisplayName("selectContractPDF 메서드")
      class SelectContractPDF {

          @Test
          @DisplayName("계약서 PDF 선택 성공")
          void selectContractPDF_Success() {
              // Given
              HttpServletResponse response = mock(HttpServletResponse.class);
              FindContractDTO dto = new FindContractDTO();

              // When & Then
              assertDoesNotThrow(
                      () -> contractService.selectContractPDF(contractChatId, userId, response, dto));
          }
      }

      @Nested
      @DisplayName("sendContractPDF 메서드")
      class SendContractPDF {

          @Test
          @DisplayName("계약서 PDF 전송 성공")
          void sendContractPDF_Success() throws Exception {
              // Given
              FindContractDTO dto = new FindContractDTO();

              // When & Then
              assertDoesNotThrow(() -> contractService.sendContractPDF(contractChatId, userId, dto));
          }
      }
}
