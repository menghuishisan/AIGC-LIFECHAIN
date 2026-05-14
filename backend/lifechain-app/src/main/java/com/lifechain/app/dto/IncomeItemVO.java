package com.lifechain.app.dto;

import com.lifechain.settlement.entity.SettlementItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收益明细视图对象
 */
@Data
public class IncomeItemVO {
    private String settleNo;
    private String roleType;
    private BigDecimal ratio;
    private Long amount;
    private String status;
    private LocalDateTime createdAt;

    public static IncomeItemVO fromEntity(SettlementItemEntity entity) {
        IncomeItemVO vo = new IncomeItemVO();
        vo.setSettleNo(entity.getSettleNo());
        vo.setRoleType(entity.getRoleType());
        vo.setRatio(entity.getRatio());
        vo.setAmount(entity.getAmount());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
