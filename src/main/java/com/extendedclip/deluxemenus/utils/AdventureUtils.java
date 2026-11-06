package com.extendedclip.deluxemenus.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class AdventureUtils {
    private final static GsonComponentSerializer gson = GsonComponentSerializer.gson();

    private AdventureUtils() {
        throw new AssertionError("Util classes should not be initialized");
    }

    public static void sendJson(@NotNull final CommandSender sender, @NotNull final String json) {
        send(sender, fromJson(json));
    }

    public static void send(CommandSender sender, Component component) {
        if (sender instanceof org.bukkit.entity.Player player) {
            player.spigot().sendMessage(net.md_5.bungee.chat.ComponentSerializer.parse(gson.serialize(component)));
        } else {
            sender.sendMessage(StringUtils.legacy(component));
        }
    }

    public static Component fromJson(String json) {
        return gson.deserialize(json);
    }

    /**
     * Sends a message to every online player. The console is deliberately not included.
     */
    public static void broadcast(@NotNull final Component message) {
        Bukkit.getOnlinePlayers().forEach(player -> send(player, message));
    }
}
