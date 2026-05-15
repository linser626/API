package com.airelay.user.controller;

import com.airelay.common.Constants;
import com.airelay.common.Result;
import com.airelay.security.SecurityUtils;
import com.airelay.user.dto.ReferralInfoVO;
import com.airelay.user.dto.ReferralRecordVO;
import com.airelay.user.service.ReferralService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "推荐返利")
@RestController
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @Operation(summary = "获取我的推荐信息")
    @GetMapping("/api/referral/info")
    public Result<ReferralInfoVO> getReferralInfo() {
        return Result.ok(referralService.getReferralInfo(SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "获取我的推荐记录")
    @GetMapping("/api/referral/records")
    public Result<IPage<ReferralRecordVO>> getReferralRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "" + Constants.DEFAULT_PAGE_SIZE) int size) {
        if (size > Constants.MAX_PAGE_SIZE) {
            size = Constants.MAX_PAGE_SIZE;
        }
        return Result.ok(referralService.getReferralRecords(SecurityUtils.getCurrentUserId(), page, size));
    }

    @Operation(summary = "应用推荐码")
    @PostMapping("/api/referral/apply")
    public Result<Void> applyReferralCode(@RequestParam String referralCode) {
        referralService.processReferral(SecurityUtils.getCurrentUserId(), referralCode);
        return Result.ok();
    }
}
