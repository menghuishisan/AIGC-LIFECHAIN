package com.lifechain.app.service;

import com.lifechain.auth.entity.AccountEntity;
import com.lifechain.auth.mapper.AccountMapper;
import com.lifechain.common.enums.*;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.regulator.service.FreezeTargetService;
import com.lifechain.trade.entity.LicenseRecordEntity;
import com.lifechain.trade.entity.TradeOrderEntity;
import com.lifechain.trade.mapper.LicenseRecordMapper;
import com.lifechain.trade.mapper.TradeOrderMapper;
import com.lifechain.work.entity.WorkEntity;
import com.lifechain.work.mapper.WorkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 冻结目标联动服务实现
 * <p>
 * 冻结/解冻操作时联动推进目标业务对象（账户、作品、订单、授权）的状态。
 * 放在 lifechain-app 模块，因为需要跨模块访问多个业务实体的 Mapper。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FreezeTargetServiceImpl implements FreezeTargetService {

    private final AccountMapper accountMapper;
    private final WorkMapper workMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final LicenseRecordMapper licenseRecordMapper;

    @Override
    public void freezeTarget(String targetType, String targetNo, String freezeNo) {
        TargetTypeEnum type = TargetTypeEnum.fromCode(targetType);
        switch (type) {
            case ACCOUNT -> freezeAccount(targetNo, freezeNo);
            case WORK -> freezeWork(targetNo, freezeNo);
            case ORDER -> freezeOrder(targetNo, freezeNo);
            case LICENSE -> freezeLicense(targetNo, freezeNo);
        }
    }

    @Override
    public void unfreezeTarget(String targetType, String targetNo, String previousStatus, String freezeNo) {
        TargetTypeEnum type = TargetTypeEnum.fromCode(targetType);
        switch (type) {
            case ACCOUNT -> unfreezeAccount(targetNo, previousStatus, freezeNo);
            case WORK -> unfreezeWork(targetNo, previousStatus, freezeNo);
            case ORDER -> unfreezeOrder(targetNo, previousStatus, freezeNo);
            case LICENSE -> unfreezeLicense(targetNo, previousStatus, freezeNo);
        }
    }

    @Override
    public String captureCurrentStatus(String targetType, String targetNo) {
        TargetTypeEnum type = TargetTypeEnum.fromCode(targetType);
        return switch (type) {
            case ACCOUNT -> {
                AccountEntity account = accountMapper.selectByAccountNo(targetNo);
                yield account != null ? account.getStatus() : null;
            }
            case WORK -> {
                WorkEntity work = workMapper.selectByWorkNo(targetNo);
                yield work != null ? work.getStatus() : null;
            }
            case ORDER -> {
                TradeOrderEntity order = tradeOrderMapper.selectByOrderNo(targetNo);
                yield order != null ? order.getOrderStatus() : null;
            }
            case LICENSE -> {
                LicenseRecordEntity license = licenseRecordMapper.selectByLicenseNo(targetNo);
                yield license != null ? license.getLicenseStatus() : null;
            }
        };
    }

    @Override
    public Long resolveTargetId(String targetType, String targetNo) {
        TargetTypeEnum type = TargetTypeEnum.fromCode(targetType);
        return switch (type) {
            case ACCOUNT -> {
                AccountEntity a = accountMapper.selectByAccountNo(targetNo);
                yield a != null ? a.getId() : null;
            }
            case WORK -> {
                WorkEntity w = workMapper.selectByWorkNo(targetNo);
                yield w != null ? w.getId() : null;
            }
            case ORDER -> {
                TradeOrderEntity o = tradeOrderMapper.selectByOrderNo(targetNo);
                yield o != null ? o.getId() : null;
            }
            case LICENSE -> {
                LicenseRecordEntity l = licenseRecordMapper.selectByLicenseNo(targetNo);
                yield l != null ? l.getId() : null;
            }
        };
    }

    @Override
    public Long resolveTargetAccountId(String targetType, String targetNo) {
        TargetTypeEnum type = TargetTypeEnum.fromCode(targetType);
        return switch (type) {
            case ACCOUNT -> {
                AccountEntity account = accountMapper.selectByAccountNo(targetNo);
                yield account != null ? account.getId() : null;
            }
            case WORK -> {
                WorkEntity work = workMapper.selectByWorkNo(targetNo);
                yield work != null ? work.getCreatorAccountId() : null;
            }
            case ORDER -> {
                TradeOrderEntity order = tradeOrderMapper.selectByOrderNo(targetNo);
                yield order != null ? order.getBuyerAccountId() : null;
            }
            case LICENSE -> {
                LicenseRecordEntity license = licenseRecordMapper.selectByLicenseNo(targetNo);
                yield license != null ? license.getLicenseeAccountId() : null;
            }
        };
    }

    private void freezeAccount(String targetNo, String freezeNo) {
        AccountEntity account = accountMapper.selectByAccountNo(targetNo);
        if (account == null) {
            log.warn("冻结目标账户不存在，targetNo={}, freezeNo={}", targetNo, freezeNo);
            return;
        }
        if (!AccountStatusEnum.ACCOUNT_FROZEN.getCode().equals(account.getStatus())) {
            account.setPreviousStatus(account.getStatus());
        }
        account.setStatus(AccountStatusEnum.ACCOUNT_FROZEN.getCode());
        account.setUpdatedAt(DateTimeUtil.nowUtc());
        accountMapper.updateById(account);
        log.info("账户已冻结，accountNo={}, freezeNo={}", targetNo, freezeNo);
    }

    private void unfreezeAccount(String targetNo, String previousStatus, String freezeNo) {
        AccountEntity account = accountMapper.selectByAccountNo(targetNo);
        if (account == null) {
            log.warn("解冻目标账户不存在，targetNo={}, freezeNo={}", targetNo, freezeNo);
            return;
        }
        if (!AccountStatusEnum.ACCOUNT_FROZEN.getCode().equals(account.getStatus())) {
            log.warn("账户当前状态非冻结，跳过解冻，accountNo={}, status={}", targetNo, account.getStatus());
            return;
        }
        String restoreStatus = firstNonBlank(account.getPreviousStatus(), previousStatus, AccountStatusEnum.AUTH_APPROVED.getCode());
        account.setStatus(restoreStatus);
        account.setPreviousStatus(null);
        account.setUpdatedAt(DateTimeUtil.nowUtc());
        accountMapper.updateById(account);
        log.info("账户已解冻，accountNo={}, freezeNo={}, restoreStatus={}", targetNo, freezeNo, restoreStatus);
    }

    private void freezeWork(String targetNo, String freezeNo) {
        WorkEntity work = workMapper.selectByWorkNo(targetNo);
        if (work == null) {
            log.warn("冻结目标作品不存在，targetNo={}, freezeNo={}", targetNo, freezeNo);
            return;
        }
        work.setStatus(WorkStatusEnum.RISK_FROZEN.getCode());
        work.setUpdatedAt(DateTimeUtil.nowUtc());
        workMapper.updateById(work);
        log.info("作品已冻结，workNo={}, freezeNo={}", targetNo, freezeNo);
    }

    private void unfreezeWork(String targetNo, String previousStatus, String freezeNo) {
        WorkEntity work = workMapper.selectByWorkNo(targetNo);
        if (work == null) {
            log.warn("解冻目标作品不存在，targetNo={}, freezeNo={}", targetNo, freezeNo);
            return;
        }
        if (!WorkStatusEnum.RISK_FROZEN.getCode().equals(work.getStatus())) {
            log.warn("作品当前状态非冻结，跳过解冻，workNo={}, status={}", targetNo, work.getStatus());
            return;
        }
        work.setStatus(firstNonBlank(previousStatus, WorkStatusEnum.OWNERSHIP_CONFIRMED.getCode()));
        work.setUpdatedAt(DateTimeUtil.nowUtc());
        workMapper.updateById(work);
        log.info("作品已解冻，workNo={}, freezeNo={}, restoreStatus={}", targetNo, freezeNo, work.getStatus());
    }

    private void freezeOrder(String targetNo, String freezeNo) {
        TradeOrderEntity order = tradeOrderMapper.selectByOrderNo(targetNo);
        if (order == null) {
            log.warn("冻结目标订单不存在，targetNo={}, freezeNo={}", targetNo, freezeNo);
            return;
        }
        order.setOrderStatus(OrderStatusEnum.ORDER_FROZEN.getCode());
        order.setUpdatedAt(DateTimeUtil.nowUtc());
        tradeOrderMapper.updateById(order);
        log.info("订单已冻结，orderNo={}, freezeNo={}", targetNo, freezeNo);
    }

    private void unfreezeOrder(String targetNo, String previousStatus, String freezeNo) {
        TradeOrderEntity order = tradeOrderMapper.selectByOrderNo(targetNo);
        if (order == null) {
            log.warn("解冻目标订单不存在，targetNo={}, freezeNo={}", targetNo, freezeNo);
            return;
        }
        if (!OrderStatusEnum.ORDER_FROZEN.getCode().equals(order.getOrderStatus())) {
            log.warn("订单当前状态非冻结，跳过解冻，orderNo={}, status={}", targetNo, order.getOrderStatus());
            return;
        }
        order.setOrderStatus(firstNonBlank(previousStatus, OrderStatusEnum.ORDER_EXCEPTION.getCode()));
        order.setUpdatedAt(DateTimeUtil.nowUtc());
        tradeOrderMapper.updateById(order);
        log.info("订单已解冻，orderNo={}, freezeNo={}, restoreStatus={}", targetNo, freezeNo, order.getOrderStatus());
    }

    private void freezeLicense(String targetNo, String freezeNo) {
        LicenseRecordEntity license = licenseRecordMapper.selectByLicenseNo(targetNo);
        if (license == null) {
            log.warn("冻结目标授权不存在，targetNo={}, freezeNo={}", targetNo, freezeNo);
            return;
        }
        license.setLicenseStatus(LicenseStatusEnum.LICENSE_FROZEN.getCode());
        license.setUpdatedAt(DateTimeUtil.nowUtc());
        licenseRecordMapper.updateById(license);
        log.info("授权已冻结，licenseNo={}, freezeNo={}", targetNo, freezeNo);
    }

    private void unfreezeLicense(String targetNo, String previousStatus, String freezeNo) {
        LicenseRecordEntity license = licenseRecordMapper.selectByLicenseNo(targetNo);
        if (license == null) {
            log.warn("解冻目标授权不存在，targetNo={}, freezeNo={}", targetNo, freezeNo);
            return;
        }
        if (!LicenseStatusEnum.LICENSE_FROZEN.getCode().equals(license.getLicenseStatus())) {
            log.warn("授权当前状态非冻结，跳过解冻，licenseNo={}, status={}", targetNo, license.getLicenseStatus());
            return;
        }
        license.setLicenseStatus(firstNonBlank(previousStatus, LicenseStatusEnum.LICENSE_ACTIVE.getCode()));
        license.setUpdatedAt(DateTimeUtil.nowUtc());
        licenseRecordMapper.updateById(license);
        log.info("授权已解冻，licenseNo={}, freezeNo={}, restoreStatus={}", targetNo, freezeNo, license.getLicenseStatus());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
