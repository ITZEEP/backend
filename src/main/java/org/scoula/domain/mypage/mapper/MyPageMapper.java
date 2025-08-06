package org.scoula.domain.mypage.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.mypage.dto.MyPageContractDto;
import org.scoula.domain.mypage.dto.MyPagePropertyDto;
import org.scoula.domain.mypage.dto.MyPageRiskAnalysisDto;
import org.scoula.domain.mypage.dto.MyPageUserInfoDto;

@Mapper
public interface MyPageMapper {

      /**
       * 사용자의 계약서 목록 조회
       *
       * @param userId 사용자 ID
       * @param offset 페이지 시작 위치
       * @param limit 페이지 크기
       * @return 계약서 목록
       */
      List<MyPageContractDto> selectContractsByUserId(
              @Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

      /**
       * 사용자의 계약서 총 개수 조회
       *
       * @param userId 사용자 ID
       * @return 계약서 총 개수
       */
      int countContractsByUserId(@Param("userId") Long userId);

      /**
       * 사용자의 매물 목록 조회
       *
       * @param userId 사용자 ID
       * @param offset 페이지 시작 위치
       * @param limit 페이지 크기
       * @return 매물 목록
       */
      List<MyPagePropertyDto> selectPropertiesByUserId(
              @Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

      /**
       * 사용자의 매물 총 개수 조회
       *
       * @param userId 사용자 ID
       * @return 매물 총 개수
       */
      int countPropertiesByUserId(@Param("userId") Long userId);

      /**
       * 사용자의 사기위험도 분석 이력 조회
       *
       * @param userId 사용자 ID
       * @param offset 페이지 시작 위치
       * @param limit 페이지 크기
       * @return 분석 이력 목록
       */
      List<MyPageRiskAnalysisDto> selectRiskAnalysesByUserId(
              @Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

      /**
       * 사용자의 사기위험도 분석 총 개수 조회
       *
       * @param userId 사용자 ID
       * @return 분석 총 개수
       */
      int countRiskAnalysesByUserId(@Param("userId") Long userId);

      /**
       * 사용자 정보 조회
       *
       * @param userId 사용자 ID
       * @return 사용자 정보
       */
      MyPageUserInfoDto selectUserInfoByUserId(@Param("userId") Long userId);

      /**
       * 닉네임 중복 확인
       *
       * @param nickname 확인할 닉네임
       * @param userId 현재 사용자 ID (본인 제외)
       * @return 중복 여부
       */
      boolean existsByNickname(@Param("nickname") String nickname, @Param("userId") Long userId);

      /**
       * 프로필 이미지 URL 업데이트
       *
       * @param userId 사용자 ID
       * @param imageUrl 이미지 URL
       */
      void updateProfileImage(@Param("userId") Long userId, @Param("imageUrl") String imageUrl);

      /**
       * 닉네임 업데이트
       *
       * @param userId 사용자 ID
       * @param nickname 새로운 닉네임
       */
      void updateNickname(@Param("userId") Long userId, @Param("nickname") String nickname);

      /**
       * 알림 설정 업데이트
       *
       * @param userId 사용자 ID
       * @param enabled 알림 활성화 여부
       */
      void updateNotificationSetting(@Param("userId") Long userId, @Param("enabled") Boolean enabled);
}
