package com.example.battle_assistant.controller;

import java.net.URI;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.example.battle_assistant.dto.TeamRequest;
import com.example.battle_assistant.model.Team;
import com.example.battle_assistant.service.TeamService;

@RestController
@RequestMapping("/api/teams")
public class TeamController {
    private final TeamService service;
    public TeamController(TeamService service) { this.service = service; }

    @GetMapping
    public List<Team> list() { return service.findAll(); }

    @GetMapping("/{id}")
    public Team get(@PathVariable long id) { return service.findById(id).orElseThrow(this::notFound); }

    @PostMapping
    public ResponseEntity<Team> create(@RequestBody TeamRequest request) {
        Team team = service.create(request);
        return ResponseEntity.created(URI.create("/api/teams/" + team.id())).body(team);
    }

    @PutMapping("/{id}")
    public Team update(@PathVariable long id, @RequestBody TeamRequest request) {
        return service.update(id, request).orElseThrow(this::notFound);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        if (!service.delete(id)) throw notFound();
        return ResponseEntity.noContent().build();
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "队伍不存在");
    }
}
