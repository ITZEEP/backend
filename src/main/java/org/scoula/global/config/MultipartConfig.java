package org.scoula.global.config;

import javax.servlet.MultipartConfigElement;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

@Configuration
public class MultipartConfig {

      private static final String LOCATION = System.getProperty("java.io.tmpdir");
      private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB
      private static final long MAX_REQUEST_SIZE = 20L * 1024 * 1024; // 20MB
      private static final int FILE_SIZE_THRESHOLD = 1024 * 1024; // 1MB

      @Bean
      public MultipartResolver multipartResolver() {
          StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
          // This resolver doesn't have a setMaxUploadSize method
          // Size limits are controlled by MultipartConfigElement
          return resolver;
      }

      @Bean
      public MultipartConfigElement multipartConfigElement() {
          // maxFileCount is not directly configurable here,
          // but we can set it via system properties
          System.setProperty(
                  "org.apache.tomcat.util.http.fileupload.FileUpload.MAX_FILE_COUNT", "100");

          return new MultipartConfigElement(
                  LOCATION, MAX_FILE_SIZE, MAX_REQUEST_SIZE, FILE_SIZE_THRESHOLD);
      }
}
