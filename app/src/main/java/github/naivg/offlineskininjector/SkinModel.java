package github.naivg.offlineskininjector;

import java.util.Locale;

/**
 * The player model a skin was drawn for.
 *
 * <p>The model is not encoded in the PNG itself; the client reads it from the {@code model}
 * entry of the skin metadata. {@link #AUTO} makes the agent inspect the arm layout and resolve
 * it to {@link #CLASSIC} or {@link #SLIM} before the metadata is built.
 */
public enum SkinModel {

    /** Detect the model from the skin's arm layout. */
    AUTO,
    /** 4-pixel arms (Steve). */
    CLASSIC,
    /** 3-pixel arms (Alex). */
    SLIM;

    static SkinModel parse(String raw) {
        if (raw == null) {
            return AUTO;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty() || "auto".equals(value)) {
            return AUTO;
        }
        if ("classic".equals(value)) {
            return CLASSIC;
        }
        if ("slim".equals(value)) {
            return SLIM;
        }
        throw new IllegalArgumentException(
                "-Dofflineskininjector.model must be auto, classic or slim");
    }

    /** The value written to the {@code model} metadata entry. */
    String metadata() {
        if (this == AUTO) {
            throw new IllegalStateException("AUTO has no metadata model");
        }
        return name().toLowerCase(Locale.ROOT);
    }
}
