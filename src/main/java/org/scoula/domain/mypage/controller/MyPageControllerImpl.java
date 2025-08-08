package org.scoula.domain.mypage.controller;

import org.scoula.domain.mypage.dto.*;
import org.scoula.domain.mypage.exception.MyPageErrorCode;
import org.scoula.domain.mypage.service.MyPageService;
import org.scoula.global.auth.dto.CustomUserDetails;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.dto.PageResponse;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
public class MyPageControllerImpl implements MyPageController {

      private final MyPageService myPageService;

      @Override
      public ResponseEntity<ApiResponse<MyPageUserInfoDto>> getMyInfo(UserDetails userDetails) {
          Long userId = getUserIdFromUserDetails(userDetails);
          MyPageUserInfoDto userInfo = myPageService.getUserInfo(userId);

          return ResponseEntity.ok(ApiResponse.success(userInfo));
      }

      @Override
      public ResponseEntity<ApiResponse<String>> updateProfileImage(
              UserDetails userDetails, MultipartFile profileImage) {

          Long userId = getUserIdFromUserDetails(userDetails);
          String imageUrl = myPageService.updateProfileImage(userId, profileImage);

          return ResponseEntity.ok(ApiResponse.success(imageUrl));
      }

      @Override
      public ResponseEntity<ApiResponse<Void>> updateNickname(
              UserDetails userDetails, UpdateNicknameDto dto) {

          Long userId = getUserIdFromUserDetails(userDetails);
          myPageService.updateNickname(userId, dto.getNickname());

          return ResponseEntity.ok(ApiResponse.success());
      }

      @Override
      public ResponseEntity<ApiResponse<Void>> updateNotification(
              UserDetails userDetails, UpdateNotificationDto dto) {

          Long userId = getUserIdFromUserDetails(userDetails);
          myPageService.updateNotificationSetting(userId, dto.getNotificationEnabled());

          return ResponseEntity.ok(ApiResponse.success());
      }

      @Override
      public ResponseEntity<ApiResponse<PageResponse<MyPageContractDto>>> getMyContracts(
              UserDetails userDetails, int page, int size) {

          Long userId = getUserIdFromUserDetails(userDetails);
          Pageable pageable = PageRequest.of(page, size, Sort.by("contractDate").descending());
          Page<MyPageContractDto> contracts = myPageService.getMyContracts(userId, pageable);

          return ResponseEntity.ok(ApiResponse.success(PageResponse.from(contracts)));
      }

      @Override
      public ResponseEntity<ApiResponse<PageResponse<MyPagePropertyDto>>> getMyProperties(
              UserDetails userDetails, int page, int size) {

          Long userId = getUserIdFromUserDetails(userDetails);
          Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
          Page<MyPagePropertyDto> properties = myPageService.getMyProperties(userId, pageable);

          return ResponseEntity.ok(ApiResponse.success(PageResponse.from(properties)));
      }

      @Override
      public ResponseEntity<ApiResponse<PageResponse<MyPageRiskAnalysisDto>>> getMyRiskAnalyses(
              UserDetails userDetails, int page, int size) {

          Long userId = getUserIdFromUserDetails(userDetails);
          Pageable pageable = PageRequest.of(page, size, Sort.by("analysisDate").descending());
          Page<MyPageRiskAnalysisDto> analyses = myPageService.getMyRiskAnalyses(userId, pageable);

          return ResponseEntity.ok(ApiResponse.success(PageResponse.from(analyses)));
      }

      private Long getUserIdFromUserDetails(UserDetails userDetails) {
          if (userDetails instanceof CustomUserDetails) {
              return ((CustomUserDetails) userDetails).getUserId();
          }
          throw new BusinessException(MyPageErrorCode.UNAUTHORIZED_ACCESS);
      }
}
