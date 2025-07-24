package org.scoula.domain.precontract.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.scoula.domain.precontract.vo.TenantJeonseInfo;
import org.scoula.domain.precontract.vo.TenantPreContractCheck;
import org.scoula.domain.precontract.vo.TenantWolseInfo;

@Mapper
public interface TenantPreContractMapper {
      int insertJeonseInfo(TenantJeonseInfo jeonseInfo);

      int insertWolseInfo(TenantWolseInfo wolseInfo);

      int insertPreContractCheck(TenantPreContractCheck preContractCheck);
}
