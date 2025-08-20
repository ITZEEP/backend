package org.scoula.domain.home.service;

import java.util.List;

import org.scoula.domain.home.dto.HomeCreateDTO;
import org.scoula.domain.home.dto.HomeResponseDTO;
import org.scoula.domain.home.dto.HomeSearchDTO;
import org.scoula.domain.home.vo.FacilityCategory;
import org.scoula.domain.home.vo.FacilityItem;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

public interface HomeService {

      // === 매물 관리 ===
      // 매물 등록 (이미지 선택사항)
      Integer createHome(HomeCreateDTO createDTO, List<MultipartFile> images, Integer userId);

      // 매물 조회
      HomeResponseDTO getHome(Integer homeId, Authentication authentication);

      // 매물 목록 조회
      List<HomeResponseDTO> getHomeList(int page, int size, Authentication authentication);

      // 매물 검색
      List<HomeResponseDTO> searchHomes(HomeSearchDTO searchDTO, Authentication authentication);

      // 매물 수정 (이미지 선택사항)
      void updateHome(
              Integer homeId, HomeCreateDTO updateDTO, List<MultipartFile> images, Integer userId);

      // 매물 삭제
      void deleteHome(Integer homeId, Integer userId);

      // 매물 상태 변경
      void updateHomeStatus(Integer homeId, String status, Integer userId);

      // 사용자별 매물 조회
      List<HomeResponseDTO> getHomesByUser(Integer userId);

      // 매물 총 개수
      int getTotalHomeCount();

      // 조건별 매물 총 개수
      int getHomeCountByCondition(HomeSearchDTO searchDTO);

      // === 편의시설 관리 ===
      // 편의시설 카테고리 목록 조회
      List<FacilityCategory> getFacilityCategories();

      // 편의시설 아이템 목록 조회
      List<FacilityItem> getFacilityItemsByCategory(Integer categoryId);

      // === 찜 관리 ===
      // 찜 등록/해제
      void toggleHomeLike(Integer userId, Integer homeId);

      // 사용자 찜 목록 조회
      List<HomeResponseDTO> getHomeLikes(Integer userId);
}
