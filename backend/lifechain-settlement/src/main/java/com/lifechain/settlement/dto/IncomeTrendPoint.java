package com.lifechain.settlement.dto;

import lombok.Data;
import java.time.LocalDate;

/** 每日已确认净收益，金额单位为分。 */
@Data
public class IncomeTrendPoint {
    private LocalDate date;
    private Long amount;
}
