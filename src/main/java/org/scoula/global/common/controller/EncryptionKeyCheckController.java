package org.scoula.global.common.controller;

import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.service.EncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 암호화 키 확인 컨트롤러 계약 ID에 대한 암호화 키 존재 여부를 확인하는 API 제공 */
@RestController
@RequestMapping("/api/encryption/key")
@Api(tags = "Encryption Key Check", description = "암호화 키 확인 API")
@RequiredArgsConstructor
@Slf4j
public class EncryptionKeyCheckController {

      @Autowired private final EncryptionService encryptionService;

      /**
       * 계약 ID에 대한 암호화 키 존재 여부 확인
       *
       * @param contractChatId 계약 채팅 ID
       * @return 키 존재 여부
       */
      @GetMapping("/check/{contractChatId}")
      @ApiOperation(value = "암호화 키 존재 확인", notes = "계약 ID에 대한 암호화 키가 Redis에 존재하는지 확인합니다.")
      public ResponseEntity<ApiResponse<KeyExistenceResponse>> checkKeyExists(
              @ApiParam(value = "계약 채팅 ID", required = true, example = "contract123") @PathVariable
                      String contractChatId) {

          log.info("Checking key existence for contractChatId: {}", contractChatId);

          boolean keyExists = encryptionService.hasKey(contractChatId);

          KeyExistenceResponse response =
                  KeyExistenceResponse.builder()
                          .contractChatId(contractChatId)
                          .keyExists(keyExists)
                          .message(keyExists ? "키가 존재합니다." : "키가 존재하지 않습니다.")
                          .build();

          return ResponseEntity.ok(ApiResponse.success(response));
      }

      /** 키 존재 여부 응답 DTO */
      @lombok.Data
      @lombok.Builder
      @lombok.NoArgsConstructor
      @lombok.AllArgsConstructor
      public static class KeyExistenceResponse {
          private String contractChatId;
          private boolean keyExists;
          private String message;
      }
}
