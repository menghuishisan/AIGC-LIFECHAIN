package com.lifechain.settlement.service;

import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.util.BizNoUtil;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.settlement.dto.CreateSettleTemplateRequest;
import com.lifechain.settlement.dto.SettleTemplateVO;
import com.lifechain.settlement.dto.TemplateItemDTO;
import com.lifechain.settlement.entity.SettleTemplateEntity;
import com.lifechain.settlement.entity.SettleTemplateItemEntity;
import com.lifechain.settlement.mapper.SettleTemplateItemMapper;
import com.lifechain.settlement.mapper.SettleTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 结算模板服务实现
 * <p>
 * 实现结算模板的创建、查询和列表功能。
 * 创建模板时校验明细比例之和必须等于1，保证分账完整性。
 * 模板编码通过 {@link BizNoUtil} 自动生成，确保全局唯一。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettleTemplateServiceImpl implements SettleTemplateService {

    private final SettleTemplateMapper templateMapper;
    private final SettleTemplateItemMapper templateItemMapper;

    /**
     * {@inheritDoc}
     * <p>
     * 业务规则：
     * <ol>
     *   <li>明细列表不能为空</li>
     *   <li>所有明细的比例之和必须等于1.0000</li>
     *   <li>自动生成模板编码，初始状态为 ACTIVE</li>
     *   <li>模板与明细在同一事务中创建</li>
     * </ol>
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettleTemplateVO createTemplate(CreateSettleTemplateRequest request) {
        log.info("创建结算模板，templateName={}", request.getTemplateName());

        // 校验比例之和等于1
        BigDecimal totalRatio = request.getItems().stream()
                .map(TemplateItemDTO::getRatio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalRatio.compareTo(BigDecimal.ONE) != 0) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID,
                    "模板明细比例之和必须等于1，当前为" + totalRatio);
        }

        LocalDateTime now = DateTimeUtil.nowUtc();

        // 创建模板
        SettleTemplateEntity template = new SettleTemplateEntity();
        template.setTemplateCode(BizNoUtil.generate("TPL"));
        template.setTemplateName(request.getTemplateName());
        template.setDescription(request.getDescription());
        template.setStatus("ACTIVE");
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        templateMapper.insert(template);

        // 创建明细
        List<TemplateItemDTO> itemDTOs = new ArrayList<>();
        for (TemplateItemDTO itemDTO : request.getItems()) {
            SettleTemplateItemEntity item = new SettleTemplateItemEntity();
            item.setTemplateId(template.getId());
            item.setRoleType(itemDTO.getRoleType());
            item.setRatio(itemDTO.getRatio());
            item.setDescription(itemDTO.getDescription());
            item.setCreatedAt(now);
            item.setUpdatedAt(now);
            templateItemMapper.insert(item);
            itemDTOs.add(itemDTO);
        }

        log.info("结算模板创建成功，templateCode={}", template.getTemplateCode());

        // 构建返回
        SettleTemplateVO vo = new SettleTemplateVO();
        vo.setTemplateCode(template.getTemplateCode());
        vo.setTemplateName(template.getTemplateName());
        vo.setDescription(template.getDescription());
        vo.setStatus(template.getStatus());
        vo.setItems(itemDTOs);
        vo.setCreatedAt(template.getCreatedAt());
        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettleTemplateVO getTemplate(String templateCode) {
        SettleTemplateEntity template = templateMapper.selectByTemplateCode(templateCode);
        if (template == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "结算模板不存在");
        }
        return buildTemplateVO(template);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SettleTemplateVO> listTemplates() {
        List<SettleTemplateEntity> templates = templateMapper.selectActiveTemplates();
        return templates.stream()
                .map(this::buildTemplateVO)
                .collect(Collectors.toList());
    }

    /**
     * 构建模板视图对象
     *
     * @param template 模板实体
     * @return 模板视图对象
     */
    private SettleTemplateVO buildTemplateVO(SettleTemplateEntity template) {
        SettleTemplateVO vo = new SettleTemplateVO();
        vo.setTemplateCode(template.getTemplateCode());
        vo.setTemplateName(template.getTemplateName());
        vo.setDescription(template.getDescription());
        vo.setStatus(template.getStatus());
        vo.setCreatedAt(template.getCreatedAt());

        List<SettleTemplateItemEntity> items = templateItemMapper.selectByTemplateId(template.getId());
        List<TemplateItemDTO> itemDTOs = items.stream().map(item -> {
            TemplateItemDTO dto = new TemplateItemDTO();
            dto.setRoleType(item.getRoleType());
            dto.setRatio(item.getRatio());
            dto.setDescription(item.getDescription());
            return dto;
        }).collect(Collectors.toList());
        vo.setItems(itemDTOs);

        return vo;
    }
}
