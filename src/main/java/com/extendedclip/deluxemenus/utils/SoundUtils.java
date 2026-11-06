package com.extendedclip.deluxemenus.utils;

import org.bukkit.Sound;

import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SoundUtils {

    /**
     * Resolves a sound from either a namespaced key ({@code entity.player.levelup},
     * {@code minecraft:entity.player.levelup}) or a legacy enum constant name
     * ({@code ENTITY_PLAYER_LEVELUP}).
     * <p>
     * Uses {@code Registry.SOUNDS} rather than {@code Registry.SOUND_EVENT}: the latter does
     * not exist on 1.20.6, the minimum supported version. On current versions they are the
     * same registry instance.
     *
     * @return the sound, or {@code null} if no sound matches
     */
    public static @Nullable Sound getSound(@NotNull final String name) {
        try {
            return Sound.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
