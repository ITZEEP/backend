package org.scoula.global.common.service;

import java.io.File;

import org.scoula.global.common.dto.FileWithHashDto;
import org.springframework.web.multipart.MultipartFile;

/**
 * 암호화/복호화 서비스 인터페이스
 *
 * <h3>사용 예제 - 다른 클래스에서 사용하기</h3>
 *
 * <pre>
 * // 1. 의존성 주입 (Constructor Injection 권장)
 * {@code
 * @Service
 * @RequiredArgsConstructor
 * public class MyService {
 *     private final EncryptionService encryptionService;
 *
 *     // 또는 @Autowired 사용
 *     @Autowired
 *     private EncryptionService encryptionService;
 * }
 * }
 * </pre>
 *
 * <h3>이미지 암호화/복호화 예제</h3>
 *
 * <pre>{@code
 * // 암호화
 * public void encryptImageExample(MultipartFile imageFile) {
 *     try {
 *         File encryptedFile = encryptionService.encryptImage(imageFile);
 *
 *         // 파일 처리 (예: S3 업로드, 파일 시스템 저장 등)
 *         uploadToS3(encryptedFile);
 *
 *         // 임시 파일은 사용 후 삭제
 *         encryptedFile.delete();
 *     } catch (Exception e) {
 *         log.error("암호화 실패", e);
 *     }
 * }
 *
 * // 복호화
 * public void decryptImageExample(MultipartFile encryptedFile) {
 *     try {
 *         File decryptedFile = encryptionService.decryptImage(encryptedFile, null);
 *
 *         // 복호화된 파일 처리
 *         processImage(decryptedFile);
 *
 *         // 임시 파일 삭제
 *         decryptedFile.delete();
 *     } catch (Exception e) {
 *         log.error("복호화 실패", e);
 *     }
 * }
 * }</pre>
 *
 * <h3>PDF 2단계 암호화 예제</h3>
 *
 * <pre>{@code
 * // Step 1: 첫 번째 사용자가 패스워드만 제공
 * String contractId = "contract123";
 * String password1 = "owner-password";
 *
 * String status = encryptionService.uploadPdfStep1(contractId, password1);
 * // Redis에 패스워드 저장됨, PDF 파일과 두 번째 패스워드 대기
 *
 * // Step 2: 두 번째 사용자가 PDF 파일과 패스워드 제공
 * MultipartFile pdfFile = ...;
 * String password2 = "tenant-password";
 * FileWithHashDto encryptedPdf = encryptionService.encryptPdfStep2(pdfFile, contractId, password2);
 *
 * // 암호화된 PDF 저장 또는 전송
 * saveToDatabase(encryptedPdf);
 * encryptedPdf.delete();
 * }</pre>
 *
 * <h3>임시 파일 처리 주의사항</h3>
 *
 * <ul>
 *   <li>반환된 File 객체는 임시 파일이므로 사용 후 반드시 삭제
 *   <li>deleteOnExit()가 설정되어 있지만, 명시적 삭제 권장
 *   <li>파일 스트림 사용 시 try-with-resources 패턴 사용
 * </ul>
 *
 * @author ITZeep Backend Team
 */
public interface EncryptionService {

      // ==================== 주요 서비스 메서드 (의존성 주입용) ====================

      /**
       * 이미지 파일 암호화 (서버 키 사용)
       *
       * @param imageFile 암호화할 이미지 파일
       * @return 암호화된 파일과 해시값 (FileWithHashDto 객체)
       */
      FileWithHashDto encryptImage(MultipartFile imageFile) throws Exception;

      /**
       * 이미지 파일 복호화 (서버 키 사용)
       *
       * @param encryptedFile 암호화된 파일 (salt와 iv 포함)
       * @param originalHash 원본 해시값 (무결성 검증용, null 가능)
       * @return 복호화된 파일 (File 객체)
       */
      File decryptImage(MultipartFile encryptedFile, String originalHash) throws Exception;

      /**
       * PDF에 비밀번호 설정 (편집 권한 제한 없음)
       *
       * @param pdfFile PDF 파일
       * @param password 설정할 비밀번호
       * @return 비밀번호가 설정된 파일과 해시값 (FileWithHashDto 객체)
       */
      FileWithHashDto addPasswordToPdf(MultipartFile pdfFile, String password) throws Exception;

      /**
       * PDF 복호화 (2-of-3 방식)
       *
       * @param encryptedFile 암호화된 PDF 파일
       * @param password 패스워드 (password1 또는 password2)
       * @param originalHash 원본 해시값 (무결성 검증용, null 가능)
       * @return 복호화된 파일 (File 객체)
       */
      File decryptPdf(MultipartFile encryptedFile, String password, String originalHash)
              throws Exception;

      // ==================== PDF 2단계 암호화 (계약 프로세스용) ====================

      /**
       * PDF 암호화 1차 - Redis에 첫 번째 패스워드만 저장
       *
       * @param contractChatId 계약 채팅 ID
       * @param password1 첫 번째 패스워드
       * @return 처리 상태
       */
      String uploadPdfStep1(String contractChatId, String password1) throws Exception;

      /**
       * PDF 암호화 2차 - PDF 파일 업로드 및 봉투키 암호화로 최종 암호화
       *
       * @param pdfFile PDF 파일
       * @param contractChatId 계약 채팅 ID
       * @param password2 두 번째 패스워드
       * @return 암호화된 파일과 해시값 (FileWithHashDto 객체)
       */
      FileWithHashDto encryptPdfStep2(MultipartFile pdfFile, String contractChatId, String password2)
              throws Exception;

      /**
       * 계약 ID로 키 존재 여부 확인
       *
       * @param contractChatId 계약 채팅 ID
       * @return 키 존재 여부 (true: 키가 존재함, false: 키가 없음)
       */
      boolean hasKey(String contractChatId);
}
