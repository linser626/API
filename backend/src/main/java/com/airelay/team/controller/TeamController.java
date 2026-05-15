package com.airelay.team.controller;

import com.airelay.common.Result;
import com.airelay.security.SecurityUtils;
import com.airelay.team.dto.*;
import com.airelay.team.entity.Team;
import com.airelay.team.entity.TeamApiKey;
import com.airelay.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "团队管理")
@RestController
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @Operation(summary = "创建团队")
    @PostMapping("/api/teams")
    public Result<Team> createTeam(@Valid @RequestBody TeamCreateRequest request) {
        return Result.ok(teamService.createTeam(SecurityUtils.getCurrentUserId(), request));
    }

    @Operation(summary = "获取我的团队列表")
    @GetMapping("/api/teams")
    public Result<List<TeamVO>> listMyTeams() {
        return Result.ok(teamService.getMyTeams(SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "获取团队详情")
    @GetMapping("/api/teams/{id}")
    public Result<TeamVO> getTeamDetail(@PathVariable Long id) {
        return Result.ok(teamService.getTeamDetailForUser(id, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "更新团队信息")
    @PutMapping("/api/teams/{id}")
    public Result<Team> updateTeam(@PathVariable Long id, @RequestBody TeamUpdateRequest request) {
        return Result.ok(teamService.updateTeam(SecurityUtils.getCurrentUserId(), id, request));
    }

    @Operation(summary = "删除团队")
    @DeleteMapping("/api/teams/{id}")
    public Result<Void> deleteTeam(@PathVariable Long id) {
        teamService.deleteTeam(SecurityUtils.getCurrentUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "获取团队成员列表")
    @GetMapping("/api/teams/{id}/members")
    public Result<List<TeamMemberVO>> listTeamMembers(@PathVariable Long id) {
        return Result.ok(teamService.getTeamMembers(id));
    }

    @Operation(summary = "邀请团队成员")
    @PostMapping("/api/teams/{id}/members/invite")
    public Result<Void> inviteMember(@PathVariable Long id, @Valid @RequestBody InviteMemberRequest request) {
        teamService.inviteMember(SecurityUtils.getCurrentUserId(), id, request);
        return Result.ok();
    }

    @Operation(summary = "移除团队成员")
    @DeleteMapping("/api/teams/{id}/members/{memberId}")
    public Result<Void> removeMember(@PathVariable Long id, @PathVariable Long memberId) {
        teamService.removeMember(SecurityUtils.getCurrentUserId(), id, memberId);
        return Result.ok();
    }

    @Operation(summary = "更新成员角色")
    @PutMapping("/api/teams/{id}/members/{memberId}/role")
    public Result<Void> updateMemberRole(@PathVariable Long id, @PathVariable Long memberId,
                                         @RequestBody Map<String, String> body) {
        teamService.updateMemberRole(SecurityUtils.getCurrentUserId(), id, memberId, body.get("role"));
        return Result.ok();
    }

    @Operation(summary = "离开团队")
    @PostMapping("/api/teams/{id}/leave")
    public Result<Void> leaveTeam(@PathVariable Long id) {
        teamService.leaveTeam(SecurityUtils.getCurrentUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "创建团队API密钥")
    @PostMapping("/api/teams/{id}/apikeys")
    public Result<TeamApiKey> createTeamApiKey(@PathVariable Long id,
                                               @RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "default");
        return Result.ok(teamService.createTeamApiKey(SecurityUtils.getCurrentUserId(), id, name));
    }

    @Operation(summary = "获取团队API密钥列表")
    @GetMapping("/api/teams/{id}/apikeys")
    public Result<List<TeamApiKeyVO>> listTeamApiKeys(@PathVariable Long id) {
        return Result.ok(teamService.listTeamApiKeys(id));
    }

    @Operation(summary = "吊销团队API密钥")
    @DeleteMapping("/api/teams/{id}/apikeys/{keyId}")
    public Result<Void> revokeTeamApiKey(@PathVariable Long id, @PathVariable Long keyId) {
        teamService.revokeTeamApiKey(SecurityUtils.getCurrentUserId(), id, keyId);
        return Result.ok();
    }

    @Operation(summary = "充值团队余额")
    @PostMapping("/api/teams/{id}/recharge")
    public Result<Void> rechargeTeamBalance(@PathVariable Long id,
                                            @RequestBody Map<String, BigDecimal> body) {
        BigDecimal amount = body.get("amount");
        teamService.rechargeTeamBalance(SecurityUtils.getCurrentUserId(), id, amount);
        return Result.ok();
    }
}
