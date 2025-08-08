package org.scoula.domain.home.controller;

import static org.scoula.domain.home.constant.HomeIdentityConstants.*;

import javax.validation.Valid;

import org.scoula.domain.home.dto.HomeIdentityVerificationDTO;
import org.scoula.domain.home.service.HomeIdentityVerificationService;
import org.scoula.global.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
public class HomeIdentityVerificationControllerImpl implements HomeIdentityVerificationController {

      private final HomeIdentityVerificationService homeIdentityVerificationService;

      @Override
      public ApiResponse<Void> verifyIdentity(
              Long homeId, Long userId, @Valid HomeIdentityVerificationDTO dto) {
          log.info(LOG_VERIFY_START, homeId, userId);
          homeIdentityVerificationService.verifyAndSaveIdentity(homeId, userId, dto);
          return ApiResponse.success(null, MSG_VERIFY_SUCCESS);
      }

      @Override
      public ApiResponse<HomeIdentityVerificationDTO> getIdentityVerification(Long homeId) {
          log.info(LOG_GET_INFO, homeId);
          HomeIdentityVerificationDTO dto =
                  homeIdentityVerificationService.getIdentityVerification(homeId);
          return ApiResponse.success(dto, MSG_GET_SUCCESS);
      }
}
