package com.example.battle_assistant.model;

import java.util.List;

public record Team(long id, String name, List<TeamMember> members) {}
