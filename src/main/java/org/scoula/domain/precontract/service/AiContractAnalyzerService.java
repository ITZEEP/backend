package org.scoula.domain.precontract.service;

import org.scoula.domain.precontract.dto.ai.ContractParseResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface AiContractAnalyzerService {

      /**
       * 계약서 특약 OCR 분석 요청
       *
       * @param file 계약서 PDF 파일
       * @return OCR 분석 결과
       */
      ContractParseResponseDto parseContractDocument(MultipartFile file);
}
