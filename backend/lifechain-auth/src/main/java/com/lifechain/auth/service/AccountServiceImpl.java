package com.lifechain.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.auth.audit.AuditService;
import com.lifechain.auth.dto.*;
import com.lifechain.auth.entity.*;
import com.lifechain.auth.mapper.*;
import com.lifechain.common.enums.*;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.common.util.BizNoUtil;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.auth.assembler.AccountVoAssembler;
import com.lifechain.common.util.MaskUtil;
import com.lifechain.infra.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 账户服务实现
 * <p>
 * 实现用户注册、登录、资料管理、实名认证提交及审核、账户冻结/解冻等核心业务逻辑。
 * 所有状态变更均写入状态变更历史，关键操作写入审计日志。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;
    private final SubjectProfileMapper subjectProfileMapper;
    private final SubjectAuthRecordMapper subjectAuthRecordMapper;
    private final AccountRoleMapper accountRoleMapper;
    private final DidRecordMapper didRecordMapper;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final RedisService redisService;

    /** 登录失败限流：5分钟内最多10次 */
    private static final int LOGIN_MAX_ATTEMPTS = 10;
    private static final int LOGIN_WINDOW_SECONDS = 300;
    private static final String LOGIN_RATE_KEY_PREFIX = "login:rate:";

    /** 短信验证码：6位数字，5分钟有效 */
    private static final String SMS_CODE_KEY_PREFIX = "sms:code:";
    private static final int SMS_CODE_TTL_SECONDS = 300;
    private static final int SMS_RATE_LIMIT_SECONDS = 60;
    private static final String SMS_RATE_KEY_PREFIX = "sms:rate:";
    private static final String SMS_IP_RATE_KEY_PREFIX = "sms:ip:rate:";
    private static final int SMS_IP_RATE_LIMIT = 10;
    private static final int SMS_IP_RATE_LIMIT_SECONDS = 3600;

    /** Refresh Token：Redis存储，7天有效 */
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:token:";
    private static final long REFRESH_TOKEN_TTL_SECONDS = 7L * 24 * 3600;

    /** Token 黑名单前缀 */
    private static final String TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:";

    private static final Random SMS_CODE_RANDOM = new SecureRandom();

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${lifechain.sms.dev-mode}")
    private boolean smsDevMode;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        log.info("用户注册，手机号={}", MaskUtil.maskMobile(request.getMobile()));

        // 校验短信验证码
        String smsKey = SMS_CODE_KEY_PREFIX + request.getMobile();
        String storedCode = redisService.getString(smsKey);
        if (storedCode == null) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "验证码已过期，请重新获取");
        }
        if (!storedCode.equals(request.getSmsCode())) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "验证码错误");
        }
        // 验证通过后立即删除，防止重放
        redisService.delete(smsKey);

        // 校验手机号唯一性
        AccountEntity existing = accountMapper.selectByMobile(request.getMobile());
        if (existing != null) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "该手机号已注册");
        }

        // 校验账户类型合法性
        AccountTypeEnum accountType = AccountTypeEnum.fromCode(request.getAccountType());

        // 普通注册只允许 PERSONAL / ENTERPRISE，不得注册 PLATFORM / REGULATOR
        if (AccountTypeEnum.PLATFORM == accountType || AccountTypeEnum.REGULATOR == accountType) {
            throw new BizException(ErrorCodeEnum.FORBIDDEN, "普通注册不允许创建平台或监管账户");
        }

        // 创建账户
        LocalDateTime now = DateTimeUtil.nowUtc();
        AccountEntity account = new AccountEntity();
        account.setAccountNo(BizNoUtil.accountNo());
        account.setMobile(request.getMobile());
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setAccountType(request.getAccountType());
        account.setStatus(AccountStatusEnum.REGISTERED.getCode());
        account.setAuthStatus("");
        account.setNickname(request.getNickname());
        accountMapper.insert(account);

        log.info("账户创建成功，accountNo={}, accountId={}", account.getAccountNo(), account.getId());

        // 创建默认BUYER角色
        AccountRoleEntity role = new AccountRoleEntity();
        role.setAccountId(account.getId());
        role.setRoleCode(RoleEnum.BUYER.getCode());
        role.setStatus("ACTIVE");
        role.setGrantedTime(now);
        accountRoleMapper.insert(role);

        // 写入状态变更历史
        auditService.writeStatusHistory(
                BizTypeEnum.ACCOUNT.getCode(), account.getId(), account.getAccountNo(),
                "", AccountStatusEnum.REGISTERED.getCode(),
                "用户注册", null, account.getId());

        // 写入审计日志
        auditService.writeAuditLog(
                TargetTypeEnum.ACCOUNT.getCode(), account.getId(), account.getAccountNo(),
                "REGISTER", "用户注册，手机号=" + MaskUtil.maskMobile(request.getMobile()),
                account.getId(), RoleEnum.BUYER.getCode(), null,
                "SUCCESS", null);

        // 生成 Access Token + Refresh Token
        List<String> roles = List.of(RoleEnum.BUYER.getCode());
        String accessToken = jwtService.generateToken(account.getId(), account.getAccountNo(),
                account.getAccountType(), roles);
        String refreshToken = generateAndStoreRefreshToken(account.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpireSeconds())
                .accountNo(account.getAccountNo())
                .nickname(account.getNickname())
                .accountType(account.getAccountType())
                .roles(roles)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request) {
        log.info("用户登录，手机号={}", MaskUtil.maskMobile(request.getMobile()));

        // 登录频率限制：5分钟内最多10次尝试
        String rateKey = LOGIN_RATE_KEY_PREFIX + request.getMobile();
        Long attempts = redisService.increment(rateKey);
        if (attempts == 1) {
            redisService.expire(rateKey, LOGIN_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        if (attempts > LOGIN_MAX_ATTEMPTS) {
            log.warn("登录频率超限，手机号={}", MaskUtil.maskMobile(request.getMobile()));
            throw new BizException(ErrorCodeEnum.RATE_LIMIT_EXCEEDED, "登录尝试过于频繁，请5分钟后再试");
        }

        // 查找账户
        AccountEntity account = accountMapper.selectByMobile(request.getMobile());
        if (account == null) {
            throw new BizException(ErrorCodeEnum.ACCOUNT_NOT_FOUND, "手机号或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new BizException(ErrorCodeEnum.UNAUTHORIZED, "手机号或密码错误");
        }

        // 检查账户状态
        String status = account.getStatus();
        if (AccountStatusEnum.ACCOUNT_FROZEN.getCode().equals(status)) {
            throw new BizException(ErrorCodeEnum.ACCOUNT_FROZEN, "账户已冻结，请联系管理员");
        }
        if (AccountStatusEnum.ACCOUNT_DISABLED.getCode().equals(status)) {
            throw new BizException(ErrorCodeEnum.ACCOUNT_LOCKED, "账户已停用");
        }

        // 登录成功，重置限流计数器
        redisService.delete(rateKey);

        // 更新最后登录信息
        LocalDateTime now = DateTimeUtil.nowUtc();
        account.setLastLoginTime(now);
        accountMapper.updateById(account);

        // 查询角色列表
        List<AccountRoleEntity> roleEntities = accountRoleMapper.selectByAccountId(account.getId());
        List<String> roles = roleEntities.stream()
                .map(AccountRoleEntity::getRoleCode)
                .collect(Collectors.toList());

        // 生成 Access Token + Refresh Token
        String token = jwtService.generateToken(account.getId(), account.getAccountNo(),
                account.getAccountType(), roles);
        String refreshToken = generateAndStoreRefreshToken(account.getId());

        log.info("用户登录成功，accountNo={}", account.getAccountNo());

        return LoginResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpireSeconds())
                .accountNo(account.getAccountNo())
                .nickname(account.getNickname())
                .accountType(account.getAccountType())
                .roles(roles)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccountProfileVO getProfile(Long accountId) {
        log.info("查询账户详情，accountId={}", accountId);

        AccountEntity account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BizException(ErrorCodeEnum.ACCOUNT_NOT_FOUND, "账户不存在");
        }

        // 加载主体信息
        SubjectProfileEntity subject = subjectProfileMapper.selectByAccountId(accountId);
        SubjectInfoVO subjectInfo = buildSubjectInfoVO(subject);

        // 加载DID信息
        DidRecordEntity didRecord = didRecordMapper.selectByAccountId(accountId);
        DidInfoVO didInfo = buildDidInfoVO(didRecord);

        // 加载角色
        List<AccountRoleEntity> roleEntities = accountRoleMapper.selectByAccountId(accountId);
        List<String> roles = roleEntities.stream()
                .map(AccountRoleEntity::getRoleCode)
                .collect(Collectors.toList());

        // 计算可执行操作
        List<String> allowedActions = computeAllowedActions(account, subject, didRecord);

        AccountProfileVO vo = AccountProfileVO.builder()
                .accountNo(account.getAccountNo())
                .mobile(account.getMobile())
                .nickname(account.getNickname())
                .email(account.getEmail())
                .avatarUrl(account.getAvatarUrl())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .authStatus(account.getAuthStatus())
                .subjectInfo(subjectInfo)
                .didInfo(didInfo)
                .roles(roles)
                .allowedActions(allowedActions)
                .build();

        AccountVoAssembler.applyVisibility(vo);
        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long accountId, UpdateProfileRequest request) {
        log.info("更新个人资料，accountId={}", accountId);

        AccountEntity account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BizException(ErrorCodeEnum.ACCOUNT_NOT_FOUND, "账户不存在");
        }

        if (request.getNickname() != null) {
            account.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            account.setEmail(request.getEmail());
        }
        if (request.getAvatarUrl() != null) {
            account.setAvatarUrl(request.getAvatarUrl());
        }

        accountMapper.updateById(account);
        log.info("个人资料更新成功，accountNo={}", account.getAccountNo());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAuth(Long accountId, AuthSubmitRequest request) {
        log.info("提交实名认证，accountId={}", accountId);

        AccountEntity account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BizException(ErrorCodeEnum.ACCOUNT_NOT_FOUND, "账户不存在");
        }

        // 校验当前状态允许提交认证
        String currentStatus = account.getStatus();
        if (!AccountStatusEnum.REGISTERED.getCode().equals(currentStatus)
                && !AccountStatusEnum.AUTH_REJECTED.getCode().equals(currentStatus)) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                    "当前状态不允许提交实名认证", null, currentStatus);
        }

        LocalDateTime now = DateTimeUtil.nowUtc();

        // 创建/更新主体信息
        SubjectProfileEntity subject = subjectProfileMapper.selectByAccountId(accountId);
        if (subject == null) {
            subject = new SubjectProfileEntity();
            subject.setSubjectNo(BizNoUtil.subjectNo());
            subject.setAccountId(accountId);
        }
        subject.setSubjectType(request.getSubjectType());
        subject.setRealName(request.getRealName());
        subject.setIdCardType(request.getIdCardType());
        subject.setIdCardNo(request.getIdCardNo());
        subject.setEnterpriseCode(request.getEnterpriseCode());
        subject.setContactName(request.getContactName());
        subject.setContactPhone(request.getContactPhone());
        subject.setAuthMaterialUrl(request.getAuthMaterialUrl());

        if (subject.getId() == null) {
            subjectProfileMapper.insert(subject);
        } else {
            subjectProfileMapper.updateById(subject);
        }

        // 创建认证记录
        SubjectAuthRecordEntity authRecord = new SubjectAuthRecordEntity();
        authRecord.setSubjectId(subject.getId());
        authRecord.setAuthAction("SUBMIT");
        authRecord.setAuthStatus(AccountStatusEnum.AUTH_PENDING.getCode());
        authRecord.setSubmitTime(now);
        subjectAuthRecordMapper.insert(authRecord);

        // 更新账户状态
        String fromStatus = account.getStatus();
        account.setStatus(AccountStatusEnum.AUTH_PENDING.getCode());
        account.setAuthStatus(AccountStatusEnum.AUTH_PENDING.getCode());
        accountMapper.updateById(account);

        // 写入状态变更历史
        auditService.writeStatusHistory(
                BizTypeEnum.ACCOUNT.getCode(), account.getId(), account.getAccountNo(),
                fromStatus, AccountStatusEnum.AUTH_PENDING.getCode(),
                "用户提交实名认证", null, accountId);

        log.info("实名认证提交成功，accountNo={}, subjectNo={}", account.getAccountNo(), subject.getSubjectNo());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewAuth(Long reviewerId, AuthReviewRequest request) {
        log.info("审核实名认证，accountNo={}, reviewResult={}", request.getAccountNo(), request.getReviewResult());

        // 查找目标账户
        AccountEntity account = accountMapper.selectByAccountNo(request.getAccountNo());
        if (account == null) {
            throw new BizException(ErrorCodeEnum.ACCOUNT_NOT_FOUND, "账户不存在");
        }

        // 校验状态
        if (!AccountStatusEnum.AUTH_PENDING.getCode().equals(account.getStatus())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                    "当前状态不允许审核", null, account.getStatus());
        }

        // 查找主体信息
        SubjectProfileEntity subject = subjectProfileMapper.selectByAccountId(account.getId());
        if (subject == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "主体信息不存在");
        }

        ReviewResultEnum reviewResult = ReviewResultEnum.fromCode(request.getReviewResult());
        LocalDateTime now = DateTimeUtil.nowUtc();
        String fromStatus = account.getStatus();

        // 创建审核记录
        SubjectAuthRecordEntity authRecord = new SubjectAuthRecordEntity();
        authRecord.setSubjectId(subject.getId());
        authRecord.setAuthAction("REVIEW");
        authRecord.setReviewerId(reviewerId);
        authRecord.setReviewComment(request.getReviewComment());
        authRecord.setReasonCode(request.getReasonCode());
        authRecord.setSubmitTime(now);
        authRecord.setReviewTime(now);

        String toStatus;
        if (ReviewResultEnum.APPROVED == reviewResult) {
            toStatus = AccountStatusEnum.AUTH_APPROVED.getCode();
            authRecord.setAuthStatus(AccountStatusEnum.AUTH_APPROVED.getCode());

            // 自动授予CREATOR角色
            grantCreatorRole(account.getId(), reviewerId, now);
            log.info("实名认证通过，已自动授予CREATOR角色，accountNo={}", account.getAccountNo());
        } else {
            toStatus = AccountStatusEnum.AUTH_REJECTED.getCode();
            authRecord.setAuthStatus(AccountStatusEnum.AUTH_REJECTED.getCode());
            log.info("实名认证驳回，accountNo={}", account.getAccountNo());
        }

        subjectAuthRecordMapper.insert(authRecord);

        // 更新账户状态
        account.setStatus(toStatus);
        account.setAuthStatus(toStatus);
        accountMapper.updateById(account);

        // 写入状态变更历史
        auditService.writeStatusHistory(
                BizTypeEnum.ACCOUNT.getCode(), account.getId(), account.getAccountNo(),
                fromStatus, toStatus,
                "认证审核：" + reviewResult.getDescription(), request.getReasonCode(), reviewerId);

        // 写入审计日志
        auditService.writeAuditLog(
                TargetTypeEnum.ACCOUNT.getCode(), account.getId(), account.getAccountNo(),
                "AUTH_REVIEW", "认证审核结果=" + reviewResult.getDescription() + "，意见=" + request.getReviewComment(),
                reviewerId, RoleEnum.PLATFORM_ADMIN.getCode(), null,
                "SUCCESS", request.getReasonCode());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freezeAccount(Long operatorId, AccountFreezeRequest request) {
        log.info("冻结账户，accountNo={}", request.getAccountNo());

        AccountEntity account = accountMapper.selectByAccountNo(request.getAccountNo());
        if (account == null) {
            throw new BizException(ErrorCodeEnum.ACCOUNT_NOT_FOUND, "账户不存在");
        }

        String currentStatus = account.getStatus();
        if (AccountStatusEnum.ACCOUNT_FROZEN.getCode().equals(currentStatus)) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID, "账户已处于冻结状态", null, currentStatus);
        }
        if (AccountStatusEnum.ACCOUNT_DISABLED.getCode().equals(currentStatus)) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID, "账户已停用，无法冻结", null, currentStatus);
        }

        // 保存冻结前状态到 previousStatus 字段，解冻时恢复
        account.setPreviousStatus(currentStatus);
        account.setStatus(AccountStatusEnum.ACCOUNT_FROZEN.getCode());
        accountMapper.updateById(account);

        // 写入状态变更历史
        auditService.writeStatusHistory(
                BizTypeEnum.ACCOUNT.getCode(), account.getId(), account.getAccountNo(),
                currentStatus, AccountStatusEnum.ACCOUNT_FROZEN.getCode(),
                request.getReason(), request.getReasonCode(), operatorId);

        // 写入审计日志
        auditService.writeAuditLog(
                TargetTypeEnum.ACCOUNT.getCode(), account.getId(), account.getAccountNo(),
                "FREEZE", "冻结账户，原因=" + request.getReason(),
                operatorId, RoleEnum.PLATFORM_ADMIN.getCode(), null,
                "SUCCESS", request.getReasonCode());

        // 清除账户状态缓存，确保冻结立即生效
        redisService.delete("account:status:" + account.getId());

        log.info("账户冻结成功，accountNo={}", request.getAccountNo());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeAccount(Long operatorId, AccountFreezeRequest request) {
        log.info("解冻账户，accountNo={}", request.getAccountNo());

        AccountEntity account = accountMapper.selectByAccountNo(request.getAccountNo());
        if (account == null) {
            throw new BizException(ErrorCodeEnum.ACCOUNT_NOT_FOUND, "账户不存在");
        }

        String currentStatus = account.getStatus();
        if (!AccountStatusEnum.ACCOUNT_FROZEN.getCode().equals(currentStatus)) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID, "账户未处于冻结状态，无法解冻", null, currentStatus);
        }

        // 恢复到冻结前的真实状态
        String restoreStatus = account.getPreviousStatus();
        if (restoreStatus == null || restoreStatus.isBlank()) {
            restoreStatus = AccountStatusEnum.AUTH_APPROVED.getCode();
        }
        account.setStatus(restoreStatus);
        account.setPreviousStatus(null);
        accountMapper.updateById(account);

        // 写入状态变更历史
        auditService.writeStatusHistory(
                BizTypeEnum.ACCOUNT.getCode(), account.getId(), account.getAccountNo(),
                currentStatus, restoreStatus,
                request.getReason(), request.getReasonCode(), operatorId);

        // 写入审计日志
        auditService.writeAuditLog(
                TargetTypeEnum.ACCOUNT.getCode(), account.getId(), account.getAccountNo(),
                "UNFREEZE", "解冻账户，原因=" + request.getReason(),
                operatorId, RoleEnum.PLATFORM_ADMIN.getCode(), null,
                "SUCCESS", request.getReasonCode());

        // 清除账户状态缓存，确保解冻立即生效
        redisService.delete("account:status:" + account.getId());

        log.info("账户解冻成功，accountNo={}", request.getAccountNo());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccountProfileVO getAccountDetail(String accountNo) {
        log.info("管理员查询账户详情，accountNo={}", accountNo);
        AccountEntity account = accountMapper.selectByAccountNo(accountNo);
        if (account == null) {
            throw new BizException(ErrorCodeEnum.ACCOUNT_NOT_FOUND, "账户不存在");
        }
        return buildAccountProfileVO(account);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<AccountProfileVO> listAccounts(PageQuery query) {
        log.info("分页查询账户列表，pageNo={}, pageSize={}", query.getPageNo(), query.getPageSize());

        Page<AccountEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<AccountEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AccountEntity::getCreatedAt);

        Page<AccountEntity> result = accountMapper.selectPage(page, wrapper);

        List<AccountProfileVO> voList = result.getRecords().stream()
                .map(account -> buildAccountProfileVO(account))
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    // ==================== 私有方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPlatformAccount(Long operatorId, CreateAccountRequest request) {
        createSpecialAccount(operatorId, request, AccountTypeEnum.PLATFORM, RoleEnum.PLATFORM_ADMIN);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRegulatorAccount(Long operatorId, CreateAccountRequest request) {
        createSpecialAccount(operatorId, request, AccountTypeEnum.REGULATOR, RoleEnum.REGULATOR);
    }

    private void createSpecialAccount(Long operatorId, CreateAccountRequest request,
                                               AccountTypeEnum accountType, RoleEnum roleEnum) {
        log.info("创建特殊账户，type={}, 操作人={}", accountType.getCode(), operatorId);

        AccountEntity existing = accountMapper.selectByMobile(request.getMobile());
        if (existing != null) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "该手机号已注册");
        }

        LocalDateTime now = DateTimeUtil.nowUtc();
        AccountEntity account = new AccountEntity();
        account.setAccountNo(BizNoUtil.accountNo());
        account.setMobile(request.getMobile());
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setAccountType(accountType.getCode());
        account.setStatus(AccountStatusEnum.AUTH_APPROVED.getCode());
        account.setAuthStatus(AccountStatusEnum.AUTH_APPROVED.getCode());
        account.setNickname(request.getNickname());
        accountMapper.insert(account);

        AccountRoleEntity role = new AccountRoleEntity();
        role.setAccountId(account.getId());
        role.setRoleCode(roleEnum.getCode());
        role.setStatus("ACTIVE");
        role.setGrantedBy(operatorId);
        role.setGrantedTime(now);
        accountRoleMapper.insert(role);

        auditService.writeStatusHistory(
                BizTypeEnum.ACCOUNT.getCode(), account.getId(), account.getAccountNo(),
                "", AccountStatusEnum.AUTH_APPROVED.getCode(),
                "管理员创建" + accountType.getDescription(), null, operatorId);

        auditService.writeAuditLog(
                TargetTypeEnum.ACCOUNT.getCode(), account.getId(), account.getAccountNo(),
                "CREATE_SPECIAL_ACCOUNT", "创建" + accountType.getDescription() + "账户",
                operatorId, RoleEnum.PLATFORM_ADMIN.getCode(), null,
                "SUCCESS", null);

        log.info("特殊账户创建成功，accountNo={}, type={}", account.getAccountNo(), accountType.getCode());
    }

    // ==================== 构建方法 ====================

    /**
     * 授予CREATOR角色
     *
     * @param accountId  账户ID
     * @param grantedBy  授予人ID
     * @param grantedTime 授予时间
     */
    private void grantCreatorRole(Long accountId, Long grantedBy, LocalDateTime grantedTime) {
        // 检查是否已有CREATOR角色
        List<AccountRoleEntity> existingRoles = accountRoleMapper.selectByAccountId(accountId);
        boolean hasCreator = existingRoles.stream()
                .anyMatch(r -> RoleEnum.CREATOR.getCode().equals(r.getRoleCode()));
        if (hasCreator) {
            return;
        }

        AccountRoleEntity roleEntity = new AccountRoleEntity();
        roleEntity.setAccountId(accountId);
        roleEntity.setRoleCode(RoleEnum.CREATOR.getCode());
        roleEntity.setStatus("ACTIVE");
        roleEntity.setGrantedBy(grantedBy);
        roleEntity.setGrantedTime(grantedTime);
        accountRoleMapper.insert(roleEntity);
    }

    /**
     * 构建主体信息视图对象（原始值，不做脱敏）
     * <p>
     * 脱敏由 {@link AccountVoAssembler} 统一处理。
     * </p>
     *
     * @param subject 主体实体
     * @return 主体信息VO
     */
    private SubjectInfoVO buildSubjectInfoVO(SubjectProfileEntity subject) {
        if (subject == null) {
            return null;
        }
        return SubjectInfoVO.builder()
                .subjectNo(subject.getSubjectNo())
                .subjectType(subject.getSubjectType())
                .realName(subject.getRealName())
                .idCardType(subject.getIdCardType())
                .idCardNo(subject.getIdCardNo())
                .build();
    }

    /**
     * 构建DID信息视图对象
     *
     * @param didRecord DID记录实体
     * @return DID信息VO
     */
    private DidInfoVO buildDidInfoVO(DidRecordEntity didRecord) {
        if (didRecord == null) {
            return null;
        }
        return DidInfoVO.builder()
                .didNo(didRecord.getDidNo())
                .didValue(didRecord.getDidValue())
                .status(didRecord.getStatus())
                .chainStatus(didRecord.getChainStatus())
                .activeTime(DateTimeUtil.formatUtc(didRecord.getActiveTime()))
                .applyTime(DateTimeUtil.formatUtc(didRecord.getApplyTime()))
                .build();
    }

    /**
     * 计算当前允许的操作列表
     *
     * @param account   账户实体
     * @param subject   主体实体
     * @param didRecord DID记录实体
     * @return 可执行操作列表
     */
    private List<String> computeAllowedActions(AccountEntity account,
                                                SubjectProfileEntity subject,
                                                DidRecordEntity didRecord) {
        List<String> actions = new ArrayList<>();
        String status = account.getStatus();

        if (AccountStatusEnum.REGISTERED.getCode().equals(status)
                || AccountStatusEnum.AUTH_REJECTED.getCode().equals(status)) {
            actions.add("SUBMIT_AUTH");
        }

        if (AccountStatusEnum.AUTH_APPROVED.getCode().equals(status)) {
            if (didRecord == null || DidStatusEnum.DID_CHAIN_FAILED.getCode().equals(didRecord.getStatus())) {
                actions.add("APPLY_DID");
            }
            actions.add("CREATE_WORK");
        }

        if (!AccountStatusEnum.ACCOUNT_FROZEN.getCode().equals(status)
                && !AccountStatusEnum.ACCOUNT_DISABLED.getCode().equals(status)) {
            actions.add("UPDATE_PROFILE");
        }

        return actions;
    }

    /**
     * 构建账户详情视图对象（原始值，不做脱敏）
     * <p>
     * 脱敏由 {@link AccountVoAssembler} 统一处理。
     * </p>
     *
     * @param account 账户实体
     * @return 账户详情VO
     */
    private AccountProfileVO buildAccountProfileVO(AccountEntity account) {
        SubjectProfileEntity subject = subjectProfileMapper.selectByAccountId(account.getId());
        DidRecordEntity didRecord = didRecordMapper.selectByAccountId(account.getId());
        List<AccountRoleEntity> roleEntities = accountRoleMapper.selectByAccountId(account.getId());

        List<String> roles = roleEntities.stream()
                .map(AccountRoleEntity::getRoleCode)
                .collect(Collectors.toList());

        AccountProfileVO vo = AccountProfileVO.builder()
                .accountNo(account.getAccountNo())
                .mobile(account.getMobile())
                .nickname(account.getNickname())
                .email(account.getEmail())
                .avatarUrl(account.getAvatarUrl())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .authStatus(account.getAuthStatus())
                .subjectInfo(buildSubjectInfoVO(subject))
                .didInfo(buildDidInfoVO(didRecord))
                .roles(roles)
                .allowedActions(computeAllowedActions(account, subject, didRecord))
                .build();

        AccountVoAssembler.applyVisibility(vo);
        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logout(String token) {
        long remainSeconds = jwtService.getRemainingSeconds(token);
        if (remainSeconds > 0) {
            // 将 Token 加入黑名单，TTL 与令牌剩余有效期一致
            redisService.set(
                    TOKEN_BLACKLIST_PREFIX + token,
                    "1",
                    remainSeconds,
                    TimeUnit.SECONDS);
        }
        log.info("用户退出登录，token已加入黑名单");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long accountId, ChangePasswordRequest request) {
        AccountEntity account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BizException(ErrorCodeEnum.ACCOUNT_NOT_FOUND, "账户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), account.getPasswordHash())) {
            throw new BizException(ErrorCodeEnum.UNAUTHORIZED, "原密码不正确");
        }
        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        accountMapper.updateById(account);

        // 写入审计日志
        auditService.writeAuditLog(
                TargetTypeEnum.ACCOUNT.getCode(), account.getId(), account.getAccountNo(),
                "CHANGE_PASSWORD", "用户修改密码",
                accountId, null, null, "SUCCESS", null);

        log.info("密码修改成功，accountId={}", accountId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendSmsCode(String mobile, String clientIp) {
        // 频率限制：每60秒只能发一次
        String rateKey = SMS_RATE_KEY_PREFIX + mobile;
        if (redisService.getString(rateKey) != null) {
            throw new BizException(ErrorCodeEnum.RATE_LIMIT_EXCEEDED, "发送过于频繁，请稍后再试");
        }
        redisService.set(rateKey, "1", SMS_RATE_LIMIT_SECONDS, TimeUnit.SECONDS);

        String normalizedClientIp = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
        String ipRateKey = SMS_IP_RATE_KEY_PREFIX + normalizedClientIp;
        Long ipCount = redisService.increment(ipRateKey);
        if (ipCount == 1) {
            redisService.expire(ipRateKey, SMS_IP_RATE_LIMIT_SECONDS, TimeUnit.SECONDS);
        }
        if (ipCount > SMS_IP_RATE_LIMIT) {
            log.warn("短信发送 IP 频率超限，ip={}, count={}", normalizedClientIp, ipCount);
            throw new BizException(ErrorCodeEnum.RATE_LIMIT_EXCEEDED, "短信发送过于频繁，请稍后再试");
        }

        // 生成6位数字验证码
        String code = String.format("%06d", SMS_CODE_RANDOM.nextInt(1_000_000));
        redisService.set(SMS_CODE_KEY_PREFIX + mobile, code, SMS_CODE_TTL_SECONDS, TimeUnit.SECONDS);
        if (!smsDevMode) {
            log.error("短信服务尚未接入正式通道，当前环境禁止发送真实短信，mobile={}", MaskUtil.maskMobile(mobile));
            throw new BizException(ErrorCodeEnum.NOTIFICATION_FAILED, "短信服务尚未接入正式发送通道");
        }

        // TODO: 接入真实短信服务商（阿里云SMS/腾讯云SMS），当前仅打印日志用于开发测试
        log.info("[SMS] 手机号={} 验证码={}（仅开发模式可见，生产环境必须接入真实短信发送）",
                MaskUtil.maskMobile(mobile), code);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verifySmsCode(String mobile, String code) {
        String smsKey = SMS_CODE_KEY_PREFIX + mobile;
        String storedCode = redisService.getString(smsKey);
        if (storedCode == null) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "验证码已过期，请重新获取");
        }
        if (!storedCode.equals(code)) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "验证码错误");
        }
        redisService.delete(smsKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoginResponse refreshToken(String refreshToken) {
        String key = REFRESH_TOKEN_KEY_PREFIX + refreshToken;
        String accountIdStr = redisService.getString(key);
        if (accountIdStr == null) {
            throw new BizException(ErrorCodeEnum.UNAUTHORIZED, "refreshToken已过期或无效");
        }

        Long accountId = Long.parseLong(accountIdStr);
        AccountEntity account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BizException(ErrorCodeEnum.ACCOUNT_NOT_FOUND, "账户不存在");
        }
        if (AccountStatusEnum.ACCOUNT_FROZEN.getCode().equals(account.getStatus())) {
            throw new BizException(ErrorCodeEnum.ACCOUNT_FROZEN, "账户已冻结，请联系管理员");
        }
        if (AccountStatusEnum.ACCOUNT_DISABLED.getCode().equals(account.getStatus())) {
            throw new BizException(ErrorCodeEnum.ACCOUNT_LOCKED, "账户已停用");
        }

        List<AccountRoleEntity> roleEntities = accountRoleMapper.selectByAccountId(accountId);
        List<String> roles = roleEntities.stream()
                .map(AccountRoleEntity::getRoleCode)
                .collect(Collectors.toList());

        // 生成新 accessToken，同时轮换 refreshToken（Refresh Token Rotation）
        String newAccessToken = jwtService.generateToken(
                account.getId(), account.getAccountNo(), account.getAccountType(), roles);
        redisService.delete(key);
        String newRefreshToken = generateAndStoreRefreshToken(account.getId());

        log.info("refreshToken 轮换成功，accountId={}", accountId);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpireSeconds())
                .accountNo(account.getAccountNo())
                .nickname(account.getNickname())
                .accountType(account.getAccountType())
                .roles(roles)
                .build();
    }

    /**
     * 生成并存储 Refresh Token，返回 token 字符串
     */
    private String generateAndStoreRefreshToken(Long accountId) {
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        redisService.set(
                REFRESH_TOKEN_KEY_PREFIX + refreshToken,
                String.valueOf(accountId),
                REFRESH_TOKEN_TTL_SECONDS,
                TimeUnit.SECONDS);
        return refreshToken;
    }
}
