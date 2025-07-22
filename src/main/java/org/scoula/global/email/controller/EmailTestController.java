package org.scoula.global.email.controller;

import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.email.dto.TestEmailDto;
import org.springframework.http.ResponseEntity;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponses;

@Api(tags = "이메일 테스트 API", description = "이메일 전송 기능 테스트용 엔드포인트")
public interface EmailTestController {

      @ApiOperation(value = "테스트 이메일 전송", notes = "지정된 이메일 주소로 테스트 이메일을 전송합니다")
      @ApiResponses({
          @io.swagger.annotations.ApiResponse(code = 200, message = "전송 성공"),
          @io.swagger.annotations.ApiResponse(code = 400, message = "잘못된 요청"),
          @io.swagger.annotations.ApiResponse(code = 500, message = "전송 실패")
      })
      ResponseEntity<ApiResponse<TestEmailDto.SendResponse>> sendTestEmail(
              @ApiParam(value = "이메일 전송 요청") TestEmailDto.SendRequest request);

      @ApiOperation(value = "환영 이메일 전송", notes = "새 사용자에게 환영 이메일을 전송합니다")
      @ApiResponses({
          @io.swagger.annotations.ApiResponse(code = 200, message = "전송 성공"),
          @io.swagger.annotations.ApiResponse(code = 400, message = "잘못된 요청"),
          @io.swagger.annotations.ApiResponse(code = 500, message = "전송 실패")
      })
      ResponseEntity<ApiResponse<TestEmailDto.SendResponse>> sendWelcomeEmail(
              @ApiParam(value = "환영 이메일 전송 요청") TestEmailDto.WelcomeRequest request);
}
