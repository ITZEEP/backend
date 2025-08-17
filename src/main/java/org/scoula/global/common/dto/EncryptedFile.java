package org.scoula.global.common.dto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.Builder;
import lombok.Data;

/** 암호화/복호화된 파일을 표현하는 DTO */
@Data
@Builder
public class EncryptedFile {
      private byte[] data; // 파일 데이터
      private String fileName; // 파일명
      private String contentType; // MIME 타입
      private String originalHash; // 원본 해시값
      private long size; // 파일 크기

      // PDF 2-of-3 암호화용 추가 정보
      private Integer threshold;
      private Integer totalShares;

      /** InputStream으로 파일 데이터 접근 */
      public InputStream getInputStream() {
          return new ByteArrayInputStream(data);
      }

      /** 파일 크기 자동 계산 */
      public long getSize() {
          return data != null ? data.length : 0;
      }

      /** 빈 파일 여부 확인 */
      public boolean isEmpty() {
          return data == null || data.length == 0;
      }

      /** 파일 데이터를 byte[]로 반환 */
      public byte[] getBytes() throws IOException {
          return data;
      }

      /** 정적 팩토리 메서드 - 암호화 결과 생성 */
      public static EncryptedFile encrypt(byte[] data, String fileName, String originalHash) {
          return EncryptedFile.builder()
                  .data(data)
                  .fileName(fileName)
                  .contentType("application/octet-stream")
                  .originalHash(originalHash)
                  .size(data.length)
                  .build();
      }

      /** 정적 팩토리 메서드 - 복호화 결과 생성 */
      public static EncryptedFile decrypt(byte[] data, String fileName, String contentType) {
          return EncryptedFile.builder()
                  .data(data)
                  .fileName(fileName)
                  .contentType(contentType != null ? contentType : "application/octet-stream")
                  .size(data.length)
                  .build();
      }
}
