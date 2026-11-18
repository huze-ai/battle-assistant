package com.example.battle_assistant.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.battle_assistant.dto.TeamRequest;
import com.example.battle_assistant.model.*;
import com.example.battle_assistant.repository.*;

@Service
public class TeamService {
    private final TeamRepository teams;
    private final ElfRepository elves;
    private final CatalogRepository catalog;

    public TeamService(TeamRepository teams, ElfRepository elves, CatalogRepository catalog) {
        this.teams = teams;
        this.elves = elves;
        this.catalog = catalog;
    }

    public List<Team> findAll() { return teams.findAll(); }
    public Optional<Team> findById(long id) { return teams.findById(id); }
    public boolean delete(long id) { return teams.delete(id); }

    @Transactional
    public Team create(TeamRequest request) {
        List<TeamMember> members = validate(request);
        return teams.create(request.name().strip(), members);
    }

    @Transactional
    public Optional<Team> update(long id, TeamRequest request) {
        List<TeamMember> members = validate(request);
        return teams.update(id, request.name().strip(), members);
    }

    private List<TeamMember> validate(TeamRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()
                || request.name().strip().length() > 120) {
            throw new IllegalArgumentException("队伍名称不能为空，且最多 120 个字符");
        }
        // 这是资料草稿的技术上限，不作为游戏对战模式的合法性判断。
        if (request.members() == null || request.members().size() > 100) {
            throw new IllegalArgumentException("members 必须为数组，最多 100 名成员；允许空队伍草稿");
        }
        List<TeamMember> members = new ArrayList<>();
        for (TeamMember member : request.members()) {
            if (member == null || member.elfId() == null || member.elfId() <= 0
                    || elves.findById(member.elfId()).isEmpty()) {
                throw new IllegalArgumentException("成员引用的精灵不存在");
            }
            if (member.nickname() != null && member.nickname().strip().length() > 120) {
                throw new IllegalArgumentException("昵称最多 120 个字符");
            }
            positive(member.level(), "等级");
            reference("traits", member.traitId());
            reference("items", member.itemId());
            List<Long> skills = member.skillIds() == null ? List.of() : member.skillIds();
            if (skills.size() > 16 || new HashSet<>(skills).size() != skills.size()) {
                throw new IllegalArgumentException("技能最多 16 个，且不能重复");
            }
            for (Long id : skills) {
                if (id == null) throw new IllegalArgumentException("技能 ID 不能为空");
                reference("skills", id);
            }
            BattleStats stats = member.stats() == null
                    ? new BattleStats(null, null, null, null, null, null) : member.stats();
            positive(stats.hp(), "最大生命值");
            positive(stats.attack(), "攻击");
            positive(stats.defense(), "防御");
            positive(stats.specialAttack(), "特攻");
            positive(stats.specialDefense(), "特防");
            positive(stats.speed(), "速度");
            members.add(new TeamMember(member.elfId(),
                    member.nickname() == null ? null : member.nickname().strip(),
                    member.level(), member.traitId(), member.itemId(), List.copyOf(skills), stats));
        }
        return List.copyOf(members);
    }

    private void reference(String kind, Long id) {
        if (id != null && (id <= 0 || catalog.findById(kind, id).isEmpty())) {
            throw new IllegalArgumentException(kind + " 引用不存在或资料类型不匹配");
        }
    }

    private void positive(Integer value, String field) {
        if (value != null && value <= 0) throw new IllegalArgumentException(field + "必须大于 0，未知时留空");
    }
}
