package org.scoula.domain.contract.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletResponse;

import org.scoula.domain.chat.mapper.ContractChatMapper;
import org.scoula.domain.chat.service.ContractChatServiceInterface;
import org.scoula.domain.chat.vo.ContractChat;
import org.scoula.domain.contract.document.ContractMongoDocument;
import org.scoula.domain.contract.dto.*;
import org.scoula.domain.contract.exception.ContractException;
import org.scoula.domain.contract.mapper.ContractMapper;
import org.scoula.domain.contract.repository.ContractMongoRepository;
import org.scoula.domain.precontract.enums.ContractDuration;
import org.scoula.domain.precontract.exception.PreContractErrorCode;
import org.scoula.domain.precontract.mapper.TenantPreContractMapper;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.util.UploadFiles;
import org.scoula.global.email.service.EmailServiceImpl;
import org.scoula.global.file.service.S3ServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class ContractServiceImpl implements ContractService {

      private final ContractChatServiceInterface contractChatService;

      private final ContractMapper contractMapper;
      private final ContractMongoRepository repository;
      private final RestTemplate restTemplate;
      private final TenantPreContractMapper tenantMapper;
      private final ContractChatMapper contractChatMapper;

      private final RedisTemplate<String, String> stringRedisTemplate;
      private final S3ServiceImpl s3Service;
      private final EmailServiceImpl emailService;

      private static final String ALGORITHM = "AES";
      private static final String TRANSFORMATION = "AES";
      private static final String SECRET_KEY = "mySuperSecretKey"; // 16글자 (128bit) ==> 환경변수에 넣기

      @Value("${ai.server.url:http://localhost:8000}")
      private String aiServerUrl;

      /** {@inheritDoc} */
      @Override
      public Void standByContract(Long contractChatId, Long userId) {

          // 시작 메세지 보내기
          contractChatService.AiMessage(contractChatId, """
          안녕하세요!
          임대인이 입장하면 바로 계약서 작성을 시작할게요.
          """);

          // 2초
          // 잠깐의 텀 (2초)
          try {
              Thread.sleep(2000);
          } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
              log.warn("standByContract sleep interrupted", ie);
          }


          contractChatService.AiMessageBtn(contractChatId, """
          기다리는 동안
          어려운 법률 용어와 법률 팁을 알아볼까요?
          """);

          // contract에 매퍼로 스텝 추가하기
          contractChatMapper.updateStatus(contractChatId, ContractChat.ContractStatus.STEP0);

          return null;
      }

      /** {@inheritDoc} */
      @Override
      public Void saveContractMongo(Long contractChatId, Long userId) {
          // userId 검증
          validateUserId(contractChatId, userId);

          // 이미 생성된 계약 문서가 있으면 저장 대신 안내 메시지 전송 후 종료
          ContractMongoDocument existing = repository.getContract(contractChatId);
          if (existing != null) {
              contractChatService.AiMessage(contractChatId, """
            이미 생성된 계약서가 있어요.
            기존 계약서를 불러올게요.
            """);
              return null;
          }

          // 계약서에 들어갈 내용들을 mapper로 가져오기
          ContractDTO dto = contractMapper.getContract(contractChatId);

//          // 특약이 null이면 빈 리스트로 세팅
//          if (dto.getSpecialContracts() == null) {
//              dto.setSpecialContracts(Collections.emptyList());
//          }
//
//          // 전화번호가 null이면 빈 문자열로 세팅
//          if (dto.getOwnerPhoneNum() == null) {
//              dto.setOwnerPhoneNum("");
//          }
//          if (dto.getBuyerPhoneNum() == null) {
//              dto.setBuyerPhoneNum("");
//          }

          // 계약 끝나는 기간
          String durationStr = contractMapper.getDuration(contractChatId);
          ContractDuration duration = ContractDuration.valueOf(durationStr);

          LocalDate startDate = dto.getContractStartDate();

          LocalDate contractEndDate = null;
          if (duration == ContractDuration.YEAR_1) {
              contractEndDate = startDate.plusYears(1);
          } else if (duration == ContractDuration.YEAR_2) {
              contractEndDate = startDate.plusYears(2);
          } else if (duration == ContractDuration.YEAR_3) {
              contractEndDate = startDate.plusYears(3);
          } else if (duration == ContractDuration.YEAR_4) {
              contractEndDate = startDate.plusYears(4);
          } else if (duration == ContractDuration.YEAR_5) {
              contractEndDate = startDate.plusYears(5);
          }

          // mongoDB에 contract 도큐멘트를 만들어서 저장한다.
          ContractMongoDocument document = repository.saveContractMongo(dto, contractEndDate);
          if (document == null) {
              throw new BusinessException(ContractException.CONTRACT_INSERT);
          }
          return null;
      }

      /** {@inheritDoc} */
      @Override
      public ContractDTO getContract(Long contractChatId, Long userId) {

          // 스텝 변경
          contractChatMapper.updateStatus(contractChatId, ContractChat.ContractStatus.STEP1);

          // 다음 단계 메세지 보내기
          contractChatService.AiMessage(contractChatId, "이번 단계는 '정보 확인' 단계입니다");

          ContractMongoDocument doc = repository.getContract(contractChatId);
          AIMessageDTO aiDto = AIMessageDTO.toDTO(doc);

          // 시작 메세지 보내기
          contractChatService.AiMessage(
                  contractChatId,
                  """
        👋🏻 안녕하세요!
        이 계약은 임대인 %s님과 임차인 %s님의 계약입니다. 시작하기 전, 정보를 먼저 확인할게요.

        제출된 정보를 토대로 계약서를 추출할게요.
        """.formatted(aiDto.getOwnerName(), aiDto.getBuyerName())
          );

          // userId 검증
          validateUserId(contractChatId, userId);

          // id로 Repository에서 값을 찾는다
          ContractMongoDocument document = repository.getContract(contractChatId);
          if (document == null) {
              throw new BusinessException(ContractException.CONTRACT_GET);
          }

          // 찾은 값을 Dto에 넣고 반환하기
          ContractDTO dto = ContractDTO.toDTO(document);

          boolean deposit = contractMapper.getDepositAdjustment(contractChatId);

          if (!deposit) {
              contractChatService.AiMessageBtn(contractChatId, """
                      다음은 2단계 '금액 조율' 단계입니다.
                      
                      두 분 모두 금액 조율 의사가 없으므로,
                      다음 단계로 자동으로 넘어갑니다.
                      """);
          }

          return dto;
      }

    @Override
    public Void getContractNext(Long contractChatId, Long userId) {
        // userId 검증
        validateUserId(contractChatId, userId);

        ContractMongoDocument doc = repository.getContract(contractChatId);
        AIMessageDTO aiDto = AIMessageDTO.toDTO(doc);

          contractChatService.AiMessageBtn(contractChatId, """
                  %s님과 %s님이 작성한 사전 조사를 토대로
                  정보를 추출한 결과가 다음과 같습니다.
                  매물 정보, 조건을 확인하셨나요?
                  다음 단계로 넘어갈까요?
                  """.formatted(aiDto.getBuyerName(), aiDto.getOwnerName()));
        return null;
    }

    @Override
    public Boolean nextStep(Long contractChatId, Long userId, NextStepDTO dto) {

        ContractChat.ContractStatus step = contractChatMapper.getStatus(contractChatId);
        // Redis Key: 계약별 step 상태를 저장
        String redisKey = String.format("contract:%s:%d", step.name(), contractChatId);

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // 1) 기존 상태 로드 (없으면 기본값 생성)
            String currentJson = stringRedisTemplate.opsForValue().get(redisKey);
            NextStepDTO state = (currentJson != null)
                    ? objectMapper.readValue(currentJson, NextStepDTO.class)
                    : new NextStepDTO();

            // 2) 이번 요청 값 반영 (이제 step은 DTO에서 받지 않음, DB 상태는 필요 시 별도 조회)
            if (dto.isOwner()) {
                state.setOwner(true);
            }
            if (dto.isBuyer()) {
                state.setBuyer(true);
            }

            // 3) 두 사람이 모두 true면 -> 키 삭제하고 true 반환
            if (state.isOwner() && state.isBuyer()) {
                stringRedisTemplate.delete(redisKey);
                return true;
            }

            // 4) 아직 한쪽만 true면 -> 상태 저장하고 false 반환
            String updatedJson = objectMapper.writeValueAsString(state);
            stringRedisTemplate.opsForValue().set(redisKey, updatedJson);
            return false;
        } catch (Exception e) {
            throw new BusinessException(ContractException.CONTRACt_REDIS, e);
        }
    }

    /** {@inheritDoc} */
      @Override
      public PaymentDTO getDepositPrice(Long contractChatId, Long userId) {

          // 스텝 변경
          contractChatMapper.updateStatus(contractChatId, ContractChat.ContractStatus.STEP2);

          // 다음 단계 메세지 보내기
          contractChatService.AiMessage(contractChatId, "이번 단계는 '금액 조율' 단계입니다");

          ContractMongoDocument doc = repository.getContract(contractChatId);
          AIMessageDTO aiDto = AIMessageDTO.toDTO(doc);

          long contract = ChronoUnit.YEARS.between(aiDto.getContractStartDate(), aiDto.getContractEndDate());
          String rentType = tenantMapper.selectRentType(contractChatId, userId)
                  .orElseThrow(() -> new BusinessException(ContractException.CONTRACT_GET, "전/월세 타입 조회 실패"));
          // 시작 메세지 보내기
          contractChatService.AiMessage(
                  contractChatId,
                  """
        다음은 2단계: ‘금액 조율’ 단계입니다.

              이 계약은 계약기간 %d년의 %s 계약입니다.
      전세 보증금은 %s,
      관리비는 %s입니다.
      """.formatted(
                          contract,
                          rentType,
                          formatWonShort(aiDto.getDepositPrice()),
                          formatWonShort(aiDto.getMaintenanceFee())));

          contractChatService.AiMessage(
                  contractChatId, """
          자유롭게 채팅 후 임대인(%s)님께서 금액을 조정해주세요. 임차인(%s)님이 수락 후 해당 조건의 확정이 가능합니다.
          """.formatted(aiDto.getBuyerName(), aiDto.getOwnerName()));

          // userId 검증
          validateUserId(contractChatId, userId);

          // MongoDB에서 보증금, 계약금, 잔금, 월세를 조회한다
          ContractMongoDocument document = repository.getDepositPrice(contractChatId);
          if (document == null) {
              throw new BusinessException(ContractException.CONTRACT_GET);
          }

          // 조회된 금액을 리턴한다.
          PaymentDTO dto = PaymentDTO.toDTO(document);
          return dto;
      }

      /** {@inheritDoc} */
      @Override
      public Void saveDepositPrice(Long contractChatId, Long userId, PaymentDTO dto) {
          // Userid 검증
          validateUserId(contractChatId, userId);

          // 레디스에 내용 저장하기 / value 값 넛기
          String redisKey = "contract:payment:" + contractChatId;
          try {
              // 3. DTO를 JSON 문자열로 변환
              ObjectMapper objectMapper = new ObjectMapper();
              String json = objectMapper.writeValueAsString(dto);

              // 4. Redis에 저장
              stringRedisTemplate.opsForValue().set(redisKey, json);

          } catch (JsonProcessingException e) {
              throw new BusinessException(ContractException.CONTRACt_REDIS, e);
          }

          return null;
      }

      /** {@inheritDoc} */
      @Override
      public Void deleteDepositPrice(Long contractChatId, Long userId) {
          // userId 검증
          validateUserId(contractChatId, userId);

          // 레디스에 내용 삭제하기
          // Redis key 정의
          String redisKey = "contract:payment:" + contractChatId;
          String json = stringRedisTemplate.opsForValue().get(redisKey);

          if (json == null) {
              throw new BusinessException(ContractException.CONTRACt_REDIS, "금액 정보가 Redis에 없습니다.");
          }

          // Redis에서 삭제
          stringRedisTemplate.delete(redisKey);

          return null;
      }

      /** {@inheritDoc} */
      @Override
      public Void updateDepositPrice(Long contractChatId, Long userId) {
          // Userid 검증
          validateUserId(contractChatId, userId);

          // 2. Redis에서 해당 금액 정보 가져오기
          String redisKey = "contract:payment:" + contractChatId; // value : 임대인 id -> 거절시 Delete
          String json = stringRedisTemplate.opsForValue().get(redisKey);

          if (json == null) {
              throw new BusinessException(ContractException.CONTRACt_REDIS, "금액 정보가 Redis에 없습니다.");
          }

          try {
              // 3. JSON -> DTO로 변환
              ObjectMapper objectMapper = new ObjectMapper();
              PaymentDTO dto = objectMapper.readValue(json, PaymentDTO.class);

              // 4. MongoDB에서 계약서 불러오기
              repository.updateDepositPrice(contractChatId, dto);

              // 7. Redis 값 삭제
              stringRedisTemplate.delete(redisKey);

          } catch (Exception e) {
              throw new BusinessException(ContractException.CONTRACT_UPDATE, e);
          }

          return null;
      }

      /** {@inheritDoc} */
      @Override
      public LegalityDTO getLegality(Long contractChatId, Long userId) {
          // userId 검증
          validateUserId(contractChatId, userId);

          // MongoDB에서 전체 부분을 조회한다
          ContractMongoDocument document = repository.getContract(contractChatId);
          if (document == null) {
              throw new BusinessException(ContractException.CONTRACT_GET);
          }
          ContractDTO dto = ContractDTO.toDTO(document);

          // AI
          try {
              // AI로 해당 데이터를 넘긴다 (restTemplate 사용)
              String url = aiServerUrl + "/api/contract/validate";
              HttpHeaders headers = new HttpHeaders();
              headers.setContentType(MediaType.APPLICATION_JSON);

              HttpEntity<ContractDTO> requestEntity = new HttpEntity<>(dto, headers);

              // 반환값을 받아오고, 그 값을 프론트에 넘겨준다.
              ResponseEntity<LegalityDTO> response =
                      restTemplate.exchange(url, HttpMethod.POST, requestEntity, LegalityDTO.class);
              LegalityDTO res = response.getBody();
              assert res != null;
              log.warn("AI 응답 값 확인: {}", res.toString());

              log.warn("AI 응답 헤더 확인: {}", response.getStatusCode());
              if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                  return response.getBody();
              } else {
                  // Sanitize response body before logging to prevent log injection
                  String responseBodyStr;
                  try {
                      ObjectMapper objectMapper = new ObjectMapper();
                      responseBodyStr = objectMapper.writeValueAsString(response.getBody());
                  } catch (Exception ex) {
                      responseBodyStr = String.valueOf(response.getBody());
                  }
                  // Remove newlines and carriage returns
                  responseBodyStr = responseBodyStr.replaceAll("[\\r\\n]", " ");
                  log.error(responseBodyStr);
                  throw new BusinessException(ContractException.CONTRACT_AI_SERVER_ERROR);
              }

          } catch (Exception e) {
              log.error(e.getMessage());
              throw new BusinessException(ContractException.CONTRACT_AI_SERVER_ERROR, e);
          }
      }

      /** {@inheritDoc} */
      @Override
      public Void saveSpecialContract(Long contractChatId, Long userId) {
          // userId 검증
          validateUserId(contractChatId, userId);

          // 몽고 DB에서 특약부분을 받아서 저장한다.
          try {
              repository.saveSpecialContract(contractChatId);
          } catch (Exception e) {
              // 예외 로그 기록 및 사용자에게 전달할 메시지 등 처리
              log.error("특약사항 저장 실패 ❌", e);
              throw new BusinessException(ContractException.CONTRACT_INSERT, e);
          }

          return null;
      }

      /** {@inheritDoc} */
      @Override
      public Void updateSpecialContract(Long contractChatId, Long userId, SpecialContractDTO dto) {
          // userId 검증
          validateUserId(contractChatId, userId);

          // 해당 번호에 맞는 특약을 계약서 몽고 DB에 update해서 수정한다.
          try {
              repository.updateSpecialContract(contractChatId, dto);
          } catch (Exception e) {
              throw new BusinessException(ContractException.CONTRACT_UPDATE);
          }
          return null;
      }

      /** {@inheritDoc} */
      @Override
      public Void sendStep4(Long contractChatId, Long userId) {
          return null;
      }

      // Userid 검증
      public void validateUserId(Long contractChatId, Long userId) {
          Long buyerId =
                  tenantMapper
                          .selectContractBuyerId(contractChatId)
                          .orElseThrow(() -> new BusinessException(PreContractErrorCode.TENANT_USER));

          if (!userId.equals(buyerId)) {
              throw new BusinessException(PreContractErrorCode.TENANT_USER);
          }
      }

    private static String formatWonShort(int amount) {
        if (amount == 0) return "0원";
        long eok = amount / 100_000_000;           // 억
        long man = (amount % 100_000_000) / 10_000; // 만원 단위

        StringBuilder sb = new StringBuilder();
        if (eok > 0) {
            sb.append(eok).append("억");
            long cheon = man / 1000; // 천만원 단위
            long remainMan = man % 1000;
            if (cheon > 0) sb.append(" ").append(cheon).append("천");
            if (cheon == 0 && remainMan > 0) sb.append(" ").append(remainMan).append("만");
            sb.append("원");
        } else {
            if (man >= 1000) {
                long cheon = man / 1000;
                long remainMan = man % 1000;
                sb.append(cheon).append("천");
                if (remainMan > 0) sb.append(" ").append(remainMan).append("만");
                sb.append("원");
            } else {
                sb.append(man).append("만원");
            }
        }
        return sb.toString().replaceAll("\\s+", " ");
    }
}
