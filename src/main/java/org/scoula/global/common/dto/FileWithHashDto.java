package org.scoula.global.common.dto;

import java.io.File;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** File 객체와 해시값을 함께 전달하는 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileWithHashDto {
      private File file; // 파일 객체 (임시 파일)
      private String originalHash; // 원본 파일의 SHA-256 해시값
      private String fileName; // 파일명
      private String contentType; // MIME 타입
}
