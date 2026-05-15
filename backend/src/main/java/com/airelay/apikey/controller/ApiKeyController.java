package com.airelay.apikey.controller;

import com.airelay.apikey.dto.ApiKeyCreateRequest;
import com.airelay.apikey.dto.ApiKeyUpdateRequest;
import com.airelay.apikey.entity.ApiKey;
import com.airelay.apikey.service.ApiKeyService;
import com.airelay.common.Result;
import com.airelay.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "API密钥管理")
@RestController
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @Operation(summary = "创建API密钥")
    @PostMapping("/api/apikeys")
    public Result<ApiKey> createApiKey(@Valid @RequestBody ApiKeyCreateRequest request) {
        return Result.ok(apiKeyService.createApiKey(SecurityUtils.getCurrentUserId(), request.getName()));
    }

    @Operation(summary = "获取我的API密钥列表")
    @GetMapping("/api/apikeys")
    public Result<List<ApiKey>> listApiKeys() {
        return Result.ok(apiKeyService.listApiKeys(SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "更新API密钥")
    @PutMapping("/api/apikeys/{id}")
    public Result<ApiKey> updateApiKey(@PathVariable Long id, @Valid @RequestBody ApiKeyUpdateRequest request) {
        return Result.ok(apiKeyService.updateApiKey(
                SecurityUtils.getCurrentUserId(), id,
                request.getName(), request.getRateLimitRpm(), request.getRateLimitTpm()
        ));
    }

    @Operation(summary = "撤销API密钥")
    @DeleteMapping("/api/apikeys/{id}")
    public Result<Void> revokeApiKey(@PathVariable Long id) {
        apiKeyService.revokeApiKey(SecurityUtils.getCurrentUserId(), id);
        return Result.ok();
    }
}
