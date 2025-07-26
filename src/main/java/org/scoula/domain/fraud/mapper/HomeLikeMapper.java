package org.scoula.domain.fraud.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.fraud.dto.response.LikedHomeResponse;
import org.scoula.global.common.dto.PageRequest;

@Mapper
public interface HomeLikeMapper {

      /**
       * 사용자가 찜한 매물 목록 조회
       *
       * @param userId 사용자 ID
       * @return 찜한 매물 목록
       */
      List<LikedHomeResponse> selectLikedHomesByUserId(@Param("userId") Long userId);

      /**
       * 사용자가 채팅 중인 매물 목록 조회 (구매자로서)
       *
       * @param userId 사용자 ID
       * @param pageRequest 페이징 정보
       * @return 채팅 중인 매물 목록
       */
      List<LikedHomeResponse> selectChattingHomesByUserId(
              @Param("userId") Long userId, @Param("pageRequest") PageRequest pageRequest);

      /**
       * 사용자가 채팅 중인 매물 수 카운트
       *
       * @param userId 사용자 ID
       * @return 채팅 중인 매물 수
       */
      long countChattingHomesByUserId(@Param("userId") Long userId);
}
