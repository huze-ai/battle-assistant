package com.example.battle_assistant.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.battle_assistant.model.Elf;

@Repository
public class ElfRepository {
    private static final String SELECT = """
            SELECT e.id, e.name, t.type_name
            FROM elves e
            LEFT JOIN elf_types t ON t.elf_id = e.id
            """;
    private static final String ORDER = " ORDER BY e.id, t.type_order";
    private final JdbcTemplate jdbc;

    public ElfRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Elf create(String name, List<String> types) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO elves (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, name);
            return statement;
        }, key);
        long id = key.getKey().longValue();
        insertTypes(id, types);
        return new Elf(id, name, types);
    }

    public List<Elf> findAll() {
        return jdbc.query(SELECT + ORDER, this::readElves);
    }

    public Optional<Elf> findById(long id) {
        List<Elf> records = jdbc.query(SELECT + " WHERE e.id = ?" + ORDER, this::readElves, id);
        return records.stream().findFirst();
    }

    @Transactional
    public Optional<Elf> update(long id, String name, List<String> types) {
        // Lock the parent row so concurrent updates cannot interleave changes to the types.
        if (jdbc.queryForList("SELECT id FROM elves WHERE id = ? FOR UPDATE", Long.class, id).isEmpty()) {
            return Optional.empty();
        }
        jdbc.update("UPDATE elves SET name = ? WHERE id = ?", name, id);
        jdbc.update("DELETE FROM elf_types WHERE elf_id = ?", id);
        insertTypes(id, types);
        return Optional.of(new Elf(id, name, types));
    }

    public boolean delete(long id) {
        return jdbc.update("DELETE FROM elves WHERE id = ?", id) != 0;
    }

    private void insertTypes(long id, List<String> types) {
        List<Object[]> rows = new ArrayList<>();
        for (int index = 0; index < types.size(); index++) {
            rows.add(new Object[]{id, index, types.get(index)});
        }
        jdbc.batchUpdate("INSERT INTO elf_types (elf_id, type_order, type_name) VALUES (?, ?, ?)", rows);
    }

    private List<Elf> readElves(ResultSet rs) throws SQLException {
        record ElfRow(String name, List<String> types) {}
        var rows = new LinkedHashMap<Long, ElfRow>();
        while (rs.next()) {
            long id = rs.getLong("id");
            var elf = rows.get(id);
            if (elf == null) {
                elf = new ElfRow(rs.getString("name"), new ArrayList<>());
                rows.put(id, elf);
            }
            String type = rs.getString("type_name");
            if (type != null) {
                elf.types().add(type);
            }
        }
        return rows.entrySet().stream()
                .map(entry -> new Elf(entry.getKey(), entry.getValue().name(), entry.getValue().types()))
                .toList();
    }
}
