package com.example.battle_assistant.preparation;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AvatarRepository {
    private final JdbcTemplate jdbc;
    public AvatarRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<AvatarEntry> list() {
        return jdbc.query("SELECT * FROM preparation_avatars ORDER BY avatar_key", (rs, n) ->
            new AvatarEntry(rs.getString("avatar_key"), rs.getString("species_key"), rs.getString("name"),
                rs.getString("form"), rs.getString("popular_trait")));
    }
    public void save(AvatarEntry e) {
        jdbc.update("""
            INSERT INTO preparation_avatars (avatar_key, species_key, name, form, popular_trait)
            VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE
            species_key = VALUES(species_key), name = VALUES(name), form = VALUES(form),
            popular_trait = VALUES(popular_trait)
            """, e.avatarKey(), e.speciesKey(), e.name(), e.form(), e.popularTrait());
    }
    public void delete(String key) { jdbc.update("DELETE FROM preparation_avatars WHERE avatar_key = ?", key); }
}
