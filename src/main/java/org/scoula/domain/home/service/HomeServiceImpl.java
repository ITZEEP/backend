package org.scoula.domain.home.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.scoula.domain.home.dto.request.HomeCreateRequestDto;
import org.scoula.domain.home.dto.request.HomeReportRequestDto;
import org.scoula.domain.home.dto.request.HomeUpdateRequestDto;
import org.scoula.domain.home.dto.response.FacilityResponseDto;
import org.scoula.domain.home.dto.response.HomeResponseDto;
import org.scoula.domain.home.dto.response.MaintenanceFeeItemResponseDto;
import org.scoula.domain.home.exception.HomeRegisterException;
import org.scoula.domain.home.mapper.HomeMapper;
import org.scoula.domain.home.vo.HomeRegisterVO;
import org.scoula.domain.home.vo.HomeReportVO;
import org.scoula.global.auth.util.S3Uploader;
import org.scoula.global.common.dto.PageRequest;
import org.scoula.global.common.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Log4j2
public class HomeServiceImpl implements HomeService {

      private final HomeMapper homeMapper;
      private final S3Uploader s3Uploader;

      @Override
      public PageResponse<HomeResponseDto> getHomeList(PageRequest pageRequest) {
          List<HomeRegisterVO> homes =
                  homeMapper.findHomes(pageRequest.getOffset(), pageRequest.getSize());
          long totalCount = homeMapper.countHomes(pageRequest);

          List<HomeResponseDto> content =
                  homes.stream()
                          .map(home -> HomeResponseDto.from(home, null, null))
                          .collect(Collectors.toList());

          return PageResponse.<HomeResponseDto>builder()
                  .content(content)
                  .page(pageRequest.getPage())
                  .size(pageRequest.getSize())
                  .totalElements(totalCount)
                  .build();
      }

      @Override
      public HomeResponseDto getHomeDetail(Long homeId) {
          HomeRegisterVO home =
                  homeMapper
                          .findHomeById(homeId)
                          .orElseThrow(() -> new HomeRegisterException("매물을 찾을 수 없습니다."));

          List<MaintenanceFeeItemResponseDto> maintenanceItems =
                  homeMapper.findHomeMaintenanceItemsByHomeId(homeId);

          List<FacilityResponseDto> facilities = homeMapper.findHomeFacilities(homeId);

          return HomeResponseDto.from(home, maintenanceItems, facilities);
      }

      @Override
      @Transactional
      public void deleteHome(Long userId, Long homeId) {
          HomeRegisterVO existingHome =
                  homeMapper
                          .findHomeById(homeId)
                          .orElseThrow(() -> new HomeRegisterException("매물을 찾을 수 없습니다."));

          if (!existingHome.getUserId().equals(userId)) {
              throw new HomeRegisterException("매물 삭제 권한이 없습니다.");
          }

          homeMapper.deleteHome(homeId);
      }

      @Override
      @Transactional
      public Long createHome(Long userId, HomeCreateRequestDto request) {
          String userName = homeMapper.findUserNameById(userId);

          HomeRegisterVO vo = HomeRegisterVO.from(userId, request);
          vo.setUserName(userName);

          // 1. insertHome 호출 후, 생성된 homeId를 받아서 VO에 설정
          homeMapper.insertHome(userId, userName, vo);
          Long homeId = vo.getHomeId();

          // 2. insertHomeDetail 호출 (home_detail의 PK를 homeId로 사용)
          // homeDetailId는 homeId와 동일한 값으로 취급
          vo.setHomeId(homeId); // vo 객체에 homeId를 명시적으로 설정
          homeMapper.insertHomeDetail(vo);

          if (request.getFacilityItemIds() != null && !request.getFacilityItemIds().isEmpty()) {
              homeMapper.insertHomeFacilities(
                      Map.of(
                              "homeDetailId",
                              homeId, // home_detail_id 대신 homeId 사용
                              "facilityItemIds",
                              request.getFacilityItemIds()));
          }

          if (request.getImageFiles() != null && !request.getImageFiles().isEmpty()) {
              List<String> imageUrls =
                      request.getImageFiles().stream()
                              .map(file -> s3Uploader.upload(file, "homes"))
                              .collect(Collectors.toList());

              homeMapper.insertHomeImages(Map.of("homeId", homeId, "imageUrls", imageUrls));
          }

          if (request.getMaintenanceFeeItems() != null
                  && !request.getMaintenanceFeeItems().isEmpty()) {
              homeMapper.insertHomeMaintenanceFees(
                      homeId,
                      request.getMaintenanceFeeItems().stream()
                              .collect(
                                      Collectors.toMap(
                                              HomeRegisterVO.MaintenanceFeeItem::getMaintenanceId,
                                              HomeRegisterVO.MaintenanceFeeItem::getFee)));
          }

          return homeId;
      }

      @Override
      @Transactional
      public void updateHome(Long userId, Long homeId, HomeUpdateRequestDto request) {
          HomeRegisterVO existingHome =
                  homeMapper
                          .findHomeById(homeId)
                          .orElseThrow(() -> new HomeRegisterException("매물을 찾을 수 없습니다."));

          if (!existingHome.getUserId().equals(userId)) {
              throw new HomeRegisterException("매물 수정 권한이 없습니다.");
          }

          HomeRegisterVO vo = HomeRegisterVO.from(userId, request);
          homeMapper.updateHome(vo);
      }

      @Override
      @Transactional
      public void addLike(Long userId, Long homeId) {
          homeMapper.insertHomeLike(userId, homeId);
      }

      @Override
      @Transactional
      public void removeLike(Long userId, Long homeId) {
          homeMapper.deleteLike(userId, homeId);
      }

      @Override
      public List<HomeResponseDto> getLikedHomes(Long userId) {
          return homeMapper.findLikedHomes(userId).stream()
                  .map(home -> HomeResponseDto.from(home, null, null))
                  .collect(Collectors.toList());
      }

      @Override
      @Transactional
      public void increaseViewCount(Long homeId) {
          homeMapper.incrementViewCount(homeId);
      }

      @Override
      @Transactional
      public void reportHome(HomeReportRequestDto requestDto) {
          LocalDateTime reportAt =
                  requestDto.getReportAt() != null ? requestDto.getReportAt() : LocalDateTime.now();
          String reportStatus =
                  requestDto.getReportStatus() != null ? requestDto.getReportStatus() : "WAITING";

          HomeReportVO vo =
                  HomeReportVO.builder()
                          .reportId(requestDto.getReportId())
                          .userId(requestDto.getUserId())
                          .homeId(requestDto.getHomeId())
                          .reportReason(requestDto.getReportReason())
                          .reportAt(reportAt)
                          .reportStatus(reportStatus)
                          .build();

          homeMapper.insertHomeReport(vo);
      }
}
