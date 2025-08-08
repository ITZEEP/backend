package org.scoula.domain.precontract.controller;

import java.util.Optional;

import javax.validation.Valid;

import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.domain.precontract.dto.common.IdentityVerificationInfoDTO;
import org.scoula.domain.precontract.service.OwnerPreContractService;
import org.scoula.domain.user.service.UserServiceImpl;
import org.scoula.domain.user.vo.User;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pre-contract/{contractChatId}")
@Log4j2
public class IdentityVerificationControllerImpl implements IdentityVerificationController {

      private final OwnerPreContractService ownerPreContractService;
      private final UserServiceImpl userService;

      @PostMapping("/verification")
      public ResponseEntity<ApiResponse<String>> saveVerification(
              @PathVariable Long contractChatId,
              Authentication authentication,
              @RequestBody @Valid IdentityVerificationInfoDTO dto) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();

          ownerPreContractService.requireVerification(contractChatId, userId, dto);

          return ResponseEntity.ok(ApiResponse.success("본인 인증 성공"));
      }
}
