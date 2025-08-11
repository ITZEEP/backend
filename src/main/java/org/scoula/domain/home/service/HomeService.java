package org.scoula.domain.home.service;

import java.util.List;

import org.scoula.domain.home.dto.request.HomeCreateRequestDto;
import org.scoula.domain.home.dto.request.HomeReportRequestDto;
import org.scoula.domain.home.dto.request.HomeUpdateRequestDto;
import org.scoula.domain.home.dto.response.HomeResponseDto;
import org.scoula.global.common.dto.PageRequest;
import org.scoula.global.common.dto.PageResponse;

public interface HomeService {

      PageResponse<HomeResponseDto> getHomeList(PageRequest pageRequest);

      HomeResponseDto getHomeDetail(Long homeId);

      Long createHome(Long userId, HomeCreateRequestDto request);

      void updateHome(Long userId, Long homeId, HomeUpdateRequestDto request);

      void deleteHome(Long userId, Long homeId);

      void addLike(Long userId, Long homeId);

      void removeLike(Long userId, Long homeId);

      List<HomeResponseDto> getLikedHomes(Long userId);

      void increaseViewCount(Long homeId);

      //      PageResponse<HomeResponseDto> getMyHomeList(Long userId, PageRequest pageRequest);

      void reportHome(HomeReportRequestDto requestDto);
}
