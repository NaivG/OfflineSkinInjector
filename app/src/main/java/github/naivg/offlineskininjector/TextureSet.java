package github.naivg.offlineskininjector;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * The textures injected into the authlib profile: skin and cape URLs plus the skin model.
 *
 * <p>The skin is required; the cape is optional and omitted from the payload when absent. The
 * client reuses the cape for elytra, so this set has no separate elytra texture.
 */
public final class TextureSet {

    private static final String DEFAULT_MODEL = "classic";

    private final String skinUrl;
    private final String capeUrl;
    private final String model;

    /**
     * A texture set without a cape.
     *
     * @param skinUrl the URL the skin is downloaded from
     * @param model  the model metadata, {@code classic} or {@code slim}
     */
    public TextureSet(String skinUrl, String model) {
        this(skinUrl, null, model);
    }

    /**
     * A texture set with an optional cape.
     *
     * @param skinUrl the URL the skin is downloaded from
     * @param capeUrl the URL the cape is downloaded from, or {@code null} when there is none
     * @param model  the model metadata, {@code classic} or {@code slim}
     */
    public TextureSet(String skinUrl, String capeUrl, String model) {
        if (skinUrl == null) {
            throw new IllegalArgumentException("skinUrl is required");
        }
        this.skinUrl = skinUrl;
        this.capeUrl = capeUrl;
        this.model = model == null ? DEFAULT_MODEL : model;
    }

    /** The URL the skin is downloaded from. */
    public String skinUrl() {
        return skinUrl;
    }

    /** The URL the cape is downloaded from, or {@code null} when there is no cape. */
    public String capeUrl() {
        return capeUrl;
    }

    /** The model metadata of the skin, {@code classic} or {@code slim}. */
    public String model() {
        return model;
    }

    /** Whether this set carries a cape. */
    public boolean hasCape() {
        return capeUrl != null && !capeUrl.isEmpty();
    }

    /**
     * The textures document the vanilla client expects, for example
     * {@code {"textures":{"SKIN":{"url":"...","metadata":{"model":"classic"}}}}}.
     *
     * <p>The cape entry is omitted completely when the set has no cape.
     */
    public String json() {
        StringBuilder json = new StringBuilder();
        json.append("{\"textures\":{\"SKIN\":{\"url\":\"").append(skinUrl).append('"');
        json.append(",\"metadata\":{\"model\":\"").append(model).append("\"}}");
        if (hasCape()) {
            json.append(",\"CAPE\":{\"url\":\"").append(capeUrl).append("\"}");
        }
        json.append("}}");
        return json.toString();
    }

    /** The base64 encoded {@link #json()}, ready to be used as a packed textures property. */
    public String payload() {
        return Base64.getEncoder().encodeToString(json().getBytes(StandardCharsets.UTF_8));
    }
}
