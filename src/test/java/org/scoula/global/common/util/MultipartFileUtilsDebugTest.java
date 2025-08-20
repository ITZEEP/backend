package org.scoula.global.common.util;

import org.junit.jupiter.api.Test;

class MultipartFileUtilsDebugTest {

      @Test
      void debugNullFileMessage() {
          try {
              MultipartFileUtils.fromFile(null);
          } catch (Exception e) {
              System.out.println("Exception type: " + e.getClass().getName());
              System.out.println("Exception message: " + e.getMessage());
          }
      }

      @Test
      void debugFileConversion() throws Exception {
          java.io.File tempFile = java.io.File.createTempFile("test", ".txt");
          java.nio.file.Files.writeString(tempFile.toPath(), "test content");

          org.springframework.web.multipart.MultipartFile multipartFile =
                  MultipartFileUtils.fromFile(tempFile);
          java.io.File convertedFile = MultipartFileUtils.toTempFile(multipartFile, "temp_", ".txt");

          String content1 = java.nio.file.Files.readString(tempFile.toPath());
          String content2 = java.nio.file.Files.readString(convertedFile.toPath());

          System.out.println("Original content: " + content1);
          System.out.println("Converted content: " + content2);
          System.out.println("Equal: " + content1.equals(content2));

          tempFile.delete();
          convertedFile.delete();
      }
}
