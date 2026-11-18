package com.example.battle_assistant.repository;

import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.example.battle_assistant.model.CatalogDetails;
import com.example.battle_assistant.model.CatalogEntry;

@Repository
public class CatalogRepository {
    private static final RowMapper<CatalogEntry> ROW = (rs, row) -> new CatalogEntry(
            rs.getLong("id"), rs.getString("kind"), new CatalogDetails(
                    rs.getString("name"), rs.getString("description"), rs.getString("category"),
                    rs.getString("element"), rs.getObject("power", Integer.class),
                    rs.getObject("accuracy", Integer.class), rs.getObject("priority", Integer.class),
                    rs.getObject("pp", Integer.class)));

    private final JdbcTemplate jdbc;

    public CatalogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CatalogEntry> findAll(String kind) {
        return jdbc.query("SELECT * FROM catalog_entries WHERE kind = ? ORDER BY id", ROW, kind);
    }

    public Optional<CatalogEntry> findById(String kind, long id) {
        return jdbc.query("SELECT * FROM catalog_entries WHERE kind = ? AND id = ?", ROW, kind, id)
                .stream().findFirst();
    }

    public CatalogEntry create(String kind, CatalogDetails details) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO catalog_entries
                        (kind, name, description, category, element, power, accuracy, priority, pp)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, kind);
            Object[] fields = fields(details);
            for (int i = 0; i < fields.length; i++) {
                statement.setObject(i + 2, fields[i]);
            }
            return statement;
        }, key);
        return new CatalogEntry(key.getKey().longValue(), kind, details);
    }

    public Optional<CatalogEntry> update(String kind, long id, CatalogDetails details) {
        var fields = details;
        int count = jdbc.update("""
                UPDATE catalog_entries SET name = ?, description = ?, category = ?, element = ?,
                    power = ?, accuracy = ?, priority = ?, pp = ?
                WHERE kind = ? AND id = ?
                """, fields.name(), fields.description(), fields.category(), fields.element(),
                fields.power(), fields.accuracy(), fields.priority(), fields.pp(), kind, id);
        return count == 0 ? Optional.empty() : Optional.of(new CatalogEntry(id, kind, details));
    }

    public boolean delete(String kind, long id) {
        return jdbc.update("DELETE FROM catalog_entries WHERE kind = ? AND id = ?", kind, id) != 0;
    }

    private Object[] fields(CatalogDetails details) {
        return new Object[]{details.name(), details.description(), details.category(), details.element(),
                details.power(), details.accuracy(), details.priority(), details.pp()};
    }
}
