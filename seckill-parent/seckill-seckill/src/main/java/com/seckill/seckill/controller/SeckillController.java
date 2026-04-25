package com.seckill.seckill.controller;

import com.seckill.common.result.Result;
import com.seckill.common.utils.UserContext;
import com.seckill.seckill.annotation.RateLimit;
import com.seckill.seckill.annotation.RateLimitScene;
import com.seckill.seckill.dto.SeckillExecuteRequest;
import com.seckill.seckill.dto.SeckillExecuteResponse;
import com.seckill.seckill.dto.SeckillPathResponse;
import com.seckill.seckill.dto.SeckillResultResponse;
import com.seckill.seckill.service.SeckillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @RateLimit(scene = RateLimitScene.SECKILL_PATH)
    @GetMapping("/path/{activityId}")
    public Result<SeckillPathResponse> getSeckillPath(@PathVariable Long activityId, @RequestParam Long goodsId) {
        return Result.success(seckillService.getSeckillPath(UserContext.getCurrentUserId(), activityId, goodsId));
    }

    @RateLimit(scene = RateLimitScene.SECKILL_EXECUTE)
    @PostMapping("/do")
    public Result<SeckillExecuteResponse> execute(@Valid @RequestBody SeckillExecuteRequest request) {
        return Result.success("秒杀请求已提交，正在处理中", seckillService.execute(UserContext.getCurrentUserId(), request));
    }

    @GetMapping("/result/{recordId}")
    public Result<SeckillResultResponse> result(@PathVariable Long recordId) {
        return Result.success(seckillService.getResult(UserContext.getCurrentUserId(), recordId));
    }
}
