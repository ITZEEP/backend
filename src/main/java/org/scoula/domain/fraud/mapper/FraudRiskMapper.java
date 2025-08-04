package org.scoula.domain.fraud.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.fraud.dto.response.RiskCheckDetailResponse;
import org.scoula.domain.fraud.dto.response.RiskCheckListResponse;
import org.scoula.domain.fraud.vo.RiskCheckDetailVO;
import org.scoula.domain.fraud.vo.RiskCheckVO;
import org.scoula.global.common.dto.PageRequest;

@Mapper
public interface FraudRiskMapper {

      // ========== RiskCheck 관련 메서드 ==========

      /**
       * 위험도 체크 정보 저장
       *
       * @param riskCheck 저장할 위험도 체크 정보
       * @return 저장된 행 수
       */
      int insertRiskCheck(RiskCheckVO riskCheck);

      /**
       * 위험도 체크 정보 수정
       *
       * @param riskCheck 수정할 위험도 체크 정보
       * @return 수정된 행 수
       */
      int updateRiskCheck(RiskCheckVO riskCheck);

      /**
       * 위험도 체크 정보 삭제
       *
       * @param riskckId 위험도 체크 ID
       * @return 삭제된 행 수
       */
      int deleteRiskCheck(@Param("riskckId") Long riskckId);

      // ========== RiskCheckDetail 관련 메서드 ==========

      /**
       * 위험도 체크 상세 정보 저장
       *
       * @param detail 저장할 상세 정보
       * @return 저장된 행 수
       */
      int insertRiskCheckDetail(RiskCheckDetailVO detail);

      /**
       * 위험도 체크 상세 정보 조회
       *
       * @param riskckId 위험도 체크 ID
       * @return 상세 정보 리스트
       */
      List<RiskCheckDetailVO> selectRiskCheckDetailByRiskCheckId(@Param("riskckId") Long riskckId);

      /**
       * 위험도 체크 상세 정보 삭제
       *
       * @param riskckId 위험도 체크 ID
       * @return 삭제된 행 수
       */
      int deleteRiskCheckDetail(@Param("riskckId") Long riskckId);

      // ========== 조회 관련 메서드 ==========

      /**
       * 사용자별 위험도 체크 목록 조회
       *
       * @param userId 사용자 ID
       * @param pageRequest 페이징 요청 정보
       * @return 위험도 체크 목록
       */
      List<RiskCheckListResponse> selectRiskChecksByUserId(
              @Param("userId") Long userId, @Param("pageRequest") PageRequest pageRequest);

      /**
       * 사용자별 위험도 체크 총 개수 조회
       *
       * @param userId 사용자 ID
       * @return 총 개수
       */
      long countRiskChecksByUserId(@Param("userId") Long userId);

      /**
       * 위험도 체크 상세 정보 조회 (DTO 반환)
       *
       * @param riskckId 위험도 체크 ID
       * @return 상세 정보 응답 DTO
       */
      RiskCheckDetailResponse selectRiskCheckDetailResponse(@Param("riskckId") Long riskckId);

      // ========== 유틸리티 메서드 ==========

      /**
       * 매물 존재 여부 확인
       *
       * @param homeId 매물 ID
       * @return 존재 여부
       */
      boolean existsHome(@Param("homeId") Long homeId);

      /**
       * 사용자가 해당 risk check의 소유자인지 확인
       *
       * @param riskckId 위험도 체크 ID
       * @param userId 사용자 ID
       * @return 소유자 여부
       */
      boolean isOwnerOfRiskCheck(@Param("riskckId") Long riskckId, @Param("userId") Long userId);

      /**
       * 매물 ID로 가장 최근의 위험도 체크 정보 조회
       *
       * @param homeId 매물 ID
       * @return 가장 최근의 위험도 체크 정보
       */
      RiskCheckVO selectLatestRiskCheckByHomeId(@Param("homeId") Long homeId);

      /**
       * 위험도 체크 ID로 위험도 체크 정보 조회
       *
       * @param riskckId 위험도 체크 ID
       * @return 위험도 체크 정보
       */
      RiskCheckVO selectRiskCheckById(@Param("riskckId") Long riskckId);

      /**
       * 오늘 분석한 위험도 체크 ID 조회
       *
       * @param userId 사용자 ID
       * @param homeId 매물 ID
       * @param startOfDay 오늘 시작 시간
       * @param endOfDay 오늘 종료 시간
       * @return 위험도 체크 ID (없으면 null)
       */
      Long selectTodayRiskCheckId(
              @Param("userId") Long userId,
              @Param("homeId") Long homeId,
              @Param("startOfDay") java.time.LocalDateTime startOfDay,
              @Param("endOfDay") java.time.LocalDateTime endOfDay);
}
