package org.scoula.domain.mypage.service;

import org.scoula.domain.mypage.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface MyPageService {

      /**
       * 사용자 기본 정보 조회
       *
       * @param userId 사용자 ID
       * @return 사용자 정보
       */
      MyPageUserInfoDto getUserInfo(Long userId);

      /**
       * 프로필 이미지 업데이트
       *
       * @param userId 사용자 ID
       * @param profileImage 프로필 이미지 파일
       * @return 업데이트된 이미지 URL
       */
      String updateProfileImage(Long userId, MultipartFile profileImage);

      /**
       * 닉네임 변경
       *
       * @param userId 사용자 ID
       * @param nickname 새로운 닉네임
       */
      void updateNickname(Long userId, String nickname);

      /**
       * 알림 설정 변경
       *
       * @param userId 사용자 ID
       * @param enabled 알림 활성화 여부
       */
      void updateNotificationSetting(Long userId, Boolean enabled);

      /**
       * 내 계약서 목록 조회
       *
       * @param userId 사용자 ID
       * @param pageable 페이징 정보
       * @return 계약서 목록
       */
      Page<MyPageContractDto> getMyContracts(Long userId, Pageable pageable);

      /**
       * 내 매물 목록 조회
       *
       * @param userId 사용자 ID
       * @param pageable 페이징 정보
       * @return 매물 목록
       */
      Page<MyPagePropertyDto> getMyProperties(Long userId, Pageable pageable);

      /**
       * 내 사기위험도 분석 이력 조회
       *
       * @param userId 사용자 ID
       * @param pageable 페이징 정보
       * @return 분석 이력 목록
       */
      Page<MyPageRiskAnalysisDto> getMyRiskAnalyses(Long userId, Pageable pageable);
}
