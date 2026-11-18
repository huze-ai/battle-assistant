package com.example.battle_assistant.dto;

import java.util.List;

// 客户端只提交资料，id 由服务端生成。
public record CreateElfRequest(String name, List<String> types) {
}
