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
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    /**
     * Parse input hỗ trợ cả & codes lẫn MiniMessage tags.
     * Trả về String với § codes (dùng cho Bukkit API sendMessage(String)).
     */
    @NotNull
    public static String parse(@Nullable String input) {
        if (input == null || input.isEmpty()) return "";

        String normalized = input.replace('§', '&');

        if (looksLikeMiniMessage(normalized)) {
            // Convert & codes → MM tags, giữ nguyên MM tags có sẵn
            String mmInput = convertLegacyToMiniMessage(normalized);
            try {
                Component component = MINI_MESSAGE.deserialize(mmInput);
                return LEGACY_SECTION.serialize(component);
            } catch (Exception ignored) {
                // MiniMessage parse failed, fallback
            }
        }

        return ChatColor.translateAlternateColorCodes('&', normalized);
    }

    /**
     * Deserialize input thành Adventure Component.
     * Hỗ trợ cả & codes lẫn MiniMessage tags.
     */
    @NotNull
    public static Component deserialize(@Nullable String input) {
        if (input == null || input.isEmpty()) return Component.empty();

        String normalized = input.replace('§', '&');

        if (looksLikeMiniMessage(normalized)) {
            String mmInput = convertLegacyToMiniMessage(normalized);
            try {
                return MINI_MESSAGE.deserialize(mmInput);
            } catch (Exception ignored) {}
        }

        return LEGACY_AMPERSAND.deserialize(normalized);
    }

    /**
     * Chuyển đổi & codes trong input sang MiniMessage tags.
     * Ví dụ: "&aHello &lworld" → "<green>Hello <bold>world"
     * Giữ nguyên các MM tags có sẵn.
     */
    private static String convertLegacyToMiniMessage(String input) {
        // Dùng LegacyComponentSerializer để parse & codes → Component,
        // rồi serialize ngược lại thành MiniMessage format.
        // Cách này xử lý đúng cả hex color (&x&#rrggbb).
        Component legacy = LEGACY_AMPERSAND.deserialize(input);
        return MINI_MESSAGE.serialize(legacy);
    }

    private static boolean looksLikeMiniMessage(String input) {
        return HAS_TAGS.matcher(input).find();
    }
}
