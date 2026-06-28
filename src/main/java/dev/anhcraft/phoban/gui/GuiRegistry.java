package dev.anhcraft.phoban.gui;

import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.config.RoomConfig;
import dev.anhcraft.palette.ui.Gui;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class GuiRegistry {
    public static RoomSelectorGui ROOM_SELECTOR;
    public static DifficultySelectorGui DIFFICULTY_SELECTOR;
    public static CategorySelectorGui CATEGORY_SELECTOR;
    public static Gui SOUND_EXPLORER;

    public static final Map<String, RoomSelectorGui> CATEGORY_ROOM_SELECTORS = new HashMap<>();
    public static final Map<String, DifficultySelectorGui> ROOM_DIFFICULTY_SELECTORS = new HashMap<>();
    public static final Map<String, DifficultySelectorGui> CATEGORY_DIFFICULTY_SELECTORS = new HashMap<>();

    public static void openRoomSelector(Player player) {
        ROOM_SELECTOR.open(player, new RoomSelectorGuiHandler());
    }

    public static void openRoomSelector(Player player, String category) {
        RoomSelectorGui gui = CATEGORY_ROOM_SELECTORS.getOrDefault(category, ROOM_SELECTOR);
        gui.open(player, new RoomSelectorGuiHandler(category));
    }

    public static void openCategorySelector(Player player) {
        CATEGORY_SELECTOR.open(player, new CategorySelectorGuiHandler());
    }

    public static void openDifficultySelector(Player player, String roomId) {
        openDifficultySelector(player, roomId, null);
    }

    public static void openDifficultySelector(Player player, String roomId, String categoryFilter) {
        DifficultySelectorGui gui = null;

        // 1. Category override (ưu tiên cao nhất)
        RoomConfig roomConfig = PhoBan.instance.gameManager.getRoomConfig(roomId);
        if (roomConfig != null && roomConfig.getCategory() != null) {
            gui = CATEGORY_DIFFICULTY_SELECTORS.get(roomConfig.getCategory());
        }

        // 2. Room override
        if (gui == null) {
            gui = ROOM_DIFFICULTY_SELECTORS.get(roomId);
        }

        // 3. Base fallback
        if (gui == null) {
            gui = DIFFICULTY_SELECTOR;
        }

        gui.open(player, new DifficultySelectorGuiHandler(roomId, categoryFilter, gui));
    }

    public static void openSoundExplorer(Player player) {
        SOUND_EXPLORER.open(player, new SoundExplorerGuiHandler());
    }
}
