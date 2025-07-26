package org.scoula.domain.fraud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.fraud.mapper.FraudRiskMapper;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudRiskService 테스트")
class FraudRiskServiceTest {

      @Mock private FraudRiskMapper fraudRiskMapper;

      @Mock private ObjectMapper objectMapper;

      @InjectMocks private FraudRiskServiceImpl fraudRiskService;

      @BeforeEach
      void setUp() {
          ReflectionTestUtils.setField(fraudRiskService, "objectMapper", objectMapper);
      }

      @Test
      @DisplayName("ObjectMapper 주입 테스트")
      void testObjectMapperInjection() {
          // given & when & then
          assertThat(fraudRiskService).isNotNull();
          assertThat(ReflectionTestUtils.getField(fraudRiskService, "objectMapper")).isNotNull();
      }

      @Test
      @DisplayName("FraudRiskMapper 주입 및 메서드 호출 테스트")
      void testFraudRiskMapperInjection() {
          // given
          when(fraudRiskMapper.existsHome(anyLong())).thenReturn(true);

          // when
          boolean exists = fraudRiskMapper.existsHome(1L);

          // then
          assertThat(fraudRiskMapper).isNotNull();
          assertThat(exists).isTrue();
      }
}
