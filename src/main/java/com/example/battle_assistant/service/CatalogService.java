package com.example.battle_assistant.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.battle_assistant.model.CatalogDetails;
import com.example.battle_assistant.model.CatalogEntry;
import com.example.battle_assistant.repository.CatalogRepository;

@Service
public class CatalogService {
    private final CatalogRepository repository;

    public CatalogService(CatalogRepository repository) {
        this.repository = repository;
    }

    public List<CatalogEntry> findAll(String kind) {
        return repository.findAll(kind);
    }

    public Optional<CatalogEntry> findById(String kind, long id) {
        return repository.findById(kind, id);
    }

    public CatalogEntry create(String kind, CatalogDetails details) {
        return repository.create(kind, validate(kind, details));
    }

    public Optional<CatalogEntry> update(String kind, long id, CatalogDetails details) {
        return repository.update(kind, id, validate(kind, details));
    }

    public boolean delete(String kind, long id) {
        return repository.delete(kind, id);
    }

    private CatalogDetails validate(String kind, CatalogDetails details) {
        String name = text(details.name(), 120, "名称");
        String description = text(details.description(), 4000, "效果说明");
        if (name == null || description == null) {
            throw new IllegalArgumentException("名称和效果说明不能为空");
        }

        String category = text(details.category(), 60, "分类");
        String element = text(details.element(), 60, "技能属性");
        Integer power = details.power();
        Integer accuracy = details.accuracy();
        Integer priority = details.priority();
        Integer pp = details.pp();
        if (!kind.equals("skills") && (element != null || power != null || accuracy != null
                || priority != null || pp != null)) {
            throw new IllegalArgumentException("技能属性、威力、命中率、优先级和 PP 只能用于技能");
        }
        if ((power != null && power < 0) || (pp != null && pp < 0)
                || (accuracy != null && (accuracy < 0 || accuracy > 100))) {
            throw new IllegalArgumentException("威力和 PP 不能为负数，命中率应为 0 到 100");
        }
        return new CatalogDetails(name, description, category, element, power, accuracy, priority, pp);
    }

    private String text(String value, int max, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > max) {
            throw new IllegalArgumentException(field + "最多 " + max + " 个字符");
        }
        return normalized;
    }
}
