package com.example.battle_assistant.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.battle_assistant.model.Elf;
import com.example.battle_assistant.repository.ElfRepository;

@Service
public class ElfService {
    private final ElfRepository repository;

    public ElfService(ElfRepository repository) {
        this.repository = repository;
    }

    public Elf create(String name, List<String> types) {
        return repository.create(validateName(name), normalizeTypes(types));
    }

    public List<Elf> findAll() {
        return repository.findAll();
    }

    public Optional<Elf> findById(long id) {
        return repository.findById(id);
    }

    public Optional<Elf> update(long id, String name, List<String> types) {
        return repository.update(id, validateName(name), normalizeTypes(types));
    }

    public boolean delete(long id) {
        return repository.delete(id);
    }

    private String validateName(String name) {
        if (name == null || name.isBlank() || name.strip().length() > 120) {
            throw new IllegalArgumentException("名称不能为空，且最多 120 个字符");
        }
        return name.strip();
    }

    private List<String> normalizeTypes(List<String> types) {
        if (types == null || types.isEmpty()
                || types.stream().anyMatch(type -> type == null || type.isBlank() || type.strip().length() > 60)) {
            throw new IllegalArgumentException("请提供至少一个非空属性名称，每个最多 60 个字符");
        }
        return types.stream().map(String::strip).distinct().toList();
    }
}
