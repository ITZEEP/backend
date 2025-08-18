package org.scoula.global.config;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class WebConfig extends AbstractAnnotationConfigDispatcherServletInitializer {

      final String LOCATION = System.getProperty("java.io.tmpdir");
      final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB
      final long MAX_REQUEST_SIZE = 20L * 1024 * 1024; // 20MB
      final int FILE_SIZE_THRESHOLD = 1 * 1024 * 1024; // 1MB - Reduced for better memory management

      @Override
      public void onStartup(ServletContext servletContext) throws ServletException {
          System.out.println("========================================");
          System.out.println("WebConfig.onStartup() called!");
          System.out.println("Starting Spring Web Application...");
          System.out.println("========================================");

          // Set Tomcat file upload limits to prevent FileCountLimitExceededException
          servletContext.setInitParameter("org.apache.tomcat.websocket.textBufferSize", "20971520");
          servletContext.setInitParameter("org.apache.tomcat.websocket.binaryBufferSize", "20971520");

          super.onStartup(servletContext);
      }

      protected Filter[] getServletFilters() {
          CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();

          characterEncodingFilter.setEncoding("UTF-8");
          characterEncodingFilter.setForceEncoding(true);

          return new Filter[] {characterEncodingFilter};
      }

      @Override
      protected Class<?>[] getRootConfigClasses() {
          return new Class<?>[] {RootConfig.class};
      }

      @Override
      protected Class<?>[] getServletConfigClasses() {
          return new Class<?>[] {ServletConfig.class, SwaggerConfig.class};
      }

      // 스프링의 Front Controller 역할을 하는 DispatcherServlet의 URL 매핑을 지정
      // "/"는 모든 요청을 처리한다는 의미
      @Override
      protected String[] getServletMappings() {

          return new String[] {
              "/", "/swagger-ui.html", "/swagger-resources/**", "/v2/api-docs/**", "/webjars/**"
          };
      }

      @Override
      protected void customizeRegistration(javax.servlet.ServletRegistration.Dynamic registration) {
          // 404 에러 발생 시 예외를 던지도록 설정
          registration.setInitParameter("throwExceptionIfNoHandlerFound", "true");

          // Tomcat file upload limits to prevent FileCountLimitExceededException
          registration.setInitParameter(
                  "org.apache.tomcat.util.http.fileupload.FileUpload.MAX_FILE_COUNT", "100");
          registration.setInitParameter("maxParameterCount", "10000");
          registration.setInitParameter("maxPostSize", "20971520");

          // 파일 업로드를 위한 MultipartConfig 설정
          registration.setMultipartConfig(
                  new javax.servlet.MultipartConfigElement(
                          LOCATION, MAX_FILE_SIZE, MAX_REQUEST_SIZE, FILE_SIZE_THRESHOLD));
      }
}
