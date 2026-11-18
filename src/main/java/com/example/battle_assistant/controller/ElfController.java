package com.example.battle_assistant.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.battle_assistant.dto.CreateElfRequest;
import com.example.battle_assistant.model.Elf;
import com.example.battle_assistant.service.ElfService;

@RestController
@RequestMapping("/api/elves")
public class ElfController {
    private final ElfService elfService;

    // Spring 通过构造方法把 ElfService 注入进来。
    public ElfController(ElfService elfService) {
        this.elfService = elfService;
    }

    @GetMapping
    public List<Elf> list() {
        return elfService.findAll();
    }

    @GetMapping("/{id}")
    public Elf get(@PathVariable long id) {
        return elfService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "精灵资料不存在"));
    }

    @PostMapping
    public ResponseEntity<Elf> create(@RequestBody CreateElfRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求内容不能为空");
        }
        Elf elf = elfService.create(request.name(), request.types());
        return ResponseEntity.created(URI.create("/api/elves/" + elf.id())).body(elf);
    }

    @PutMapping("/{id}")
    public Elf update(@PathVariable long id, @RequestBody CreateElfRequest request) {
        return elfService.update(id, request.name(), request.types())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "精灵资料不存在"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        if (!elfService.delete(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "精灵资料不存在");
        }
        return ResponseEntity.noContent().build();
    }
}
