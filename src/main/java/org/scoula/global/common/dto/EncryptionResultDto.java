package org.scoula.global.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 암호화 결과 DTO - 암호화된 파일과 메타데이터 포함 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptionResultDto {
      private byte[] encryptedFile; // 암호화된 파일 데이터
      private String originalHash; // 원본 파일의 해시값
      private String fileName; // 파일명
      private String iv; // 초기화 벡터 (이미지 암호화용)
      private String salt; // 솔트 (이미지 암호화용)
      private Integer threshold; // 임계값 (PDF 2-of-3 암호화용)
      private Integer totalShares; // 전체 공유 수 (PDF 2-of-3 암호화용)
}
