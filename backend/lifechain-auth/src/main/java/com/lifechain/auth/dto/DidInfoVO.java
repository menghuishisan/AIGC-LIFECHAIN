package com.lifechain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DID信息视图对象
 * <p>
 * 展示数字身份的基本信息，包括DID编号、DID值、当前状态、
 * 链上状态及关键时间节点。
 * </p>
 *
 * @author LifeChain
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DidInfoVO implements Serializable {

    /** DID编号 */
    private String didNo;

    /** DID标识值 */
    private String didValue;

    /** DID状态 */
    private String status;

    /** 链上状态 */
    private String chainStatus;

    /** 激活时间 */
    private String activeTime;

    /** 申请时间 */
    private String applyTime;
}
