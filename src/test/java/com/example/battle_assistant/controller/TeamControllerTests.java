package com.example.battle_assistant.controller;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.example.battle_assistant.model.Team;
import com.example.battle_assistant.service.TeamService;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TeamControllerTests {
    private TeamService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(TeamService.class);
        mvc = MockMvcBuilders.standaloneSetup(new TeamController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void createReturns201AndLocation() throws Exception {
        when(service.create(any())).thenReturn(new Team(5, "队伍", List.of()));
        mvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"队伍","members":[]}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/teams/5"))
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void missingTeamAndMalformedRequestHaveErrors() throws Exception {
        when(service.findById(99)).thenReturn(Optional.empty());
        mvc.perform(get("/api/teams/99")).andExpect(status().isNotFound());
        mvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/teams/not-a-number")).andExpect(status().isBadRequest());
        verify(service, never()).create(any());
    }

    @Test
    void deletionAndConstraintConflictAreReported() throws Exception {
        when(service.delete(1)).thenReturn(true);
        mvc.perform(delete("/api/teams/1")).andExpect(status().isNoContent());
        when(service.delete(2)).thenThrow(new DataIntegrityViolationException("private SQL"));
        mvc.perform(delete("/api/teams/2")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "数据约束冲突；资料可能被队伍引用或已被其他请求修改，请刷新后重试"));
    }
}
