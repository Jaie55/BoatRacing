package es.jaie55.boatracing.cosmetics;

import java.util.Locale;

/** Cosmetic categories used by the shop and the admin unlock commands. */
public enum CosmeticCategory {
    TRAIL("trail"),
    TITLE("title"),
    EFFECT("effect"),
    SOUND("sound"),
    CHECKPOINT("checkpoint");

    private final String id;

    CosmeticCategory(String id) {
        this.id = id;
    }

    public String id() { return id; }

    public String messageKey() { return "cosmetics.category." + id; }

    public static CosmeticCategory byId(String id) {
        if (id == null || id.isBlank()) return null;
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (CosmeticCategory category : values()) {
            if (category.id.equals(normalized)) return category;
        }
        return null;
    }
}
