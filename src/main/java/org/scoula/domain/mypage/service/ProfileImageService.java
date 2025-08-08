package org.scoula.domain.mypage.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 프로필 이미지 관리 서비스 인터페이스
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
public interface ProfileImageService {

      /**
       * 프로필 이미지를 업로드하고 URL을 반환합니다.
       *
       * @param file 업로드할 이미지 파일
       * @param userId 사용자 ID
       * @param previousImageUrl 이전 이미지 URL (삭제용, nullable)
       * @return 업로드된 이미지의 S3 URL
       */
      String uploadProfileImage(MultipartFile file, Long userId, String previousImageUrl);

      /**
       * 외부 URL에서 프로필 이미지를 다운로드하여 S3에 업로드합니다. (OAuth 로그인 시 프로필 이미지 처리용)
       *
       * @param imageUrl 외부 이미지 URL
       * @param userId 사용자 ID
       * @return 업로드된 이미지의 S3 URL, 실패 시 null
       */
      String uploadProfileImageFromUrl(String imageUrl, Long userId);

      /**
       * 프로필 이미지를 삭제합니다.
       *
       * @param imageUrl 삭제할 이미지의 S3 URL
       * @return 삭제 성공 여부
       */
      boolean deleteProfileImage(String imageUrl);

      /**
       * 프로필 이미지 파일의 유효성을 검증합니다.
       *
       * @param file 검증할 파일
       * @throws org.scoula.global.common.exception.BusinessException 유효하지 않은 파일인 경우
       */
      void validateProfileImage(MultipartFile file);
}
