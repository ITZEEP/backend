package org.scoula.global.common.dto;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 파일 처리 결과 DTO - Resource 기반 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResultDto {
      private Resource resource; // Spring Resource (ByteArrayResource)
      private String fileName; // 파일명
      private String contentType; // MIME 타입
      private long fileSize; // 파일 크기
      private String originalHash; // 원본 파일의 해시값

      // PDF 2-of-3 암호화용 추가 정보
      private Integer threshold;
      private Integer totalShares;

      /** byte[]로부터 FileResultDto 생성하는 정적 메서드 */
      public static FileResultDto fromBytes(byte[] data, String fileName, String contentType) {
          return FileResultDto.builder()
                  .resource(new ByteArrayResource(data))
                  .fileName(fileName)
                  .contentType(contentType)
                  .fileSize(data.length)
                  .build();
      }

      /** 암호화 결과로부터 생성 */
      public static FileResultDto fromEncryption(
              byte[] encryptedData, String fileName, String originalHash) {
          return FileResultDto.builder()
                  .resource(new ByteArrayResource(encryptedData))
                  .fileName(fileName)
                  .contentType("application/octet-stream")
                  .fileSize(encryptedData.length)
                  .originalHash(originalHash)
                  .build();
      }
}
