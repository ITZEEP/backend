package org.scoula.global.common.util;

import java.io.*;
import java.nio.file.Files;
import java.util.Objects;

import org.springframework.web.multipart.MultipartFile;

/** File ↔ MultipartFile 변환 유틸 (운영 코드용, spring-test 불필요) */
// 파일 변환 유틸
public final class MultipartFileUtils {

      private MultipartFileUtils() {}

      /** File → MultipartFile (커스텀 구현체로 감싸기) */
      public static MultipartFile fromFile(File file) {
          return fromFile(file, file.getName(), probeContentType(file));
      }

      /** File → MultipartFile (파일명/컨텐츠타입 지정 가능) */
      public static MultipartFile fromFile(File file, String originalFilename, String contentType) {
          Objects.requireNonNull(file, "file must not be null");
          return new FileMultipartFile(file, originalFilename, contentType);
      }

      /** MultipartFile → 임시 File (호출자가 삭제 책임) */
      public static File toTempFile(MultipartFile multipart, String prefix, String suffix)
              throws IOException {
          Objects.requireNonNull(multipart, "multipart must not be null");
          File temp = File.createTempFile(prefix, suffix);
          try (InputStream in = multipart.getInputStream()) {
              Files.copy(in, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
          }
          return temp;
      }

      /** InputStream → 임시 File (기본 prefix/suffix 사용: "contract_", ".pdf") */
      public static File inputStreamToTempFile(InputStream in) throws IOException {
          return inputStreamToTempFile(in, "contract_", ".pdf");
      }

      /** InputStream → 임시 File (호출자가 삭제 책임) */
      public static File inputStreamToTempFile(InputStream in, String prefix, String suffix)
              throws IOException {
          Objects.requireNonNull(in, "inputStream must not be null");
          if (prefix == null || prefix.isBlank()) prefix = "contract_";
          if (suffix == null || suffix.isBlank()) suffix = ".pdf";

          File temp = File.createTempFile(prefix, suffix);
          try (InputStream src = in) {
              Files.copy(src, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
          }
          return temp;
      }

      /** InputStream → MultipartFile (기본 값 사용) */
      public static MultipartFile inputStreamToMultipartFile(InputStream in) throws IOException {
          return inputStreamToMultipartFile(in, "file", "application/octet-stream");
      }

      /** InputStream → MultipartFile (파일명 및 컨텐츠타입 지정 가능) */
      public static MultipartFile inputStreamToMultipartFile(
              InputStream in, String originalFilename, String contentType) throws IOException {
          Objects.requireNonNull(in, "inputStream must not be null");
          if (originalFilename == null || originalFilename.isBlank()) {
              originalFilename = "file";
          }
          if (contentType == null || contentType.isBlank()) {
              contentType = "application/octet-stream";
          }
          // 임시 파일을 만든 뒤 MultipartFile로 변환
          File tempFile = inputStreamToTempFile(in);
          return fromFile(tempFile, originalFilename, contentType);
      }

      /** File → byte[] (전체 파일 메모리에 적재) */
      public static byte[] fileToBytes(File file) throws IOException {
          Objects.requireNonNull(file, "file must not be null");
          return Files.readAllBytes(file.toPath());
      }

      /** MultipartFile → byte[] (전체 파일 메모리에 적재) */
      public static byte[] multipartToBytes(MultipartFile multipart) throws IOException {
          Objects.requireNonNull(multipart, "multipart must not be null");
          return multipart.getBytes(); // 내부적으로 InputStream을 모두 읽어 byte[]로 반환
      }

      /** InputStream → byte[] (전체 스트림 메모리에 적재) */
      public static byte[] inputStreamToBytes(InputStream in) throws IOException {
          Objects.requireNonNull(in, "inputStream must not be null");
          try (InputStream src = in;
                  ByteArrayOutputStream out = new ByteArrayOutputStream()) {
              byte[] buf = new byte[8192];
              int n;
              while ((n = src.read(buf)) != -1) {
                  out.write(buf, 0, n);
              }
              return out.toByteArray();
          }
      }

      /** byte[] → 임시 File (호출자가 삭제 책임) */
      public static File bytesToTempFile(byte[] bytes, String prefix, String suffix)
              throws IOException {
          Objects.requireNonNull(bytes, "bytes must not be null");
          if (prefix == null || prefix.isBlank()) prefix = "contract_";
          if (suffix == null || suffix.isBlank()) suffix = ".bin";
          File temp = File.createTempFile(prefix, suffix);
          Files.write(temp.toPath(), bytes);
          return temp;
      }

      // 파일 삭제 예시
      //    File tempFile = MultipartFileUtils.toTempFile(multipart, "sig_", ".png");
      //
      // try {
      //        // tempFile 사용 (예: 암호화, 업로드 등)
      //        s3Service.uploadFile(tempFile);
      //    } finally {
      //        // 사용 후 직접 삭제
      //        if (tempFile.delete()) {
      //            log.info("임시 파일 삭제 완료: {}", tempFile.getAbsolutePath());
      //        } else {
      //            log.warn("임시 파일 삭제 실패: {}", tempFile.getAbsolutePath());
      //        }
      //    }

      private static String probeContentType(File file) {
          try {
              String ct = Files.probeContentType(file.toPath());
              return (ct != null) ? ct : "application/octet-stream";
          } catch (IOException e) {
              return "application/octet-stream";
          }
      }

      /** 운영 코드에서 사용할 간단한 MultipartFile 구현체 (파일을 래핑) */
      private static final class FileMultipartFile implements MultipartFile {
          private final File file;
          private final String originalFilename;
          private final String contentType;

          FileMultipartFile(File file, String originalFilename, String contentType) {
              this.file = file;
              this.originalFilename = originalFilename;
              this.contentType = contentType;
          }

          @Override
          public String getName() {
              return "file";
          }

          @Override
          public String getOriginalFilename() {
              return originalFilename;
          }

          @Override
          public String getContentType() {
              return contentType;
          }

          @Override
          public boolean isEmpty() {
              return file.length() == 0;
          }

          @Override
          public long getSize() {
              return file.length();
          }

          @Override
          public byte[] getBytes() throws IOException {
              return Files.readAllBytes(file.toPath());
          }

          @Override
          public InputStream getInputStream() throws IOException {
              return new BufferedInputStream(new FileInputStream(file));
          }

          @Override
          public void transferTo(File dest) throws IOException {
              Files.copy(
                      file.toPath(),
                      dest.toPath(),
                      java.nio.file.StandardCopyOption.REPLACE_EXISTING);
          }
      }
}
