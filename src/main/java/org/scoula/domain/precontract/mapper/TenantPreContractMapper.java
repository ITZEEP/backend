package org.scoula.domain.precontract.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.scoula.domain.precontract.vo.TenantJeonseInfoVO;
import org.scoula.domain.precontract.vo.TenantPreContractCheckVO;
import org.scoula.domain.precontract.vo.TenantWolseInfoVO;

@Mapper
public interface TenantPreContractMapper {
      int insertJeonseInfo(TenantJeonseInfoVO jeonseInfo);

      int insertWolseInfo(TenantWolseInfoVO wolseInfo);

      int insertPreContractCheck(TenantPreContractCheckVO preContractCheck);
}
