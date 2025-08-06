package org.scoula.domain.precontract.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerJeonseInfoVO {
      private Long ownerJeonseId;
      private Long contractChatId;
      private Boolean allowJeonseRightRegistration;
}
