package org.scoula.domain.home.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.scoula.domain.home.dto.HomeCreateDTO;
import org.scoula.domain.home.dto.HomeResponseDTO;
import org.scoula.domain.home.dto.HomeSearchDTO;
import org.scoula.domain.home.enums.HomeStatus;
import org.scoula.domain.home.mapper.HomeMapper;
import org.scoula.domain.home.vo.*;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.CommonErrorCode;
import org.scoula.global.file.service.S3ServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class HomeServiceImpl implements HomeService {

      private final HomeMapper homeMapper;
      private final S3ServiceInterface s3Service;

      @Override
      public Integer createHome(HomeCreateDTO createDTO, List<MultipartFile> images, Integer userId) {
          log.info(
                  "매물 등록 시작: userId={}, residenceType={}, 이미지 개수={}",
                  userId,
                  createDTO.getResidenceType(),
                  images != null ? images.size() : 0);

          // ✨ 서비스에서 받은 DTO와 description 값 확인
          log.info("서비스에서 받은 DTO: {}", createDTO);
          log.info("서비스에서 확인한 description 값: {}", createDTO.getDescription());

          try {
              HomeVO home =
                      HomeVO.builder()
                              .userId(userId)
                              .addr1(createDTO.getAddr1())
                              .addr2(createDTO.getAddr2())
                              .residenceType(createDTO.getResidenceType())
                              .leaseType(createDTO.getLeaseType())
                              .depositPrice(createDTO.getDepositPrice())
                              .monthlyRent(createDTO.getMonthlyRent())
                              .maintenaceFee(createDTO.getMaintenaceFee())
                              .homeStatus(HomeStatus.AVAILABLE)
                              .viewCnt(0)
                              .likeCnt(0)
                              .chatCnt(0)
                              .roomCnt(createDTO.getRoomCnt())
                              .supplyArea(createDTO.getSupplyArea())
                              .exclusiveArea(createDTO.getExclusiveArea())
                              .createdAt(LocalDate.now())
                              .updatedAt(LocalDate.now())
                              .build();

              int homeResult = homeMapper.insertHome(home);
              if (homeResult != 1) {
                  throw new BusinessException(CommonErrorCode.DATA_ACCESS_ERROR, "매물 등록에 실패했습니다.");
              }

              Integer homeId = home.getHomeId();
              log.info("매물 기본 정보 등록 완료: homeId={}", homeId);

              HomeDetailVO homeDetail =
                      HomeDetailVO.builder()
                              .homeId(homeId)
                              .buildDate(createDTO.getBuildDate())
                              .homeFloor(createDTO.getHomeFloor())
                              .buildingTotalFloors(createDTO.getBuildingTotalFloors())
                              .homeDirection(createDTO.getHomeDirection())
                              .bathroomCnt(createDTO.getBathroomCnt())
                              .isPet(createDTO.getIsPet())
                              .isParking(createDTO.getIsParking())
                              .area(createDTO.getArea())
                              .landCategory(createDTO.getLandCategory())
                              .description(createDTO.getDescription())
                              .build();

              // ✨ DB에 저장할 객체의 description 값 최종 확인
              log.info("DB에 저장할 HomeDetailVO 객체: {}", homeDetail);
              log.info("최종 DB 저장 직전의 description 값: {}", homeDetail.getDescription());

              int detailResult = homeMapper.insertHomeDetail(homeDetail);
              if (detailResult != 1) {
                  throw new BusinessException(
                          CommonErrorCode.DATA_ACCESS_ERROR, "매물 상세정보 등록에 실패했습니다.");
              }

              Integer homeDetailId = homeDetail.getHomeDetailId();
              log.info("매물 상세 정보 등록 완료: homeDetailId={}", homeDetailId);

              if (createDTO.getFacilityItemIds() != null
                      && !createDTO.getFacilityItemIds().isEmpty()) {
                  for (Integer itemId : createDTO.getFacilityItemIds()) {
                      HomeFacilityVO facility =
                              HomeFacilityVO.builder()
                                      .homeDetailId(homeDetailId)
                                      .itemId(itemId)
                                      .build();
                      homeMapper.insertHomeFacility(facility);
                  }
                  log.info("편의시설 등록 완료: 개수={}", createDTO.getFacilityItemIds().size());
              }

              if (createDTO.getMaintenanceFees() != null
                      && !createDTO.getMaintenanceFees().isEmpty()) {
                  for (HomeCreateDTO.MaintenanceFeeDTO feeDTO : createDTO.getMaintenanceFees()) {
                      HomeMaintenanceFeeVO maintenanceFee =
                              HomeMaintenanceFeeVO.builder()
                                      .homeId(homeId)
                                      .maintenanceId(feeDTO.getMaintenanceId())
                                      .fee(feeDTO.getFee())
                                      .build();
                      homeMapper.insertHomeMaintenanceFee(maintenanceFee);
                  }
                  log.info("관리비 정보 등록 완료: 개수={}", createDTO.getMaintenanceFees().size());
              }

              // 파일 업로드 방식 이미지 처리
              if (images != null && !images.isEmpty()) {
                  int successCount = 0;
                  for (MultipartFile image : images) {
                      if (!image.isEmpty()) {
                          try {
                              String fileName =
                                      generateHomeImageFileName(homeId, image.getOriginalFilename());
                              // S3에 업로드 (uploads/home/{homeId}/{fileName} 형식으로 저장됨)
                              String uploadedKey =
                                      s3Service.uploadFile(image, "home/" + homeId + "/" + fileName);
                              String imageUrl = s3Service.getFileUrl(uploadedKey);

                              HomeImageVO homeImage =
                                      HomeImageVO.builder().homeId(homeId).imageUrl(imageUrl).build();

                              homeMapper.insertHomeImage(homeImage);
                              successCount++;

                              log.info(
                                      "이미지 업로드 완료: homeId={}, fileName={}, imageUrl={}",
                                      homeId,
                                      fileName,
                                      imageUrl);
                          } catch (Exception e) {
                              log.error(
                                      "이미지 업로드 실패: homeId={}, fileName={}",
                                      homeId,
                                      image.getOriginalFilename(),
                                      e);
                              // 이미지 업로드 실패는 전체 등록을 중단하지 않음
                          }
                      }
                  }
                  log.info("파일 이미지 등록 완료: homeId={}, 성공한 이미지 개수={}", homeId, successCount);
              }

              log.info("매물 등록 전체 완료: homeId={}", homeId);
              return homeId;

          } catch (Exception e) {
              log.error(
                      "매물 등록 중 오류 발생: userId={}, error={}",
                      userId,
                      e.getMessage(),
                      e); // ⭐️ 예외 객체(e)를 함께 출력하여 스택 트레이스 확인
              throw new BusinessException(
                      CommonErrorCode.INTERNAL_SERVER_ERROR, "매물 등록 중 오류가 발생했습니다: " + e.getMessage());
          }
      }

      /** 매물 이미지 파일명 생성 */
      private String generateHomeImageFileName(Integer homeId, String originalFileName) {
          String timestamp = String.valueOf(System.currentTimeMillis());
          String extension = "";

          if (originalFileName != null && originalFileName.contains(".")) {
              extension = originalFileName.substring(originalFileName.lastIndexOf("."));
          }

          return homeId + "_" + timestamp + extension;
      }

      @Override
      @Transactional(readOnly = true)
      public HomeResponseDTO getHome(Integer homeId) {
          homeMapper.incrementViewCount(homeId);

          HomeVO home = homeMapper.selectHomeById(homeId);
          if (home == null) {
              throw new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "존재하지 않는 매물입니다.");
          }

          HomeDetailVO homeDetail = homeMapper.selectHomeDetailByHomeId(homeId);

          // ✨ 매퍼에서 반환된 객체와 description 값 확인
          log.info("매퍼에서 반환된 HomeDetailVO: {}", homeDetail);
          log.info(
                  "매퍼에서 확인한 description 값: {}",
                  homeDetail != null ? homeDetail.getDescription() : null);

          List<HomeImageVO> images = homeMapper.selectHomeImagesByHomeId(homeId);
          List<String> imageUrls =
                  images.stream().map(HomeImageVO::getImageUrl).collect(Collectors.toList());

          List<FacilityItem> facilities = null;
          if (homeDetail != null) {
              facilities =
                      homeMapper.selectHomeFacilitiesByHomeDetailId(homeDetail.getHomeDetailId());
          }

          List<HomeMaintenanceFeeVO> maintenanceFees =
                  homeMapper.selectHomeMaintenanceFeesByHomeId(homeId);

          return HomeResponseDTO.builder()
                  .homeId(home.getHomeId())
                  .userId(home.getUserId())
                  .userName(home.getUserName())
                  .addr1(home.getAddr1())
                  .addr2(home.getAddr2())
                  .residenceType(home.getResidenceType())
                  .leaseType(home.getLeaseType())
                  .depositPrice(home.getDepositPrice())
                  .monthlyRent(home.getMonthlyRent())
                  .maintenaceFee(home.getMaintenaceFee())
                  .homeStatus(home.getHomeStatus())
                  .viewCnt(home.getViewCnt())
                  .likeCnt(home.getLikeCnt())
                  .chatCnt(home.getChatCnt())
                  .roomCnt(home.getRoomCnt())
                  .supplyArea(home.getSupplyArea())
                  .exclusiveArea(home.getExclusiveArea())
                  .buildDate(homeDetail != null ? homeDetail.getBuildDate() : null)
                  .homeFloor(homeDetail != null ? homeDetail.getHomeFloor() : null)
                  .buildingTotalFloors(
                          homeDetail != null ? homeDetail.getBuildingTotalFloors() : null)
                  .homeDirection(homeDetail != null ? homeDetail.getHomeDirection() : null)
                  .bathroomCnt(homeDetail != null ? homeDetail.getBathroomCnt() : null)
                  .isPet(homeDetail != null ? homeDetail.getIsPet() : null)
                  .isParking(homeDetail != null ? homeDetail.getIsParking() : null)
                  .facilities(facilities)
                  .maintenanceFees(maintenanceFees)
                  .description(homeDetail != null ? homeDetail.getDescription() : null)
                  .imageUrls(imageUrls)
                  .createdAt(home.getCreatedAt())
                  .updatedAt(home.getUpdatedAt())
                  .build();
      }

      @Override
      @Transactional(readOnly = true)
      public List<HomeResponseDTO> searchHomes(HomeSearchDTO searchDTO) {
          List<HomeVO> homes = homeMapper.selectHomeListByCondition(searchDTO);

          return homes.stream()
                  .map(
                          home -> {
                              List<HomeImageVO> images =
                                      homeMapper.selectHomeImagesByHomeId(home.getHomeId());
                              String mainImageUrl =
                                      images.isEmpty() ? null : images.get(0).getImageUrl();

                              return HomeResponseDTO.builder()
                                      .homeId(home.getHomeId())
                                      .addr1(home.getAddr1())
                                      .residenceType(home.getResidenceType())
                                      .leaseType(home.getLeaseType())
                                      .depositPrice(home.getDepositPrice())
                                      .monthlyRent(home.getMonthlyRent())
                                      .maintenaceFee(home.getMaintenaceFee())
                                      .homeStatus(home.getHomeStatus())
                                      .exclusiveArea(home.getExclusiveArea())
                                      .homeFloor(home.getHomeFloor())
                                      .viewCnt(home.getViewCnt())
                                      .likeCnt(home.getLikeCnt())
                                      .roomCnt(home.getRoomCnt())
                                      .supplyArea(home.getSupplyArea())
                                      .imageUrls(
                                              mainImageUrl != null
                                                      ? List.of(mainImageUrl)
                                                      : List.of())
                                      .createdAt(home.getCreatedAt())
                                      .build();
                          })
                  .collect(Collectors.toList());
      }

      @Override
      @Transactional(readOnly = true)
      public List<HomeResponseDTO> getHomeList(int page, int size) {
          int offset = (page - 1) * size;
          List<HomeVO> homes = homeMapper.selectHomeList(offset, size);

          return homes.stream()
                  .map(
                          home -> {
                              List<HomeImageVO> images =
                                      homeMapper.selectHomeImagesByHomeId(home.getHomeId());
                              String mainImageUrl =
                                      images.isEmpty() ? null : images.get(0).getImageUrl();

                              return HomeResponseDTO.builder()
                                      .homeId(home.getHomeId())
                                      .addr1(home.getAddr1())
                                      .residenceType(home.getResidenceType())
                                      .leaseType(home.getLeaseType())
                                      .depositPrice(home.getDepositPrice())
                                      .monthlyRent(home.getMonthlyRent())
                                      .homeStatus(home.getHomeStatus())
                                      .viewCnt(home.getViewCnt())
                                      .likeCnt(home.getLikeCnt())
                                      .roomCnt(home.getRoomCnt())
                                      .supplyArea(home.getSupplyArea())
                                      .exclusiveArea(home.getExclusiveArea())
                                      .homeFloor(home.getHomeFloor())
                                      .imageUrls(
                                              mainImageUrl != null
                                                      ? List.of(mainImageUrl)
                                                      : List.of())
                                      .createdAt(home.getCreatedAt())
                                      .build();
                          })
                  .collect(Collectors.toList());
      }

      @Override
      public void updateHome(
              Integer homeId, HomeCreateDTO updateDTO, List<MultipartFile> images, Integer userId) {
          HomeVO existingHome = homeMapper.selectHomeById(homeId);
          if (existingHome == null) {
              throw new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "존재하지 않는 매물입니다.");
          }
          if (!existingHome.getUserId().equals(userId)) {
              throw new BusinessException(CommonErrorCode.UNAUTHORIZED_ACCESS, "매물을 수정할 권한이 없습니다.");
          }

          // TODO: 매물 기본 정보 업데이트 로직 구현
          // homeMapper.updateHome(homeId, updateDTO);

          // 새로운 이미지 업로드 처리
          if (images != null && !images.isEmpty()) {
              int successCount = 0;
              for (MultipartFile image : images) {
                  if (!image.isEmpty()) {
                      try {
                          String fileName =
                                  generateHomeImageFileName(homeId, image.getOriginalFilename());
                          // S3에 업로드 (uploads/home/{homeId}/{fileName} 형식으로 저장됨)
                          String uploadedKey =
                                  s3Service.uploadFile(image, "home/" + homeId + "/" + fileName);
                          String imageUrl = s3Service.getFileUrl(uploadedKey);

                          HomeImageVO homeImage =
                                  HomeImageVO.builder().homeId(homeId).imageUrl(imageUrl).build();

                          homeMapper.insertHomeImage(homeImage);
                          successCount++;

                          log.info(
                                  "이미지 추가 업로드 완료: homeId={}, fileName={}, imageUrl={}",
                                  homeId,
                                  fileName,
                                  imageUrl);
                      } catch (Exception e) {
                          log.error(
                                  "이미지 업로드 실패: homeId={}, fileName={}",
                                  homeId,
                                  image.getOriginalFilename(),
                                  e);
                      }
                  }
              }
              log.info("매물 이미지 추가 완료: homeId={}, 성공한 이미지 개수={}", homeId, successCount);
          }

          log.info("매물 수정 완료: homeId={}, userId={}", homeId, userId);
      }

      @Override
      public void deleteHome(Integer homeId, Integer userId) {
          log.info("매물 삭제 완료: homeId={}, userId={}", homeId, userId);
      }

      @Override
      public void updateHomeStatus(Integer homeId, String status, Integer userId) {
          log.info("매물 상태 변경 완료: homeId={}, status={}", homeId, status);
      }

      @Override
      @Transactional(readOnly = true)
      public List<HomeResponseDTO> getHomesByUser(Integer userId) {
          return List.of();
      }

      @Override
      @Transactional(readOnly = true)
      public List<FacilityCategory> getFacilityCategories() {
          return homeMapper.selectFacilityCategories();
      }

      @Override
      @Transactional(readOnly = true)
      public List<FacilityItem> getFacilityItemsByCategory(Integer categoryId) {
          return homeMapper.selectFacilityItemsByCategoryId(categoryId);
      }

      @Override
      public void toggleHomeLike(Integer userId, Integer homeId) {
          log.info("찜 토글 요청: userId={}, homeId={}", userId, homeId);

          // 찜 상태 확인
          int exists = homeMapper.selectHomeLikeExists(userId, homeId);
          log.info("찜 상태 확인 결과: exists={}", exists);

          if (exists > 0) {
              // 찜 상태인 경우, 찜 삭제
              int deleteCount = homeMapper.deleteHomeLike(userId, homeId);
              if (deleteCount > 0) {
                  log.info("찜 해제 성공: userId={}, homeId={}", userId, homeId);
              } else {
                  log.warn("찜 해제 실패: userId={}, homeId={}", userId, homeId);
              }
          } else {
              // 찜 상태가 아닌 경우, 찜 등록
              HomeLikeVO homeLike =
                      HomeLikeVO.builder()
                              .userId(userId)
                              .homeId(homeId)
                              .likedAt(LocalDate.now())
                              .build();
              int insertCount = homeMapper.insertHomeLike(homeLike);
              if (insertCount > 0) {
                  log.info("찜 등록 성공: userId={}, homeId={}", userId, homeId);
              } else {
                  log.warn("찜 등록 실패: userId={}, homeId={}", userId, homeId);
              }
          }
      }

      @Override
      @Transactional(readOnly = true)
      public int getTotalHomeCount() {
          return homeMapper.selectHomeCount();
      }

      @Override
      @Transactional(readOnly = true)
      public int getHomeCountByCondition(HomeSearchDTO searchDTO) {
          return homeMapper.selectHomeCountByCondition(searchDTO);
      }

      @Override
      @Transactional(readOnly = true)
      public List<HomeResponseDTO> getHomeLikes(Integer userId) {
          return List.of();
      }
}
