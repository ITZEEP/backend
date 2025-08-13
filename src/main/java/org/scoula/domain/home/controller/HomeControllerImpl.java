package org.scoula.domain.home.controller;

import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.scoula.domain.home.dto.HomeCreateDTO;
import org.scoula.domain.home.dto.HomeCreateRequestDto;
import org.scoula.domain.home.dto.HomeResponseDTO;
import org.scoula.domain.home.dto.HomeSearchDTO;
import org.scoula.domain.home.dto.HomeUpdateRequestDto;
import org.scoula.domain.home.service.HomeService;
import org.scoula.domain.home.vo.FacilityCategory;
import org.scoula.domain.home.vo.FacilityItem;
import org.scoula.domain.user.service.UserServiceInterface;
import org.scoula.domain.user.vo.User;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.CommonErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping("/api/homes")
@RequiredArgsConstructor
public class HomeControllerImpl implements HomeController {

      private final HomeService homeService;
      private final UserServiceInterface userService;

      @Override
      @PostMapping(consumes = "multipart/form-data")
      public ResponseEntity<ApiResponse<Integer>> createHome(
              @Valid @RequestPart HomeCreateRequestDto requestDto, Authentication authentication) {

          Integer userId = getCurrentUserId(authentication);

          // 이미지 파일 리스트 처리
          List<MultipartFile> imageList = requestDto.getImages();

          // HomeCreateRequestDto를 HomeCreateDTO로 변환
          HomeCreateDTO createDTO =
                  HomeCreateDTO.builder()
                          .addr1(requestDto.getAddr1())
                          .addr2(requestDto.getAddr2())
                          .residenceType(requestDto.getResidenceType())
                          .leaseType(requestDto.getLeaseType())
                          .depositPrice(requestDto.getDepositPrice())
                          .monthlyRent(requestDto.getMonthlyRent())
                          .maintenaceFee(requestDto.getMaintenanceFee())
                          .roomCnt(requestDto.getRoomCnt())
                          .supplyArea(requestDto.getSupplyArea())
                          .exclusiveArea(requestDto.getExclusiveArea())
                          .buildDate(requestDto.getBuildDate())
                          .homeFloor(requestDto.getHomeFloor())
                          .buildingTotalFloors(requestDto.getBuildingTotalFloors())
                          .homeDirection(requestDto.getHomeDirection())
                          .area(requestDto.getArea())
                          .landCategory(requestDto.getLandCategory())
                          .bathroomCnt(requestDto.getBathroomCnt())
                          .isPet(requestDto.getIsPet())
                          .isParking(requestDto.getIsParking())
                          .facilityItemIds(requestDto.getFacilityItemIds())
                          .maintenanceFees(
                                  requestDto.getMaintenanceFees() != null
                                          ? requestDto.getMaintenanceFees().stream()
                                                  .map(
                                                          fee ->
                                                                  HomeCreateDTO.MaintenanceFeeDTO
                                                                          .builder()
                                                                          .maintenanceId(
                                                                                  fee
                                                                                          .getMaintenanceId())
                                                                          .fee(fee.getFee())
                                                                          .build())
                                                  .collect(java.util.stream.Collectors.toList())
                                          : null)
                          .build();

          Integer homeId = homeService.createHome(createDTO, imageList, userId);

          return ResponseEntity.ok(ApiResponse.success(homeId, "매물이 성공적으로 등록되었습니다."));
      }

      @Override
      @GetMapping("/{homeId}")
      public ResponseEntity<ApiResponse<HomeResponseDTO>> getHome(@PathVariable Integer homeId) {

          log.info("매물 상세 조회 요청: homeId={}", homeId);

          HomeResponseDTO home = homeService.getHome(homeId);

          return ResponseEntity.ok(ApiResponse.success(home, "매물 조회가 완료되었습니다."));
      }

      @Override
      @GetMapping
      public ResponseEntity<ApiResponse<List<HomeResponseDTO>>> getHomeList(
              @RequestParam(defaultValue = "1") int page,
              @RequestParam(defaultValue = "20") int size) {

          log.info("매물 목록 조회 요청: page={}, size={}", page, size);

          List<HomeResponseDTO> homes = homeService.getHomeList(page, size);
          int totalCount = homeService.getTotalHomeCount();

          return ResponseEntity.ok(
                  ApiResponse.success(
                          homes, String.format("매물 목록 조회가 완료되었습니다. (총 %d개)", totalCount)));
      }

      @Override
      @GetMapping("/search")
      public ResponseEntity<ApiResponse<List<HomeResponseDTO>>> searchHomes(
              @ModelAttribute HomeSearchDTO searchDTO) {

          log.info(
                  "매물 검색 요청: residenceType={}, leaseType={}, addr1={}",
                  searchDTO.getResidenceType(),
                  searchDTO.getLeaseType(),
                  searchDTO.getAddr1());

          List<HomeResponseDTO> homes = homeService.searchHomes(searchDTO);
          int totalCount = homeService.getHomeCountByCondition(searchDTO);

          return ResponseEntity.ok(
                  ApiResponse.success(homes, String.format("매물 검색이 완료되었습니다. (총 %d개)", totalCount)));
      }

      @Override
      @PutMapping("/{homeId}")
      public ResponseEntity<ApiResponse<Void>> updateHome(
              @PathVariable Integer homeId,
              @Valid @ModelAttribute HomeUpdateRequestDto updateDto,
              @RequestParam(value = "newImages", required = false) List<MultipartFile> newImages,
              Authentication authentication) {

          Integer userId = getCurrentUserId(authentication);

          log.info(
                  "매물 수정 요청: homeId={}, userId={}, 새 이미지 개수={}, 삭제할 이미지 개수={}",
                  homeId,
                  userId,
                  newImages != null ? newImages.size() : 0,
                  updateDto.getDeleteImageIds() != null ? updateDto.getDeleteImageIds().size() : 0);

          // 요청 DTO의 homeId 설정 (경로 변수의 homeId 사용)
          updateDto.setHomeId(homeId);

          // HomeUpdateRequestDto를 HomeCreateDTO로 변환
          HomeCreateDTO updateDTO =
                  HomeCreateDTO.builder()
                          .addr1(updateDto.getAddr1())
                          .addr2(updateDto.getAddr2())
                          .residenceType(updateDto.getResidenceType())
                          .leaseType(updateDto.getLeaseType())
                          .depositPrice(updateDto.getDepositPrice())
                          .monthlyRent(updateDto.getMonthlyRent())
                          .maintenaceFee(updateDto.getMaintenanceFee())
                          .roomCnt(updateDto.getRoomCnt())
                          .supplyArea(updateDto.getSupplyArea())
                          .exclusiveArea(updateDto.getExclusiveArea())
                          .buildDate(updateDto.getBuildDate())
                          .homeFloor(updateDto.getHomeFloor())
                          .buildingTotalFloors(updateDto.getBuildingTotalFloors())
                          .homeDirection(updateDto.getHomeDirection())
                          .bathroomCnt(updateDto.getBathroomCnt())
                          .isPet(updateDto.getIsPet())
                          .isParking(updateDto.getIsParking())
                          .facilityItemIds(updateDto.getFacilityItemIds())
                          .maintenanceFees(
                                  updateDto.getMaintenanceFees() != null
                                          ? updateDto.getMaintenanceFees().stream()
                                                  .map(
                                                          fee ->
                                                                  HomeCreateDTO.MaintenanceFeeDTO
                                                                          .builder()
                                                                          .maintenanceId(
                                                                                  fee
                                                                                          .getMaintenanceId())
                                                                          .fee(fee.getFee())
                                                                          .build())
                                                  .collect(java.util.stream.Collectors.toList())
                                          : null)
                          .build();

          homeService.updateHome(homeId, updateDTO, newImages, userId);

          return ResponseEntity.ok(ApiResponse.success(null, "매물이 성공적으로 수정되었습니다."));
      }

      @Override
      @DeleteMapping("/{homeId}")
      public ResponseEntity<ApiResponse<Void>> deleteHome(
              @PathVariable Integer homeId, Authentication authentication) {

          Integer userId = getCurrentUserId(authentication);

          log.info("매물 삭제 요청: homeId={}, userId={}", homeId, userId);

          homeService.deleteHome(homeId, userId);

          return ResponseEntity.ok(ApiResponse.success(null, "매물이 성공적으로 삭제되었습니다."));
      }

      @Override
      @PatchMapping("/{homeId}/status")
      public ResponseEntity<ApiResponse<Void>> updateHomeStatus(
              @PathVariable Integer homeId,
              @RequestParam String status,
              Authentication authentication) {

          Integer userId = getCurrentUserId(authentication);

          log.info("매물 상태 변경 요청: homeId={}, status={}, userId={}", homeId, status, userId);

          homeService.updateHomeStatus(homeId, status, userId);

          return ResponseEntity.ok(ApiResponse.success(null, "매물 상태가 성공적으로 변경되었습니다."));
      }

      @Override
      @GetMapping("/my")
      public ResponseEntity<ApiResponse<List<HomeResponseDTO>>> getMyHomes(
              Authentication authentication) {

          Integer userId = getCurrentUserId(authentication);

          log.info("내 매물 목록 조회 요청: userId={}", userId);

          List<HomeResponseDTO> homes = homeService.getHomesByUser(userId);

          return ResponseEntity.ok(
                  ApiResponse.success(
                          homes, String.format("내 매물 목록 조회가 완료되었습니다. (총 %d개)", homes.size())));
      }

      @Override
      @PostMapping("/{homeId}/like")
      public ResponseEntity<ApiResponse<Void>> toggleHomeLike(
              @PathVariable Integer homeId, Authentication authentication) {

          Integer userId = getCurrentUserId(authentication);

          log.info("매물 찜 토글 요청: homeId={}, userId={}", homeId, userId);

          homeService.toggleHomeLike(userId, homeId);

          return ResponseEntity.ok(ApiResponse.success(null, "찜 상태가 변경되었습니다."));
      }

      @Override
      @GetMapping("/likes")
      public ResponseEntity<ApiResponse<List<HomeResponseDTO>>> getHomeLikes(
              Authentication authentication) {

          Integer userId = getCurrentUserId(authentication);

          log.info("찜한 매물 목록 조회 요청: userId={}", userId);

          List<HomeResponseDTO> homes = homeService.getHomeLikes(userId);

          return ResponseEntity.ok(
                  ApiResponse.success(
                          homes, String.format("찜한 매물 목록 조회가 완료되었습니다. (총 %d개)", homes.size())));
      }

      @Override
      @GetMapping("/facilities/categories")
      public ResponseEntity<ApiResponse<List<FacilityCategory>>> getFacilityCategories() {

          log.info("편의시설 카테고리 조회 요청");

          List<FacilityCategory> categories = homeService.getFacilityCategories();

          return ResponseEntity.ok(ApiResponse.success(categories, "편의시설 카테고리 조회가 완료되었습니다."));
      }

      @Override
      @GetMapping("/facilities/categories/{categoryId}/items")
      public ResponseEntity<ApiResponse<List<FacilityItem>>> getFacilityItems(
              @PathVariable Integer categoryId) {

          log.info("편의시설 아이템 조회 요청: categoryId={}", categoryId);

          List<FacilityItem> items = homeService.getFacilityItemsByCategory(categoryId);

          return ResponseEntity.ok(ApiResponse.success(items, "편의시설 아이템 조회가 완료되었습니다."));
      }

      /** Authentication에서 사용자 ID를 추출하는 메서드 실제 구현시에는 JWT에서 사용자 정보를 추출 */
      private Integer getCurrentUserId(Authentication authentication) {
          if (authentication == null || !authentication.isAuthenticated()) {
              throw new BusinessException(CommonErrorCode.AUTHENTICATION_FAILED, "인증되지 않은 사용자입니다.");
          }

          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(CommonErrorCode.AUTHENTICATION_FAILED, "사용자를 찾을 수 없습니다.");
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();

          // Long을 Integer로 변환 (기존 코드와의 호환성을 위해)
          return userId.intValue();
      }
}
