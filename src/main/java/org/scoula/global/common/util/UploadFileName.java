package org.scoula.global.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.CommonErrorCode;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for generating unique file names Provides methods to create unique file names for
 * uploaded files
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UploadFileName {

      private static final DateTimeFormatter TIMESTAMP_FORMATTER =
              DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
      private static final String SEPARATOR = "_";

      /**
       * Generate a unique filename with timestamp
       *
       * @param originalFilename the original filename
       * @return unique filename with timestamp
       * @throws BusinessException if filename is invalid
       */
      public static String getUniqueName(String originalFilename) {
          validateFilename(originalFilename);

          FilenameParts parts = parseFilename(originalFilename);
          String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

          return String.format(
                  "%s%s%s.%s", parts.getName(), SEPARATOR, timestamp, parts.getExtension());
      }

      /**
       * Generate a UUID-based unique filename
       *
       * @param originalFilename the original filename
       * @return unique filename with UUID
       * @throws BusinessException if filename is invalid
       */
      public static String getUuidBasedName(String originalFilename) {
          validateFilename(originalFilename);

          String extension = extractFileExtension(originalFilename);
          return UUID.randomUUID().toString() + "." + extension;
      }

      /**
       * Extract file extension from filename
       *
       * @param filename the filename
       * @return file extension without dot
       * @throws BusinessException if filename has no extension
       */
      public static String extractFileExtension(String filename) {
          validateFilename(filename);

          int lastDotIndex = filename.lastIndexOf(".");
          if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
              throw new BusinessException(
                      CommonErrorCode.INVALID_INPUT_VALUE,
                      "Filename must have a valid extension: " + filename);
          }

          return filename.substring(lastDotIndex + 1).toLowerCase();
      }

      /**
       * Extract filename without extension
       *
       * @param filename the filename
       * @return filename without extension
       */
      public static String extractFileNameWithoutExtension(String filename) {
          validateFilename(filename);

          int lastDotIndex = filename.lastIndexOf(".");
          if (lastDotIndex == -1) {
              return filename;
          }

          return filename.substring(0, lastDotIndex);
      }

      private static void validateFilename(String filename) {
          if (!StringUtils.hasText(filename)) {
              throw new BusinessException(
                      CommonErrorCode.INVALID_INPUT_VALUE, "Filename cannot be null or empty");
          }

          if (filename.trim().length() == 0) {
              throw new BusinessException(
                      CommonErrorCode.INVALID_INPUT_VALUE, "Filename cannot be blank");
          }

          // Check for invalid characters
          String invalidChars = "<>:\"/|?*";
          for (char c : invalidChars.toCharArray()) {
              if (filename.indexOf(c) != -1) {
                  throw new BusinessException(
                          CommonErrorCode.INVALID_INPUT_VALUE,
                          "Filename contains invalid character: " + c);
              }
          }
      }

      private static FilenameParts parseFilename(String filename) {
          int lastDotIndex = filename.lastIndexOf(".");
          if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
              throw new BusinessException(
                      CommonErrorCode.INVALID_INPUT_VALUE,
                      "Filename must have a valid extension: " + filename);
          }

          String name = filename.substring(0, lastDotIndex);
          String extension = filename.substring(lastDotIndex + 1).toLowerCase();

          return new FilenameParts(name, extension);
      }

      private static class FilenameParts {
          private final String name;
          private final String extension;

          public FilenameParts(String name, String extension) {
              this.name = name;
              this.extension = extension;
          }

          public String getName() {
              return name;
          }

          public String getExtension() {
              return extension;
          }
      }
}
