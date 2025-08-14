package org.scoula.domain.home.controller;

import java.util.List;

import javax.validation.Valid;

import org.scoula.domain.home.dto.HomeCreateRequestDto;
import org.scoula.domain.home.dto.HomeResponseDTO;
import org.scoula.domain.home.dto.HomeSearchDTO;
import org.scoula.domain.home.dto.HomeUpdateRequestDto;
import org.scoula.domain.home.vo.FacilityCategory;
import org.scoula.domain.home.vo.FacilityItem;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = "매물 관리", description = "매물 등록, 조회, 수정, 삭제 API")
@RequestMapping("/api/homes")
public interface HomeController {

      @ApiOperation(value = "매물 등록", notes = "새로운 매물을 등록합니다. 이미지 파일을 함께 업로드할 수 있습니다.")
      @PostMapping(consumes = "multipart/form-data")
      ResponseEntity<ApiResponse<Integer>> createHome(
              @Valid @ModelAttribute HomeCreateRequestDto requestDto, Authentication authentication);

      @ApiOperation(value = "매물 상세 조회", notes = "매물 ID로 상세 정보를 조회합니다.")
      @GetMapping("/{homeId}")
      ResponseEntity<ApiResponse<HomeResponseDTO>> getHome(
              @ApiParam(value = "매물 ID", required = true) @PathVariable Integer homeId);

      @ApiOperation(value = "매물 목록 조회", notes = "페이징된 매물 목록을 조회합니다.")
      @GetMapping
      ResponseEntity<PageResponse<HomeResponseDTO>> getHomeList(
              @ApiParam(value = "페이지 번호 (1부터 시작)", defaultValue = "1")
                      @RequestParam(defaultValue = "1")
                      int page,
              @ApiParam(value = "페이지 크기", defaultValue = "21") @RequestParam(defaultValue = "21")
                      int size);

      @ApiOperation(value = "매물 검색", notes = "조건에 따라 매물을 검색합니다.")
      @GetMapping("/search")
      ResponseEntity<PageResponse<HomeResponseDTO>> searchHomes(
              @ApiParam(value = "검색 조건") @ModelAttribute HomeSearchDTO searchDTO);

      @ApiOperation(
              value = "매물 수정",
              notes = "기존 매물 정보를 수정합니다. 이미지 파일을 추가로 업로드하거나 기존 이미지를 삭제할 수 있습니다.")
      @PutMapping(value = "/{homeId}", consumes = "multipart/form-data")
      ResponseEntity<ApiResponse<Void>> updateHome(
              @ApiParam(value = "매물 ID", required = true, example = "123") @PathVariable
                      Integer homeId,
              @Valid @ModelAttribute HomeUpdateRequestDto updateDto,
              @ApiParam(value = "새로 추가할 이미지 파일들") @RequestParam(value = "newImages", required = false)
                      List<MultipartFile> newImages,
              Authentication authentication);

      @ApiOperation(value = "매물 삭제", notes = "매물을 삭제합니다.")
      @DeleteMapping("/{homeId}")
      ResponseEntity<ApiResponse<Void>> deleteHome(
              @ApiParam(value = "매물 ID", required = true) @PathVariable Integer homeId,
              Authentication authentication);

      @ApiOperation(value = "매물 상태 변경", notes = "매물의 상태를 변경합니다.")
      @PatchMapping("/{homeId}/status")
      ResponseEntity<ApiResponse<Void>> updateHomeStatus(
              @ApiParam(value = "매물 ID", required = true) @PathVariable Integer homeId,
              @ApiParam(value = "변경할 상태", required = true) @RequestParam String status,
              Authentication authentication);

      @ApiOperation(value = "내 매물 목록 조회", notes = "로그인한 사용자의 매물 목록을 조회합니다.")
      @GetMapping("/my")
      ResponseEntity<ApiResponse<List<HomeResponseDTO>>> getMyHomes(Authentication authentication);

      @ApiOperation(value = "매물 찜하기/해제", notes = "매물을 찜하거나 찜을 해제합니다.")
      @PostMapping("/{homeId}/like")
      ResponseEntity<ApiResponse<Void>> toggleHomeLike(
              @ApiParam(value = "매물 ID", required = true) @PathVariable Integer homeId,
              Authentication authentication);

      @ApiOperation(value = "찜한 매물 목록 조회", notes = "사용자가 찜한 매물 목록을 조회합니다.")
      @GetMapping("/likes")
      ResponseEntity<ApiResponse<List<HomeResponseDTO>>> getHomeLikes(Authentication authentication);

      @ApiOperation(value = "편의시설 카테고리 조회", notes = "편의시설 카테고리 목록을 조회합니다.")
      @GetMapping("/facilities/categories")
      ResponseEntity<ApiResponse<List<FacilityCategory>>> getFacilityCategories();

      @ApiOperation(value = "편의시설 아이템 조회", notes = "특정 카테고리의 편의시설 아이템 목록을 조회합니다.")
      @GetMapping("/facilities/categories/{categoryId}/items")
      ResponseEntity<ApiResponse<List<FacilityItem>>> getFacilityItems(
              @ApiParam(value = "카테고리 ID", required = true) @PathVariable Integer categoryId);
}
