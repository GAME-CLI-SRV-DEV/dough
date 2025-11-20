package io.github.bakedlibs.dough.skins;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

import io.github.bakedlibs.dough.reflection.ReflectionUtils;
import io.github.bakedlibs.dough.versions.MinecraftVersion;
import io.github.bakedlibs.dough.versions.UnknownServerVersionException;

public final class CustomGameProfile {

    private static final String PLAYER_NAME = "CS-CoreLib";
    private static final String PROPERTY_KEY = "textures";

    private final GameProfile delegate;
    private final @Nullable String texture;   // base64 JSON payload
    private final @Nullable URL skinUrl;      // provided or derived

    public CustomGameProfile(@Nonnull UUID uuid, @Nullable String texture, @Nullable URL url) {
        this.delegate = new GameProfile(Objects.requireNonNull(uuid), PLAYER_NAME);
        this.texture = normalize(texture);
        this.skinUrl = url != null ? url : deriveUrlFromTexture(this.texture);

        if (this.texture != null) {
            PropertyMap props = delegate.properties(); // Authlib 7.x accessor
            props.put(PROPERTY_KEY, new Property(PROPERTY_KEY, this.texture));
        }
    }

    public void apply(@Nonnull SkullMeta meta)
            throws NoSuchFieldException, IllegalAccessException, UnknownServerVersionException {
        if (this.texture == null && this.skinUrl == null) {
            return; // prevent Steve fallback
        }

        if (MinecraftVersion.get().isAtLeast(MinecraftVersion.parse("1.20"))) {
            if (this.skinUrl != null) {
                PlayerProfile playerProfile = Bukkit.createPlayerProfile(this.delegate.id(), PLAYER_NAME);
                PlayerTextures playerTextures = playerProfile.getTextures();
                playerTextures.setSkin(this.skinUrl);
                playerProfile.setTextures(playerTextures);
                meta.setOwnerProfile(playerProfile);
                return;
            }
        }

        // Reflection path for older versions or base64-only case
        ReflectionUtils.setFieldValue(meta, "profile", this.delegate);
        meta.setOwningPlayer(meta.getOwningPlayer());
        ReflectionUtils.setFieldValue(meta, "profile", this.delegate);
    }

    @Nullable
    public String getBase64Texture() {
        return this.texture;
    }

    public GameProfile getHandle() {
        return this.delegate;
    }

    // --- helpers ---

    private @Nullable String normalize(@Nullable String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private @Nullable URL deriveUrlFromTexture(@Nullable String base64Texture) {
        if (base64Texture == null) return null;
        try {
            String json = new String(Base64.getDecoder().decode(base64Texture));
            String url = extractSkinUrl(json);
            return url != null ? new URL(url) : null;
        } catch (IllegalArgumentException | MalformedURLException e) {
            return null;
        }
    }

    private @Nullable String extractSkinUrl(String decodedJson) {
        int skinIdx = decodedJson.indexOf("\"SKIN\"");
        if (skinIdx == -1) return null;
        int urlKeyIdx = decodedJson.indexOf("\"url\":\"", skinIdx);
        if (urlKeyIdx == -1) return null;
        int start = urlKeyIdx + 7;
        int end = decodedJson.indexOf("\"", start);
        if (end == -1) return null;
        String candidate = decodedJson.substring(start, end);
        return candidate.startsWith("http") ? candidate : null;
    }
}