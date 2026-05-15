package com.airelay.user.controller;

import com.airelay.common.Constants;
import com.airelay.common.Result;
import com.airelay.security.SecurityUtils;
import com.airelay.user.dto.*;
import com.airelay.user.entity.User;
import com.airelay.user.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/api/auth/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(userService.register(request));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/api/auth/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/api/auth/refresh")
    public Result<TokenResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return Result.ok(userService.refreshToken(request.getRefreshToken()));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/api/user/profile")
    public Result<User> getProfile() {
        return Result.ok(userService.getUserById(SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/api/user/profile")
    public Result<User> updateProfile(@Valid @RequestBody UserUpdateRequest request) {
        return Result.ok(userService.updateUser(SecurityUtils.getCurrentUserId(), request));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/api/user/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(SecurityUtils.getCurrentUserId(), request);
        return Result.ok();
    }

    @Operation(summary = "用户列表(管理员)")
    @GetMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<User>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "" + Constants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(required = false) String keyword) {
        if (size > Constants.MAX_PAGE_SIZE) {
            size = Constants.MAX_PAGE_SIZE;
        }
        return Result.ok(userService.listUsers(page, size, keyword));
    }

    @Operation(summary = "更新用户状态(管理员)")
    @PutMapping("/api/admin/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateUserStatus(id, status);
        return Result.ok();
    }
}
