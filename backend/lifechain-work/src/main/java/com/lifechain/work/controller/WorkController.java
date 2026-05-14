package com.lifechain.work.controller;

import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.work.dto.*;
import com.lifechain.work.service.WorkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 作品控制器
 * <p>
 * 处理作品上传、元数据更新、特征提取、作品详情查询和我的作品列表等请求。
 * 所有接口需要登录认证。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequestMapping("/api/works")
@RequiredArgsConstructor
@Tag(name = "作品管理", description = "作品上传、查询与特征提取")
public class WorkController {

    private final WorkService workService;

    /**
     * 上传作品
     *
     * @param request 上传请求参数（JSON部分）
     * @param files   作品文件列表
     * @return 作品详情
     */
    @PostMapping("/upload")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "上传作品", description = "上传新的AIGC作品，包含文件和元数据")
    public ApiResponse<WorkDetailVO> uploadWork(@Valid @RequestPart("request") WorkUploadRequest request,
                                                @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        Long accountId = UserContext.getUserId();
        log.info("上传作品请求，accountId={}", accountId);
        WorkDetailVO result = workService.uploadWork(accountId, request, files);
        return ApiResponse.success(result);
    }

    /**
     * 更新作品元数据
     *
     * @param workNo  作品编号
     * @param request 更新请求参数
     * @return 成功响应
     */
    @PutMapping("/{workNo}/meta")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "更新作品元数据", description = "更新作品标题、描述、封面及AIGC元数据")
    public ApiResponse<Void> updateWorkMeta(@PathVariable String workNo,
                                            @Valid @RequestBody WorkMetaUpdateRequest request) {
        Long accountId = UserContext.getUserId();
        workService.updateWorkMeta(accountId, workNo, request);
        return ApiResponse.success();
    }

    /**
     * 触发特征提取
     *
     * @param workNo 作品编号
     * @return 成功响应
     */
    @PostMapping("/{workNo}/feature-extract")
    @Operation(summary = "触发特征提取", description = "对已上传作品触发特征提取和相似度检测")
    @Idempotent(key = "#requestId")
    public ApiResponse<Void> triggerFeatureExtract(@PathVariable String workNo,
                                                   @RequestParam @jakarta.validation.constraints.NotBlank String requestId) {
        Long accountId = UserContext.getUserId();
        workService.triggerFeatureExtract(accountId, workNo);
        return ApiResponse.success();
    }

    /**
     * 查询作品特征
     *
     * @param workNo 作品编号
     * @return 作品特征信息
     */
    @GetMapping("/{workNo}/feature")
    @Operation(summary = "查询作品特征", description = "查询作品的特征提取结果")
    public ApiResponse<WorkFeatureVO> getWorkFeature(@PathVariable String workNo) {
        WorkFeatureVO result = workService.getWorkFeature(workNo, UserContext.getUserId());
        return ApiResponse.success(result);
    }

    /**
     * 查询我的作品列表
     *
     * @param status 状态筛选（可选）
     * @param query  分页参数
     * @return 分页作品列表
     */
    @GetMapping("/mine")
    @Operation(summary = "我的作品列表", description = "查询当前登录用户的作品列表，支持状态筛选")
    public ApiResponse<PageResult<WorkListVO>> listMyWorks(@RequestParam(required = false) String status,
                                                           @Valid PageQuery query) {
        Long accountId = UserContext.getUserId();
        PageResult<WorkListVO> result = workService.listMyWorks(accountId, status, query);
        return ApiResponse.success(result);
    }

    /**
     * 查询作品详情
     *
     * @param workNo 作品编号
     * @return 作品详情
     */
    @GetMapping("/{workNo}")
    @Operation(summary = "作品详情", description = "查询作品的完整详情信息")
    public ApiResponse<WorkDetailVO> getWorkDetail(@PathVariable String workNo) {
        Long accountId = UserContext.getUserId();
        WorkDetailVO result = workService.getWorkDetail(workNo, accountId);
        return ApiResponse.success(result);
    }
}
