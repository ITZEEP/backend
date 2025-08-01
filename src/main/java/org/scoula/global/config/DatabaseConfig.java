package org.scoula.global.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.RequiredArgsConstructor;

/** 데이터베이스 설정 클래스 */
@Configuration
@EnableTransactionManagement
@MapperScan(
          basePackages = {
              "org.scoula.domain.user.mapper",
              "org.scoula.domain.fraud.mapper",
              "org.scoula.domain.precontract.mapper",
              "org.scoula.domain.chat.mapper"
          })
@RequiredArgsConstructor
public class DatabaseConfig {

      @Value("${spring.datasource.driver-class-name}")
      private String driver;

      @Value("${spring.datasource.url}")
      private String url;

      @Value("${spring.datasource.username}")
      private String username;

      @Value("${spring.datasource.password}")
      private String password;

      private final ApplicationContext applicationContext;

      @Bean
      public DataSource dataSource() {
          HikariConfig config = new HikariConfig();
          config.setDriverClassName(driver);
          config.setJdbcUrl(url);
          config.setUsername(username);
          config.setPassword(password);

          return new HikariDataSource(config);
      }

      @Bean
      public SqlSessionFactory sqlSessionFactory() throws Exception {
          SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
          sqlSessionFactoryBean.setConfigLocation(
                  applicationContext.getResource("classpath:mybatis-config.xml"));
          sqlSessionFactoryBean.setDataSource(dataSource());
          return sqlSessionFactoryBean.getObject();
      }

      @Bean
      public DataSourceTransactionManager transactionManager() {
          return new DataSourceTransactionManager(dataSource());
      }

      @Bean
      public JdbcTemplate jdbcTemplate(DataSource dataSource) {
          return new JdbcTemplate(dataSource);
      }
}
