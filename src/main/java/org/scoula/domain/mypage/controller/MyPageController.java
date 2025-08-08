package org.scoula.domain.mypage.controller;

import javax.validation.Valid;

import org.scoula.domain.mypage.dto.*;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = "마이페이지 API")
@RequestMapping("/api/mypage")
public interface MyPageController {

      @ApiOperation("내 정보 조회")
      @GetMapping("/info")
      ResponseEntity<ApiResponse<MyPageUserInfoDto>> getMyInfo(
              @AuthenticationPrincipal UserDetails userDetails);

      @ApiOperation("프로필 이미지 수정")
      @PutMapping(value = "/profile-image", consumes = "multipart/form-data")
      ResponseEntity<ApiResponse<String>> updateProfileImage(
              @AuthenticationPrincipal UserDetails userDetails,
              @RequestParam("profileImage") MultipartFile profileImage);

      @ApiOperation("닉네임 변경")
      @PutMapping("/nickname")
      ResponseEntity<ApiResponse<Void>> updateNickname(
              @AuthenticationPrincipal UserDetails userDetails,
              @Valid @RequestBody UpdateNicknameDto dto);

      @ApiOperation("알림 설정 변경")
      @PutMapping("/notification")
      ResponseEntity<ApiResponse<Void>> updateNotification(
              @AuthenticationPrincipal UserDetails userDetails,
              @Valid @RequestBody UpdateNotificationDto dto);

      @ApiOperation("내 계약서 목록 조회")
      @GetMapping("/contracts")
      ResponseEntity<ApiResponse<PageResponse<MyPageContractDto>>> getMyContracts(
              @AuthenticationPrincipal UserDetails userDetails,
              @RequestParam(defaultValue = "0") int page,
              @RequestParam(defaultValue = "10") int size);

      @ApiOperation("내 매물 목록 조회")
      @GetMapping("/properties")
      ResponseEntity<ApiResponse<PageResponse<MyPagePropertyDto>>> getMyProperties(
              @AuthenticationPrincipal UserDetails userDetails,
              @RequestParam(defaultValue = "0") int page,
              @RequestParam(defaultValue = "10") int size);

      @ApiOperation("내 사기위험도 분석 이력 조회")
      @GetMapping("/risk-analyses")
      ResponseEntity<ApiResponse<PageResponse<MyPageRiskAnalysisDto>>> getMyRiskAnalyses(
              @AuthenticationPrincipal UserDetails userDetails,
              @RequestParam(defaultValue = "0") int page,
              @RequestParam(defaultValue = "10") int size);
}
