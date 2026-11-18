CREATE TABLE IF NOT EXISTS elves (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL
);

CREATE TABLE IF NOT EXISTS elf_types (
    elf_id BIGINT NOT NULL,
    type_order INTEGER NOT NULL,
    type_name VARCHAR(60) NOT NULL,
    PRIMARY KEY (elf_id, type_order),
    CONSTRAINT fk_elf_types_elf FOREIGN KEY (elf_id) REFERENCES elves (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS catalog_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kind VARCHAR(16) NOT NULL CHECK (kind IN ('skills', 'traits', 'items')),
    name VARCHAR(120) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    category VARCHAR(60),
    element VARCHAR(60),
    power INTEGER,
    accuracy INTEGER,
    priority INTEGER,
    pp INTEGER
);

-- 新增表，已有精灵和资料数据保持不变。
CREATE TABLE IF NOT EXISTS teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL
);
CREATE TABLE IF NOT EXISTS team_members (
    team_id BIGINT NOT NULL,
    position INTEGER NOT NULL,
    elf_id BIGINT NOT NULL,
    nickname VARCHAR(120),
    level INTEGER,
    trait_id BIGINT,
    item_id BIGINT,
    hp INTEGER,
    attack INTEGER,
    defense INTEGER,
    special_attack INTEGER,
    special_defense INTEGER,
    speed INTEGER,
    PRIMARY KEY (team_id, position),
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    FOREIGN KEY (elf_id) REFERENCES elves(id),
    FOREIGN KEY (trait_id) REFERENCES catalog_entries(id),
    FOREIGN KEY (item_id) REFERENCES catalog_entries(id)
);
CREATE TABLE IF NOT EXISTS team_member_skills (
    team_id BIGINT NOT NULL,
    position INTEGER NOT NULL,
    slot INTEGER NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (team_id, position, slot),
    FOREIGN KEY (team_id, position) REFERENCES team_members(team_id, position) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES catalog_entries(id)
);

-- 一行对应一个可识别外观；多张头像可以映射到相同精灵及形态。
CREATE TABLE IF NOT EXISTS preparation_avatars (
    avatar_key VARCHAR(240) PRIMARY KEY,
    species_key VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    form VARCHAR(120) NOT NULL,
    popular_trait VARCHAR(120) NOT NULL
);
