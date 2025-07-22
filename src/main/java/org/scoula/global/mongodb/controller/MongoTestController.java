package org.scoula.global.mongodb.controller;

import javax.validation.Valid;

import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.mongodb.dto.MongoDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * MongoDB 테스트 컨트롤러 인터페이스
 *
 * <p>MongoDB 기능을 테스트하기 위한 API 엔드포인트들을 정의합니다. 문서 저장, 조회, 수정, 삭제 및 통계 조회 기능을 제공합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Api(tags = "MongoDB 테스트", description = "MongoDB 기능 테스트를 위한 API")
public interface MongoTestController {

      @ApiOperation(value = "문서 저장", notes = "지정된 컬렉션에 문서를 저장합니다.")
      ResponseEntity<ApiResponse<MongoDto.SaveDocumentResponse>> saveDocument(
              @ApiParam(value = "문서 저장 정보", required = true) @Valid @RequestBody
                      MongoDto.SaveDocumentRequest request);

      @ApiOperation(value = "문서 조회", notes = "지정된 조건으로 문서를 조회합니다.")
      ResponseEntity<ApiResponse<MongoDto.FindDocumentResponse>> findDocuments(
              @ApiParam(value = "문서 조회 조건", required = true) @Valid @RequestBody
                      MongoDto.FindDocumentRequest request);

      @ApiOperation(value = "ID로 문서 조회", notes = "문서 ID로 특정 문서를 조회합니다.")
      ResponseEntity<ApiResponse<MongoDto.FindDocumentResponse>> findDocumentById(
              @ApiParam(value = "컬렉션명", required = true, example = "test_collection") @PathVariable
                      String collectionName,
              @ApiParam(value = "문서 ID", required = true, example = "507f1f77bcf86cd799439011")
                      @PathVariable
                      String id);

      @ApiOperation(value = "문서 업데이트", notes = "지정된 ID의 문서를 업데이트합니다.")
      ResponseEntity<ApiResponse<MongoDto.UpdateDocumentResponse>> updateDocument(
              @ApiParam(value = "문서 업데이트 정보", required = true) @Valid @RequestBody
                      MongoDto.UpdateDocumentRequest request);

      @ApiOperation(value = "문서 삭제", notes = "지정된 조건으로 문서를 삭제합니다.")
      ResponseEntity<ApiResponse<MongoDto.DeleteDocumentResponse>> deleteDocuments(
              @ApiParam(value = "문서 삭제 조건", required = true) @Valid @RequestBody
                      MongoDto.DeleteDocumentRequest request);

      @ApiOperation(value = "ID로 문서 삭제", notes = "문서 ID로 특정 문서를 삭제합니다.")
      ResponseEntity<ApiResponse<MongoDto.DeleteDocumentResponse>> deleteDocumentById(
              @ApiParam(value = "컬렉션명", required = true, example = "test_collection") @PathVariable
                      String collectionName,
              @ApiParam(value = "문서 ID", required = true, example = "507f1f77bcf86cd799439011")
                      @PathVariable
                      String id);

      @ApiOperation(value = "컬렉션 통계 조회", notes = "지정된 컬렉션의 통계 정보를 조회합니다.")
      ResponseEntity<ApiResponse<MongoDto.CollectionStatsResponse>> getCollectionStats(
              @ApiParam(value = "컬렉션명", required = true, example = "test_collection") @PathVariable
                      String collectionName);

      @ApiOperation(value = "데이터베이스 통계 조회", notes = "현재 데이터베이스의 통계 정보를 조회합니다.")
      ResponseEntity<ApiResponse<MongoDto.DatabaseStatsResponse>> getDatabaseStats();

      @ApiOperation(value = "서버 상태 조회", notes = "MongoDB 서버의 상태 정보를 조회합니다.")
      ResponseEntity<ApiResponse<MongoDto.ServerStatusResponse>> getServerStatus();

      @ApiOperation(value = "컬렉션 생성", notes = "새로운 컬렉션을 생성합니다.")
      ResponseEntity<ApiResponse<String>> createCollection(
              @ApiParam(value = "컬렉션명", required = true, example = "test_collection") @PathVariable
                      String collectionName);

      @ApiOperation(value = "컬렉션 삭제", notes = "지정된 컬렉션을 삭제합니다.")
      ResponseEntity<ApiResponse<String>> dropCollection(
              @ApiParam(value = "컬렉션명", required = true, example = "test_collection") @PathVariable
                      String collectionName);

      @ApiOperation(value = "컬렉션 존재 여부 확인", notes = "지정된 컬렉션의 존재 여부를 확인합니다.")
      ResponseEntity<ApiResponse<Boolean>> checkCollectionExists(
              @ApiParam(value = "컬렉션명", required = true, example = "test_collection") @PathVariable
                      String collectionName);
}
