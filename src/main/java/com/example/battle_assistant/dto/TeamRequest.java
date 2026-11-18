package com.example.battle_assistant.dto;

import java.util.List;
import com.example.battle_assistant.model.TeamMember;

public record TeamRequest(String name, List<TeamMember> members) {}
