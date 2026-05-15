package com.airelay.team.service;

import com.airelay.billing.service.BillingService;
import com.airelay.common.BusinessException;
import com.airelay.common.ErrorCode;
import com.airelay.team.dto.*;
import com.airelay.team.entity.Team;
import com.airelay.team.entity.TeamApiKey;
import com.airelay.team.entity.TeamMember;
import com.airelay.team.mapper.TeamApiKeyMapper;
import com.airelay.team.mapper.TeamMapper;
import com.airelay.team.mapper.TeamMemberMapper;
import com.airelay.user.entity.User;
import com.airelay.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final TeamApiKeyMapper teamApiKeyMapper;
    private final UserMapper userMapper;
    private final BillingService billingService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public Team createTeam(Long userId, TeamCreateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        Team team = new Team();
        team.setName(request.getName());
        team.setOwnerId(userId);
        team.setDescription(request.getDescription() != null ? request.getDescription() : "");
        team.setAvatar("");
        team.setBalance(BigDecimal.ZERO);
        team.setStatus(1);
        team.setMaxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : 10);
        teamMapper.insert(team);

        TeamMember ownerMember = new TeamMember();
        ownerMember.setTeamId(team.getId());
        ownerMember.setUserId(userId);
        ownerMember.setRole("owner");
        ownerMember.setNickname(user.getUsername());
        ownerMember.setStatus(1);
        ownerMember.setJoinedAt(LocalDateTime.now());
        teamMemberMapper.insert(ownerMember);

        return team;
    }

    @Transactional
    public Team updateTeam(Long userId, Long teamId, TeamUpdateRequest request) {
        TeamMember member = getTeamMember(teamId, userId);
        if (member == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "您不是该团队成员");
        }
        if (!isAdminRole(member.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有所有者或管理员可以更新团队信息");
        }

        Team team = getTeamOrThrow(teamId);
        if (request.getName() != null) {
            team.setName(request.getName());
        }
        if (request.getDescription() != null) {
            team.setDescription(request.getDescription());
        }
        if (request.getAvatar() != null) {
            team.setAvatar(request.getAvatar());
        }
        if (request.getMaxMembers() != null) {
            long currentCount = teamMemberMapper.selectCount(
                    new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, teamId)
            );
            if (request.getMaxMembers() < currentCount) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "最大成员数不能少于当前成员数");
            }
            team.setMaxMembers(request.getMaxMembers());
        }

        teamMapper.updateById(team);
        return team;
    }

    @Transactional
    public void deleteTeam(Long userId, Long teamId) {
        Team team = getTeamOrThrow(teamId);
        if (!team.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有所有者可以删除团队");
        }

        teamMapper.deleteById(teamId);

        teamMemberMapper.delete(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, teamId)
        );

        teamApiKeyMapper.delete(
                new LambdaQueryWrapper<TeamApiKey>().eq(TeamApiKey::getTeamId, teamId)
        );
    }

    public TeamVO getTeamDetail(Long teamId) {
        Team team = getTeamOrThrow(teamId);
        User owner = userMapper.selectById(team.getOwnerId());
        long memberCount = teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, teamId)
        );

        return TeamVO.builder()
                .id(team.getId())
                .name(team.getName())
                .ownerId(team.getOwnerId())
                .ownerName(owner != null ? owner.getUsername() : "")
                .description(team.getDescription())
                .avatar(team.getAvatar())
                .balance(team.getBalance())
                .memberCount((int) memberCount)
                .maxMembers(team.getMaxMembers())
                .status(team.getStatus())
                .createdAt(team.getCreatedAt())
                .build();
    }

    public TeamVO getTeamDetailForUser(Long teamId, Long userId) {
        TeamVO vo = getTeamDetail(teamId);
        TeamMember member = getTeamMember(teamId, userId);
        vo.setMyRole(member != null ? member.getRole() : null);
        return vo;
    }

    public List<TeamVO> getMyTeams(Long userId) {
        List<TeamMember> memberships = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
                        .eq(TeamMember::getStatus, 1)
        );

        return memberships.stream().map(membership -> {
            Team team = teamMapper.selectById(membership.getTeamId());
            if (team == null) {
                return null;
            }
            User owner = userMapper.selectById(team.getOwnerId());
            long memberCount = teamMemberMapper.selectCount(
                    new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, team.getId())
            );

            return TeamVO.builder()
                    .id(team.getId())
                    .name(team.getName())
                    .ownerId(team.getOwnerId())
                    .ownerName(owner != null ? owner.getUsername() : "")
                    .description(team.getDescription())
                    .avatar(team.getAvatar())
                    .balance(team.getBalance())
                    .memberCount((int) memberCount)
                    .maxMembers(team.getMaxMembers())
                    .myRole(membership.getRole())
                    .status(team.getStatus())
                    .createdAt(team.getCreatedAt())
                    .build();
        }).filter(vo -> vo != null).collect(Collectors.toList());
    }

    @Transactional
    public void inviteMember(Long userId, Long teamId, InviteMemberRequest request) {
        TeamMember inviter = getTeamMember(teamId, userId);
        if (inviter == null || !isAdminRole(inviter.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有所有者或管理员可以邀请成员");
        }

        Team team = getTeamOrThrow(teamId);

        User invitee = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())
        );
        if (invitee == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该邮箱对应的用户不存在");
        }

        TeamMember existing = getTeamMember(teamId, invitee.getId());
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该用户已经是团队成员");
        }

        long currentCount = teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, teamId)
        );
        if (currentCount >= team.getMaxMembers()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "团队成员数已达上限");
        }

        String role = request.getRole();
        if (role == null || role.isEmpty()) {
            role = "member";
        }
        if (!"admin".equals(role) && !"member".equals(role)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的角色，只能邀请为admin或member");
        }

        TeamMember newMember = new TeamMember();
        newMember.setTeamId(teamId);
        newMember.setUserId(invitee.getId());
        newMember.setRole(role);
        newMember.setNickname(invitee.getUsername());
        newMember.setStatus(1);
        newMember.setJoinedAt(LocalDateTime.now());
        teamMemberMapper.insert(newMember);
    }

    @Transactional
    public void removeMember(Long userId, Long teamId, Long memberId) {
        TeamMember operator = getTeamMember(teamId, userId);
        if (operator == null || !isAdminRole(operator.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有所有者或管理员可以移除成员");
        }

        TeamMember target = teamMemberMapper.selectById(memberId);
        if (target == null || !target.getTeamId().equals(teamId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "成员不存在");
        }

        if ("owner".equals(target.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能移除团队所有者");
        }

        teamMemberMapper.deleteById(memberId);
    }

    @Transactional
    public void updateMemberRole(Long userId, Long teamId, Long memberId, String role) {
        Team team = getTeamOrThrow(teamId);
        if (!team.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有所有者可以更改成员角色");
        }

        TeamMember target = teamMemberMapper.selectById(memberId);
        if (target == null || !target.getTeamId().equals(teamId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "成员不存在");
        }

        if ("owner".equals(target.getRole())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能更改所有者角色");
        }

        if (!"admin".equals(role) && !"member".equals(role)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的角色");
        }

        target.setRole(role);
        teamMemberMapper.updateById(target);
    }

    public List<TeamMemberVO> getTeamMembers(Long teamId) {
        List<TeamMember> members = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
                        .eq(TeamMember::getStatus, 1)
                        .orderByAsc(TeamMember::getJoinedAt)
        );

        return members.stream().map(member -> {
            User user = userMapper.selectById(member.getUserId());
            return TeamMemberVO.builder()
                    .id(member.getId())
                    .userId(member.getUserId())
                    .username(user != null ? user.getUsername() : "")
                    .role(member.getRole())
                    .nickname(member.getNickname())
                    .status(member.getStatus())
                    .joinedAt(member.getJoinedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public void leaveTeam(Long userId, Long teamId) {
        TeamMember member = getTeamMember(teamId, userId);
        if (member == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "您不是该团队成员");
        }

        if ("owner".equals(member.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "所有者不能离开团队，请先转让所有权或删除团队");
        }

        teamMemberMapper.deleteById(member.getId());
    }

    @Transactional
    public TeamApiKey createTeamApiKey(Long userId, Long teamId, String name) {
        TeamMember member = getTeamMember(teamId, userId);
        if (member == null || !isAdminRole(member.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有所有者或管理员可以创建团队API密钥");
        }

        Team team = getTeamOrThrow(teamId);

        String keyValue = generateTeamApiKey();

        TeamApiKey apiKey = new TeamApiKey();
        apiKey.setTeamId(teamId);
        apiKey.setKeyValue(keyValue);
        apiKey.setName(name != null ? name : "default");
        apiKey.setCreatedBy(userId);
        apiKey.setStatus(1);
        apiKey.setRateLimitRpm(60);
        apiKey.setRateLimitTpm(100000);
        apiKey.setTotalQuota(-1L);
        apiKey.setUsedQuota(0L);

        teamApiKeyMapper.insert(apiKey);
        return apiKey;
    }

    public List<TeamApiKeyVO> listTeamApiKeys(Long teamId) {
        List<TeamApiKey> keys = teamApiKeyMapper.selectList(
                new LambdaQueryWrapper<TeamApiKey>()
                        .eq(TeamApiKey::getTeamId, teamId)
                        .orderByDesc(TeamApiKey::getCreatedAt)
        );

        return keys.stream().map(key -> TeamApiKeyVO.builder()
                .id(key.getId())
                .name(key.getName())
                .keyValue(maskApiKey(key.getKeyValue()))
                .status(key.getStatus())
                .rateLimitRpm(key.getRateLimitRpm())
                .rateLimitTpm(key.getRateLimitTpm())
                .totalQuota(key.getTotalQuota())
                .usedQuota(key.getUsedQuota())
                .expiresAt(key.getExpiresAt())
                .lastUsedAt(key.getLastUsedAt())
                .createdAt(key.getCreatedAt())
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional
    public void revokeTeamApiKey(Long userId, Long teamId, Long keyId) {
        TeamMember member = getTeamMember(teamId, userId);
        if (member == null || !isAdminRole(member.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有所有者或管理员可以吊销团队API密钥");
        }

        TeamApiKey key = teamApiKeyMapper.selectById(keyId);
        if (key == null || !key.getTeamId().equals(teamId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "API密钥不存在");
        }

        teamApiKeyMapper.deleteById(keyId);
    }

    public BigDecimal getTeamBalance(Long teamId) {
        Team team = getTeamOrThrow(teamId);
        return team.getBalance();
    }

    @Transactional
    public void rechargeTeamBalance(Long userId, Long teamId, BigDecimal amount) {
        TeamMember member = getTeamMember(teamId, userId);
        if (member == null || !isAdminRole(member.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有所有者或管理员可以充值团队余额");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "充值金额必须大于0");
        }

        User user = userMapper.selectById(userId);
        if (user.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "个人余额不足");
        }

        billingService.deductBalance(userId, amount, "团队余额充值-团队ID:" + teamId, null);

        Team team = teamMapper.selectById(teamId);
        team.setBalance(team.getBalance().add(amount));
        teamMapper.updateById(team);
    }

    public TeamApiKey validateTeamApiKey(String keyValue) {
        TeamApiKey apiKey = teamApiKeyMapper.selectOne(
                new LambdaQueryWrapper<TeamApiKey>().eq(TeamApiKey::getKeyValue, keyValue)
        );
        if (apiKey == null) {
            throw new BusinessException(ErrorCode.API_KEY_INVALID, "团队API密钥不存在");
        }
        if (apiKey.getStatus() == 0) {
            throw new BusinessException(ErrorCode.API_KEY_INVALID, "团队API密钥已被禁用");
        }
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.API_KEY_INVALID, "团队API密钥已过期");
        }

        Team team = teamMapper.selectById(apiKey.getTeamId());
        if (team == null || team.getStatus() == 0) {
            throw new BusinessException(ErrorCode.API_KEY_INVALID, "团队API密钥所属团队不可用");
        }

        return apiKey;
    }

    @Transactional
    public void deductTeamBalance(Long teamId, BigDecimal cost) {
        if (cost.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Team team = teamMapper.selectById(teamId);
        if (team.getBalance().compareTo(cost) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "团队余额不足，当前余额: " + team.getBalance());
        }
        team.setBalance(team.getBalance().subtract(cost));
        teamMapper.updateById(team);
    }

    @Transactional
    public void updateTeamApiKeyUsage(Long keyId, int tokens) {
        if (tokens <= 0) {
            return;
        }
        TeamApiKey key = teamApiKeyMapper.selectById(keyId);
        if (key != null) {
            key.setUsedQuota(key.getUsedQuota() + tokens);
            key.setLastUsedAt(LocalDateTime.now());
            teamApiKeyMapper.updateById(key);
        }
    }

    private Team getTeamOrThrow(Long teamId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "团队不存在");
        }
        return team;
    }

    private TeamMember getTeamMember(Long teamId, Long userId) {
        return teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
                        .eq(TeamMember::getUserId, userId)
        );
    }

    private boolean isAdminRole(String role) {
        return "owner".equals(role) || "admin".equals(role);
    }

    private String generateTeamApiKey() {
        byte[] randomBytes = new byte[36];
        SECURE_RANDOM.nextBytes(randomBytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return "sk-team-" + randomPart.substring(0, 44);
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() <= 12) {
            return key;
        }
        return key.substring(0, 8) + "****" + key.substring(key.length() - 4);
    }
}
