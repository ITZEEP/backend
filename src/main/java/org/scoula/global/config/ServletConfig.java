package org.scoula.global.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
              "org.scoula.global.websocket.controller",
              "org.scoula.global.file.controller",
              "org.scoula.global.oauth2.controller",
              "org.scoula.domain.precontract.controller",
          })
public class ServletConfig implements WebMvcConfigurer {

      @Autowired private ObjectMapper objectMapper;

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
      public void addCorsMappings(CorsRegistry registry) {
          registry.addMapping("/**")
                  .allowedOriginPatterns("*")
                  .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                  .allowedHeaders("*")
                  .allowCredentials(true)
                  .maxAge(3600);
      }
}
