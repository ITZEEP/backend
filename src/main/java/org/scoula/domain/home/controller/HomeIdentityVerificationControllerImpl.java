package org.scoula.domain.home.controller;

import static org.scoula.domain.home.constant.HomeIdentityConstants.*;

import java.util.Optional;

import javax.validation.Valid;

import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.domain.home.dto.HomeIdentityVerificationDTO;
import org.scoula.domain.home.service.HomeIdentityVerificationService;
import org.scoula.domain.user.service.UserServiceImpl;
import org.scoula.domain.user.vo.User;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
public class HomeIdentityVerificationControllerImpl implements HomeIdentityVerificationController {

      private final HomeIdentityVerificationService homeIdentityVerificationService;
      private final UserServiceImpl userService;

      @Override
      public ApiResponse<Void> verifyIdentity(
              Long homeId, Authentication authentication, @Valid HomeIdentityVerificationDTO dto) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();

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
