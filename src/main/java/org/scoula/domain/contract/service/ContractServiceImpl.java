package org.scoula.domain.contract.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
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
import org.scoula.domain.precontract.service.IdentityVerificationService;
import org.scoula.domain.precontract.service.IdentityVerificationServiceImpl;
import org.scoula.domain.precontract.vo.IdentityVerificationInfoVO;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.email.service.EmailServiceImpl;
import org.scoula.global.file.service.S3ServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Log4j2
public class ContractServiceImpl implements ContractService {

    private final @Lazy ContractChatServiceInterface contractChatService;

      private final ContractMongoRepository repository;
      private final IdentityVerificationService identityVerificationService;
    private final ContractChatMapper contractChatMapper;
    private final ContractMapper contractMapper;
    private final TenantPreContractMapper tenantMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

      private final RedisTemplate<String, String> stringRedisTemplate;
      private final S3ServiceImpl s3Service;
      private final EmailServiceImpl emailService;



      /** {@inheritDoc} */
      @Override
      public Void saveContractMongo(Long contractChatId, Long userId) {
          // userId 검증
          validateIsOwner(contractChatId, userId);

          // 이미 생성된 계약 문서가 있으면 저장 대신 안내 메시지 전송 후 종료
          ContractMongoDocument existing = repository.getContract(contractChatId);
          if (existing != null) {
              contractChatService.AiMessage(contractChatId, " 이미 생성된 계약서가 있어요.\n" + "기존 계약서를 불러올게요.");
              return null;
          }

          // 계약서에 들어갈 내용들을 mapper로 가져오기
          ContractDTO dto = contractMapper.getContract(contractChatId);

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
      // 계약서 조회하기
      @Override
      public ContractDTO getContract(Long contractChatId, Long userId) {

          // userId 검증
          validateUserId(contractChatId, userId);

          // id로 Repository에서 값을 찾는다
          ContractMongoDocument document = repository.getContract(contractChatId);
          if (document == null) {
              throw new BusinessException(ContractException.CONTRACT_GET);
          }

          Long ownerContractId = contractMapper.getOwnerId(contractChatId);
          Long buyerContractId = contractMapper.getBuyerId(contractChatId);

          IdentityVerificationInfoVO ownerVO = identityVerificationService.getDecryptedVerificationInfo(contractChatId, ownerContractId);
          IdentityVerificationInfoVO buyerVO = identityVerificationService.getDecryptedVerificationInfo(contractChatId, buyerContractId);

          // 찾은 값을 Dto에 넣고 반환하기
          ContractDTO dto = ContractDTO.toDTO(document, ownerVO, buyerVO);

          return dto;
      }

    @Override
    // 해당 스텝 메세지 & 다음 단계로 넘어가는지
    public Void getContractNext(Long contractChatId, Long userId) {

        // userId 검증
        validateUserId(contractChatId, userId);

        ContractMongoDocument doc = repository.getContract(contractChatId);
        AIMessageDTO aiDto = AIMessageDTO.toDTO(doc);

        // 시작 메세지 보내기
        contractChatService.AiMessage(
                contractChatId,
                """
        🎉 안녕하세요!
      이 계약은 임대인 %s님과 임차인 %s님의 계약입니다. 
      이번 단계는 정보 확인 단계에요.
      """.formatted(aiDto.getOwnerName(), aiDto.getBuyerName())
        );

      // 스텝 변경
      contractChatMapper.updateStatus(contractChatId, ContractChat.ContractStatus.STEP0);

        // 2초 대기
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

          contractChatService.AiMessageBtn(contractChatId, """
                  %s님과 %s님이 작성한 사전 조사를 토대로
                  정보를 추출한 결과, 👉오른쪽 계약서와 같아요.
                  🏠매물 정보, 조건을 확인해주세요.
                  다음 단계로 넘어갈까요?
                  """.formatted(aiDto.getOwnerName(), aiDto.getBuyerName()));

        return null;
    }

    String step3StartMessage = "다음은 3단계: ‘특약 조율' 단계입니다.\n"
        + "\n"
        + "'특약'은 계약 당사자 간의 특별한 상호 합의로서 명확한 권리, 의무 관계를 명시해야 해요. \n"
        + "\n"
        + "하지만, 특약으로 기재했다고 모든 조항이 효력을 갖는 것이 아니에요. \n"
        + "\n"
        + "주택임대차보호법의 범위를 넘어서지 않도록 AI가 도와줄게요.";

    @Override
    public Boolean nextStep(Long contractChatId, Long userId, NextStepDTO dto) {

        // userId 검증
        validateUserId(contractChatId, userId);

        Boolean nextSteps = nextSteps(contractChatId, userId, dto);

        if (nextSteps) {
            boolean deposit = contractMapper.getDepositAdjustment(contractChatId);

          // 스텝 변경
          contractChatMapper.updateStatus(contractChatId, ContractChat.ContractStatus.STEP1);

            if (deposit) {
                contractChatService.AiMessage(contractChatId, "다음은 2단계 '금액 조율' 단계입니다.");
            } else if (!deposit) {
                contractChatService.AiMessageBtn(contractChatId, """
                        다음은 2단계 '금액 조율' 단계입니다.
                                              
                        두 분 모두 금액 조율 의사가 없으므로,
                        다음 단계로 자동으로 넘어갑니다.
                        """);

                // 2초 대기
                try {
                    Thread.sleep(2000);

                  // 다음 단계 메세지 보내기
                  contractChatService.AiMessage(contractChatId, step3StartMessage);

                  // 스텝 변경
                  contractChatMapper.updateStatus(contractChatId, ContractChat.ContractStatus.STEP2);
                    Thread.sleep(2000);

                  // 특약 초안 메시지
                  contractChatService.AiMessageBtn(contractChatId, "특약 초안이 생성되었습니다. 각 조항을 검토하고 수락 / 거절을 선택하세요.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

            }

        }

        return nextSteps;

    }

    /** {@inheritDoc} */
      @Override
      public PaymentDTO getDepositPrice(Long contractChatId, Long userId) {

          // userId 검증
          validateUserId(contractChatId, userId);

          ContractMongoDocument doc = repository.getContract(contractChatId);
          AIMessageDTO aiDto = AIMessageDTO.toDTO(doc);

          long contract = ChronoUnit.YEARS.between(aiDto.getContractStartDate(), aiDto.getContractEndDate());
          String rentType = tenantMapper.selectRentTypeAll(contractChatId, userId)
                  .orElseThrow(() -> new BusinessException(ContractException.CONTRACT_GET, "전/월세 타입 조회 실패"));
          String rentTypeKr;
          if(rentType.equals("JEONSE")){
              rentTypeKr = "전세";
          }else{
              rentTypeKr="월세";
          }
          // 시작 메세지 보내기
          contractChatService.AiMessage(
                  contractChatId,
                  """
              이 계약은 계약기간 %d년의 %s 계약입니다.
      보증금은 %s,
      관리비는 %s입니다.
      """.formatted(
                          contract,
                          rentTypeKr,
                          formatWonShort(aiDto.getDepositPrice()),
                          formatWonShort(aiDto.getMaintenanceFee())));

          // 대기
          try {
              Thread.sleep(1000);
          } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
          }

          contractChatService.AiMessage(
                  contractChatId, """
          자유롭게 채팅 후 임대인(%s)님께서 금액을 조정해주세요. 임차인(%s)님이 수락 후 해당 조건의 확정이 가능합니다.
          """.formatted(aiDto.getBuyerName(), aiDto.getOwnerName()));

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

        String redisKey = "contract:payment:" + contractChatId;

        String paymentValue = dto.getDepositPrice() + "," + dto.getMonthlyRent();
        stringRedisTemplate.opsForValue().set(redisKey, paymentValue);

        Long ownerId = contractMapper.getOwnerId(contractChatId);

        String userRole = userId.equals(ownerId) ? "임대인" : "임차인";

        String depositFormatted = formatWonShort(dto.getDepositPrice());
        String monthlyRentFormatted = formatWonShort(dto.getMonthlyRent());

        String message;
        if (dto.getMonthlyRent() > 0) {
            message = String.format("%s이 보증금 %s, 월세 %s로 금액 조정을 요청했습니다.",
                    userRole, depositFormatted, monthlyRentFormatted);
        } else {
            message = String.format("%s이 전세금 %s로 금액 조정을 요청했습니다.",
                    userRole, depositFormatted);
        }

        contractChatService.AiMessage(contractChatId, message);

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
              throw new BusinessException(ContractException.CONTRACT_REDIS, "금액 정보가 Redis에 없습니다.");
          }

          // Redis에서 삭제
          stringRedisTemplate.delete(redisKey);

          return null;
      }

    /** {@inheritDoc} */
    @Override
    public Void updateDepositPrice(Long contractChatId, Long userId) {
        validateUserId(contractChatId, userId);

        String redisKey = "contract:payment:" + contractChatId;
        String paymentValue = stringRedisTemplate.opsForValue().get(redisKey);

        if (paymentValue == null) {
            throw new BusinessException(ContractException.CONTRACT_REDIS, "금액 정보가 Redis에 없습니다.");
        }

        try {
            String[] amounts = paymentValue.split(",");
            if (amounts.length != 2) {
                throw new BusinessException(ContractException.CONTRACT_REDIS, "Redis 금액 데이터 형식이 올바르지 않습니다.");
            }

            int depositPrice = Integer.parseInt(amounts[0]);
            int monthlyRent = Integer.parseInt(amounts[1]);

            PaymentDTO dto = PaymentDTO.builder()
                    .depositPrice(depositPrice)
                    .monthlyRent(monthlyRent)
                    .build();

            repository.updateDepositPrice(contractChatId, dto);

            String depositFormatted = formatWonShort(depositPrice);
            String monthlyRentFormatted = formatWonShort(monthlyRent);

            String acceptMessage;
            if (monthlyRent > 0) {
                acceptMessage = String.format("금액 조정이 수락되었습니다!\n보증금: %s\n월세: %s",
                        depositFormatted, monthlyRentFormatted);
            } else {
                acceptMessage = String.format("금액 조정이 수락되었습니다!\n전세금: %s", depositFormatted);
            }

            contractChatService.AiMessage(contractChatId, acceptMessage);

            stringRedisTemplate.delete(redisKey);

            contractChatMapper.updateStatus(contractChatId, ContractChat.ContractStatus.STEP2);

            contractChatService.AiMessage(contractChatId, step3StartMessage);

            Thread.sleep(2000);

            contractChatService.AiMessageBtn(contractChatId, "특약 초안이 생성되었습니다. 각 조항을 검토하고 수락 / 거절을 선택하세요.");

        } catch (NumberFormatException e) {
            throw new BusinessException(ContractException.CONTRACT_REDIS, "Redis의 금액 데이터를 파싱할 수 없습니다.", e);
        } catch (Exception e) {
            throw new BusinessException(ContractException.CONTRACT_UPDATE, e);
        }

        return null;
    }


      /** {@inheritDoc} */
      // ai로 적법성 검사하기 -> 암호화 풀어서 보내기


      // 임대인 삭제
    @Override
    @Transactional
    public String deleteOwnerLegality(Long contractChatId, Long userId) {
        // userId 검증
        validateUserId(contractChatId, userId);

        Long ownerContractId = contractMapper.getOwnerId(contractChatId);

        String redisKey =
                "final-contract:legality:" + contractChatId + ":" + ownerContractId;
        String valueDataJson = stringRedisTemplate.opsForValue().get(redisKey);
        if (valueDataJson == null) {
            throw new IllegalArgumentException("대기중인 수정 요청이 없습니다.");
        }

        try {
            stringRedisTemplate.delete(redisKey);
//            contractChatService.AiMessage(contractChatId, "임대인");
        } catch (Exception e) {
            log.error("수정 요청 응답 처리 실패", e);
            throw new RuntimeException("응답 처리 중 오류가 발생했습니다.");
        }


        return "임대인이 적법성 검사를 삭제했습니다.";
    }

    // 임대인 수정요청
    @Override
    @Transactional
      public Void updateOwnerLegality(Long contractChatId, Long userId, UpdateLegalityDTO updateLegalityDTO) {

          // userId 검증
          validateUserId(contractChatId, userId);
        Long ownerContractId = contractMapper.getOwnerId(contractChatId);

          String redisKey = "final-contract:legality:" + contractChatId + ":" + ownerContractId;

          String existingRequest = stringRedisTemplate.opsForValue().get(redisKey);
          if (existingRequest != null) {
              throw new IllegalArgumentException("해당 조항에 대한 수정 요청이 이미 대기중입니다.");
          }

          LegalityRequestDTO requestData =
                  LegalityRequestDTO.builder()
                          .legalBasis(updateLegalityDTO.getLegalBasis())
                          .requestId(userId)
                          .createdAt(LocalDateTime.now().toString())
                          .build();
          try {
              String jsonData = objectMapper.writeValueAsString(requestData);
              // Store as valid JSON for correct parsing later
              String valueData = String.format("{\"requestData\":%s}", jsonData);
              stringRedisTemplate.opsForValue().set(redisKey, valueData);

              contractChatService.AiMessage(contractChatId, "임대인이 적법성 검사 수정을 요청합니다.");
          }  catch (Exception e) {
              log.error("수정 요청 저장 실패", e);
              throw new RuntimeException("수정 요청 저장 중 오류가 발생했습니다.");
          }

          return null;
      }

    // 임차인 수정
   @Override
    @Transactional
    public Void updateBuyerLegality(Long contractChatId, Long userId, SpecialContractUpdateDTO dto) {

        // userId 검증
        validateUserId(contractChatId, userId);

        Long ownerContractId = contractMapper.getOwnerId(contractChatId);

        String redisKey =
                "final-contract:legality:" + contractChatId + ":" + ownerContractId;
        String valueDataJson = stringRedisTemplate.opsForValue().get(redisKey);
        if (valueDataJson == null) {
            throw new IllegalArgumentException("대기중인 수정 요청이 없습니다.");
        }

        try{
            // JSON에서 clauseOrder와 requestData 추출
            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(valueDataJson);
            String requestDataJson = rootNode.get("requestData").toString();

            LegalityRequestDTO requestData =
                    objectMapper.readValue(requestDataJson, LegalityRequestDTO.class);

            // 수정하는 로직 짜기
            updateSpecialContract(contractChatId, userId, dto);
            String resultMessage;
            resultMessage =
                    String.format("적법성 수정 후 레디스에서 삭제되었습니다.");

            stringRedisTemplate.delete(redisKey);
            contractChatService.AiMessage(contractChatId, resultMessage);

        }catch (Exception e) {
            log.error("수정 요청 응답 처리 실패", e);
            throw new RuntimeException("응답 처리 중 오류가 발생했습니다.");
        }


        return null;
    }


    // 임차인 거절
    @Override
    @Transactional
    public String rejectBuyerLegality(Long contractChatId, Long userId) {

        // userId 검증
        validateUserId(contractChatId, userId);

        Long ownerContractId = contractMapper.getOwnerId(contractChatId);

        String redisKey =
                "final-contract:legality:" + contractChatId + ":" + ownerContractId;
        String valueDataJson = stringRedisTemplate.opsForValue().get(redisKey);
        if (valueDataJson == null) {
            throw new IllegalArgumentException("대기중인 수정 요청이 없습니다.");
        }

        try {
            stringRedisTemplate.delete(redisKey);
//            contractChatService.AiMessage(contractChatId, "임대인이 적법성 수정을 거절했습니다.");
        } catch (Exception e) {
            log.error("수정 요청 응답 처리 실패", e);
            throw new RuntimeException("응답 처리 중 오류가 발생했습니다.");
        }

        return "임대인이 적법성 수정을 거절했습니다.";
    }

      /** {@inheritDoc} */
      // 수정 확정
      @Override
      public Void updateSpecialContract(Long contractChatId, Long userId, SpecialContractUpdateDTO dto) {
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

          // userId 검증
          validateUserId(contractChatId, userId);

          contractChatService.AiMessage(contractChatId, "계약서 작성이 완료되었습니다.");

          return null;
      }

      // ---------------------------------------
      // Userid 검증
      public void validateUserId(Long contractChatId, Long userId) {

          if (userId == null) {
              throw new BusinessException(PreContractErrorCode.TENANT_USER);
          }

          Long ownerContractId = contractMapper.getOwnerId(contractChatId);
          Long buyerContractId = contractMapper.getBuyerId(contractChatId);

          if (userId.equals(ownerContractId)) {
              validateIsOwner(contractChatId, userId);
              return;
          }

          if (userId.equals(buyerContractId)) {
              Long buyerId = tenantMapper
                      .selectContractBuyerId(contractChatId)
                      .orElseThrow(() -> new BusinessException(PreContractErrorCode.TENANT_USER));

              if (!userId.equals(buyerId)) {
                  throw new BusinessException(PreContractErrorCode.TENANT_USER);
              }
              return;
          }

          throw new BusinessException(PreContractErrorCode.TENANT_USER);
      }

      public void validateIsOwner(Long contractChatId, Long userId) {
          Long ownerId=
                  tenantMapper.selectContractOwnerId(contractChatId).orElseThrow(() -> new BusinessException(PreContractErrorCode.TENANT_USER));
          if (!userId.equals(ownerId)) {
              throw new BusinessException(PreContractErrorCode.TENANT_USER);
          }
      }

    public Boolean nextSteps(Long contractChatId, Long userId, NextStepDTO dto) {
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
                // 스텝 변경
                contractChatMapper.updateStatus(contractChatId, ContractChat.ContractStatus.STEP1);
                return true;
            }

            // 4) 아직 한쪽만 true면 -> 상태 저장하고 false 반환
            String updatedJson = objectMapper.writeValueAsString(state);
            stringRedisTemplate.opsForValue().set(redisKey, updatedJson);
            return false;
        } catch (Exception e) {
            throw new BusinessException(ContractException.CONTRACT_REDIS, e);
        }
    }

    private static String formatWonShort(int amount) {
        if (amount == 0) return "0원";

        long eok = amount / 100_000_000;           // 억
        long man = (amount % 100_000_000) / 10_000; // 만원 단위

        StringBuilder sb = new StringBuilder();

        if (eok > 0) {
            sb.append(eok).append("억");
            if (man > 0) {
                sb.append(" ").append(man).append("만");
            }
            sb.append("원");
        } else {
            if (man > 0) {
                sb.append(man).append("만원");
            } else {
                // 만원 미만인 경우
                long cheon = (amount % 10_000) / 1_000;
                if (cheon > 0) {
                    sb.append(cheon).append("천원");
                } else {
                    sb.append(amount).append("원");
                }
            }
        }

        return sb.toString();
    }
}
