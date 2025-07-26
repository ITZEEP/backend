package org.scoula.domain.precontract.service;

import org.scoula.domain.precontract.dto.TenantPreContractDTO;

public interface PreContractService {

      /**
       * 임차인의 계약 전 사전조사 정보를 저장합니다.
       *
       * @param request 임차인의 계약 전 정보 객체
       * @return 데이터베이스에 등록된 임차인의 계약 전 정보 객체
       */
      TenantPreContractDTO saveTenantInfo(TenantPreContractDTO request);
}
