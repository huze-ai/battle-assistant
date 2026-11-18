package com.example.battle_assistant.repository;

import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.battle_assistant.model.*;

@Repository
public class TeamRepository {
    private final JdbcTemplate jdbc;

    public TeamRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public List<Team> findAll() {
        return jdbc.query("SELECT id, name FROM teams ORDER BY id",
                (rs, row) -> new Team(rs.getLong("id"), rs.getString("name"), List.of()))
                .stream().map(t -> new Team(t.id(), t.name(), members(t.id()))).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Team> findById(long id) {
        return jdbc.query("SELECT id, name FROM teams WHERE id = ?",
                (rs, row) -> new Team(id, rs.getString("name"), List.of()), id)
                .stream().findFirst().map(t -> new Team(id, t.name(), members(id)));
    }

    public Team create(String name, List<TeamMember> members) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO teams (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, name);
            return statement;
        }, key);
        long id = key.getKey().longValue();
        insertMembers(id, members);
        return new Team(id, name, members);
    }

    public Optional<Team> update(long id, String name, List<TeamMember> members) {
        if (jdbc.queryForList("SELECT id FROM teams WHERE id = ? FOR UPDATE", Long.class, id).isEmpty()) {
            return Optional.empty();
        }
        jdbc.update("UPDATE teams SET name = ? WHERE id = ?", name, id);
        jdbc.update("DELETE FROM team_members WHERE team_id = ?", id);
        insertMembers(id, members);
        return Optional.of(new Team(id, name, members));
    }

    public boolean delete(long id) {
        return jdbc.update("DELETE FROM teams WHERE id = ?", id) != 0;
    }

    private void insertMembers(long teamId, List<TeamMember> members) {
        for (int position = 0; position < members.size(); position++) {
            TeamMember member = members.get(position);
            BattleStats stats = member.stats();
            jdbc.update("""
                    INSERT INTO team_members
                    (team_id, position, elf_id, nickname, level, trait_id, item_id,
                     hp, attack, defense, special_attack, special_defense, speed)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, teamId, position, member.elfId(), member.nickname(), member.level(),
                    member.traitId(), member.itemId(), stats.hp(), stats.attack(), stats.defense(),
                    stats.specialAttack(), stats.specialDefense(), stats.speed());
            for (int slot = 0; slot < member.skillIds().size(); slot++) {
                jdbc.update("INSERT INTO team_member_skills (team_id, position, slot, skill_id) VALUES (?, ?, ?, ?)",
                        teamId, position, slot, member.skillIds().get(slot));
            }
        }
    }

    private List<TeamMember> members(long teamId) {
        return jdbc.query("SELECT * FROM team_members WHERE team_id = ? ORDER BY position", (rs, row) -> {
            int position = rs.getInt("position");
            List<Long> skills = jdbc.queryForList(
                    "SELECT skill_id FROM team_member_skills WHERE team_id = ? AND position = ? ORDER BY slot",
                    Long.class, teamId, position);
            return new TeamMember(rs.getLong("elf_id"), rs.getString("nickname"),
                    rs.getObject("level", Integer.class), rs.getObject("trait_id", Long.class),
                    rs.getObject("item_id", Long.class), skills, new BattleStats(
                    rs.getObject("hp", Integer.class), rs.getObject("attack", Integer.class),
                    rs.getObject("defense", Integer.class), rs.getObject("special_attack", Integer.class),
                    rs.getObject("special_defense", Integer.class), rs.getObject("speed", Integer.class)));
        }, teamId);
    }
}
