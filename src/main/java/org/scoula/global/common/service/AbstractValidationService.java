package org.scoula.global.common.service;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.IErrorCode;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.log4j.Log4j2;

/** 공통 검증 로직을 제공하는 추상 서비스 클래스 모든 서비스에서 공통으로 사용하는 검증 메서드들을 정의 */
@Log4j2
public abstract class AbstractValidationService {

      private static final Pattern EMAIL_PATTERN =
              Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

      private static final List<String> ALLOWED_IMAGE_TYPES =
              List.of("image/jpeg", "image/png", "image/gif", "image/webp");

      private static final List<String> ALLOWED_DOCUMENT_TYPES =
              List.of(
                      "application/pdf",
                      "application/msword",
                      "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

      /** 문자열이 비어있지 않은지 검증 */
      protected void validateNotEmpty(String value, String fieldName, IErrorCode errorCode) {
          if (!StringUtils.hasText(value)) {
              log.warn("Validation failed: {} is empty", fieldName);
              throw new BusinessException(errorCode, fieldName + "이(가) 비어있을 수 없습니다");
          }
      }

      /** 이메일 형식 검증 */
      protected void validateEmail(String email, IErrorCode errorCode) {
          if (!StringUtils.hasText(email) || !EMAIL_PATTERN.matcher(email).matches()) {
              log.warn("Validation failed: Invalid email format - {}", email);
              throw new BusinessException(errorCode, "올바른 이메일 형식이 아닙니다");
          }
      }

      /** 파일 크기 검증 */
      protected void validateFileSize(long size, long maxSize, IErrorCode errorCode) {
          if (size > maxSize) {
              log.warn("Validation failed: File size {} exceeds maximum {}", size, maxSize);
              throw new BusinessException(
                      errorCode, String.format("파일 크기가 제한을 초과했습니다. (최대: %d bytes)", maxSize));
          }
      }

      /** 파일 업로드 종합 검증 */
      protected void validateFileUpload(
              MultipartFile file, long maxSize, List<String> allowedTypes, IErrorCode errorCode) {
          if (file == null || file.isEmpty()) {
              throw new BusinessException(errorCode, "업로드할 파일이 없습니다");
          }

          validateFileSize(file.getSize(), maxSize, errorCode);

          String contentType = file.getContentType();
          if (!allowedTypes.contains(contentType)) {
              log.warn("Validation failed: Invalid file type - {}", contentType);
              throw new BusinessException(errorCode, "허용되지 않는 파일 형식입니다");
          }
      }

      /** 컬렉션 크기 검증 */
      protected void validateCollectionSize(
              Collection<?> items, int maxSize, String operationName, IErrorCode errorCode) {
          if (items == null || items.isEmpty()) {
              throw new BusinessException(errorCode, operationName + "할 항목이 없습니다");
          }

          if (items.size() > maxSize) {
              log.warn(
                      "Validation failed: Collection size {} exceeds maximum {} for {}",
                      items.size(),
                      maxSize,
                      operationName);
              throw new BusinessException(
                      errorCode,
                      String.format("%s할 수 있는 최대 항목 수를 초과했습니다. (최대: %d개)", operationName, maxSize));
          }
      }

      /** 서비스 가용성 검증 */
      protected void validateServiceAvailability(
              Object service, String serviceName, IErrorCode errorCode) {
          if (service == null) {
              log.error("Service validation failed: {} is not available", serviceName);
              throw new BusinessException(errorCode, serviceName + " 서비스를 사용할 수 없습니다");
          }
      }

      /** 숫자 범위 검증 */
      protected void validateNumberRange(
              Number value, Number min, Number max, String fieldName, IErrorCode errorCode) {
          if (value == null) {
              throw new BusinessException(errorCode, fieldName + "이(가) 필요합니다");
          }

          if (value.doubleValue() < min.doubleValue() || value.doubleValue() > max.doubleValue()) {
              log.warn(
                      "Validation failed: {} value {} is out of range [{}, {}]",
                      fieldName,
                      value,
                      min,
                      max);
              throw new BusinessException(
                      errorCode, String.format("%s은(는) %s ~ %s 범위여야 합니다", fieldName, min, max));
          }
      }

      /** 이미지 파일 검증 */
      protected void validateImageFile(MultipartFile file, long maxSize, IErrorCode errorCode) {
          validateFileUpload(file, maxSize, ALLOWED_IMAGE_TYPES, errorCode);
      }

      /** 문서 파일 검증 */
      protected void validateDocumentFile(MultipartFile file, long maxSize, IErrorCode errorCode) {
          validateFileUpload(file, maxSize, ALLOWED_DOCUMENT_TYPES, errorCode);
      }
}
