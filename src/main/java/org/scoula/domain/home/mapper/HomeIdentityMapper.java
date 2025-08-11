package org.scoula.domain.home.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.home.vo.HomeIdentityVO;

@Mapper
public interface HomeIdentityMapper {

      // 본인 인증 정보 저장
      int insertHomeIdentity(@Param("vo") HomeIdentityVO vo);

      // 본인 인증 정보 조회
      Optional<HomeIdentityVO> selectHomeIdentity(
              @Param("homeId") Long homeId, @Param("userId") Long userId);

      // 본인 인증 정보 업데이트
      int updateHomeIdentity(@Param("vo") HomeIdentityVO vo);

      // 매물 ID로 본인 인증 정보 조회
      Optional<HomeIdentityVO> selectHomeIdentityByHomeId(@Param("homeId") Long homeId);
}
