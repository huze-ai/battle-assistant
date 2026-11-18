package com.example.battle_assistant.preparation;

public record AvatarEntry(String avatarKey, String speciesKey, String name, String form, String popularTrait) {
    public AvatarEntry {
        if (avatarKey == null || !avatarKey.matches("avatars/[A-Za-z0-9_/-]+\\.(png|jpg|jpeg)")
                || avatarKey.contains("..") || avatarKey.length() > 240)
            throw new IllegalArgumentException("头像 Key 需为 avatars/ 下的 png/jpg/jpeg，使用英文、数字、横线或下划线");
        speciesKey = required(speciesKey, 80);
        name = required(name, 120);
        form = required(form, 120);
        popularTrait = required(popularTrait, 120);
    }
    private static String required(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max)
            throw new IllegalArgumentException("精灵标识、名称、形态、热门特性必填且不可超长");
        return value.strip();
    }
}
