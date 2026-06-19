package dev.anhcraft.phoban.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class MiniMessageUtil {
    private static final Pattern HAS_TAGS = Pattern.compile("<[a-zA-Z#][^>]*>");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    @NotNull
    public static String parse(@Nullable String input) {
        if (input == null || input.isEmpty()) return "";

        String normalized = input.replace('§', '&');

        if (looksLikeMiniMessage(normalized)) {
            String legacyConverted = ChatColor.translateAlternateColorCodes('&', normalized);

            try {
                Component component = MINI_MESSAGE.deserialize(legacyConverted);
                return LegacyComponentSerializer.legacySection().serialize(component);
            } catch (Exception e) {
                return legacyConverted;
            }
        }

        return ChatColor.translateAlternateColorCodes('&', normalized);
    }

    @NotNull
    public static Component deserialize(@Nullable String input) {
        if (input == null || input.isEmpty()) return Component.empty();

        String normalized = input.replace('§', '&');

        if (looksLikeMiniMessage(normalized)) {
            try {
                return MINI_MESSAGE.deserialize(normalized);
            } catch (Exception ignored) {}
        }

        return LEGACY_AMPERSAND.deserialize(normalized);
    }

    private static boolean looksLikeMiniMessage(String input) {
        return HAS_TAGS.matcher(input).find();
    }
}