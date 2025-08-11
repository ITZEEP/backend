package org.scoula.domain.precontract.controller;

import java.util.Optional;

import javax.validation.Valid;

import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.domain.precontract.dto.common.IdentityVerificationInfoDTO;
import org.scoula.domain.precontract.service.IdentityVerificationService;
import org.scoula.domain.precontract.service.OwnerPreContractService;
import org.scoula.domain.precontract.vo.IdentityVerificationInfoVO;
import org.scoula.domain.user.service.UserServiceImpl;
import org.scoula.domain.user.vo.User;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
      private final IdentityVerificationService identityVerificationService;
      private final UserServiceImpl userService;

      @Override
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

      @Override
      @GetMapping("/verification")
      public ResponseEntity<ApiResponse<IdentityVerificationInfoDTO>> getVerificationInfo(
              @PathVariable Long contractChatId, Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();

          // 마스킹된 정보 조회
          IdentityVerificationInfoVO maskedInfo =
                  identityVerificationService.getMaskedVerificationInfo(contractChatId, userId);

          // VO를 DTO로 변환
          IdentityVerificationInfoDTO dto =
                  IdentityVerificationInfoDTO.builder()
                          .name(maskedInfo.getName())
                          .ssnFront(maskedInfo.getSsnFront())
                          .ssnBack(maskedInfo.getSsnBack())
                          .addr1(maskedInfo.getAddr1())
                          .addr2(maskedInfo.getAddr2())
                          .phoneNumber(maskedInfo.getPhoneNumber())
                          .build();

          return ResponseEntity.ok(ApiResponse.success(dto));
      }
}
