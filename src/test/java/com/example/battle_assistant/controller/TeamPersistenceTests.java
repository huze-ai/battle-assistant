package com.example.battle_assistant.controller;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.example.battle_assistant.dto.TeamRequest;
import com.example.battle_assistant.model.*;
import com.example.battle_assistant.repository.*;
import com.example.battle_assistant.service.TeamService;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "BATTLE_TEST_MYSQL", matches = "1")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TeamPersistenceTests {
    @Autowired private ElfRepository elves;
    @Autowired private CatalogRepository catalog;
    @Autowired private TeamService teams;

    @Test
    void storesMemberRelationsUpdatesAndCascadesOnlyTeamData() {
        Elf elf = elves.create("队伍集成测试精灵", List.of("水"));
        CatalogEntry skill = catalog.create("skills",
                new CatalogDetails("测试技能", "测试效果", null, null, null, null, null, null));
        TeamMember member = new TeamMember(elf.id(), "主力", 50, null, null,
                List.of(skill.id()), new BattleStats(200, null, null, null, null, 100));
        Team created = teams.create(new TeamRequest("测试队伍", List.of(member)));
        assertEquals(List.of(member), teams.findById(created.id()).orElseThrow().members());

        assertThrows(DataIntegrityViolationException.class, () -> elves.delete(elf.id()));
        assertThrows(DataIntegrityViolationException.class, () -> catalog.delete("skills", skill.id()));
        assertThrows(IllegalArgumentException.class, () -> teams.update(created.id(), new TeamRequest(
                "无效修改", List.of(new TeamMember(elf.id(), null, null, skill.id(), null, List.of(), null)))));
        assertEquals("测试队伍", teams.findById(created.id()).orElseThrow().name());

        assertTrue(teams.update(created.id(), new TeamRequest("空队伍", List.of()))
                .orElseThrow().members().isEmpty());
        assertTrue(teams.delete(created.id()));
        assertTrue(teams.findById(created.id()).isEmpty());
        assertTrue(elves.findById(elf.id()).isPresent());
        assertTrue(catalog.findById("skills", skill.id()).isPresent());
        assertTrue(catalog.delete("skills", skill.id()));
        assertTrue(elves.delete(elf.id()));
    }
}
