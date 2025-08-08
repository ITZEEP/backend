package org.scoula.domain.home.mapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.home.vo.HomeRegisterVO;
import org.scoula.domain.home.vo.HomeReportVO;
import org.scoula.global.common.dto.PageRequest;

@Mapper
public interface HomeMapper {

      /** 사용자 이름 조회 (identity_verification 테이블에서) */
      String findUserNameById(@Param("userId") Long userId);

      /** 매물 전체 조회 (페이징 포함) */
      List<HomeRegisterVO> findHomes(@Param("pageRequest") PageRequest pageRequest);

      /** 매물 총 개수 조회 */
      long countHomes(@Param("pageRequest") PageRequest pageRequest);

      /** 매물 단건 조회 */
      Optional<HomeRegisterVO> findHomeById(@Param("homeId") Long homeId);

      /** 특정 매물 이미지 URL 리스트 조회 추가 */
      List<String> findHomeImagesByHomeId(@Param("homeId") Long homeId);

      /** 매물 등록 */
      void insertHome(
              @Param("userId") Long userId,
              @Param("userName") String userName,
              @Param("home") HomeRegisterVO home);

      /** 매물 수정 */
      void updateHome(@Param("home") HomeRegisterVO home);

      /** 매물 삭제 */
      void deleteHome(@Param("homeId") Long homeId);

      /** 찜 추가 */
      void insertHomeLike(@Param("userId") Long userId, @Param("homeId") Long homeId);

      /** 찜 제거 */
      void deleteLike(@Param("userId") Long userId, @Param("homeId") Long homeId);

      /** 찜한 매물 목록 조회 */
      List<HomeRegisterVO> findLikedHomes(@Param("userId") Long userId);

      /** 조회수 증가 */
      void incrementViewCount(@Param("homeId") Long homeId);

      /** 내가 등록한 매물 리스트 & 개수 */
      List<HomeRegisterVO> findMyHomes(
              @Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);

      long countMyHomes(@Param("userId") Long userId);

      /** 상세 정보 등록 */
      void insertHomeDetail(HomeRegisterVO vo);

      /** 옵션 정보 등록 */
      void insertHomeFacilities(Map<String, Object> param);

      /** 이미지 등록 */
      void insertHomeImages(Map<String, Object> param);

      /** 관리비 항목 등록 */
      void insertHomeMaintenanceFees(
              @Param("homeId") Long homeId, @Param("fees") Map<Long, Integer> fees);

      /** 신고 정보 등록 */
      void insertHomeReport(@Param("report") HomeReportVO report);
}
