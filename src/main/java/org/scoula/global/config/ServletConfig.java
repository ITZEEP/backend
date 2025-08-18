package org.scoula.global.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebMvc
@ComponentScan(
          basePackages = {
              "org.scoula.domain",
              "org.scoula.domain.chat.controller",
              "org.scoula.global.common.exception",
              "org.scoula.global.common.controller",
              "org.scoula.global.auth.controller",
              "org.scoula.global.email.controller",
              "org.scoula.global.redis.controller",
              "org.scoula.global.mongodb.controller",
              "org.scoula.global.file.controller",
              "org.scoula.global.oauth2.controller",
              "org.scoula.domain.precontract.controller",
              "org.scoula.domain.home.controller",
              "org.scoula.domain.mypage.controller",
              "org.scoula.domain.contract.controller",
          })
@RequiredArgsConstructor
public class ServletConfig implements WebMvcConfigurer {

      private final ObjectMapper objectMapper;

      @Bean
      public MultipartResolver multipartResolver() {
          return new StandardServletMultipartResolver();
      }

      @Override
      public void addResourceHandlers(ResourceHandlerRegistry registry) {
          registry.addResourceHandler("/resources/**").addResourceLocations("/resources/");

          registry.addResourceHandler("/swagger-ui.html")
                  .addResourceLocations("classpath:/META-INF/resources/");

          registry.addResourceHandler("/swagger-ui/**")
                  .addResourceLocations("classpath:/META-INF/resources/swagger-ui/");

          registry.addResourceHandler("/webjars/**")
                  .addResourceLocations("classpath:/META-INF/resources/webjars/");

          registry.addResourceHandler("/swagger-resources/**")
                  .addResourceLocations("classpath:/META-INF/resources/swagger-resources/");

          registry.addResourceHandler("/v2/api-docs/**")
                  .addResourceLocations("classpath:/META-INF/resources/");
      }

      @Override
      public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
          // ByteArrayHttpMessageConverter for PDF and other binary data
          ByteArrayHttpMessageConverter byteArrayConverter = new ByteArrayHttpMessageConverter();
          byteArrayConverter.setSupportedMediaTypes(
                  List.of(
                          MediaType.APPLICATION_PDF,
                          MediaType.APPLICATION_OCTET_STREAM,
                          MediaType.IMAGE_PNG,
                          MediaType.IMAGE_JPEG,
                          MediaType.ALL));
          converters.add(byteArrayConverter);

          // JSON converter
          MappingJackson2HttpMessageConverter jsonConverter =
                  new MappingJackson2HttpMessageConverter();
          jsonConverter.setObjectMapper(objectMapper);
          converters.add(jsonConverter);
      }

      @Override
      public void addFormatters(FormatterRegistry registry) {
          // Add LocalDate converter
          registry.addConverter(new Converter<String, LocalDate>() {
              @Override
              public LocalDate convert(String source) {
                  if (source == null || source.isEmpty()) {
                      return null;
                  }
                  return LocalDate.parse(source, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
              }
          });
          
          // Add LocalDateTime converter
          registry.addConverter(new Converter<String, LocalDateTime>() {
              @Override
              public LocalDateTime convert(String source) {
                  if (source == null || source.isEmpty()) {
                      return null;
                  }
                  // Try different formats
                  try {
                      return LocalDateTime.parse(source, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                  } catch (Exception e) {
                      // If that fails, try ISO format
                      return LocalDateTime.parse(source + "T00:00:00");
                  }
              }
          });
          
          // Add default date formatters
          DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
          registrar.setDateFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
          registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
          registrar.registerFormatters(registry);
      }

      @Override
      public void addCorsMappings(CorsRegistry registry) {
          registry.addMapping("/**")
                  .allowedOrigins(
                          "http://localhost:5173",
                          "http://localhost:8080",
                          "https://itzeep.ariogi.kr",
                          "https://www.itzeep.ariogi.kr",
                          "http://itzeep.ariogi.kr",
                          "http://www.itzeep.ariogi.kr")
                  .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                  .allowedHeaders("*")
                  .allowCredentials(true)
                  .maxAge(3600);
      }

      //      @Override
      //      public void addCorsMappings(CorsRegistry registry) {
      //          registry.addMapping("/**")
      //                  .allowedOrigins(
      //                          "http://localhost:5173",
      //                          "http://localhost:8080",
      //                          "https://itzeep.ariogi.kr",
      //                          "https://www.itzeep.ariogi.kr",
      //                          "http://itzeep.ariogi.kr",
      //                          "http://www.itzeep.ariogi.kr")
      //                  .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
      //                  .allowedHeaders("*")
      //                  .allowCredentials(true)
      //                  .maxAge(3600);
      //      }
}
