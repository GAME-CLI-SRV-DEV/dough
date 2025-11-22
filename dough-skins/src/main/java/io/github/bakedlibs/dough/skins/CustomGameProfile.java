package io.github.bakedlibs.dough.skins;

import java.net.URL;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;

import com.mojang.authlib.GameProfile;

import io.github.bakedlibs.dough.reflection.ReflectionUtils;
import io.github.bakedlibs.dough.versions.MinecraftVersion;
import io.github.bakedlibs.dough.versions.UnknownServerVersionException;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

public final class CustomGameProfile {

    private static final String PLAYER_NAME = "CS-CoreLib";

    private final GameProfile delegate;
    private final URL skinUrl;
    private final String texture; // base64 texture string

    public CustomGameProfile(@Nonnull UUID uuid, @Nullable String texture, @Nonnull URL url) {
        this.delegate = new GameProfile(uuid, PLAYER_NAME);
        this.skinUrl = url;
        this.texture = texture;
        // ⚠️ Do not mutate delegate.properties() on 1.21.9+ (immutable)
    }

    public GameProfile getDelegate() {
        return delegate;
    }

    public UUID getId() {
        return delegate.id(); // new API
    }

    public String getName() {
        return delegate.name(); // new API
    }

    @Nullable
    public String getBase64Texture() {
        return this.texture;
    }

    public URL getSkinUrl() {
        return this.skinUrl;
    }

    public void apply(@Nonnull SkullMeta meta)
            throws NoSuchFieldException, IllegalAccessException, UnknownServerVersionException {

        if (MinecraftVersion.get().isAtLeast(MinecraftVersion.parse("1.20"))) {
            PlayerProfile playerProfile = Bukkit.createPlayerProfile(this.getId(), PLAYER_NAME);
            PlayerTextures playerTextures = playerProfile.getTextures();

            // Prefer URL if available
            if (this.skinUrl != null) {
                playerTextures.setSkin(this.skinUrl);
            }

            // Some forks of Paper also support setting base64 directly:
            // if (this.texture != null) {
            //     playerTextures.setSkin(this.texture);
            // }

            playerProfile.setTextures(playerTextures);
            meta.setOwnerProfile(playerProfile);
        } else {
            // Legacy fallback for <1.20
            ReflectionUtils.setFieldValue(meta, "profile", this.delegate);
            meta.setOwningPlayer(meta.getOwningPlayer());
            ReflectionUtils.setFieldValue(meta, "profile", this.delegate);
        }
    }
}
