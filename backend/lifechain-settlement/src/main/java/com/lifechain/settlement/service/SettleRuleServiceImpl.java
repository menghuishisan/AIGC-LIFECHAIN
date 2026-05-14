package com.lifechain.settlement.service;

import com.lifechain.auth.entity.AccountEntity;
import com.lifechain.auth.mapper.AccountMapper;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.settlement.dto.BindSettleRuleRequest;
import com.lifechain.settlement.dto.WorkSettleRuleVO;
import com.lifechain.settlement.entity.SettleTemplateEntity;
import com.lifechain.settlement.entity.WorkSettleRuleEntity;
import com.lifechain.settlement.mapper.SettleTemplateMapper;
import com.lifechain.settlement.mapper.WorkSettleRuleMapper;
import com.lifechain.work.entity.WorkEntity;
import com.lifechain.work.mapper.WorkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算规则服务实现
 * <p>
 * 实现作品与分账规则的绑定管理。绑定时校验模板有效性和比例合规性，
 * 并将同一作品的旧规则置为失效，确保同一时间仅有一条生效规则。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettleRuleServiceImpl implements SettleRuleService {

    private final WorkSettleRuleMapper ruleMapper;
    private final SettleTemplateMapper templateMapper;
    private final AccountMapper accountMapper;
    private final WorkMapper workMapper;

    /**
     * {@inheritDoc}
     * <p>
     * 业务规则：
     * <ol>
     *   <li>若指定模板编码，模板必须存在且为 ACTIVE 状态</li>
     *   <li>平台比例 + 创作者比例必须等于1</li>
     *   <li>同一作品已有的 ACTIVE 规则自动置为 INACTIVE</li>
     *   <li>新规则立即生效，effective_time = 当前UTC时间</li>
     * </ol>
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkSettleRuleVO bindRule(BindSettleRuleRequest request) {
        log.info("绑定结算规则，workNo={}, templateCode={}", request.getWorkNo(), request.getTemplateCode());

        // 校验各比例不为负数
        if (request.getPlatformRatio().compareTo(BigDecimal.ZERO) < 0
                || request.getCreatorRatio().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "比例不能为负数");
        }

        // 校验比例之和
        BigDecimal totalRatio = request.getPlatformRatio().add(request.getCreatorRatio());
        if (totalRatio.compareTo(BigDecimal.ONE) != 0) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID,
                    "平台比例与创作者比例之和必须等于1，当前为" + totalRatio);
        }

        // 若指定了模板编码，校验模板存在且有效
        Long templateId = null;
        if (request.getTemplateCode() != null && !request.getTemplateCode().isBlank()) {
            SettleTemplateEntity template = templateMapper.selectByTemplateCode(request.getTemplateCode());
            if (template == null) {
                throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "结算模板不存在");
            }
            if (!"ACTIVE".equals(template.getStatus())) {
                throw new BizException(ErrorCodeEnum.STATUS_INVALID, "结算模板未生效");
            }
            templateId = template.getId();
        }

        // 校验作品存在性
        WorkEntity work = workMapper.selectByWorkNo(request.getWorkNo());
        if (work == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "作品不存在: " + request.getWorkNo());
        }

        // 解析创作者accountNo → accountId
        AccountEntity creatorAccount = accountMapper.selectByAccountNo(request.getCreatorAccountNo());
        if (creatorAccount == null) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "创作者账户不存在: " + request.getCreatorAccountNo());
        }

        // 校验创作者与作品归属一致
        if (!work.getCreatorAccountId().equals(creatorAccount.getId())) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "创作者账户与作品归属不一致");
        }

        // 将同一作品的旧规则置为失效
        WorkSettleRuleEntity existingRule = ruleMapper.selectEffectiveRuleByWorkNo(request.getWorkNo());
        if (existingRule != null) {
            existingRule.setStatus("INACTIVE");
            existingRule.setUpdatedAt(DateTimeUtil.nowUtc());
            ruleMapper.updateById(existingRule);
            log.info("作品旧规则已失效，workNo={}, ruleId={}", request.getWorkNo(), existingRule.getId());
        }

        // 创建新规则
        LocalDateTime now = DateTimeUtil.nowUtc();
        WorkSettleRuleEntity rule = new WorkSettleRuleEntity();
        rule.setWorkId(work.getId());
        rule.setWorkNo(request.getWorkNo());
        rule.setTemplateId(templateId);
        rule.setCreatorAccountId(creatorAccount.getId());
        rule.setCreatorRatio(request.getCreatorRatio());
        rule.setPlatformRatio(request.getPlatformRatio());
        rule.setEffectiveTime(now);
        rule.setStatus("ACTIVE");
        rule.setCreatedAt(now);
        rule.setUpdatedAt(now);
        ruleMapper.insert(rule);

        log.info("结算规则绑定成功，workNo={}, ruleId={}", request.getWorkNo(), rule.getId());

        return buildRuleVO(rule);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkSettleRuleVO getEffectiveRule(String workNo) {
        WorkSettleRuleEntity rule = ruleMapper.selectEffectiveRuleByWorkNo(workNo);
        if (rule == null) {
            return null;
        }
        return buildRuleVO(rule);
    }

    /**
     * 构建规则视图对象
     *
     * @param rule 规则实体
     * @return 规则视图对象
     */
    private WorkSettleRuleVO buildRuleVO(WorkSettleRuleEntity rule) {
        WorkSettleRuleVO vo = new WorkSettleRuleVO();
        vo.setWorkNo(rule.getWorkNo());
        if (rule.getTemplateId() != null) {
            SettleTemplateEntity template = templateMapper.selectById(rule.getTemplateId());
            vo.setTemplateCode(template != null ? template.getTemplateCode() : null);
        }
        vo.setPlatformRatio(rule.getPlatformRatio());
        vo.setCreatorRatio(rule.getCreatorRatio());
        vo.setEffectiveTime(rule.getEffectiveTime());
        vo.setRuleStatus(rule.getStatus());
        return vo;
    }
}
