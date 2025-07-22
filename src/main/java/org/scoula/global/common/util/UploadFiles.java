package org.scoula.global.common.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.scoula.global.common.constant.Constants;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.CommonErrorCode;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Utility class for file upload operations Provides methods for uploading, downloading, and
 * managing files
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Log4j2
public final class UploadFiles {

      private static final long MAX_FILE_SIZE = Constants.File.MAX_FILE_SIZE_BYTES;
      private static final List<String> ALLOWED_EXTENSIONS =
              Arrays.asList(Constants.File.ALLOWED_IMAGE_EXTENSIONS);
      private static final List<String> ALLOWED_DOCUMENT_EXTENSIONS =
              Arrays.asList(Constants.File.ALLOWED_DOCUMENT_EXTENSIONS);

      @Data
      @Builder
      public static class UploadResult {
          private String filePath;
          private String fileName;
          private String originalFileName;
          private long fileSize;
          private String contentType;
      }

      /**
       * Upload a file to the specified directory
       *
       * @param baseDir target directory path
       * @param file multipart file to upload
       * @return UploadResult containing upload information
       * @throws BusinessException if upload fails
       */
      public static UploadResult upload(String baseDir, MultipartFile file) {
          validateUploadParameters(baseDir, file);

          try {
              File baseDirectory = createDirectoryIfNotExists(baseDir);
              String uniqueFileName = UploadFileName.getUniqueName(file.getOriginalFilename());
              File destinationFile = new File(baseDirectory, uniqueFileName);

              file.transferTo(destinationFile);
              log.info(
                      "File uploaded successfully: {} -> {}",
                      file.getOriginalFilename(),
                      destinationFile.getPath());

              return UploadResult.builder()
                      .filePath(destinationFile.getPath())
                      .fileName(uniqueFileName)
                      .originalFileName(file.getOriginalFilename())
                      .fileSize(file.getSize())
                      .contentType(file.getContentType())
                      .build();

          } catch (IOException e) {
              log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
              throw new BusinessException(
                      CommonErrorCode.FILE_UPLOAD_FAILED, "Failed to upload file", e);
          }
      }

      /**
       * Format file size into human-readable string
       *
       * @param size file size in bytes
       * @return formatted size string
       */
      public static String getFormatSize(Long size) {
          if (size == null || size <= 0) {
              return "0 Bytes";
          }

          final String[] units = {"Bytes", "KB", "MB", "GB", "TB"};
          int digitGroups = Math.min((int) (Math.log10(size) / Math.log10(1024)), units.length - 1);

          double formattedSize = size / Math.pow(1024, digitGroups);
          DecimalFormat formatter = new DecimalFormat("#,##0.##");

          return formatter.format(formattedSize) + " " + units[digitGroups];
      }

      /**
       * Download a file through HTTP response
       *
       * @param response HTTP servlet response
       * @param file file to download
       * @param originalName original filename for download
       * @throws BusinessException if download fails
       */
      public static void download(HttpServletResponse response, File file, String originalName) {
          validateDownloadParameters(response, file, originalName);

          try {
              if (!file.exists() || !file.canRead()) {
                  throw new BusinessException(
                          CommonErrorCode.FILE_NOT_FOUND,
                          "File not found or not readable: " + file.getPath());
              }

              String mimeType = Files.probeContentType(file.toPath());
              if (mimeType == null) {
                  mimeType = "application/octet-stream";
              }

              response.setContentType(mimeType);
              response.setContentLengthLong(file.length());

              String encodedFileName =
                      URLEncoder.encode(originalName, StandardCharsets.UTF_8)
                              .replaceAll(
                                      "\\+", "%20"); // Replace + with %20 for better compatibility

              response.setHeader(
                      "Content-Disposition",
                      String.format(
                              "attachment; filename=\"%s\"; filename*=UTF-8''%s",
                              originalName, encodedFileName));

              try (OutputStream outputStream = response.getOutputStream();
                      BufferedOutputStream bufferedOutputStream =
                              new BufferedOutputStream(outputStream)) {

                  Path filePath = Paths.get(file.getPath());
                  Files.copy(filePath, bufferedOutputStream);
                  bufferedOutputStream.flush();

                  log.info("File downloaded successfully: {}", originalName);
              }

          } catch (IOException e) {
              log.error("Failed to download file: {}", originalName, e);
              throw new BusinessException(
                      CommonErrorCode.FILE_DOWNLOAD_FAILED, "Failed to download file", e);
          }
      }

      /**
       * Check if file extension is allowed
       *
       * @param fileName file name to check
       * @return true if extension is allowed
       */
      public static boolean isAllowedExtension(String fileName) {
          if (!StringUtils.hasText(fileName)) {
              return false;
          }

          try {
              String extension = UploadFileName.extractFileExtension(fileName);
              return ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
          } catch (BusinessException e) {
              return false;
          }
      }

      /**
       * Delete a file safely
       *
       * @param filePath path to the file to delete
       * @return true if file was deleted successfully
       */
      public static boolean deleteFile(String filePath) {
          if (!StringUtils.hasText(filePath)) {
              log.warn("Attempted to delete file with empty path");
              return false;
          }

          try {
              Path path = Paths.get(filePath);
              boolean deleted = Files.deleteIfExists(path);

              if (deleted) {
                  log.info("File deleted successfully: {}", filePath);
              } else {
                  log.warn("File not found for deletion: {}", filePath);
              }

              return deleted;
          } catch (IOException e) {
              log.error("Failed to delete file: {}", filePath, e);
              return false;
          }
      }

      // Private validation methods

      private static void validateUploadParameters(String baseDir, MultipartFile file) {
          if (!StringUtils.hasText(baseDir)) {
              throw new BusinessException(
                      CommonErrorCode.INVALID_INPUT_VALUE, "Base directory cannot be empty");
          }

          if (file == null || file.isEmpty()) {
              throw new BusinessException(
                      CommonErrorCode.INVALID_INPUT_VALUE, "File cannot be null or empty");
          }

          if (file.getSize() > MAX_FILE_SIZE) {
              throw new BusinessException(
                      CommonErrorCode.FILE_SIZE_EXCEEDED,
                      String.format(
                              "File size %d exceeds maximum allowed size %d",
                              file.getSize(), MAX_FILE_SIZE));
          }

          String originalFilename = file.getOriginalFilename();
          if (!isAllowedExtension(originalFilename)) {
              throw new BusinessException(
                      CommonErrorCode.INVALID_FILE_TYPE,
                      "File type not allowed: " + originalFilename);
          }
      }

      private static void validateDownloadParameters(
              HttpServletResponse response, File file, String originalName) {
          if (response == null) {
              throw new BusinessException(
                      CommonErrorCode.INVALID_INPUT_VALUE, "HTTP response cannot be null");
          }

          if (file == null) {
              throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "File cannot be null");
          }

          if (!StringUtils.hasText(originalName)) {
              throw new BusinessException(
                      CommonErrorCode.INVALID_INPUT_VALUE, "Original filename cannot be empty");
          }
      }

      private static File createDirectoryIfNotExists(String baseDir) {
          File directory = new File(baseDir);

          if (!directory.exists()) {
              boolean created = directory.mkdirs();
              if (!created) {
                  throw new BusinessException(
                          CommonErrorCode.FILE_UPLOAD_FAILED,
                          "Failed to create directory: " + baseDir);
              }
              log.info("Created directory: {}", baseDir);
          }

          if (!directory.canWrite()) {
              throw new BusinessException(
                      CommonErrorCode.FILE_UPLOAD_FAILED, "Directory is not writable: " + baseDir);
          }

          return directory;
      }
}
