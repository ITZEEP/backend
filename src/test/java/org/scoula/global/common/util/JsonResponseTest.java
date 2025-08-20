package org.scoula.global.common.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonResponse 테스트")
class JsonResponseTest {

      @Mock private HttpServletResponse response;
      private StringWriter stringWriter;
      private PrintWriter printWriter;

      @BeforeEach
      void setUp() throws IOException {
          stringWriter = new StringWriter();
          printWriter = new PrintWriter(stringWriter);
          when(response.getWriter()).thenReturn(printWriter);
      }

      @Nested
      @DisplayName("sendError 메서드 테스트")
      class SendErrorTest {

          @Test
          @DisplayName("에러 응답 전송 성공 - 400 Bad Request")
          void sendError_BadRequest() throws IOException {
              // Given
              HttpStatus status = HttpStatus.BAD_REQUEST;
              String message = "잘못된 요청입니다";

              // When
              JsonResponse.sendError(response, status, message);

              // Then
              verify(response).setStatus(400);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertTrue(jsonOutput.contains("\"status\":400"));
              assertTrue(jsonOutput.contains("\"message\":\"잘못된 요청입니다\""));
          }

          @Test
          @DisplayName("에러 응답 전송 성공 - 401 Unauthorized")
          void sendError_Unauthorized() throws IOException {
              // Given
              HttpStatus status = HttpStatus.UNAUTHORIZED;
              String message = "인증이 필요합니다";

              // When
              JsonResponse.sendError(response, status, message);

              // Then
              verify(response).setStatus(401);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertTrue(jsonOutput.contains("\"status\":401"));
              assertTrue(jsonOutput.contains("\"message\":\"인증이 필요합니다\""));
          }

          @Test
          @DisplayName("에러 응답 전송 성공 - 403 Forbidden")
          void sendError_Forbidden() throws IOException {
              // Given
              HttpStatus status = HttpStatus.FORBIDDEN;
              String message = "접근이 금지되었습니다";

              // When
              JsonResponse.sendError(response, status, message);

              // Then
              verify(response).setStatus(403);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertTrue(jsonOutput.contains("\"status\":403"));
              assertTrue(jsonOutput.contains("\"message\":\"접근이 금지되었습니다\""));
          }

          @Test
          @DisplayName("에러 응답 전송 성공 - 404 Not Found")
          void sendError_NotFound() throws IOException {
              // Given
              HttpStatus status = HttpStatus.NOT_FOUND;
              String message = "리소스를 찾을 수 없습니다";

              // When
              JsonResponse.sendError(response, status, message);

              // Then
              verify(response).setStatus(404);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertTrue(jsonOutput.contains("\"status\":404"));
              assertTrue(jsonOutput.contains("\"message\":\"리소스를 찾을 수 없습니다\""));
          }

          @Test
          @DisplayName("에러 응답 전송 성공 - 500 Internal Server Error")
          void sendError_InternalServerError() throws IOException {
              // Given
              HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
              String message = "서버 내부 오류가 발생했습니다";

              // When
              JsonResponse.sendError(response, status, message);

              // Then
              verify(response).setStatus(500);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertTrue(jsonOutput.contains("\"status\":500"));
              assertTrue(jsonOutput.contains("\"message\":\"서버 내부 오류가 발생했습니다\""));
          }

          @Test
          @DisplayName("빈 메시지로 에러 응답 전송")
          void sendError_EmptyMessage() throws IOException {
              // Given
              HttpStatus status = HttpStatus.BAD_REQUEST;
              String message = "";

              // When
              JsonResponse.sendError(response, status, message);

              // Then
              verify(response).setStatus(400);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertTrue(jsonOutput.contains("\"status\":400"));
              assertTrue(jsonOutput.contains("\"message\":\"\""));
          }

          @Test
          @DisplayName("null 메시지로 에러 응답 전송")
          void sendError_NullMessage() throws IOException {
              // Given
              HttpStatus status = HttpStatus.BAD_REQUEST;
              String message = null;

              // When
              JsonResponse.sendError(response, status, message);

              // Then
              verify(response).setStatus(400);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertTrue(jsonOutput.contains("\"status\":400"));
              assertTrue(jsonOutput.contains("\"message\":null"));
          }

          @Test
          @DisplayName("특수 문자가 포함된 메시지로 에러 응답 전송")
          void sendError_SpecialCharactersMessage() throws IOException {
              // Given
              HttpStatus status = HttpStatus.BAD_REQUEST;
              String message = "에러: \"잘못된 입력\", 다시 시도하세요 & 검토해주세요";

              // When
              JsonResponse.sendError(response, status, message);

              // Then
              verify(response).setStatus(400);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertTrue(jsonOutput.contains("\"status\":400"));
              // JSON 이스케이프 처리 확인
              assertTrue(jsonOutput.contains("\\\"잘못된 입력\\\""));
          }

          @Test
          @DisplayName("긴 메시지로 에러 응답 전송")
          void sendError_LongMessage() throws IOException {
              // Given
              HttpStatus status = HttpStatus.BAD_REQUEST;
              String message = "매우 긴 에러 메시지입니다. ".repeat(50);

              // When
              JsonResponse.sendError(response, status, message);

              // Then
              verify(response).setStatus(400);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertTrue(jsonOutput.contains("\"status\":400"));
              assertTrue(jsonOutput.contains("\"message\":"));
              assertTrue(jsonOutput.length() > 500); // 긴 메시지 확인 (더 보수적인 값)
          }

          @ParameterizedTest
          @ValueSource(ints = {100, 200, 300, 400, 401, 403, 404, 500, 502, 503})
          @DisplayName("다양한 HTTP 상태 코드로 에러 응답 전송")
          void sendError_VariousStatusCodes(int statusCode) throws IOException {
              // Given
              HttpStatus status = HttpStatus.valueOf(statusCode);
              String message = "상태 코드 " + statusCode + " 에러";

              // When
              JsonResponse.sendError(response, status, message);

              // Then
              verify(response).setStatus(statusCode);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertTrue(jsonOutput.contains("\"status\":" + statusCode));
              assertTrue(jsonOutput.contains("\"message\":\"상태 코드 " + statusCode + " 에러\""));
          }

          @Test
          @DisplayName("IOException 발생 시 예외 전파")
          void sendError_IOExceptionThrown() throws IOException {
              // Given
              HttpStatus status = HttpStatus.BAD_REQUEST;
              String message = "에러 메시지";
              when(response.getWriter()).thenThrow(new IOException("Writer 에러"));

              // When & Then
              IOException exception =
                      assertThrows(
                              IOException.class,
                              () -> JsonResponse.sendError(response, status, message));
              assertEquals("Writer 에러", exception.getMessage());
          }
      }

      @Nested
      @DisplayName("sendSuccess 메서드 테스트")
      class SendSuccessTest {

          @Test
          @DisplayName("문자열 데이터로 성공 응답 전송")
          void sendSuccess_StringData() throws IOException {
              // Given
              String data = "성공적으로 처리되었습니다";

              // When
              JsonResponse.sendSuccess(response, data);

              // Then
              verify(response).setStatus(200);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertEquals("\"성공적으로 처리되었습니다\"", jsonOutput);
          }

          @Test
          @DisplayName("숫자 데이터로 성공 응답 전송")
          void sendSuccess_NumberData() throws IOException {
              // Given
              Integer data = 12345;

              // When
              JsonResponse.sendSuccess(response, data);

              // Then
              verify(response).setStatus(200);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertEquals("12345", jsonOutput);
          }

          @Test
          @DisplayName("boolean 데이터로 성공 응답 전송")
          void sendSuccess_BooleanData() throws IOException {
              // Given
              Boolean data = true;

              // When
              JsonResponse.sendSuccess(response, data);

              // Then
              verify(response).setStatus(200);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertEquals("true", jsonOutput);
          }

          @Test
          @DisplayName("객체 데이터로 성공 응답 전송")
          void sendSuccess_ObjectData() throws IOException {
              // Given
              TestObject data = new TestObject("홍길동", 30);

              // When
              JsonResponse.sendSuccess(response, data);

              // Then
              verify(response).setStatus(200);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertTrue(jsonOutput.contains("\"name\":\"홍길동\""));
              assertTrue(jsonOutput.contains("\"age\":30"));
          }

          @Test
          @DisplayName("배열 데이터로 성공 응답 전송")
          void sendSuccess_ArrayData() throws IOException {
              // Given
              String[] data = {"apple", "banana", "cherry"};

              // When
              JsonResponse.sendSuccess(response, data);

              // Then
              verify(response).setStatus(200);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertTrue(jsonOutput.contains("\"apple\""));
              assertTrue(jsonOutput.contains("\"banana\""));
              assertTrue(jsonOutput.contains("\"cherry\""));
              assertTrue(jsonOutput.startsWith("["));
              assertTrue(jsonOutput.endsWith("]"));
          }

          @Test
          @DisplayName("null 데이터로 성공 응답 전송")
          void sendSuccess_NullData() throws IOException {
              // Given
              Object data = null;

              // When
              JsonResponse.sendSuccess(response, data);

              // Then
              verify(response).setStatus(200);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertEquals("null", jsonOutput);
          }

          @Test
          @DisplayName("빈 문자열 데이터로 성공 응답 전송")
          void sendSuccess_EmptyStringData() throws IOException {
              // Given
              String data = "";

              // When
              JsonResponse.sendSuccess(response, data);

              // Then
              verify(response).setStatus(200);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertEquals("\"\"", jsonOutput);
          }

          @Test
          @DisplayName("복잡한 중첩 객체로 성공 응답 전송")
          void sendSuccess_NestedObjectData() throws IOException {
              // Given
              NestedTestObject data = new NestedTestObject("상위 객체", new TestObject("중첩 객체", 25));

              // When
              JsonResponse.sendSuccess(response, data);

              // Then
              verify(response).setStatus(200);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              assertTrue(jsonOutput.contains("\"title\":\"상위 객체\""));
              assertTrue(jsonOutput.contains("\"name\":\"중첩 객체\""));
              assertTrue(jsonOutput.contains("\"age\":25"));
          }

          @Test
          @DisplayName("IOException 발생 시 예외 전파")
          void sendSuccess_IOExceptionThrown() throws IOException {
              // Given
              String data = "테스트 데이터";
              when(response.getWriter()).thenThrow(new IOException("Writer 에러"));

              // When & Then
              IOException exception =
                      assertThrows(IOException.class, () -> JsonResponse.sendSuccess(response, data));
              assertEquals("Writer 에러", exception.getMessage());
          }

          @Test
          @DisplayName("특수 문자가 포함된 데이터로 성공 응답 전송")
          void sendSuccess_SpecialCharactersData() throws IOException {
              // Given
              String data = "특수문자: \"따옴표\", \n줄바꿈, \t탭, \\백슬래시";

              // When
              JsonResponse.sendSuccess(response, data);

              // Then
              verify(response).setStatus(200);
              verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
              verify(response).setCharacterEncoding("UTF-8");

              printWriter.flush();
              String jsonOutput = stringWriter.toString();
              // JSON 이스케이프 처리 확인
              assertTrue(jsonOutput.contains("\\\"따옴표\\\""));
              assertTrue(jsonOutput.contains("\\n"));
              assertTrue(jsonOutput.contains("\\t"));
              assertTrue(jsonOutput.contains("\\\\"));
          }
      }

      // 테스트용 헬퍼 클래스들
      public static class TestObject {
          private String name;
          private int age;

          public TestObject(String name, int age) {
              this.name = name;
              this.age = age;
          }

          public String getName() {
              return name;
          }

          public int getAge() {
              return age;
          }
      }

      public static class NestedTestObject {
          private String title;
          private TestObject nested;

          public NestedTestObject(String title, TestObject nested) {
              this.title = title;
              this.nested = nested;
          }

          public String getTitle() {
              return title;
          }

          public TestObject getNested() {
              return nested;
          }
      }
}
