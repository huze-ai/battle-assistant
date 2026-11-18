package com.example.battle_assistant.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.example.battle_assistant.dto.TeamRequest;
import com.example.battle_assistant.model.*;
import com.example.battle_assistant.repository.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamServiceTests {
    private TeamRepository teams;
    private ElfRepository elves;
    private CatalogRepository catalog;
    private TeamService service;

    @BeforeEach
    void setUp() {
        teams = mock(TeamRepository.class);
        elves = mock(ElfRepository.class);
        catalog = mock(CatalogRepository.class);
        service = new TeamService(teams, elves, catalog);
        when(elves.findById(1)).thenReturn(Optional.of(new Elf(1, "测试精灵", List.of("水"))));
    }

    private TeamMember member(Long traitId, List<Long> skills, BattleStats stats) {
        return new TeamMember(1L, " 队员 ", 50, traitId, null, skills, stats);
    }

    @Test
    void savesNormalizedTeamAndPreservesUnknownStats() {
        when(teams.create(eq("测试队伍"), anyList())).thenAnswer(invocation ->
                new Team(1, invocation.getArgument(0), invocation.getArgument(1)));
        Team team = service.create(new TeamRequest(" 测试队伍 ",
                List.of(member(null, null, null))));
        assertEquals("测试队伍", team.name());
        assertEquals("队员", team.members().getFirst().nickname());
        assertEquals(List.of(), team.members().getFirst().skillIds());
        assertNull(team.members().getFirst().stats().speed());
    }

    @Test
    void rejectsMissingOrWrongKindReferencesBeforeWriting() {
        assertThrows(IllegalArgumentException.class, () ->
                service.create(new TeamRequest("队伍", List.of(member(9L, List.of(), null)))));
        verifyNoInteractions(teams);
    }

    @Test
    void rejectsDuplicateSkillsNullMembersAndNonPositiveStats() {
        assertThrows(IllegalArgumentException.class, () -> service.create(
                new TeamRequest("队伍", List.of(member(null, List.of(2L, 2L), null)))));
        assertThrows(IllegalArgumentException.class, () -> service.create(
                new TeamRequest("队伍", java.util.Arrays.asList((TeamMember) null))));
        assertThrows(IllegalArgumentException.class, () -> service.create(
                new TeamRequest("队伍", List.of(member(null, List.of(),
                        new BattleStats(0, null, null, null, null, null))))));
        verifyNoInteractions(teams);
    }

    @Test
    void rejectsMissingElfAndAllowsEmptyDraft() {
        assertThrows(IllegalArgumentException.class, () -> service.create(new TeamRequest(
                "队伍", List.of(new TeamMember(99L, null, null, null, null, null, null)))));
        when(teams.create("草稿", List.of())).thenReturn(new Team(2, "草稿", List.of()));
        assertTrue(service.create(new TeamRequest("草稿", List.of())).members().isEmpty());
    }
}
