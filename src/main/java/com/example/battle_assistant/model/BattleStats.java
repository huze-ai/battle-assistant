package com.example.battle_assistant.model;

// 实际配置数值，不是种族值；未知时可以不填。
public record BattleStats(Integer hp, Integer attack, Integer defense,
                          Integer specialAttack, Integer specialDefense, Integer speed) {}
