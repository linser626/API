package com.airelay.channel.controller;

import com.airelay.channel.dto.ChannelCreateRequest;
import com.airelay.channel.dto.ChannelUpdateRequest;
import com.airelay.channel.entity.Channel;
import com.airelay.channel.service.ChannelService;
import com.airelay.common.Constants;
import com.airelay.common.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "通道管理")
@RestController
@RequestMapping("/api/admin/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @Operation(summary = "创建通道")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Channel> createChannel(@Valid @RequestBody ChannelCreateRequest request) {
        return Result.ok(channelService.createChannel(request));
    }

    @Operation(summary = "更新通道")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Channel> updateChannel(@PathVariable Long id, @Valid @RequestBody ChannelUpdateRequest request) {
        return Result.ok(channelService.updateChannel(id, request));
    }

    @Operation(summary = "删除通道")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteChannel(@PathVariable Long id) {
        channelService.deleteChannel(id);
        return Result.ok();
    }

    @Operation(summary = "获取通道列表")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<Channel>> listChannels(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "" + Constants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {
        if (size > Constants.MAX_PAGE_SIZE) {
            size = Constants.MAX_PAGE_SIZE;
        }
        return Result.ok(channelService.listChannels(page, size, type, status));
    }

    @Operation(summary = "获取通道详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Channel> getChannel(@PathVariable Long id) {
        return Result.ok(channelService.getChannelById(id));
    }

    @Operation(summary = "测试通道连通性")
    @PostMapping("/{id}/test")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> testChannel(@PathVariable Long id) {
        return Result.ok(channelService.testChannel(id));
    }
}
