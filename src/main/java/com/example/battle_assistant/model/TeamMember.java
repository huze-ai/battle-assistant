package com.example.battle_assistant.model;

import java.util.List;

public record TeamMember(Long elfId, String nickname, Integer level, Long traitId, Long itemId,
                         List<Long> skillIds, BattleStats stats) {}
