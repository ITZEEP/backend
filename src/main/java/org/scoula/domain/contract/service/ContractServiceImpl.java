package org.scoula.domain.contract.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
import org.scoula.domain.chat.mapper.ContractChatMapper;
import org.scoula.domain.chat.service.ContractChatServiceInterface;
import org.scoula.domain.chat.vo.ContractChat;
import org.scoula.domain.fraud.mapper.FraudRiskMapper;
import org.scoula.domain.fraud.vo.BuildingDocumentVO;
import org.scoula.domain.fraud.vo.RiskCheckVO;
import org.scoula.domain.contract.document.ContractMongoDocument;
import org.scoula.domain.contract.dto.*;
import org.scoula.domain.contract.enums.SignedType;
import org.scoula.domain.contract.exception.ContractException;
import org.scoula.domain.contract.mapper.ContractMapper;
import org.scoula.domain.contract.repository.ContractMongoRepository;
import org.scoula.domain.contract.vo.ElectronicSignature;
import org.scoula.domain.contract.vo.FinalContract;
import org.scoula.domain.precontract.enums.ContractDuration;
import org.scoula.domain.precontract.enums.RentType;
import org.scoula.domain.precontract.exception.PreContractErrorCode;
import org.scoula.domain.precontract.mapper.TenantPreContractMapper;
import org.scoula.domain.precontract.service.IdentityVerificationService;
import org.scoula.domain.precontract.vo.IdentityVerificationInfoVO;
import org.scoula.global.common.dto.FileWithHashDto;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.service.EncryptionService;
import org.scoula.global.common.util.*;
import org.scoula.global.email.service.EmailServiceImpl;
import org.scoula.global.file.service.S3ServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

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
    private final RestTemplate restTemplate;
    private final FraudRiskMapper fraudRiskMapper;
    private final EncryptionService encryptionService;

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final S3ServiceImpl s3Service;
    private final EmailServiceImpl emailService;

    private final AesCryptoUtil aesCryptoUtil;
    private final ImgAesCryptoUtil imgAesCryptoUtil;
    private final NumberFormatUtil numberFormatUtil;

//    private static final String ALGORITHM = "AES";
//    private static final String TRANSFORMATION = "AES";
//    private static final String SECRET_KEY = "mySuperSecretKey"; // 16글자 (128bit) ==> 환경변수에 넣기

    @Value("${ai.server.url:http://localhost:8000}")
    private String aiServerUrl;


    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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
              Thread.sleep(2000);
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
            Thread.sleep(2000);


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
    // ========================================================================

    // 금액을 한글로 변환하는 헬퍼 메서드
    private String convertToKoreanWon(int amount) {
        // NumberFormatUtil이 있다면 그것을 사용하고, 없다면 간단한 변환
        try {
            return numberFormatUtil.toKoreanNumber(amount);
        } catch (Exception e) {
            // 기본 변환 로직
            return String.format("%,d원", amount);
        }
    }

    // 계약서 내보내기 시작 - AI 서버에서 초기 PDF 생성
    @Override
    @Transactional
    public byte[] startContractExport(Long contractChatId, Long userId) {
        log.info("Starting contract export for contractChatId: {}, userId: {}", contractChatId, userId);
        
        try {
            // userId 인증
            validateUserId(contractChatId, userId);

            // MongoDB에서 계약 정보 조회
            log.info("Fetching contract from MongoDB...");
            ContractMongoDocument document = repository.getContract(contractChatId);
            if (document == null) {
                log.error("Contract not found in MongoDB for contractChatId: {}", contractChatId);
                throw new BusinessException(ContractException.CONTRACT_NOT_FOUND);
            }
            log.info("MongoDB document found");

            // DB에서 필요한 정보 조회
            log.info("Fetching contract data from DB...");
            DBFinalContractDTO dbDTO = contractMapper.selectFinalContractPDF(contractChatId);
            log.info("DB data fetched");

            // 복호화
            log.info("Getting owner and buyer IDs...");
            Long ownerContractId = contractMapper.getOwnerId(contractChatId);
            Long buyerContractId = contractMapper.getBuyerId(contractChatId);
            log.info("OwnerID: {}, BuyerID: {}", ownerContractId, buyerContractId);

            log.info("Getting identity verification info...");
            IdentityVerificationInfoVO ownerVO = identityVerificationService.getDecryptedVerificationInfo(contractChatId, ownerContractId);
            IdentityVerificationInfoVO buyerVO = identityVerificationService.getDecryptedVerificationInfo(contractChatId, buyerContractId);
            log.info("Identity verification info retrieved");

            // ContractChat 정보 조회하여 homeId 가져오기
            log.info("Getting contract chat info...");
            ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
            BuildingDocumentVO buildingDocument = null;

            if (contractChat != null && contractChat.getHomeId() != null) {
                log.info("HomeId found: {}", contractChat.getHomeId());
                // homeId로 가장 최근의 위험도 체크 조회
                RiskCheckVO latestRiskCheck = fraudRiskMapper.selectLatestRiskCheckByHomeId(contractChat.getHomeId());

                if (latestRiskCheck != null) {
                    log.info("Risk check found, getting building document...");
                    // 위험도 체크 ID로 건축물대장 정보 조회
                    buildingDocument = fraudRiskMapper.selectBuildingDocumentByRiskCheckId(latestRiskCheck.getRiskckId());
                }
            }

            // SaveFinalContractDTO 생성
            log.info("Building SaveFinalContractDTO...");
            // homeId 정보 가져오기
            ContractChat contractChatForHome = contractChatMapper.findByContractChatId(contractChatId);
            Long homeId = contractChatForHome != null ? contractChatForHome.getHomeId() : null;
            SaveFinalContractDTO dto = buildSaveFinalContractDTO(document, dbDTO, ownerVO, buyerVO, buildingDocument, homeId);
            log.info("DTO built successfully");

            // AI 서버에 PDF 생성 요청
            log.info("Requesting PDF generation from AI server at: {}", aiServerUrl);
            // DTO가 null이 아닌지 확인
            if (dto == null) {
                log.error("SaveFinalContractDTO가 null입니다.");
                throw new BusinessException(ContractException.PDF_GENERATION_FAILED);
            }

            // 필수 필드 확인을 위한 디버깅
            log.info("DTO 필드 확인 - leaseType: {}, ownerNickname: {}, buyerNickname: {}, addr1: {}",
                    dto.getLeaseType(), dto.getOwnerNickname(), dto.getBuyerNickname(), dto.getAddr1());

            // 서명 이미지를 빈 문자열로 초기화 (서명 없는 미리보기용)
            dto.setOwnerSign1Base64("");
            dto.setOwnerSign2Base64("");
            dto.setOwnerSign3Base64("");
            dto.setBuyerSignBase64("");

            // JSON으로 전송
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/pdf");

            // DTO를 JSON 문자열로 변환하여 로그 출력
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writeValueAsString(dto);
            log.info("AI 서버로 전송할 JSON 데이터: {}", jsonPayload);

            HttpEntity<SaveFinalContractDTO> request = new HttpEntity<>(dto, headers);

            // PDF 바이트 배열로 직접 응답 받기
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    aiServerUrl + "/api/contract/generate-json",
                    HttpMethod.POST,
                    request,
                    byte[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                byte[] pdfData = response.getBody();
                
                // PDF 데이터 크기 확인
                if (pdfData.length == 0) {
                    log.error("AI 서버에서 빈 응답을 받았습니다");
                    return generateFallbackPdf(contractChatId, dto);
                }
                
                // PDF 헤더 확인 (%PDF)
                if (pdfData.length > 4) {
                    String header = new String(pdfData, 0, 4);
                    if (!header.startsWith("%PDF")) {
                        // PDF가 아닌 경우, 텍스트 응답인지 확인
                        String textResponse = new String(pdfData, 0, Math.min(1000, pdfData.length));
                        log.error("AI 서버에서 PDF가 아닌 응답을 받았습니다. 처음 1000 바이트: {}", textResponse);
                        
                        // URL 패턴인지 확인 (uploads/ 또는 http로 시작)
                        if (textResponse.contains("uploads/") || textResponse.startsWith("http")) {
                            log.info("응답이 URL 형식입니다. URL에서 PDF 다운로드 시도: {}", textResponse.trim());
                            
                            try {
                                String fullUrl = textResponse.trim();
                                if (!fullUrl.startsWith("http")) {
                                    // 상대 경로인 경우 AI 서버 URL과 조합
                                    fullUrl = aiServerUrl + "/" + fullUrl;
                                }
                                
                                ResponseEntity<byte[]> pdfResponse = restTemplate.getForEntity(fullUrl, byte[].class);
                                
                                if (pdfResponse.getStatusCode() == HttpStatus.OK && pdfResponse.getBody() != null) {
                                    byte[] downloadedPdf = pdfResponse.getBody();
                                    
                                    // 다운로드한 파일이 PDF인지 확인
                                    if (downloadedPdf.length > 4) {
                                        String pdfHeader = new String(downloadedPdf, 0, 4);
                                        if (pdfHeader.startsWith("%PDF")) {
                                            log.info("PDF 다운로드 성공, 크기: {} bytes", downloadedPdf.length);
                                            return downloadedPdf;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.error("URL에서 PDF 다운로드 실패: ", e);
                            }
                        }
                        
                        // Fallback으로 기본 PDF 생성
                        return generateFallbackPdf(contractChatId, dto);
                    }
                }
                
                log.info("PDF 생성 성공, 크기: {} bytes", pdfData.length);
                return pdfData;
            } else {
                log.error("AI 서버 응답 실패: Status={}", response.getStatusCode());
                throw new BusinessException(ContractException.PDF_GENERATION_FAILED);
            }
        } catch (BusinessException be) {
            log.error("Business exception in PDF generation: ", be);
            throw be;
        } catch (Exception e) {
            log.error("Unexpected error in PDF generation: ", e);
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("Cause: {}", e.getCause().getMessage());
            }
            
            // Fallback PDF 생성 시도
            try {
                log.info("Attempting to generate fallback PDF due to error");
                return generateFallbackPdf(contractChatId, null);
            } catch (Exception fallbackError) {
                log.error("Fallback PDF generation also failed: ", fallbackError);
                throw new BusinessException(ContractException.PDF_GENERATION_FAILED);
            }
        }
    }

    // Fallback PDF 생성 메서드
    private byte[] generateFallbackPdf(Long contractChatId, SaveFinalContractDTO dto) {
        log.info("Fallback PDF 생성 시작 - contractChatId: {}", contractChatId);
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(baos);
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDoc);
            
            // 한글 폰트 설정 (기본 폰트 사용)
            com.itextpdf.kernel.font.PdfFont font = com.itextpdf.kernel.font.PdfFontFactory.createFont(
                    "Helvetica", "Identity-H", com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            
            // 제목
            document.add(new com.itextpdf.layout.element.Paragraph("부동산 임대차 계약서")
                    .setFont(font)
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            
            document.add(new com.itextpdf.layout.element.Paragraph(""));
            
            // 계약 정보
            document.add(new com.itextpdf.layout.element.Paragraph("계약 번호: " + contractChatId)
                    .setFont(font));
            
            if (dto != null) {
                document.add(new com.itextpdf.layout.element.Paragraph("임대 유형: " + 
                        (dto.getLeaseType() ? "전세" : "월세"))
                        .setFont(font));
                
                document.add(new com.itextpdf.layout.element.Paragraph(""));
                
                // 당사자 정보
                document.add(new com.itextpdf.layout.element.Paragraph("[ 임대인 ]")
                        .setFont(font)
                        .setBold());
                document.add(new com.itextpdf.layout.element.Paragraph("성명: " + 
                        (dto.getOwnerNickname() != null ? dto.getOwnerNickname() : "임대인"))
                        .setFont(font));
                
                document.add(new com.itextpdf.layout.element.Paragraph(""));
                
                document.add(new com.itextpdf.layout.element.Paragraph("[ 임차인 ]")
                        .setFont(font)
                        .setBold());
                document.add(new com.itextpdf.layout.element.Paragraph("성명: " + 
                        (dto.getBuyerNickname() != null ? dto.getBuyerNickname() : "임차인"))
                        .setFont(font));
                
                document.add(new com.itextpdf.layout.element.Paragraph(""));
                
                // 부동산 정보
                document.add(new com.itextpdf.layout.element.Paragraph("[ 부동산 정보 ]")
                        .setFont(font)
                        .setBold());
                document.add(new com.itextpdf.layout.element.Paragraph("주소: " + 
                        dto.getAddr1() + " " + (dto.getAddr2() != null ? dto.getAddr2() : ""))
                        .setFont(font));
            } else {
                // dto가 null인 경우 기본 정보만 표시
                document.add(new com.itextpdf.layout.element.Paragraph(""));
                document.add(new com.itextpdf.layout.element.Paragraph("※ 계약 정보를 불러올 수 없습니다.")
                        .setFont(font)
                        .setItalic());
            }
            
            document.add(new com.itextpdf.layout.element.Paragraph(""));
            
            // 안내 메시지
            document.add(new com.itextpdf.layout.element.Paragraph(
                    "※ 이 문서는 임시 생성된 계약서입니다. AI 서버 연결 문제로 정식 계약서를 생성할 수 없습니다.")
                    .setFont(font)
                    .setFontSize(10)
                    .setItalic());
            
            document.add(new com.itextpdf.layout.element.Paragraph("생성 일시: " + new java.util.Date())
                    .setFont(font)
                    .setFontSize(10));
            
            document.close();
            
            byte[] pdfBytes = baos.toByteArray();
            log.info("Fallback PDF 생성 완료, 크기: {} bytes", pdfBytes.length);
            return pdfBytes;
            
        } catch (Exception e) {
            log.error("Fallback PDF 생성 실패: ", e);
            // 최후의 수단으로 빈 PDF 반환
            return new byte[0];
        }
    }
    
    // SaveFinalContractDTO 빌드 헬퍼 메서드
    private SaveFinalContractDTO buildSaveFinalContractDTO(
            ContractMongoDocument document,
            DBFinalContractDTO dbDTO,
            IdentityVerificationInfoVO ownerVO,
            IdentityVerificationInfoVO buyerVO,
            BuildingDocumentVO buildingDocument,
            Long homeId) {

        SaveFinalContractDTO dto = new SaveFinalContractDTO();

        // 임대 유형 (전세: true, 월세: false)
        dto.setLeaseType(RentType.JEONSE.name().equals(dbDTO.getLeaseType()));

        // 임대인/임차인 정보
        dto.setOwnerNickname(ownerVO.getName());
        dto.setBuyerNickname(buyerVO.getName());

        // 주소 정보 - 여러 소스에서 가져오기 (우선순위: DB -> MongoDB -> BuildingDocument)
        String addr1 = dbDTO.getHomeAddr1(); // DB에서 먼저 가져오기
        String addr2 = dbDTO.getHomeAddr2();
        
        log.info("Address from DB - addr1: '{}', addr2: '{}'", addr1, addr2);
        
        // DB의 주소가 비어있으면 MongoDB document에서 가져오기
        if (addr1 == null || addr1.trim().isEmpty()) {
            addr1 = document.getHomeAddr1();
            addr2 = document.getHomeAddr2();
            log.info("Address from MongoDB - addr1: '{}', addr2: '{}'", addr1, addr2);
        }
        
        // MongoDB document의 주소도 비어있으면 BuildingDocument에서 가져오기
        if ((addr1 == null || addr1.trim().isEmpty()) && buildingDocument != null) {
            String roadAddress = buildingDocument.getRoadAddress();
            log.info("BuildingDocument roadAddress: '{}'", roadAddress);
            if (roadAddress != null && !roadAddress.trim().isEmpty()) {
                addr1 = roadAddress;
                log.info("Using address from BuildingDocument: {}", addr1);
            }
        }
        
        // 여전히 비어있으면 기본값 설정
        if (addr1 == null || addr1.trim().isEmpty()) {
            addr1 = "주소 정보 없음";
            log.warn("No address information found for contract {}, using default", document.getContractChatId());
        }
        
        dto.setAddr1(addr1);
        dto.setAddr2(addr2 != null ? addr2 : "");
        
        log.info("Final address set - addr1: '{}', addr2: '{}'", dto.getAddr1(), dto.getAddr2());

        // 건물 정보
        dto.setLandCategory(dbDTO.getLandCategory());
        dto.setArea(String.valueOf(dbDTO.getArea()));
        dto.setBuildingStructure(dbDTO.getBuildingStructure() != null ? dbDTO.getBuildingStructure() : "철근콘크리트 구조");

        // 건축물대장 정보가 있으면 사용, 없으면 기본값
        if (buildingDocument != null) {
            dto.setPurpose(buildingDocument.getPurpose() != null ? buildingDocument.getPurpose() : "주택");
            dto.setTotalFloorArea(buildingDocument.getTotalFloorArea() != null ?
                    buildingDocument.getTotalFloorArea().toString() : "100");
        } else {
            dto.setPurpose("주택"); // 기본값
            dto.setTotalFloorArea("100"); // 기본값
        }

        dto.setSupplyArea(String.valueOf(document.getExclusiveArea()));

        // 체크박스
        dto.setHasTaxArrears(false); // 초기값
        dto.setHasPriorFixedDate(false); // 초기값

        // 금액 정보
        dto.setTextDepositPrice(convertToKoreanWon(document.getDepositPrice()));
        dto.setDepositPrice(String.valueOf(document.getDepositPrice()));
        dto.setMonthlyRent(String.valueOf(document.getMonthlyRent()));
        dto.setPaymentDueDay(String.valueOf(dbDTO.getPaymentDueDay()));
        dto.setBankAccount(dbDTO.getBankAccount());
        dto.setTextMaintenanceFee(convertToKoreanWon(document.getMaintenanceFee()));
        dto.setMaintenanceFee(String.valueOf(document.getMaintenanceFee()));

        // 날짜 정보
        LocalDate moveInDate = dbDTO.getExpectedMoveInDate();
        // 계약 기간을 사용하여 퇴거 날짜 계산
        LocalDate moveOutDate = moveInDate.plusYears(dbDTO.getContractDuration().getYears());
        LocalDate contractDate = LocalDate.now();

        dto.setExpectedMoveInYear(String.valueOf(moveInDate.getYear()));
        dto.setExpectedMoveInMonth(String.valueOf(moveInDate.getMonthValue()));
        dto.setExpectedMoveInDay(String.valueOf(moveInDate.getDayOfMonth()));
        dto.setExpectedMoveOutYear(String.valueOf(moveOutDate.getYear()));
        dto.setExpectedMoveOutMonth(String.valueOf(moveOutDate.getMonthValue()));
        dto.setExpectedMoveOutDay(String.valueOf(moveOutDate.getDayOfMonth()));
        dto.setContractDateYear(String.valueOf(contractDate.getYear()));
        dto.setContractDateMonth(String.valueOf(contractDate.getMonthValue()));
        dto.setContractDateDay(String.valueOf(contractDate.getDayOfMonth()));

        // 개인정보
        dto.setOwnerAddr(ownerVO.getAddr1() + " " + ownerVO.getAddr2());
        dto.setOwnerSsn(dbDTO.getOwnerSsnFront() + "-" + aesCryptoUtil.decrypt(dbDTO.getOwnerSsnBack()));
        dto.setOwnerPhoneNumber(ownerVO.getPhoneNumber());
        dto.setBuyerAddr(buyerVO.getAddr1() + " " + buyerVO.getAddr2());
        dto.setBuyerSsn(dbDTO.getBuyerSsnFront() + "-" + aesCryptoUtil.decrypt(dbDTO.getBuyerSsnBack()));
        dto.setBuyerPhoneNumber(buyerVO.getPhoneNumber());

        // 특약사항 - SpecialContract 객체 리스트를 String 리스트로 변환
        if (document.getSpecialContracts() != null) {
            List<String> specialStrings = document.getSpecialContracts().stream()
                    .map(sc -> sc.getContent())
                    .collect(Collectors.toList());
            dto.setSpecial(specialStrings);
        }

        return dto;
    }

    // 최종 계약서 작성하기 PDF -> AI
    @Override
    @Transactional
    public MultipartFile finalContractPDF(Long contractChatId, Long userId) {
        // userId 인증
        validateUserId(contractChatId, userId);

        ContractMongoDocument document = repository.getContract(contractChatId);

        int depositPrice = document.getDepositPrice();
        int monthlyRent = document.getMonthlyRent();
        int maintenanceFee = document.getMaintenanceFee();

        int finalContract = contractMapper.insertFinalContractInit(contractChatId, depositPrice, monthlyRent, maintenanceFee);
        if (finalContract != 1) throw new BusinessException(ContractException.CONTRACT_DB_INSERT);

        // DB에서 값을 가져온다
        DBFinalContractDTO dbDTO = contractMapper.selectFinalContractPDF(contractChatId);

        // 복호화 하기
        Long ownerContractId = contractMapper.getOwnerId(contractChatId);
        Long buyerContractId = contractMapper.getBuyerId(contractChatId);

        IdentityVerificationInfoVO ownerVO = identityVerificationService.getDecryptedVerificationInfo(contractChatId, ownerContractId);
        IdentityVerificationInfoVO buyerVO = identityVerificationService.getDecryptedVerificationInfo(contractChatId, buyerContractId);

        String ownerSsnFront = dbDTO.getOwnerSsnFront();
        String ownerSsnBack = aesCryptoUtil.decrypt(dbDTO.getOwnerSsnBack());
        String buyerSsnFront = dbDTO.getBuyerSsnFront();
        String buyerSsnBack = aesCryptoUtil.decrypt(dbDTO.getBuyerSsnBack());

        // 추가 작업 해야할거 하기
        boolean leaseType;

        if (RentType.JEONSE.name().equals(dbDTO.getLeaseType())) {
            leaseType = true;
        } else if (RentType.WOLSE.name().equals(dbDTO.getLeaseType())) {
            leaseType = false; // WOLSE일 때 명시적으로 false
        } else {
            leaseType = false; // 기타 타입도 false
        }

        String buildingStructure = "철근 콘크리트 구조";
        String ownerSsn = ownerSsnFront + "-" + ownerSsnBack;
        String buyerSsn = buyerSsnFront + "-" + buyerSsnBack;

        String textDepositPrice = numberFormatUtil.toKoreanNumber(document.getDepositPrice());
        String textMaintenanceFee = numberFormatUtil.toKoreanNumber(document.getMaintenanceFee());

        LocalDate expectedMoveOut =
                dbDTO.getExpectedMoveInDate()
                        .plusYears(dbDTO.getContractDuration().getYears());

        // DTO 만들기
        SaveFinalContractDTO finalDTO = SaveFinalContractDTO.toDTO(dbDTO, leaseType, buildingStructure, textDepositPrice, textMaintenanceFee, expectedMoveOut, ownerSsn, buyerSsn, document, ownerVO, buyerVO);

        MultipartFile result;
        // AI로 보내서 받기
        try {
            // AI로 해당 데이터를 넘긴다 (restTemplate 사용)
            String url = aiServerUrl + "/api/contract/이건 다시 받기!";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<SaveFinalContractDTO> requestEntity = new HttpEntity<>(finalDTO, headers);

            // 반환값을 받아오고, 그 값을 프론트에 넘겨준다.
            ResponseEntity<byte[]> response =
                    restTemplate.exchange(url, HttpMethod.POST, requestEntity, byte[].class);

            log.warn("AI 응답 헤더 확인: {}", response.getStatusCode());
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 응답 바이트를 파일로 저장
                byte[] fileBytes = response.getBody();
                File tempFile = File.createTempFile("contract_", ".pdf");
                Files.write(tempFile.toPath(), fileBytes);
                result = MultipartFileUtils.fromFile(tempFile, "contract.pdf", "application/pdf");

                // s3에 파일 업로드 하기
                String key = s3Service.uploadFile(result);
                int update = contractMapper.insertContract(contractChatId, key);
                if (update != 1) throw new BusinessException(ContractException.CONTRACT_DB_UPDATE);

                // 받은 PDF를 반환하기
                return result;
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


    @Override
    @Transactional
    public Boolean saveSignature(Long contractChatId, Long userId, SaveSignatureDTO signatureDTO, List<MultipartFile> imgFiles) throws Exception {
        // userId 인증
        validateUserId(contractChatId, userId);

        if (signatureDTO == null) {
            throw new BusinessException(ContractException.CONTRACT_REDIS, "signatureDTO가 비었습니다.");
        }
        if (imgFiles == null || imgFiles.isEmpty()) {
            throw new BusinessException(ContractException.CONTRACT_REDIS, "서명 이미지가 비어 있습니다.");
        }

        // signedType이 null인 경우 기본값 설정
        if (signatureDTO.getSignedType() == null) {
            // userId로 역할 확인하여 자동 설정
            // 임시로 OWNER_CONTRACT로 설정 (실제로는 사용자 역할 확인 필요)
            signatureDTO.setSignedType(SignedType.OWNER_CONTRACT);
            log.warn("signedType이 null이어서 기본값으로 설정: {}", signatureDTO.getSignedType());
        }

        log.info("[saveSignature] start ccId={}, userId={}, signedType={}, 파일 개수={}",
                contractChatId, userId, signatureDTO.getSignedType(), imgFiles.size());

        // 첫 번째 서명 이미지 처리 (메인 서명)
        MultipartFile mainSignature = imgFiles.get(0);
        log.info("메인 서명 처리: fileName={}, size={}",
                mainSignature.getOriginalFilename(), mainSignature.getSize());

        // 사진 암호화 & 해시값 생성
        FileWithHashDto imgDTO = encryptionService.encryptImage(mainSignature);

//          MultipartFile imgFile = MultipartFileUtils.fromFile(imgDTO.getFile());
        MultipartFile imgFile;
        try {
            imgFile = MultipartFileUtils.fromFile(imgDTO.getFile());
            if (imgFile == null || imgFile.isEmpty()) {
                throw new IllegalStateException("변환된 파일이 비어 있습니다.");
            }
        } catch (Exception e) {
            throw new BusinessException(ContractException.CONTRACT_REDIS, "파일 변환 실패", e);
        }

        // s3에 저장하기
        String s3Key = s3Service.uploadFile(imgFile);

        log.info("[updateSignature] ccId={}, type={}, s3Key={}, hash={}",
                contractChatId, signatureDTO.getSignedType(), s3Key, imgDTO.getOriginalHash());
        int save = contractMapper.insertSignature(contractChatId, s3Key, imgDTO.getOriginalHash(), signatureDTO.getSignedType(), userId);
        log.info("[updateSignature] affectedRows={}", save);

        // electronic_signature에 저장하기
//          int save = contractMapper.updateSignature(contractChatId, s3Key, imgDTO.getOriginalHash(), signatureDTO.getSignedType());
        if (save != 1) throw new BusinessException(ContractException.CONTRACT_DB_UPDATE, "업데이트가 안 됐어요");

        List<ElectronicSignature> signatures = contractMapper.selectSignature(contractChatId, userId);
        log.info("============서명=============");
        log.info(signatureDTO.getSignedType());
        // 상대 서명이 이미 있는지 확인
        if (signatureDTO.getSignedType() == SignedType.OWNER_CONTRACT) {
            for (ElectronicSignature sig : signatures) {
                if (sig.getSignedType() == SignedType.BUYER_CONTRACT) {
                    return true; // 임차인 서명 있음
                }
            }
            return false; // 임차인 서명 없음
        } else if (signatureDTO.getSignedType() == SignedType.BUYER_CONTRACT) {
            for (ElectronicSignature sig : signatures) {
                if (sig.getSignedType() == SignedType.OWNER_CONTRACT) {
                    return true; // 임대인 서명 있음
                }
            }
            return false; // 임대인 서명 없음
        }
        if (signatureDTO.getSignedType() == SignedType.TAX || signatureDTO.getSignedType() == SignedType.PRIORITY)
            return false;
        throw new BusinessException(ContractException.CONTRACT_GET); // 예외처리 다시 하기!
    }

    @Override
    @Transactional
    public byte[] saveFinalContract(Long contractChatId, Long userId, ContractPasswordDTO dto) {
        // userId 인증
        validateUserId(contractChatId, userId);

        byte[] resultBytes = null; // 최종 PDF 바이트 (없으면 null 반환)

        // 동의 여부 확인
        if (!dto.getMediationAgree()) throw new BusinessException(ContractException.CONTRACT_AGREEMENT);

        // 1. 최종 사인이 있는지 여부를 확인한다.
        List<ElectronicSignature> signatures = contractMapper.selectSignature(contractChatId, userId);

        // DB에서 값을 가져온다
        DBFinalContractDTO dbDTO = contractMapper.selectFinalContractPDF(contractChatId);

        // 몽고 DB에서 값을 가져오기
        ContractMongoDocument document = repository.getContract(contractChatId);

        // 복호화 하기
        Long ownerContractId = contractMapper.getOwnerId(contractChatId);
        Long buyerContractId = contractMapper.getBuyerId(contractChatId);

        IdentityVerificationInfoVO ownerVO = identityVerificationService.getDecryptedVerificationInfo(contractChatId, ownerContractId);
        IdentityVerificationInfoVO buyerVO = identityVerificationService.getDecryptedVerificationInfo(contractChatId, buyerContractId);

        String ownerSsnFront = dbDTO.getOwnerSsnFront();
        String ownerSsnBack = aesCryptoUtil.decrypt(dbDTO.getOwnerSsnBack());
        String buyerSsnFront = dbDTO.getBuyerSsnFront();
        String buyerSsnBack = aesCryptoUtil.decrypt(dbDTO.getBuyerSsnBack());

        // 추가 작업 해야할거 하기
        boolean leaseType;

        if (RentType.JEONSE.name().equals(dbDTO.getLeaseType())) {
            leaseType = true;
        } else if (RentType.WOLSE.name().equals(dbDTO.getLeaseType())) {
            leaseType = false; // WOLSE일 때 명시적으로 false
        } else {
            leaseType = false; // 기타 타입도 false
        }

        String buildingStructure = "철근 콘크리트 구조";
        String ownerSsn = ownerSsnFront + "-" + ownerSsnBack;
        String buyerSsn = buyerSsnFront + "-" + buyerSsnBack;

        String textDepositPrice = numberFormatUtil.toKoreanNumber(document.getDepositPrice());
        String textMaintenanceFee = numberFormatUtil.toKoreanNumber(document.getMaintenanceFee());

        LocalDate expectedMoveOut =
                dbDTO.getExpectedMoveInDate()
                        .plusYears(dbDTO.getContractDuration().getYears());

        // --------
        FinalContractDTO.FinalContractDTOBuilder builder = FinalContractDTO.builder();
        builder.leaseType(leaseType);
        builder.ownerNickname(document.getOwnerName());
        builder.buyerNickname(document.getBuyerName());
        builder.addr1(document.getHomeAddr1());
        builder.landCategory(dbDTO.getLandCategory());
        builder.area(dbDTO.getArea());
        builder.buildingStructure(buildingStructure);
//        builder.purpose(dbDTO.getPurpose());
//        builder.totalFloorArea(dbDTO.getTotalFloorArea());
        builder.addr2(document.getHomeAddr2());
        builder.supplyArea(document.getExclusiveArea());
        builder.hasTaxArrears(dbDTO.isHasTaxArrears());
        builder.hasPriorFixedDate(dbDTO.isHasPriorFixedDate());
        builder.textDepositPrice(textDepositPrice);
        builder.depositPrice(document.getDepositPrice());
        builder.monthlyRent(document.getMonthlyRent());
        builder.paymentDueDay(dbDTO.getPaymentDueDay());
        builder.bankAccount(dbDTO.getBankAccount());
        builder.textMaintenanceFee(textMaintenanceFee);
        builder.maintenanceFee(document.getMaintenanceFee());
        builder.expectedMoveInYear(dbDTO.getExpectedMoveInDate().getYear());
        builder.expectedMoveInMonth(dbDTO.getExpectedMoveInDate().getMonthValue());
        builder.expectedMoveInDay(dbDTO.getExpectedMoveInDate().getDayOfMonth());
        builder.expectedMoveOutYear(expectedMoveOut.getYear());
        builder.expectedMoveOutMonth(expectedMoveOut.getMonthValue());
        builder.expectedMoveOutDay(expectedMoveOut.getDayOfMonth());
        builder.contractDateYear(dbDTO.getContractDate().getYear());
        builder.contractDateMonth(dbDTO.getContractDate().getMonthValue());
        builder.contractDateDay(dbDTO.getContractDate().getDayOfMonth());
        builder.ownerAddr(ownerVO.getAddr1() + " " + ownerVO.getAddr2());
        builder.ownerSsn(ownerSsn);
        builder.ownerPhoneNumber(ownerVO.getPhoneNumber());
        builder.buyerAddr(buyerVO.getAddr1() + " " + buyerVO.getAddr2());
        builder.buyerSsn(buyerSsn);
        builder.buyerPhoneNumber(buyerVO.getPhoneNumber());
        FinalContractDTO basePayLoad = builder.build();

//        FinalContractDTO finalDTO = FinalContractDTO.toDTO(dbDTO, leaseType, buildingStructure, textDepositPrice, textMaintenanceFee, expectedMoveOut, ownerSsn, buyerSsn, document, ownerVO, buyerVO);

//        for (ElectronicSignature sign : signatures) {
//            try (InputStream s3File = s3Service.downloadFile(sign.getSignatureFileKey())) {
//                File contractFile = MultipartFileUtils.inputStreamToTempFile(s3File);
//
//                switch (sign.getSignedType()) {
//                    case TAX:
//                        builder.ownerTaxSignature(contractFile);
//                        break;
//                    case PRIORITY:
//                        builder.ownerPrioritySignature(contractFile);
//                        break;
//                    case OWNER_CONTRACT:
//                        builder.ownerContractSignature(contractFile);
//                        break;
//                    case BUYER_CONTRACT:
//                        builder.buyerContractSignature(contractFile);
//                        break;
//                    default:
//                        throw new IllegalArgumentException("지원하지 않는 서명 타입입니다: " + sign.getSignedType());
//                }
//            } catch (IOException e) {
//                throw new BusinessException(ContractException.CONTRACT_INSERT, e);
//            }
//        }

        // ------

        for (ElectronicSignature sign : signatures) {

            try (InputStream s3File = s3Service.downloadFile(sign.getSignatureFileKey())) {
                File contractFile = MultipartFileUtils.inputStreamToTempFile(s3File);

                switch (sign.getSignedType()) {
                    case TAX:
                        builder.ownerTaxSignature(contractFile);
                        break;
                    case PRIORITY:
                        builder.ownerPrioritySignature(contractFile);
                        break;
                    case OWNER_CONTRACT:
                        builder.ownerContractSignature(contractFile);
                        break;
                    case BUYER_CONTRACT:
                        builder.buyerContractSignature(contractFile);
                        break;
                    default:
                        throw new IllegalArgumentException("지원하지 않는 서명 타입입니다: " + sign.getSignedType());
                }
            } catch (IOException e) {
                throw new BusinessException(ContractException.CONTRACT_INSERT, e);
            }

            String redisKey = "contract:sign:" + contractChatId;

            if (sign.getSignedType() != null && sign.getSignedType() == SignedType.OWNER_CONTRACT) {

                try {
                    String existing = stringRedisTemplate.opsForValue().get(redisKey);
                    if (existing != null) {

//                        // 3. DTO를 JSON 문자열로 변환
//                        ObjectMapper objectMapper = new ObjectMapper();
//                        String json = objectMapper.writeValueAsString(dto);
//
//                        // 4. Redis에 저장
//                        stringRedisTemplate.opsForValue().set(redisKey, json);

                        FinalContractDTO finalDTO = basePayLoad.toBuilder()
                                .ownerMediationAgree(dto.getMediationAgree())
                                .build();

                        File tempFile;
                        // AI에 사인 & 동의 여부를 넘기기 -> 여기서 pdf를 같이 넘겨야 하는지 or 다시 처음부터 모든 값을 넘겨야 하는지 물어보기
                        try {
                            // AI로 해당 데이터를 넘긴다 (restTemplate 사용)
                            String url = aiServerUrl + "/api/contract/generate";
                            HttpHeaders headers = new HttpHeaders();
                            headers.setContentType(MediaType.APPLICATION_JSON);

                            HttpEntity<FinalContractDTO> requestEntity = new HttpEntity<>(finalDTO, headers);

                            // 반환값을 받아오고, 그 값을 프론트에 넘겨준다.
                            ResponseEntity<byte[]> response =
                                    restTemplate.exchange(url, HttpMethod.POST, requestEntity, byte[].class);

                            log.warn("AI 응답 헤더 확인: {}", response.getStatusCode());

                            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                                // 응답 바이트를 파일로 저장
                                byte[] fileBytes = response.getBody();
                                tempFile = File.createTempFile("contract_", ".pdf");
                                Files.write(tempFile.toPath(), fileBytes);

                            } else {
                                // Sanitize response body before logging to prevent log injection
                                String responseBodyStr;
                                try {
                                    responseBodyStr = objectMapper.writeValueAsString(response.getBody());
                                } catch (Exception ex) {
                                    responseBodyStr = String.valueOf(response.getBody());
                                }
                                // Remove newlines and carriage returns
                                responseBodyStr = responseBodyStr.replaceAll("[\\r\\n]", " ");
                                log.error(responseBodyStr);
                                throw new BusinessException(ContractException.CONTRACT_AI_SERVER_ERROR);
                            }

                            // AI 쪽에서 최종 값을 받기 (pdf)
                            MultipartFile contracts = MultipartFileUtils.fromFile(tempFile);
                            byte[] finalContract = MultipartFileUtils.fileToBytes(tempFile);

                            resultBytes = finalContract;

                            try {
                                // 1단계 업로드 실행 (반환 값을 사용하지 않으면 변수에 담지 않아도 됩니다)
                                // Convert MultipartFile to String (base64 or file path)
                                String contractData = Base64.getEncoder().encodeToString(contracts.getBytes());
                                encryptionService.uploadPdfStep1(contractData, dto.getContractPassword());
                                // 임대인과 임차인의 생년월일 가져오기
                                Long ownerId = contractMapper.getOwnerId(contractChatId);
                                Long buyerId = contractMapper.getBuyerId(contractChatId);

                                String ownerBirthDate = contractMapper.selectBirth(ownerId).replace("-", "");
                                String buyerBirthDate = contractMapper.selectBirth(buyerId).replace("-", "");

                                // 생년월일을 YYMMDD 형식으로 변환
                                String ownerKey = ownerBirthDate.substring(2); // YYYY-MM-DD -> YYMMDD
                                String buyerKey = buyerBirthDate.substring(2); // YYYY-MM-DD -> YYMMDD

                                // 두 생년월일을 조합한 암호화 키 생성 (예: owner_buyer)
                                String combinedKey = ownerKey + "_" + buyerKey;

                                // PDF 암호화 및 S3 업로드
                                FileWithHashDto encryptedPdf = encryptionService.addPasswordToPdf(contracts, combinedKey);
                                String s3Key = s3Service.uploadFile(MultipartFileUtils.fromFile(encryptedPdf.getFile()));

                                // final_contract 테이블에 저장
                                int update = contractMapper.updateFinalContract(contractChatId, s3Key, encryptedPdf.getOriginalHash());
                                if (update != 1) throw new BusinessException(ContractException.CONTRACT_DB_UPDATE);

                                log.info("최종 계약서 저장 완료 - contractChatId: {}, S3 Key: {}", contractChatId, s3Key);
                            } catch (Exception ex) {
                                // 업로드 과정의 예외를 비즈니스 예외로 변환
                                throw new BusinessException(ContractException.CONTRACT_INSERT, ex);
                            }

                            // 알림 보내기 ------------------------------------------------

                            stringRedisTemplate.delete(redisKey);

                        } catch (Exception e) {
                            log.error(e.getMessage());
                            throw new BusinessException(ContractException.CONTRACT_AI_SERVER_ERROR, e);
                        }
                    } else if (existing == null) {

                        // 3. DTO를 JSON 문자열로 변환
                        ObjectMapper objectMapper = new ObjectMapper();
                        String json = objectMapper.writeValueAsString(dto);

                        // 4. Redis에 저장
                        stringRedisTemplate.opsForValue().set(redisKey, json);

//                        try {
//                            FileWithHashDto uploadStep2 = encryptionService.encryptPdfStep2(String.valueOf(contractChatId), dto.getContractPassword());
//
//                            // S3에 저장하기
//                            MultipartFile multipartContract = MultipartFileUtils.fromFile(uploadStep2.getFile());
//                            String s3Keys = s3Service.uploadFile(multipartContract);
//
//                            // final_contract에 값을 저장하기
//                            int update = contractMapper.updateFinalContract(contractChatId, s3Keys, uploadStep2.getOriginalHash());
//                            if (update != 1) throw new BusinessException(ContractException.CONTRACT_DB_UPDATE);
//
//                        } catch (Exception ex) {
//                            // 업로드 과정의 예외를 비즈니스 예외로 변환
//                            throw new BusinessException(ContractException.CONTRACT_INSERT, ex);
//                        }
                    }

                } catch (Exception e) {
                    log.error(e.getMessage());
                    throw new BusinessException(ContractException.CONTRACT_AI_SERVER_ERROR, e);
                }

            } else if (sign.getSignedType() != null && sign.getSignedType() == SignedType.BUYER_CONTRACT) {
                try {
                    String existing = stringRedisTemplate.opsForValue().get(redisKey);
                    if (existing != null) {

                        FinalContractDTO finalDTO = basePayLoad.toBuilder()
                                .ownerMediationAgree(dto.getMediationAgree())
                                .build();


                        File tempFile;
                        // AI에 사인 & 동의 여부를 넘기기 -> 여기서 pdf를 같이 넘겨야 하는지 or 다시 처음부터 모든 값을 넘겨야 하는지 물어보기
                        try {
                            // AI로 해당 데이터를 넘긴다 (restTemplate 사용)
                            String url = aiServerUrl + "/api/contract/generate";
                            HttpHeaders headers = new HttpHeaders();
                            headers.setContentType(MediaType.APPLICATION_JSON);

                            HttpEntity<FinalContractDTO> requestEntity = new HttpEntity<>(finalDTO, headers);

                            // 반환값을 받아오고, 그 값을 프론트에 넘겨준다.
                            ResponseEntity<byte[]> response =
                                    restTemplate.exchange(url, HttpMethod.POST, requestEntity, byte[].class);

                            log.warn("AI 응답 헤더 확인: {}", response.getStatusCode());

                            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                                // 응답 바이트를 파일로 저장
                                byte[] fileBytes = response.getBody();
                                tempFile = File.createTempFile("contract_", ".pdf");
                                Files.write(tempFile.toPath(), fileBytes);

                            } else {
                                // Sanitize response body before logging to prevent log injection
                                String responseBodyStr;
                                try {
                                    responseBodyStr = objectMapper.writeValueAsString(response.getBody());
                                } catch (Exception ex) {
                                    responseBodyStr = String.valueOf(response.getBody());
                                }
                                // Remove newlines and carriage returns
                                responseBodyStr = responseBodyStr.replaceAll("[\\r\\n]", " ");
                                log.error(responseBodyStr);
                                throw new BusinessException(ContractException.CONTRACT_AI_SERVER_ERROR);
                            }

                            // AI 쪽에서 최종 값을 받기 (pdf)
                            MultipartFile contracts = MultipartFileUtils.fromFile(tempFile);
                            byte[] finalContract = MultipartFileUtils.fileToBytes(tempFile);

                            resultBytes = finalContract;

                            try {
                                // 1단계 업로드 실행 (반환 값을 사용하지 않으면 변수에 담지 않아도 됩니다)
                                // Convert MultipartFile to String (base64 or file path)
                                String contractData = Base64.getEncoder().encodeToString(contracts.getBytes());
                                encryptionService.uploadPdfStep1(contractData, dto.getContractPassword());
                                // 임대인과 임차인의 생년월일 가져오기
                                Long ownerId = contractMapper.getOwnerId(contractChatId);
                                Long buyerId = contractMapper.getBuyerId(contractChatId);

                                String ownerBirthDate = contractMapper.selectBirth(ownerId).replace("-", "");
                                String buyerBirthDate = contractMapper.selectBirth(buyerId).replace("-", "");

                                // 생년월일을 YYMMDD 형식으로 변환
                                String ownerKey = ownerBirthDate.substring(2); // YYYY-MM-DD -> YYMMDD
                                String buyerKey = buyerBirthDate.substring(2); // YYYY-MM-DD -> YYMMDD

                                // 두 생년월일을 조합한 암호화 키 생성 (예: owner_buyer)
                                String combinedKey = ownerKey + "_" + buyerKey;

                                // PDF 암호화 및 S3 업로드
                                FileWithHashDto encryptedPdf = encryptionService.addPasswordToPdf(contracts, combinedKey);
                                String s3Key = s3Service.uploadFile(MultipartFileUtils.fromFile(encryptedPdf.getFile()));

                                // final_contract 테이블에 저장
                                int update = contractMapper.updateFinalContract(contractChatId, s3Key, encryptedPdf.getOriginalHash());
                                if (update != 1) throw new BusinessException(ContractException.CONTRACT_DB_UPDATE);

                                log.info("최종 계약서 저장 완료 - contractChatId: {}, S3 Key: {}", contractChatId, s3Key);
                            } catch (Exception ex) {
                                // 업로드 과정의 예외를 비즈니스 예외로 변환
                                throw new BusinessException(ContractException.CONTRACT_INSERT, ex);
                            }

                            // 알림 보내기  --------------------------------------

                            stringRedisTemplate.delete(redisKey);

                        } catch (Exception e) {
                            log.error(e.getMessage());
                            throw new BusinessException(ContractException.CONTRACT_AI_SERVER_ERROR, e);
                        }
                    } else if (existing == null) {

//                        try {
//                            FileWithHashDto uploadStep2 = encryptionService.encryptPdfStep2(String.valueOf(contractChatId), dto.getContractPassword());
//
//                            // S3에 저장하기
//                            MultipartFile multipartContract = MultipartFileUtils.fromFile(uploadStep2.getFile());
//                            String s3Keys = s3Service.uploadFile(multipartContract);
//
//                            // final_contract에 값을 저장하기
//                            int update = contractMapper.updateFinalContract(contractChatId, s3Keys, uploadStep2.getOriginalHash());
//                            if (update != 1) throw new BusinessException(ContractException.CONTRACT_DB_UPDATE);
//
//                            stringRedisTemplate.delete(redisKey);
//                        } catch (Exception ex) {
//                            // 업로드 과정의 예외를 비즈니스 예외로 변환
//                            throw new BusinessException(ContractException.CONTRACT_INSERT, ex);
//                        }

                        // 3. DTO를 JSON 문자열로 변환
                        ObjectMapper objectMapper = new ObjectMapper();
                        String json = objectMapper.writeValueAsString(dto);

                        // 4. Redis에 저장
                        stringRedisTemplate.opsForValue().set(redisKey, json);
                    }

                } catch (Exception e) {
                    log.error(e.getMessage());
                    throw new BusinessException(ContractException.CONTRACT_AI_SERVER_ERROR, e);
                }
            }
        }

        return resultBytes;
    }

    @Override
    public byte[] selectContractPDF(Long contractChatId, Long userId, ContractPasswordDTO dto) {

        // userId 인증
        validateUserId(contractChatId, userId);

        // s3에서 pdf를 가져온다.
        // 1. 최종 사인이 있는지 여부를 확인한다.
        List<ElectronicSignature> signatures = contractMapper.selectSignature(contractChatId, userId);

        // DB에서 값을 가져온다
        DBFinalContractDTO dbDTO = contractMapper.selectFinalContractPDF(contractChatId);

        // 몽고 DB에서 값을 가져오기
        ContractMongoDocument document = repository.getContract(contractChatId);

        // 복호화 하기
        Long ownerContractId = contractMapper.getOwnerId(contractChatId);
        Long buyerContractId = contractMapper.getBuyerId(contractChatId);

        IdentityVerificationInfoVO ownerVO = identityVerificationService.getDecryptedVerificationInfo(contractChatId, ownerContractId);
        IdentityVerificationInfoVO buyerVO = identityVerificationService.getDecryptedVerificationInfo(contractChatId, buyerContractId);

        String ownerSsnFront = dbDTO.getOwnerSsnFront();
        String ownerSsnBack = aesCryptoUtil.decrypt(dbDTO.getOwnerSsnBack());
        String buyerSsnFront = dbDTO.getBuyerSsnFront();
        String buyerSsnBack = aesCryptoUtil.decrypt(dbDTO.getBuyerSsnBack());

        // 추가 작업 해야할거 하기
        boolean leaseType;

        if (RentType.JEONSE.name().equals(dbDTO.getLeaseType())) {
            leaseType = true;
        } else if (RentType.WOLSE.name().equals(dbDTO.getLeaseType())) {
            leaseType = false; // WOLSE일 때 명시적으로 false
        } else {
            leaseType = false; // 기타 타입도 false
        }

        String buildingStructure = "철근 콘크리트 구조";
        String ownerSsn = ownerSsnFront + "-" + ownerSsnBack;
        String buyerSsn = buyerSsnFront + "-" + buyerSsnBack;

        String textDepositPrice = numberFormatUtil.toKoreanNumber(document.getDepositPrice());
        String textMaintenanceFee = numberFormatUtil.toKoreanNumber(document.getMaintenanceFee());

        LocalDate expectedMoveOut =
                dbDTO.getExpectedMoveInDate()
                        .plusYears(dbDTO.getContractDuration().getYears());

        // --------
        FinalContractDTO.FinalContractDTOBuilder builder = FinalContractDTO.builder();
        builder.leaseType(leaseType);
        builder.ownerNickname(document.getOwnerName());
        builder.buyerNickname(document.getBuyerName());
        builder.addr1(document.getHomeAddr1());
        builder.landCategory(dbDTO.getLandCategory());
        builder.area(dbDTO.getArea());
        builder.buildingStructure(buildingStructure);
//        builder.purpose(dbDTO.getPurpose());
//        builder.totalFloorArea(dbDTO.getTotalFloorArea());
        builder.addr2(document.getHomeAddr2());
        builder.supplyArea(document.getExclusiveArea());
        builder.hasTaxArrears(dbDTO.isHasTaxArrears());
        builder.hasPriorFixedDate(dbDTO.isHasPriorFixedDate());
        builder.textDepositPrice(textDepositPrice);
        builder.depositPrice(document.getDepositPrice());
        builder.monthlyRent(document.getMonthlyRent());
        builder.paymentDueDay(dbDTO.getPaymentDueDay());
        builder.bankAccount(dbDTO.getBankAccount());
        builder.textMaintenanceFee(textMaintenanceFee);
        builder.maintenanceFee(document.getMaintenanceFee());
        builder.expectedMoveInYear(dbDTO.getExpectedMoveInDate().getYear());
        builder.expectedMoveInMonth(dbDTO.getExpectedMoveInDate().getMonthValue());
        builder.expectedMoveInDay(dbDTO.getExpectedMoveInDate().getDayOfMonth());
        builder.expectedMoveOutYear(expectedMoveOut.getYear());
        builder.expectedMoveOutMonth(expectedMoveOut.getMonthValue());
        builder.expectedMoveOutDay(expectedMoveOut.getDayOfMonth());
        builder.contractDateYear(dbDTO.getContractDate().getYear());
        builder.contractDateMonth(dbDTO.getContractDate().getMonthValue());
        builder.contractDateDay(dbDTO.getContractDate().getDayOfMonth());
        builder.ownerAddr(ownerVO.getAddr1() + " " + ownerVO.getAddr2());
        builder.ownerSsn(ownerSsn);
        builder.ownerPhoneNumber(ownerVO.getPhoneNumber());
        builder.buyerAddr(buyerVO.getAddr1() + " " + buyerVO.getAddr2());
        builder.buyerSsn(buyerSsn);
        builder.buyerPhoneNumber(buyerVO.getPhoneNumber());
        FinalContractDTO basePayLoad = builder.build();

        for (ElectronicSignature sign : signatures) {

            try (InputStream s3File = s3Service.downloadFile(sign.getSignatureFileKey())) {
                File contractFile = MultipartFileUtils.inputStreamToTempFile(s3File);

                switch (sign.getSignedType()) {
                    case TAX:
                        builder.ownerTaxSignature(contractFile);
                        break;
                    case PRIORITY:
                        builder.ownerPrioritySignature(contractFile);
                        break;
                    case OWNER_CONTRACT:
                        builder.ownerContractSignature(contractFile);
                        break;
                    case BUYER_CONTRACT:
                        builder.buyerContractSignature(contractFile);
                        break;
                    default:
                        throw new IllegalArgumentException("지원하지 않는 서명 타입입니다: " + sign.getSignedType());
                }
            } catch (IOException e) {
                throw new BusinessException(ContractException.CONTRACT_INSERT, e);
            }

            FinalContractDTO finalDTO = basePayLoad.toBuilder()
                    .ownerMediationAgree(true)
                    .build();

            File tempFile;
            // AI에 사인 & 동의 여부를 넘기기 -> 여기서 pdf를 같이 넘겨야 하는지 or 다시 처음부터 모든 값을 넘겨야 하는지 물어보기
            try {
                // AI로 해당 데이터를 넘긴다 (restTemplate 사용)
                String url = aiServerUrl + "/api/contract/generate";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<FinalContractDTO> requestEntity = new HttpEntity<>(finalDTO, headers);

                // 반환값을 받아오고, 그 값을 프론트에 넘겨준다.
                ResponseEntity<byte[]> response =
                        restTemplate.exchange(url, HttpMethod.POST, requestEntity, byte[].class);

                log.warn("AI 응답 헤더 확인: {}", response.getStatusCode());

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    // 응답 바이트를 파일로 저장
                    byte[] fileBytes = response.getBody();
                    tempFile = File.createTempFile("contract_", ".pdf");
                    Files.write(tempFile.toPath(), fileBytes);

                } else {
                    // Sanitize response body before logging to prevent log injection
                    String responseBodyStr;
                    try {
                        responseBodyStr = objectMapper.writeValueAsString(response.getBody());
                    } catch (Exception ex) {
                        responseBodyStr = String.valueOf(response.getBody());
                    }
                    // Remove newlines and carriage returns
                    responseBodyStr = responseBodyStr.replaceAll("[\\r\\n]", " ");
                    log.error(responseBodyStr);
                    throw new BusinessException(ContractException.CONTRACT_AI_SERVER_ERROR);
                }
                // AI 쪽에서 최종 값을 받기 (pdf)
                MultipartFile contracts = MultipartFileUtils.fromFile(tempFile);

                // 이거 step2로 바꿔야 함
                try {
                    // 임대인과 임차인의 생년월일 가져오기 (주민번호 앞자리 사용)
                    Long ownerId = contractMapper.getOwnerId(contractChatId);
                    Long buyerId = contractMapper.getBuyerId(contractChatId);

                    // 주민번호 앞자리(YYMMDD)를 직접 사용
                    String ownerKey = contractMapper.selectSsnFront(ownerId, contractChatId);
                    String buyerKey = contractMapper.selectSsnFront(buyerId, contractChatId);

                    if (ownerKey == null || buyerKey == null) {
                        log.error("SSN front not found - owner: {}, buyer: {}", ownerKey, buyerKey);
                        throw new BusinessException(ContractException.CONTRACT_INSERT);
                    }

                    // 두 생년월일을 조합한 암호화 키 생성
                    String combinedKey = ownerKey + "_" + buyerKey;

                    // 기존 PDF 파일이 있는지 확인하고 암호화
                    // 여기서는 Redis에서 가져온 계약서를 사용
                    // PDF 암호화 및 S3 업로드는 위에서 이미 처리됨
                    log.info("계약서 암호화 키 생성 완료 - contractChatId: {}", contractChatId);
                    // 1단계 업로드 실행 (반환 값을 사용하지 않으면 변수에 담지 않아도 됩니다)
                    // Convert MultipartFile to String (base64 or file path)
                    String contractData = Base64.getEncoder().encodeToString(contracts.getBytes());
                    encryptionService.uploadPdfStep1(contractData, dto.getContractPassword());

                    // S3에 저장하기
//                    MultipartFile multipartContract = MultipartFileUtils.fromFile(uploadStep2.getFile());
//                    String s3Keys = s3Service.uploadFile(multipartContract);

                    // final_contract에 값을 저장하기, DB 확인
//                    int update = contractMapper.updateFinalContract(contractChatId, s3Keys, uploadStep2.getOriginalHash());
//                    if (update != 1) throw new BusinessException(ContractException.CONTRACT_DB_UPDATE);
                } catch (Exception ex) {
                    // 업로드 과정의 예외를 비즈니스 예외로 변환
                    throw new BusinessException(ContractException.CONTRACT_INSERT, ex);
                }

            } catch (Exception e) {
                log.error(e.getMessage());
                throw new BusinessException(ContractException.CONTRACT_AI_SERVER_ERROR, e);
            }
            }

        return null;
    }

    @Override
    @Transactional
    public Void selectContractPDF(Long contractChatId, Long userId, HttpServletResponse response, FindContractDTO
            dto) throws Exception {
        // userId 인증
        validateUserId(contractChatId, userId);

        // 최종 계약서 PDF를 S3에서 가져온다
        FinalContract key = contractMapper.selectFinalContract(contractChatId);

        // 최종 계약서가 존재하는지 확인
        if (key == null || key.getContractPdfKey() == null) {
            log.error("최종 계약서가 존재하지 않습니다. contractChatId: {}", contractChatId);
            throw new IllegalStateException("최종 계약서가 존재하지 않습니다. 먼저 계약서를 완료해주세요.");
        }

        InputStream s3Contract = s3Service.downloadFile(key.getContractPdfKey());

        MultipartFile files = MultipartFileUtils.inputStreamToMultipartFile(s3Contract);

        // 임대인과 임차인의 생년월일을 조합한 키로 복호화 (주민번호 앞자리 사용)
        Long ownerId = contractMapper.getOwnerId(contractChatId);
        Long buyerId = contractMapper.getBuyerId(contractChatId);

        // 주민번호 앞자리(YYMMDD)를 직접 사용
        String ownerKey = contractMapper.selectSsnFront(ownerId, contractChatId);
        String buyerKey = contractMapper.selectSsnFront(buyerId, contractChatId);

        if (ownerKey == null || buyerKey == null) {
            log.error("SSN front not found for decryption - owner: {}, buyer: {}", ownerKey, buyerKey);
            throw new IllegalStateException("복호화를 위한 키 생성 실패: 주민번호 정보를 찾을 수 없습니다");
        }

        // 두 생년월일을 조합한 복호화 키
        String combinedKey = ownerKey + "_" + buyerKey;

        // PDF 복호화 하기 (조합된 키로 복호화)
        File finalContract = encryptionService.decryptPdf(files, combinedKey, key.getContractPdfHash());

        // 실제 파일명 -> 내가 원하는 파일명 넣어서 보내기
        String originalName = "contract_" + contractChatId + ".pdf";

        //        // 유틸로 응답 보내기
        UploadFiles.download(response, finalContract, originalName);

        return null;
    }

    @Override
    @Transactional
    public Void sendContractPDF(Long contractChatId, Long userId, FindContractDTO dto) throws Exception {
        // userId 인증
        validateUserId(contractChatId, userId);

        // 최종 계약서 PDF를 S3에서 가져온다
        FinalContract key = contractMapper.selectFinalContract(contractChatId);

        // 최종 계약서가 존재하는지 확인
        if (key == null || key.getContractPdfKey() == null) {
            log.error("최종 계약서가 존재하지 않습니다. contractChatId: {}", contractChatId);
            throw new IllegalStateException("최종 계약서가 존재하지 않습니다. 먼저 계약서를 완료해주세요.");
        }

        InputStream s3Contract = s3Service.downloadFile(key.getContractPdfKey());

        MultipartFile files = MultipartFileUtils.inputStreamToMultipartFile(s3Contract);

        // 임대인과 임차인의 생년월일을 조합한 키로 복호화 (주민번호 앞자리 사용)
        Long ownerId = contractMapper.getOwnerId(contractChatId);
        Long buyerId = contractMapper.getBuyerId(contractChatId);

        // 주민번호 앞자리(YYMMDD)를 직접 사용
        String ownerKey = contractMapper.selectSsnFront(ownerId, contractChatId);
        String buyerKey = contractMapper.selectSsnFront(buyerId, contractChatId);

        if (ownerKey == null || buyerKey == null) {
            log.error("SSN front not found for decryption - owner: {}, buyer: {}", ownerKey, buyerKey);
            throw new IllegalStateException("복호화를 위한 키 생성 실패: 주민번호 정보를 찾을 수 없습니다");
        }

        // 두 생년월일을 조합한 복호화 키
        String combinedKey = ownerKey + "_" + buyerKey;

        // PDF 복호화 하기 (조합된 키로 복호화)
        File finalContract = encryptionService.decryptPdf(files, combinedKey, key.getContractPdfHash());

        MultipartFile finalFile = MultipartFileUtils.fromFile(finalContract);

        // PDF에 사용자 개인의 생년월일로 비밀번호 걸어서 보내기 (주민번호 앞자리 사용)
        String userPassword = contractMapper.selectSsnFront(userId, contractChatId);
        if (userPassword == null) {
            // fallback to birth_date if SSN not found
            String userBirthDate = contractMapper.selectBirth(userId);
            if (userBirthDate != null) {
                userPassword = userBirthDate.replace("-", "").substring(2); // YYYY-MM-DD -> YYMMDD
            } else {
                log.error("No birth date or SSN found for user {}", userId);
                throw new IllegalStateException("사용자 생년월일 정보를 찾을 수 없습니다");
            }
        }
        FileWithHashDto pdfContract = encryptionService.addPasswordToPdf(finalFile, userPassword);

        // 이메일 주소 (요청된 이메일 또는 사용자 기본 이메일)
        String email = dto.getEmail() != null && !dto.getEmail().isEmpty()
            ? dto.getEmail()
            : contractMapper.selectMail(userId);

        String subject = "[ITZeep] 계약서 PDF를 보내드립니다.";
        String text = String.format(
            "요청하신 계약서를 보내드립니다.\n\n" +
            "PDF 열람 비밀번호: 귀하의 생년월일 6자리(YYMMDD)\n" +
            "예시: 1990년 1월 1일생 → 900101\n\n" +
            "문의사항이 있으시면 언제든 연락 주시기 바랍니다."
        );

        emailService.sendEmailWithAttachment(email, subject, text, pdfContract.getFile().getAbsolutePath());

//                  String pathFile = tempFile.getAbsolutePath();
////           변환된 파일을 이메일에 넣어서 보내기
//                  emailService.sendEmailWithAttachment(email,subject, text, pathFile);
////
////           파일 삭제하기
//                  if (tempFile.delete()) {
//                      log.info("임시 파일 삭제 성공: {}", tempFile.getAbsolutePath());
//                  } else {
//                      log.warn("임시 파일 삭제 실패: {}", tempFile.getAbsolutePath());
//                  }

        return null;
    }

    @Override
    public String getUserBirthDate(Long contractChatId, Long userId, String userRole) {
        log.info("Getting birth date for contractChatId: {}, userId: {}, role: {}", contractChatId, userId, userRole);

        // 먼저 주민번호 앞자리(생년월일)를 가져옴
        String ssnFront = contractMapper.selectSsnFront(userId, contractChatId);

        if (ssnFront != null && !ssnFront.isEmpty()) {
            // 주민번호 앞자리가 있으면 그대로 사용 (이미 YYMMDD 형식)
            log.info("Using SSN front for birth date: {}", ssnFront);
            return ssnFront;
        }

        // 주민번호가 없으면 user 테이블의 birth_date 사용
        log.info("SSN front not found, falling back to birth_date from user table");
        String birthDate = contractMapper.selectBirth(userId);

        if (birthDate == null || birthDate.isEmpty()) {
            log.error("Both SSN and birth date are null or empty for userId: {}", userId);
            // 기본값 반환 (임시)
            return "000000";
        }

        birthDate = birthDate.replace("-", "");

        // YYYYMMDD 형식인지 확인
        if (birthDate.length() < 8) {
            log.error("Invalid birth date format: {} for userId: {}", birthDate, userId);
            return "000000";
        }

        return birthDate.substring(2); // YYYY-MM-DD -> YYMMDD
    }

    @Override
    @Transactional
    public void saveFinalContractToDatabase(Long contractChatId, String s3Url, String pdfHash) {
        try {
            // final_contract 테이블에 저장 또는 업데이트 (INSERT ... ON DUPLICATE KEY UPDATE)
            int result = contractMapper.insertOrUpdateFinalContract(contractChatId, s3Url, pdfHash);
            if (result < 1) {
                log.error("Failed to save final contract to database - contractChatId: {}", contractChatId);
                throw new IllegalStateException("최종 계약서 저장에 실패했습니다");
            }
            log.info("Final contract saved to database - contractChatId: {}, s3Url: {}, hash: {}",
                    contractChatId, s3Url, pdfHash);
        } catch (Exception e) {
            log.error("Error saving final contract to database", e);
            throw new RuntimeException("최종 계약서 데이터베이스 저장 실패", e);
        }
    }

    // ===================================================

    // Userid 검증
    public void validateUserId (Long contractChatId, Long userId){

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
    @Override
    public byte[] generateContractWithSignatures(
            Long contractChatId,
            Long userId,
            java.util.List<String> ownerSignatures,
            java.util.List<String> buyerSignatures,
            boolean ownerHasTaxArrears,
            boolean ownerHasPriorFixedDate,
            boolean ownerMediationAgree,
            boolean buyerMediationAgree) {

        log.info("Generating contract with signatures for contractChatId: {}", contractChatId);

        // userId 인증
        validateUserId(contractChatId, userId);

        // MongoDB에서 계약 정보 조회
        ContractMongoDocument document = repository.getContract(contractChatId);
        if (document == null) {
            throw new BusinessException(ContractException.CONTRACT_NOT_FOUND);
        }

        // DB에서 필요한 정보 조회
        DBFinalContractDTO dbDTO = contractMapper.selectFinalContractPDF(contractChatId);

        // 복호화
        Long ownerContractId = contractMapper.getOwnerId(contractChatId);
        Long buyerContractId = contractMapper.getBuyerId(contractChatId);

        IdentityVerificationInfoVO ownerVO = identityVerificationService.getDecryptedVerificationInfo(contractChatId, ownerContractId);
        IdentityVerificationInfoVO buyerVO = identityVerificationService.getDecryptedVerificationInfo(contractChatId, buyerContractId);

        // ContractChat 정보 조회하여 homeId 가져오기
        ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
        BuildingDocumentVO buildingDocument = null;

        if (contractChat != null && contractChat.getHomeId() != null) {
            // homeId로 가장 최근의 위험도 체크 조회
            RiskCheckVO latestRiskCheck = fraudRiskMapper.selectLatestRiskCheckByHomeId(contractChat.getHomeId());

            if (latestRiskCheck != null) {
                // 위험도 체크 ID로 건축물대장 정보 조회
                buildingDocument = fraudRiskMapper.selectBuildingDocumentByRiskCheckId(latestRiskCheck.getRiskckId());
            }
        }

        // SaveFinalContractDTO 생성 (homeId 전달하여 주소 정보 보완)
        Long homeId = contractChat != null ? contractChat.getHomeId() : null;
        SaveFinalContractDTO dto = buildSaveFinalContractDTO(document, dbDTO, ownerVO, buyerVO, buildingDocument, homeId);

        // AI 서버에 PDF 생성 요청 (서명 포함)
        try {
            // DTO가 null이 아닌지 확인
            if (dto == null) {
                log.error("SaveFinalContractDTO가 null입니다.");
                throw new BusinessException(ContractException.PDF_GENERATION_FAILED);
            }

            // 체크박스 상태 설정
            dto.setHasTaxArrears(ownerHasTaxArrears);
            dto.setHasPriorFixedDate(ownerHasPriorFixedDate);

            // 실제 서명 이미지 설정 (이미 Base64로 변환되어 있음 - updateSignature에서 처리)
            log.info("Setting signatures from status data");
            
            if (ownerSignatures != null && !ownerSignatures.isEmpty()) {
                if (ownerSignatures.size() > 0 && ownerSignatures.get(0) != null) {
                    String ownerSign1 = ownerSignatures.get(0);
                    // 이미 순수 Base64 문자열이므로 그대로 설정
                    dto.setOwnerSign1Base64(ownerSign1);
                    log.info("Owner signature 1 set - length: {}", ownerSign1.length());
                }
                if (ownerSignatures.size() > 1 && ownerSignatures.get(1) != null) {
                    String ownerSign2 = ownerSignatures.get(1);
                    dto.setOwnerSign2Base64(ownerSign2);
                    log.info("Owner signature 2 set - length: {}", ownerSign2.length());
                }
                if (ownerSignatures.size() > 2 && ownerSignatures.get(2) != null) {
                    String ownerSign3 = ownerSignatures.get(2);
                    dto.setOwnerSign3Base64(ownerSign3);
                    log.info("Owner signature 3 set - length: {}", ownerSign3.length());
                }
            } else {
                dto.setOwnerSign1Base64("");
                dto.setOwnerSign2Base64("");
                dto.setOwnerSign3Base64("");
            }

            if (buyerSignatures != null && !buyerSignatures.isEmpty() && buyerSignatures.get(0) != null) {
                String buyerSign1 = buyerSignatures.get(0);
                log.info("Processing buyer signature - length: {}", buyerSign1.length());
                
                // 이미 순수 Base64 문자열이므로 그대로 설정
                dto.setBuyerSignBase64(buyerSign1);
                log.info("Buyer signature set - length: {}", buyerSign1.length());
                if (buyerSign1.length() > 50) {
                    log.info("Buyer signature preview: {}", buyerSign1.substring(0, 50) + "...");
                }
            } else {
                log.warn("No buyer signature provided - setting empty string");
                dto.setBuyerSignBase64("");
            }

            log.info("Signatures set - Owner: {}, Buyer: {}",
                    ownerSignatures != null ? ownerSignatures.size() : 0,
                    buyerSignatures != null ? buyerSignatures.size() : 0);

            // 서명 데이터가 실제로 있는지 확인
            log.info("Final DTO check before sending to AI server:");
            log.info("  - Owner Sign1 empty: {}, length: {}",
                    dto.getOwnerSign1Base64().isEmpty(),
                    dto.getOwnerSign1Base64().length());
            log.info("  - Buyer Sign1 empty: {}, length: {}",
                    dto.getBuyerSignBase64().isEmpty(),
                    dto.getBuyerSignBase64().length());

            // 임차인 서명이 정말 설정되었는지 최종 확인
            if (dto.getBuyerSignBase64() != null && !dto.getBuyerSignBase64().isEmpty()) {
                log.info("✓ Buyer signature is SET and will be sent to AI server");
                log.info("  Buyer signature data starts with: {}",
                        dto.getBuyerSignBase64().substring(0, Math.min(30, dto.getBuyerSignBase64().length())));
            } else {
                log.error("✗ Buyer signature is NULL or EMPTY - AI server will NOT receive buyer signature!");
            }

            // JSON으로 전송
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/pdf");

            HttpEntity<SaveFinalContractDTO> request = new HttpEntity<>(dto, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    aiServerUrl + "/api/contract/generate-json",
                    HttpMethod.POST,
                    request,
                    byte[].class);

            if (response.getStatusCode() == HttpStatus.OK) {
                byte[] pdfBytes = response.getBody();
                log.info("AI 서버에서 서명 포함 PDF 생성 성공. 크기: {} bytes",
                        pdfBytes != null ? pdfBytes.length : 0);
                return pdfBytes;
            } else {
                log.error("AI 서버 PDF 생성 실패. HTTP 상태: {}", response.getStatusCode());
                throw new BusinessException(ContractException.PDF_GENERATION_FAILED);
            }

        } catch (Exception e) {
            log.error("AI 서버 PDF 생성 중 오류 발생", e);
            throw new BusinessException(ContractException.PDF_GENERATION_FAILED, e);
        }
    }

    @Override
    public byte[] getExistingContractPdf(Long contractChatId) {
        try {
            log.info("Attempting to retrieve existing contract PDF for contractChatId: {}", contractChatId);

            // MongoDB에서 계약서 조회
            ContractMongoDocument document = repository.getContract(contractChatId);
            if (document == null) {
                log.warn("No contract found in MongoDB for contractChatId: {}", contractChatId);
                return null;
            }

            // 간단한 PDF 생성 (기본 계약서 템플릿)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(baos);
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdfDoc);

            // 계약서 기본 내용 추가
            doc.add(new com.itextpdf.layout.element.Paragraph("부동산 임대차 계약서")
                    .setFontSize(18)
                    .setBold());
            doc.add(new com.itextpdf.layout.element.Paragraph("계약 ID: " + contractChatId));

            // MongoDB 문서에서 데이터 추출하여 추가
            if (document.getOwnerName() != null) {
                doc.add(new com.itextpdf.layout.element.Paragraph("임대인: " + document.getOwnerName()));
            }
            if (document.getBuyerName() != null) {
                doc.add(new com.itextpdf.layout.element.Paragraph("임차인: " + document.getBuyerName()));
            }
            if (document.getHomeAddr1() != null) {
                doc.add(new com.itextpdf.layout.element.Paragraph("주소: " + document.getHomeAddr1() + " " +
                    (document.getHomeAddr2() != null ? document.getHomeAddr2() : "")));
            }

            doc.close();

            byte[] pdfBytes = baos.toByteArray();
            log.info("Generated existing contract PDF with size: {} bytes", pdfBytes.length);
            return pdfBytes;

        } catch (Exception e) {
            log.error("Failed to retrieve or generate existing contract PDF", e);
            return null;
        }
    }
}
