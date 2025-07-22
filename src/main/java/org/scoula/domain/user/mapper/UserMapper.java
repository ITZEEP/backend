package org.scoula.domain.user.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.user.vo.User;

/**
 * 사용자 매퍼
 *
 * <p>사용자 데이터베이스 접근을 담당하는 매퍼입니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@Mapper
public interface UserMapper {

      /**
       * 사용자 등록
       *
       * @param user 사용자 정보
       * @return 처리된 행 수
       */
      int insert(User user);

      /**
       * 사용자 ID로 조회
       *
       * @param userId 사용자 ID
       * @return 사용자 정보
       */
      Optional<User> selectById(@Param("userId") Long userId);

      /**
       * 이메일로 사용자 조회
       *
       * @param email 이메일
       * @return 사용자 정보
       */
      Optional<User> selectByEmail(@Param("email") String email);

      /**
       * 닉네임 존재 여부 확인
       *
       * @param nickname 닉네임
       * @return 존재 여부
       */
      boolean existsByNickname(@Param("nickname") String nickname);

      /**
       * 이메일 존재 여부 확인
       *
       * @param email 이메일
       * @return 존재 여부
       */
      boolean existsByEmail(@Param("email") String email);

      /**
       * 사용자 정보 업데이트
       *
       * @param user 사용자 정보
       * @return 처리된 행 수
       */
      int update(User user);
}
