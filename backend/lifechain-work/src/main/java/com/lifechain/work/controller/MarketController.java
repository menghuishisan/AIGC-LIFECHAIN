package com.lifechain.work.controller;

import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.work.dto.WorkDetailVO;
import com.lifechain.work.dto.WorkListVO;
import com.lifechain.work.service.WorkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 市場控制器
 * <p>
 * 提供面向买家的市场作品浏览接口，仅展示已上架（LISTED）的作品。
 * 所有接口需要登录认证。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@Tag(name = "市场浏览", description = "市场作品展示，面向买家")
public class MarketController {

    private final WorkService workService;

    /**
     * 查询市场作品列表
     *
     * @param workType 作品类型筛选（可选）
     * @param query    分页参数
     * @return 分页作品列表
     */
    @GetMapping("/works")
    @Operation(summary = "市场作品列表", description = "查询已上架的市场作品列表，支持按类型筛选和关键词搜索")
    public ApiResponse<PageResult<WorkListVO>> listMarketWorks(@RequestParam(required = false) String workType,
                                                               @RequestParam(required = false) String keyword,
                                                               @Valid PageQuery query) {
        PageResult<WorkListVO> result = workService.listMarketWorks(workType, keyword, query);
        return ApiResponse.success(result);
    }

    /**
     * 查询市场作品详情
     *
     * @param workNo 作品编号
     * @return 作品详情
     */
    @GetMapping("/works/{workNo}")
    @Operation(summary = "市场作品详情", description = "查询市场作品的详细信息，仅返回已上架作品")
    public ApiResponse<WorkDetailVO> getMarketWorkDetail(@PathVariable String workNo) {
        WorkDetailVO result = workService.getMarketWorkDetail(workNo);
        return ApiResponse.success(result);
    }
}
