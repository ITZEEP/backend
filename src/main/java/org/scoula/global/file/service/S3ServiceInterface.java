package org.scoula.global.file.service;

import java.io.InputStream;

import org.scoula.global.common.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

/**
 * AWS S3 파일 저장소 서비스 인터페이스
 *
 * <p>AWS S3를 이용한 파일 업로드, 다운로드, 삭제 기능을 정의합니다. 미리 서명된 URL 생성 및 파일 존재 여부 확인 기능도 제공합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
public interface S3ServiceInterface {

      /**
       * 파일을 S3에 업로드합니다.
       *
       * @param file 업로드할 파일
       * @return 업로드된 파일의 S3 키
       * @throws BusinessException 파일 업로드 실패 시
       */
      String uploadFile(MultipartFile file);

      /**
       * 파일명을 지정하여 파일을 S3에 업로드합니다.
       *
       * @param file 업로드할 파일
       * @param fileName 지정할 파일명
       * @return 업로드된 파일의 S3 키
       * @throws BusinessException 파일 업로드 실패 시
       */
      String uploadFile(MultipartFile file, String fileName);

      /**
       * S3에서 파일을 다운로드합니다.
       *
       * @param key S3 파일 키
       * @return 파일의 InputStream
       * @throws BusinessException 파일 다운로드 실패 시
       */
      InputStream downloadFile(String key);

      /**
       * S3에서 파일을 삭제합니다.
       *
       * @param key 삭제할 파일의 S3 키
       * @return 삭제 성공 여부
       * @throws BusinessException 파일 삭제 실패 시
       */
      boolean deleteFile(String key);

      /**
       * 파일의 공개 URL을 가져옵니다.
       *
       * @param key S3 파일 키
       * @return 파일의 공개 URL
       */
      String getFileUrl(String key);

      /**
       * 미리 서명된 URL을 생성합니다.
       *
       * @param key S3 파일 키
       * @param durationMinutes URL 유효 시간 (분)
       * @return 미리 서명된 URL
       * @throws BusinessException URL 생성 실패 시
       */
      String generatePresignedUrl(String key, int durationMinutes);

      /**
       * 파일의 존재 여부를 확인합니다.
       *
       * @param key 확인할 파일의 S3 키
       * @return 파일 존재 여부
       */
      boolean fileExists(String key);

      /**
       * 파일 크기를 가져옵니다.
       *
       * @param key S3 파일 키
       * @return 파일 크기 (바이트)
       * @throws BusinessException 파일을 찾을 수 없거나 조회 실패 시
       */
      long getFileSize(String key);

      /**
       * URL에서 프로필 이미지를 다운로드하고 S3에 업로드합니다.
       *
       * @param imageUrl 프로필 이미지 URL
       * @param userId 사용자 ID
       * @return S3에 저장된 이미지 URL
       */
      String uploadProfileImageFromUrl(String imageUrl, Long userId);
}
