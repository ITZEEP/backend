package org.scoula.domain.home.controller;

import java.util.List;

import javax.validation.Valid;

import org.scoula.domain.home.dto.request.HomeCreateRequestDto;
import org.scoula.domain.home.dto.request.HomeReportRequestDto;
import org.scoula.domain.home.dto.request.HomeUpdateRequestDto;
import org.scoula.domain.home.dto.response.HomeResponseDto;
import org.scoula.domain.home.service.HomeService;
import org.scoula.global.auth.dto.CustomUserDetails;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.dto.PageRequest;
import org.scoula.global.common.dto.PageResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/homes")
@RequiredArgsConstructor
@Api(tags = {"매물 API"})
public class HomeController {

      private final HomeService homeService;

      @ApiOperation(value = "매물 등록", notes = "새로운 매물을 등록합니다.")
      @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
      public ResponseEntity<ApiResponse<Long>> createHome(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @ModelAttribute HomeCreateRequestDto request) {

          Long homeId = homeService.createHome(userDetails.getUserId(), request);
          return ResponseEntity.ok(ApiResponse.success(homeId));
      }

      @ApiOperation(value = "매물 수정", notes = "기존 매물 정보를 수정합니다.")
      @PutMapping("/{homeId}")
      public ResponseEntity<ApiResponse<Void>> updateHome(
              @AuthenticationPrincipal CustomUserDetails userDetails,
              @PathVariable Long homeId,
              @Valid @ModelAttribute HomeUpdateRequestDto request) {
          homeService.updateHome(userDetails.getUserId(), homeId, request);
          return ResponseEntity.ok(ApiResponse.success());
      }

      @ApiOperation(value = "매물 상세 조회", notes = "homeId에 해당하는 매물 정보를 조회합니다.")
      @GetMapping("/{homeId}")
      public ResponseEntity<ApiResponse<HomeResponseDto>> getHomeDetail(
              @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long homeId) {
          HomeResponseDto response = homeService.getHomeDetail(homeId);
          return ResponseEntity.ok(ApiResponse.success(response));
      }

      //      @ApiOperation(value = "내가 등록한 매물 목록 조회", notes = "사용자가 등록한 매물 목록을 페이지네이션하여 조회합니다.")
      //      @GetMapping("/my")
      //      public ResponseEntity<PageResponse<HomeResponseDto>> getMyHomes(
      //              @AuthenticationPrincipal CustomUserDetails userDetails,
      //              @ApiParam(value = "페이지 번호", defaultValue = "1") @RequestParam(defaultValue =
      // "1")
      //                      String pageStr,
      //              @ApiParam(value = "페이지 크기", defaultValue = "10") @RequestParam(defaultValue =
      // "10")
      //                      String sizeStr) {
      //
      //          int page = parseOrDefault(pageStr, 1);
      //          int size = parseOrDefault(sizeStr, 10);
      //
      //          PageRequest pageRequest = PageRequest.builder().page(page).size(size).build();
      //          PageResponse<HomeResponseDto> response =
      //                  homeService.getMyHomeList(userDetails.getUserId(), pageRequest);
      //          return ResponseEntity.ok(response);
      //      }

      @ApiOperation(value = "매물 삭제", notes = "매물 ID에 해당하는 매물을 삭제합니다.")
      @DeleteMapping("/{homeId}")
      public ResponseEntity<ApiResponse<Void>> deleteHome(
              @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long homeId) {
          homeService.deleteHome(userDetails.getUserId(), homeId);
          return ResponseEntity.ok(ApiResponse.success());
      }

      @ApiOperation(value = "모든 매물 검색", notes = "전체 매물을 페이징하여 조회합니다.")
      @GetMapping
      public ResponseEntity<PageResponse<HomeResponseDto>> getAllHomes(
              @ApiParam(value = "페이지 번호", defaultValue = "1") @RequestParam(defaultValue = "1")
                      String pageStr,
              @ApiParam(value = "페이지 크기", defaultValue = "10") @RequestParam(defaultValue = "10")
                      String sizeStr) {

          int page = parseOrDefault(pageStr, 1);
          int size = parseOrDefault(sizeStr, 10);

          PageRequest pageRequest = PageRequest.builder().page(page).size(size).build();
          PageResponse<HomeResponseDto> response = homeService.getHomeList(pageRequest);
          return ResponseEntity.ok(response);
      }

      @ApiOperation(value = "매물 찜하기", notes = "해당 매물을 찜합니다.")
      @PostMapping("/{homeId}/like")
      public ResponseEntity<ApiResponse<Void>> likeHome(
              @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long homeId) {
          homeService.addLike(userDetails.getUserId(), homeId);
          return ResponseEntity.ok(ApiResponse.success());
      }

      @ApiOperation(value = "매물 찜 해제", notes = "해당 매물의 찜을 취소합니다.")
      @DeleteMapping("/{homeId}/like")
      public ResponseEntity<ApiResponse<Void>> unlikeHome(
              @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long homeId) {
          homeService.removeLike(userDetails.getUserId(), homeId);
          return ResponseEntity.ok(ApiResponse.success());
      }

      @ApiOperation(value = "찜한 매물 목록 조회", notes = "내가 찜한 매물 목록을 조회합니다.")
      @GetMapping("/likes")
      public ResponseEntity<ApiResponse<List<HomeResponseDto>>> getLikedHomes(
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          List<HomeResponseDto> likedHomes = homeService.getLikedHomes(userDetails.getUserId());
          return ResponseEntity.ok(ApiResponse.success(likedHomes));
      }

      @ApiOperation(value = "조회수 증가", notes = "해당 매물의 조회수를 1 증가시킵니다.")
      @PostMapping("/{homeId}/view")
      public ResponseEntity<ApiResponse<Void>> increaseViewCount(@PathVariable Long homeId) {
          homeService.increaseViewCount(homeId);
          return ResponseEntity.ok(ApiResponse.success());
      }

      @ApiOperation(value = "매물 신고", notes = "해당 매물을 신고합니다.")
      @PostMapping("/report")
      public ResponseEntity<ApiResponse<Void>> reportHome(
              @RequestBody HomeReportRequestDto requestDto,
              @AuthenticationPrincipal CustomUserDetails userDetails) {
          HomeReportRequestDto reportRequest =
                  HomeReportRequestDto.builder()
                          .reportId(requestDto.getReportId())
                          .userId(userDetails.getUserId())
                          .homeId(requestDto.getHomeId())
                          .reportReason(requestDto.getReportReason())
                          .reportAt(requestDto.getReportAt())
                          .reportStatus(requestDto.getReportStatus())
                          .build();
          homeService.reportHome(reportRequest);
          return ResponseEntity.ok(ApiResponse.success());
      }

      // 유틸 메서드: 숫자 변환 실패 시 기본값 반환
      private int parseOrDefault(String str, int defaultValue) {
          try {
              int val = Integer.parseInt(str);
              if (val < 1) return defaultValue; // 페이지 번호, 크기 음수 방지용
              return val;
          } catch (NumberFormatException e) {
              return defaultValue;
          }
      }
}
