package org.scoula.domain.home.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.home.dto.HomeResponseDTO;
import org.scoula.domain.home.dto.HomeSearchDTO;
import org.scoula.domain.home.mapper.HomeMapper;
import org.scoula.domain.home.vo.HomeVO;
import org.scoula.global.file.service.S3ServiceInterface;

@ExtendWith(MockitoExtension.class)
@DisplayName("HomeServiceImpl 간단 테스트")
class HomeServiceSimpleTest {

      @Mock private HomeMapper homeMapper;
      @Mock private S3ServiceInterface s3Service;

      @InjectMocks private HomeServiceImpl homeService;

      @Test
      @DisplayName("매물 조회 테스트")
      void getHome_Success() {
          // Given
          Integer homeId = 1;
          HomeVO homeVO = new HomeVO();
          homeVO.setHomeId(homeId);

          when(homeMapper.selectHomeById(homeId)).thenReturn(homeVO);

          // When
          HomeResponseDTO result = homeService.getHome(homeId);

          // Then
          assertNotNull(result);
          verify(homeMapper).selectHomeById(homeId);
      }

      @Test
      @DisplayName("매물 목록 조회 테스트")
      void getHomeList_Success() {
          // Given
          int page = 0;
          int size = 10;
          List<HomeVO> expectedHomes = Arrays.asList(new HomeVO(), new HomeVO());
          when(homeMapper.selectHomeList(page * size, size)).thenReturn(expectedHomes);

          // When
          List<HomeResponseDTO> result = homeService.getHomeList(page, size);

          // Then
          assertNotNull(result);
          verify(homeMapper).selectHomeList(0, 10);
      }

      @Test
      @DisplayName("매물 검색 테스트")
      void searchHomes_Success() {
          // Given
          HomeSearchDTO searchDTO = new HomeSearchDTO();
          List<HomeVO> expectedHomes = Arrays.asList(new HomeVO());
          when(homeMapper.selectHomeListByCondition(searchDTO)).thenReturn(expectedHomes);

          // When
          List<HomeResponseDTO> result = homeService.searchHomes(searchDTO);

          // Then
          assertNotNull(result);
          verify(homeMapper).selectHomeListByCondition(searchDTO);
      }

      @Test
      @DisplayName("사용자별 매물 목록 조회 테스트")
      void getHomesByUser_Success() {
          // Given
          Integer userId = 1;
          List<HomeVO> expectedHomes = Arrays.asList(new HomeVO(), new HomeVO());
          when(homeMapper.selectHomeListByUserId(userId)).thenReturn(expectedHomes);

          // When
          List<HomeResponseDTO> result = homeService.getHomesByUser(userId);

          // Then
          assertNotNull(result);
          verify(homeMapper).selectHomeListByUserId(userId);
      }

      @Test
      @DisplayName("매물 찜하기 토글 테스트")
      void toggleHomeLike_Success() {
          // Given
          Integer userId = 1;
          Integer homeId = 1;
          when(homeMapper.selectHomeLikeExists(userId, homeId)).thenReturn(0);

          // When
          homeService.toggleHomeLike(userId, homeId);

          // Then
          verify(homeMapper).selectHomeLikeExists(userId, homeId);
      }

      @Test
      @DisplayName("매물 상태 업데이트 테스트")
      void updateHomeStatus_Success() {
          // Given
          Integer homeId = 1;
          String status = "SOLD";
          Integer userId = 1;
          HomeVO homeVO = new HomeVO();
          homeVO.setUserId(userId);

          when(homeMapper.selectHomeById(homeId)).thenReturn(homeVO);
          when(homeMapper.updateHomeStatus(homeId, status)).thenReturn(1);

          // When
          homeService.updateHomeStatus(homeId, status, userId);

          // Then
          verify(homeMapper).selectHomeById(homeId);
          verify(homeMapper).updateHomeStatus(homeId, status);
      }
}
