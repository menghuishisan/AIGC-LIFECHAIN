package com.lifechain.auth.service;

import com.lifechain.auth.dto.*;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;

/**
 * 账户服务接口
 * <p>
 * 提供用户注册、登录、个人资料管理、实名认证、认证审核、
 * 账户冻结/解冻、账户列表查询等核心功能。
 * </p>
 *
 * @author LifeChain
 */
public interface AccountService {

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 登录响应（含JWT令牌）
     */
    LoginResponse register(RegisterRequest request);

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（含JWT令牌）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户退出登录（将当前Token加入黑名单）
     *
     * @param token 当前JWT令牌（不含Bearer前缀）
     */
    void logout(String token);

    /**
     * 使用 Refresh Token 换取新的 Access Token（无感刷新）
     *
     * @param refreshToken Refresh Token
     * @return 新的登录响应（含新 accessToken 和 refreshToken）
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * 发送手机验证码
     *
     * @param mobile 手机号
     */
    void sendSmsCode(String mobile, String clientIp);

    /**
     * 校验手机验证码
     *
     * @param mobile  手机号
     * @param code    验证码
     */
    void verifySmsCode(String mobile, String code);

    /**
     * 修改密码
     *
     * @param accountId 账户ID
     * @param request   修改密码请求
     */
    void changePassword(Long accountId, ChangePasswordRequest request);

    /**
     * 获取账户详情
     *
     * @param accountId 账户ID
     * @return 账户详情视图对象
     */
    AccountProfileVO getProfile(Long accountId);

    /**
     * 更新个人资料
     *
     * @param accountId 账户ID
     * @param request   更新请求
     */
    void updateProfile(Long accountId, UpdateProfileRequest request);

    /**
     * 提交实名认证
     *
     * @param accountId 账户ID
     * @param request   认证提交请求
     */
    void submitAuth(Long accountId, AuthSubmitRequest request);

    /**
     * 审核实名认证（管理员）
     *
     * @param reviewerId 审核人ID
     * @param request    审核请求
     */
    void reviewAuth(Long reviewerId, AuthReviewRequest request);

    /**
     * 创建平台管理员账户（管理员后台操作，无需短信验证码）
     *
     * @param operatorId 操作人ID
     * @param request    创建账户请求
     */
    void createPlatformAccount(Long operatorId, CreateAccountRequest request);

    /**
     * 创建监管员账户（管理员后台操作，无需短信验证码）
     *
     * @param operatorId 操作人ID
     * @param request    创建账户请求
     */
    void createRegulatorAccount(Long operatorId, CreateAccountRequest request);

    /**
     * 冻结账户（管理员）
     *
     * @param operatorId 操作人ID
     * @param request    冻结请求
     */
    void freezeAccount(Long operatorId, AccountFreezeRequest request);

    /**
     * 解冻账户（管理员）
     *
     * @param operatorId 操作人ID
     * @param request    解冻请求
     */
    void unfreezeAccount(Long operatorId, AccountFreezeRequest request);

    /**
     * 查询账户详情（管理员）
     *
     * @param accountNo 账户编号
     * @return 账户详情视图对象
     */
    AccountProfileVO getAccountDetail(String accountNo);

    /**
     * 分页查询账户列表（管理员）
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    PageResult<AccountProfileVO> listAccounts(PageQuery query);
}
