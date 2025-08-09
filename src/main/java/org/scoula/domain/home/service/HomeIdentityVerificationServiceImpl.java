package org.scoula.domain.home.service;

import static org.scoula.domain.home.constant.HomeIdentityConstants.*;

import java.time.LocalDate;

import org.scoula.domain.home.dto.HomeIdentityVerificationDTO;
import org.scoula.domain.home.exception.HomeErrorCode;
import org.scoula.domain.home.mapper.HomeIdentityMapper;
import org.scoula.domain.home.util.HomeIdentityUtil;
import org.scoula.domain.home.vo.HomeIdentityVO;
import org.scoula.domain.verification.dto.request.IdCardVerificationRequest;
import org.scoula.domain.verification.service.IdCardVerificationService;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class HomeIdentityVerificationServiceImpl implements HomeIdentityVerificationService {

      private final IdCardVerificationService idCardVerificationService;
      private final HomeIdentityMapper homeIdentityMapper;

      @Override
      @Transactional
      public Void verifyAndSaveIdentity(Long homeId, Long userId, HomeIdentityVerificationDTO dto) {
          log.info(LOG_VERIFY_START, homeId, userId);

          // 생년월일 추출 및 설정
          LocalDate birthDate =
                  HomeIdentityUtil.extractBirthDateFromSSN(dto.getSsnFront(), dto.getSsnBack());
          dto.setBirthDate(birthDate);

          // 신분증 진위 확인
          verifyIdCard(dto);

          // 본인 인증 정보 저장 또는 업데이트
          saveOrUpdateIdentity(homeId, userId, dto.getName(), birthDate);

          return null;
      }

      /** 신분증 진위 확인 */
      private void verifyIdCard(HomeIdentityVerificationDTO dto) {
          IdCardVerificationRequest request = buildVerificationRequest(dto);
          boolean verificationResult = idCardVerificationService.verifyIdCard(request);

          if (!verificationResult) {
              log.warn(LOG_VERIFY_FAIL, dto.getName());
              throw new BusinessException(HomeErrorCode.HOME_IDENTITY_VERIFICATION_FAILED);
          }
          log.info(LOG_VERIFY_SUCCESS);
      }

      /** 신분증 진위 확인 요청 객체 생성 */
      private IdCardVerificationRequest buildVerificationRequest(HomeIdentityVerificationDTO dto) {
          return IdCardVerificationRequest.builder()
                  .name(dto.getName())
                  .rrn1(dto.getSsnFront())
                  .rrn2(dto.getSsnBack())
                  .date(dto.getIssuedDate())
                  .build();
      }

      /** 본인 인증 정보 저장 또는 업데이트 */
      private void saveOrUpdateIdentity(Long homeId, Long userId, String name, LocalDate birthDate) {
          HomeIdentityVO vo = buildIdentityVO(homeId, userId, name, birthDate);

          homeIdentityMapper
                  .selectHomeIdentity(homeId, userId)
                  .ifPresentOrElse(
                          existing -> updateIdentity(homeId, userId, vo),
                          () -> insertIdentity(homeId, userId, vo));
      }

      /** 본인 인증 정보 VO 생성 */
      private HomeIdentityVO buildIdentityVO(
              Long homeId, Long userId, String name, LocalDate birthDate) {
          return HomeIdentityVO.builder()
                  .homeId(homeId)
                  .userId(userId)
                  .name(name)
                  .birthDate(birthDate)
                  .build();
      }

      /** 본인 인증 정보 업데이트 */
      private void updateIdentity(Long homeId, Long userId, HomeIdentityVO vo) {
          int updated = homeIdentityMapper.updateHomeIdentity(vo);
          if (updated == 0) {
              throw new BusinessException(HomeErrorCode.HOME_IDENTITY_UPDATE_FAILED);
          }
          log.info(LOG_UPDATE_SUCCESS, homeId, userId);
      }

      /** 본인 인증 정보 신규 저장 */
      private void insertIdentity(Long homeId, Long userId, HomeIdentityVO vo) {
          homeIdentityMapper.insertHomeIdentity(vo);
          log.info(LOG_INSERT_SUCCESS, homeId, userId);
      }

      @Override
      public HomeIdentityVerificationDTO getIdentityVerification(Long homeId) {
          HomeIdentityVO identity = findIdentityByHomeId(homeId);
          return buildMaskedIdentityDTO(identity);
      }

      /** 매물 ID로 본인 인증 정보 조회 */
      private HomeIdentityVO findIdentityByHomeId(Long homeId) {
          return homeIdentityMapper
                  .selectHomeIdentityByHomeId(homeId)
                  .orElseThrow(() -> new BusinessException(HomeErrorCode.HOME_IDENTITY_NOT_FOUND));
      }

      /** 마스킹된 본인 인증 정보 DTO 생성 */
      private HomeIdentityVerificationDTO buildMaskedIdentityDTO(HomeIdentityVO identity) {
          return HomeIdentityVerificationDTO.builder()
                  .name(HomeIdentityUtil.maskName(identity.getName()))
                  .ssnFront(HomeIdentityUtil.maskSSNFront(identity.getBirthDate()))
                  .ssnBack(HomeIdentityUtil.maskSSNBack())
                  .issuedDate(null) // 발급일자는 저장하지 않음
                  .birthDate(identity.getBirthDate())
                  .build();
      }
}
