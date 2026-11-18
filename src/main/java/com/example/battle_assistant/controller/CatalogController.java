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

import com.example.battle_assistant.model.CatalogDetails;
import com.example.battle_assistant.model.CatalogEntry;
import com.example.battle_assistant.service.CatalogService;

@RestController
@RequestMapping("/api/{kind:skills|traits|items}")
public class CatalogController {
    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    @GetMapping
    public List<CatalogEntry> list(@PathVariable String kind) {
        return service.findAll(kind);
    }

    @GetMapping("/{id}")
    public CatalogEntry get(@PathVariable String kind, @PathVariable long id) {
        return service.findById(kind, id).orElseThrow(this::notFound);
    }

    @PostMapping
    public ResponseEntity<CatalogEntry> create(@PathVariable String kind, @RequestBody CatalogDetails request) {
        CatalogEntry entry = service.create(kind, request);
        return ResponseEntity.created(URI.create("/api/" + kind + "/" + entry.id())).body(entry);
    }

    @PutMapping("/{id}")
    public CatalogEntry update(@PathVariable String kind, @PathVariable long id,
                               @RequestBody CatalogDetails request) {
        return service.update(kind, id, request).orElseThrow(this::notFound);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String kind, @PathVariable long id) {
        if (!service.delete(kind, id)) {
            throw notFound();
        }
        return ResponseEntity.noContent().build();
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "资料不存在");
    }
}
