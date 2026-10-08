package com.alfy.api.service;
import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.TeamMemberResponse;
import com.alfy.api.dto.TeamMemberUpsertRequest;
import com.alfy.api.entity.TeamMember;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.TeamMemberMapper;
import com.alfy.api.security.AdminPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
@Service @RequiredArgsConstructor
public class TeamMemberService {
    private final TeamMemberMapper mapper; private final AdminOperationLogService logs;
    public List<TeamMemberResponse> list(boolean publicOnly) { return mapper.selectList(new LambdaQueryWrapper<TeamMember>().eq(publicOnly, TeamMember::getEnabled, 1).orderByAsc(TeamMember::getSortOrder).orderByAsc(TeamMember::getId)).stream().map(this::toResponse).toList(); }
    public TeamMemberResponse get(Long id) { return toResponse(require(id)); }
    @Transactional public TeamMemberResponse create(TeamMemberUpsertRequest r, AdminPrincipal p) { TeamMember x = new TeamMember(); apply(x, r); mapper.insert(x); logs.record(p.id(), "CREATE", "TEAM_MEMBER", x.getId(), "创建团队成员 " + x.getName()); return get(x.getId()); }
    @Transactional public TeamMemberResponse update(Long id, TeamMemberUpsertRequest r, AdminPrincipal p) { TeamMember x = require(id); if (r.version() == null || !r.version().equals(x.getVersion())) throw new BusinessException(ErrorCode.CONFLICT, "团队成员已被其他管理员修改，请刷新后重试"); apply(x, r); if (mapper.updateById(x) != 1) throw new BusinessException(ErrorCode.CONFLICT, "团队成员已被其他管理员修改"); logs.record(p.id(), "UPDATE", "TEAM_MEMBER", id, "更新团队成员 " + x.getName()); return get(id); }
    @Transactional public void delete(Long id, AdminPrincipal p) { TeamMember x = require(id); mapper.deleteById(id); logs.record(p.id(), "DELETE", "TEAM_MEMBER", id, "删除团队成员 " + x.getName()); }
    private void apply(TeamMember x, TeamMemberUpsertRequest r) { x.setRole(r.role().trim()); x.setName(r.name().trim()); x.setBio(trim(r.bio())); x.setPhotoMediaId(r.photoMediaId()); x.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder()); x.setEnabled(r.enabled() == null || Boolean.TRUE.equals(r.enabled()) ? 1 : 0); }
    private TeamMember require(Long id) { TeamMember x = mapper.selectById(id); if (x == null) throw new BusinessException(ErrorCode.NOT_FOUND, "团队成员不存在"); return x; }
    private TeamMemberResponse toResponse(TeamMember x) { return new TeamMemberResponse(x.getId(), x.getRole(), x.getName(), x.getBio(), x.getPhotoMediaId(), url(x.getPhotoMediaId()), x.getSortOrder(), Integer.valueOf(1).equals(x.getEnabled()), x.getVersion(), x.getUpdatedAt()); }
    private static String trim(String value) { return value == null ? null : value.trim(); }
    private static String url(Long id) { return id == null ? null : "/api/v1/public/media/" + id; }
}
