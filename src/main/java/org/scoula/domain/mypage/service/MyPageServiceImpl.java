package org.scoula.domain.mypage.service;

import java.util.List;

import org.scoula.domain.mypage.dto.*;
import org.scoula.domain.mypage.exception.MyPageErrorCode;
import org.scoula.domain.mypage.mapper.MyPageMapper;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional(readOnly = true)
public class MyPageServiceImpl implements MyPageService {

      private final MyPageMapper myPageMapper;
      private final ProfileImageService profileImageService;

      @Override
      public MyPageUserInfoDto getUserInfo(Long userId) {
          log.info("사용자 정보 조회 - userId: {}", userId);

          MyPageUserInfoDto userInfo = myPageMapper.selectUserInfoByUserId(userId);
          if (userInfo == null) {
              throw new BusinessException(MyPageErrorCode.USER_NOT_FOUND);
          }

          return userInfo;
      }

      @Override
      @Transactional
      public String updateProfileImage(Long userId, MultipartFile profileImage) {
          log.info("프로필 이미지 업데이트 시작 - userId: {}", userId);

          try {
              // 현재 프로필 이미지 URL 조회 (삭제용)
              MyPageUserInfoDto currentUser = myPageMapper.selectUserInfoByUserId(userId);
              String previousImageUrl = currentUser != null ? currentUser.getProfileImageUrl() : null;
              log.info("현재 프로필 이미지: {}", previousImageUrl);

              // ProfileImageService를 통해 새 이미지 업로드 (이전 이미지 자동 삭제)
              String newImageUrl =
                      profileImageService.uploadProfileImage(profileImage, userId, previousImageUrl);
              log.info("S3 업로드 완료 - 새 이미지 URL: {}", newImageUrl);

              // DB에 새 이미지 URL 업데이트
              myPageMapper.updateProfileImage(userId, newImageUrl);
              log.info("DB 업데이트 완료 - userId: {}, imageUrl: {}", userId, newImageUrl);

              return newImageUrl;

          } catch (BusinessException e) {
              throw e;  // 비즈니스 예외는 그대로 전파
          } catch (Exception e) {
              log.error("프로필 이미지 업데이트 실패 - userId: {}", userId, e);
              throw new BusinessException(MyPageErrorCode.IMAGE_UPLOAD_FAILED);
          }
      }

      @Override
      @Transactional
      public void updateNickname(Long userId, String nickname) {
          log.info("닉네임 변경 - userId: {}, nickname: {}", userId, nickname);

          // 닉네임 중복 체크
          boolean isDuplicated = myPageMapper.existsByNickname(nickname, userId);
          if (isDuplicated) {
              throw new BusinessException(MyPageErrorCode.DUPLICATE_NICKNAME);
          }

          // 닉네임 업데이트
          myPageMapper.updateNickname(userId, nickname);

          log.info("닉네임 변경 완료 - userId: {}", userId);
      }

      @Override
      @Transactional
      public void updateNotificationSetting(Long userId, Boolean enabled) {
          log.info("알림 설정 변경 - userId: {}, enabled: {}", userId, enabled);

          // 알림 설정 업데이트
          myPageMapper.updateNotificationSetting(userId, enabled);

          log.info("알림 설정 변경 완료 - userId: {}", userId);
      }

      @Override
      public Page<MyPageContractDto> getMyContracts(Long userId, Pageable pageable) {
          log.info("내 계약서 목록 조회 - userId: {}", userId);

          int offset = (int) pageable.getOffset();
          int limit = pageable.getPageSize();

          List<MyPageContractDto> contracts =
                  myPageMapper.selectContractsByUserId(userId, offset, limit);
          int total = myPageMapper.countContractsByUserId(userId);

          return new PageImpl<>(contracts, pageable, total);
      }

      @Override
      public Page<MyPagePropertyDto> getMyProperties(Long userId, Pageable pageable) {
          log.info("내 매물 목록 조회 - userId: {}", userId);

          int offset = (int) pageable.getOffset();
          int limit = pageable.getPageSize();

          List<MyPagePropertyDto> properties =
                  myPageMapper.selectPropertiesByUserId(userId, offset, limit);
          int total = myPageMapper.countPropertiesByUserId(userId);

          return new PageImpl<>(properties, pageable, total);
      }

      @Override
      public Page<MyPageRiskAnalysisDto> getMyRiskAnalyses(Long userId, Pageable pageable) {
          log.info("내 사기위험도 분석 이력 조회 - userId: {}", userId);

          int offset = (int) pageable.getOffset();
          int limit = pageable.getPageSize();

          List<MyPageRiskAnalysisDto> analyses =
                  myPageMapper.selectRiskAnalysesByUserId(userId, offset, limit);
          int total = myPageMapper.countRiskAnalysesByUserId(userId);

          return new PageImpl<>(analyses, pageable, total);
      }
}
