package org.scoula.domain.home.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.home.dto.HomeSearchDTO;
import org.scoula.domain.home.vo.*;

@Mapper
public interface HomeMapper {

      HomeVO findHomeById(@Param("id") Long id);

      // === 기본 매물 관리 ===
      // 매물 등록
      int insertHome(HomeVO home);

      // 매물 상세 정보 등록
      int insertHomeDetail(HomeDetailVO homeDetail);

      // 매물 조회
      HomeVO selectHomeById(@Param("homeId") Integer homeId);

      // 매물 상세 조회
      HomeDetailVO selectHomeDetailByHomeId(@Param("homeId") Integer homeId);

      // 매물 목록 조회 (페이징)
      List<HomeVO> selectHomeList(@Param("offset") int offset, @Param("limit") int limit);

      // 매물 검색 (조건별)
      List<HomeVO> selectHomeListByCondition(@Param("search") HomeSearchDTO searchDTO);

      // 매물 총 개수
      int selectHomeCount();

      // 조건별 매물 총 개수
      int selectHomeCountByCondition(@Param("search") HomeSearchDTO searchDTO);

      // 매물 수정
      int updateHome(HomeVO home);

      // 매물 상세 수정
      int updateHomeDetail(HomeDetailVO homeDetail);

      // 매물 삭제
      int deleteHome(@Param("homeId") Integer homeId);

      // 매물 상태 변경
      int updateHomeStatus(@Param("homeId") Integer homeId, @Param("status") String status);

      // 조회수 증가
      int incrementViewCount(@Param("homeId") Integer homeId);

      // 사용자별 매물 조회
      List<HomeVO> selectHomeListByUserId(@Param("userId") Integer userId);

      // === 이미지 관리 ===
      // 매물 이미지 등록
      int insertHomeImage(HomeImageVO homeImage);

      // 매물 이미지 조회
      List<HomeImageVO> selectHomeImagesByHomeId(@Param("homeId") Integer homeId);

      // 매물 이미지 삭제
      int deleteHomeImagesByHomeId(@Param("homeId") Integer homeId);

      // === 편의시설 관리 ===
      // 매물 편의시설 등록
      int insertHomeFacility(HomeFacilityVO homeFacility);

      // 매물 편의시설 조회
      List<FacilityItem> selectHomeFacilitiesByHomeDetailId(
              @Param("homeDetailId") Integer homeDetailId);

      // 매물 편의시설 삭제
      int deleteHomeFacilitiesByHomeDetailId(@Param("homeDetailId") Integer homeDetailId);

      // 편의시설 카테고리 조회
      List<FacilityCategory> selectFacilityCategories();

      // 편의시설 아이템 조회
      List<FacilityItem> selectFacilityItemsByCategoryId(@Param("categoryId") Integer categoryId);

      // === 관리비 관리 ===
      // 매물 관리비 등록
      int insertHomeMaintenanceFee(HomeMaintenanceFeeVO maintenanceFee);

      // 매물 관리비 조회
      List<HomeMaintenanceFeeVO> selectHomeMaintenanceFeesByHomeId(@Param("homeId") Integer homeId);

      // 매물 관리비 삭제
      int deleteHomeMaintenanceFeesByHomeId(@Param("homeId") Integer homeId);

      // === 찜 관리 ===
      // 찜 등록
      int insertHomeLike(HomeLikeVO homeLike);

      // 찜 삭제
      int deleteHomeLike(@Param("userId") Integer userId, @Param("homeId") Integer homeId);

      // 찜 여부 확인
      int selectHomeLikeExists(@Param("userId") Integer userId, @Param("homeId") Integer homeId);

      // 사용자 찜 목록 조회
      List<HomeVO> selectHomeLikesByUserId(@Param("userId") Integer userId);
}
