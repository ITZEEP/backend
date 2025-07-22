package org.scoula.global.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@EnableWebMvc
@ComponentScan(
          basePackages = {
              "org.scoula.domain",
              "org.scoula.global.common.exception",
              "org.scoula.global.common.controller",
              "org.scoula.global.auth.controller",
              "org.scoula.global.email.controller",
              "org.scoula.global.redis.controller",
              "org.scoula.global.mongodb.controller",
              "org.scoula.global.websocket.controller",
              "org.scoula.global.file.controller",
              "org.scoula.global.oauth2.controller",
          })
public class ServletConfig implements WebMvcConfigurer {

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
          ObjectMapper objectMapper = new ObjectMapper();
          objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
          objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
          objectMapper.registerModule(new JavaTimeModule());
          converter.setObjectMapper(objectMapper);
          converters.add(converter);
      }

      @Override
      public void addCorsMappings(CorsRegistry registry) {
          registry.addMapping("/**")
                  .allowedOriginPatterns("*")
                  .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                  .allowedHeaders("*")
                  .allowCredentials(true)
                  .maxAge(3600);
      }
}
