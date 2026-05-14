package com.lifechain.settlement.service;

import com.lifechain.settlement.dto.CreateSettleTemplateRequest;
import com.lifechain.settlement.dto.SettleTemplateVO;

import java.util.List;

/**
 * 结算模板服务接口
 * <p>
 * 管理分账结算模板的创建、查询和列表等操作，
 * 模板定义了各角色在分账中的比例分配方案。
 * </p>
 *
 * @author LifeChain
 */
public interface SettleTemplateService {

    /**
     * 创建结算模板
     * <p>
     * 创建一套新的分账模板及其明细，模板编码自动生成。
     * 明细中各角色的比例之和必须等于1。
     * </p>
     *
     * @param request 创建模板请求
     * @return 模板视图对象
     */
    SettleTemplateVO createTemplate(CreateSettleTemplateRequest request);

    /**
     * 根据模板编码查询模板详情
     *
     * @param templateCode 模板编码
     * @return 模板视图对象
     */
    SettleTemplateVO getTemplate(String templateCode);

    /**
     * 查询所有生效中的模板列表
     *
     * @return 模板视图列表
     */
    List<SettleTemplateVO> listTemplates();
}
