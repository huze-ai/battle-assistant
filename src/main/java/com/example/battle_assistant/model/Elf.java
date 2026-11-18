package com.example.battle_assistant.model;

import java.util.List;

// 先只记录名称和属性，其他游戏资料后续逐步添加。
public record Elf(long id, String name, List<String> types) {
    public Elf {
        types = List.copyOf(types);
    }
}
