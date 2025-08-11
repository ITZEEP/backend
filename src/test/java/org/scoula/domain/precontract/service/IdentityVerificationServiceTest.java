package org.scoula.domain.precontract.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.precontract.exception.PreContractErrorCode;
import org.scoula.domain.precontract.mapper.OwnerPreContractMapper;
import org.scoula.domain.precontract.vo.IdentityVerificationInfoVO;
import org.scoula.domain.precontract.vo.IdentityVerificationInfoVO.ContractStep;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.util.AesCryptoUtil;

@ExtendWith(MockitoExtension.class)
class IdentityVerificationServiceTest {

      @Mock private OwnerPreContractMapper ownerMapper;

      @Mock private AesCryptoUtil aesCryptoUtil;

      @InjectMocks private IdentityVerificationServiceImpl identityVerificationService;

      private IdentityVerificationInfoVO testVerificationInfo;

      @BeforeEach
      void setUp() {
          testVerificationInfo =
                  IdentityVerificationInfoVO.builder()
                          .identityId(1L)
                          .userId(100L)
                          .contractId(200L)
                          .name("홍길동")
                          .ssnFront("900101")
                          .ssnBack("encrypted_1234567")
                          .addr1("서울시 강남구")
                          .addr2("encrypted_address")
                          .phoneNumber("encrypted_phone")
                          .contractStep(ContractStep.START)
                          .build();
      }

      @Test
      @DisplayName("암호화된 본인 인증 정보를 조회하고 복호화한다")
      void getDecryptedVerificationInfo_Success() {
          // Given
          Long contractChatId = 200L;
          Long userId = 100L;

          when(ownerMapper.selectIdentityVerificationInfo(contractChatId, userId))
                  .thenReturn(Optional.of(testVerificationInfo));
          when(aesCryptoUtil.isEncrypted("encrypted_1234567")).thenReturn(true);
          when(aesCryptoUtil.isEncrypted("encrypted_address")).thenReturn(true);
          when(aesCryptoUtil.isEncrypted("encrypted_phone")).thenReturn(true);
          when(aesCryptoUtil.decrypt("encrypted_1234567")).thenReturn("1234567");
          when(aesCryptoUtil.decrypt("encrypted_address")).thenReturn("101호");
          when(aesCryptoUtil.decrypt("encrypted_phone")).thenReturn("01012345678");

          // When
          IdentityVerificationInfoVO result =
                  identityVerificationService.getDecryptedVerificationInfo(contractChatId, userId);

          // Then
          assertNotNull(result);
          assertEquals("홍길동", result.getName());
          assertEquals("900101", result.getSsnFront());
          assertEquals("1234567", result.getSsnBack());
          assertEquals("서울시 강남구", result.getAddr1());
          assertEquals("101호", result.getAddr2());
          assertEquals("01012345678", result.getPhoneNumber());
      }

      @Test
      @DisplayName("본인 인증 정보가 없으면 예외를 발생시킨다")
      void getDecryptedVerificationInfo_NotFound() {
          // Given
          Long contractChatId = 200L;
          Long userId = 100L;

          when(ownerMapper.selectIdentityVerificationInfo(contractChatId, userId))
                  .thenReturn(Optional.empty());

          // When & Then
          BusinessException exception =
                  assertThrows(
                          BusinessException.class,
                          () ->
                                  identityVerificationService.getDecryptedVerificationInfo(
                                          contractChatId, userId));

          assertEquals(PreContractErrorCode.TENANT_SELECT, exception.getErrorCode());
      }

      @Test
      @DisplayName("마스킹된 본인 인증 정보를 반환한다")
      void getMaskedVerificationInfo_Success() {
          // Given
          Long contractChatId = 200L;
          Long userId = 100L;

          when(ownerMapper.selectIdentityVerificationInfo(contractChatId, userId))
                  .thenReturn(Optional.of(testVerificationInfo));
          when(aesCryptoUtil.isEncrypted(anyString())).thenReturn(true);
          when(aesCryptoUtil.decrypt("encrypted_1234567")).thenReturn("1234567");
          when(aesCryptoUtil.decrypt("encrypted_address")).thenReturn("101호");
          when(aesCryptoUtil.decrypt("encrypted_phone")).thenReturn("01012345678");

          // When
          IdentityVerificationInfoVO result =
                  identityVerificationService.getMaskedVerificationInfo(contractChatId, userId);

          // Then
          assertNotNull(result);
          assertEquals("홍길동", result.getName());
          assertEquals("900101", result.getSsnFront());
          assertEquals("1******", result.getSsnBack()); // 마스킹됨
          assertEquals("서울시 강남구", result.getAddr1());
          assertEquals("101호", result.getAddr2()); // 상세 주소는 유지
          assertEquals("010-****-5678", result.getPhoneNumber()); // 마스킹됨
      }

      @Test
      @DisplayName("암호화되지 않은 데이터는 그대로 반환한다")
      void getDecryptedVerificationInfo_UnencryptedData() {
          // Given
          Long contractChatId = 200L;
          Long userId = 100L;

          IdentityVerificationInfoVO unencryptedInfo =
                  IdentityVerificationInfoVO.builder()
                          .identityId(1L)
                          .userId(100L)
                          .contractId(200L)
                          .name("홍길동")
                          .ssnFront("900101")
                          .ssnBack("1234567") // 암호화되지 않은 데이터
                          .addr1("서울시 강남구")
                          .addr2("101호") // 암호화되지 않은 데이터
                          .phoneNumber("01012345678") // 암호화되지 않은 데이터
                          .contractStep(ContractStep.START)
                          .build();

          when(ownerMapper.selectIdentityVerificationInfo(contractChatId, userId))
                  .thenReturn(Optional.of(unencryptedInfo));
          when(aesCryptoUtil.isEncrypted(anyString())).thenReturn(false);

          // When
          IdentityVerificationInfoVO result =
                  identityVerificationService.getDecryptedVerificationInfo(contractChatId, userId);

          // Then
          assertNotNull(result);
          assertEquals("1234567", result.getSsnBack());
          assertEquals("101호", result.getAddr2());
          assertEquals("01012345678", result.getPhoneNumber());
      }
}
