package org.scoula.global.redis.controller;

import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.redis.dto.RedisDto;
import org.springframework.http.ResponseEntity;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;

@Api(tags = "Redis 헬스체크 API", description = "Redis 연결 상태 확인용 엔드포인트")
public interface RedisTestController {

      @ApiOperation(value = "Redis 연결 상태 확인", notes = "Redis 서버와의 연결 상태를 확인합니다")
      @ApiResponses({
          @io.swagger.annotations.ApiResponse(code = 200, message = "헬스체크 완료 (연결 성공/실패 모두 포함)"),
          @io.swagger.annotations.ApiResponse(code = 500, message = "헬스체크 자체 실행 오류")
      })
      ResponseEntity<ApiResponse<RedisDto.HealthResponse>> checkRedisHealth();
}
