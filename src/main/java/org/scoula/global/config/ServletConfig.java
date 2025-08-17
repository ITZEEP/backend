package org.scoula.global.config;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Configuration
@EnableWebMvc
@EnableAsync
@Log4j2
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
          MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
          converter.setObjectMapper(objectMapper);
          converters.add(converter);
      }

      @Override
      public void addFormatters(FormatterRegistry registry) {
          // LocalDate converter for form data
          registry.addConverter(
                  new Converter<String, LocalDate>() {
                      @Override
                      public LocalDate convert(String source) {
                          if (source == null || source.trim().isEmpty()) {
                              return null;
                          }
                          return LocalDate.parse(source, DateTimeFormatter.ISO_LOCAL_DATE);
                      }
                  });
      }
}
