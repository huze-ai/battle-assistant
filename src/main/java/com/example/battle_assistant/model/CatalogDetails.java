package com.example.battle_assistant.model;

// 未核实的数值可以留空；description 保存游戏内效果说明。
public record CatalogDetails(String name, String description, String category,
                             String element, Integer power, Integer accuracy,
                             Integer priority, Integer pp) {
}
