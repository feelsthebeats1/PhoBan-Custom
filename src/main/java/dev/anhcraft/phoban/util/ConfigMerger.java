package dev.anhcraft.phoban.util;

import dev.anhcraft.palette.ui.element.Component;
import dev.anhcraft.phoban.game.Stage;
import dev.anhcraft.phoban.gui.RoomSelectorGui;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

public class ConfigMerger {

    public static RoomSelectorGui mergeRoomSelector(RoomSelectorGui base, YamlConfiguration override) {
        RoomSelectorGui merged = new RoomSelectorGui();

        // title
        merged.title = override.contains("title")
                ? override.getString("title")
                : base.title;

        // layout
        merged.layout = override.contains("layout")
                ? override.getStringList("layout")
                : base.layout;

        // openSound
        if (override.contains("open-sound")) {
            try {
                merged.openSound = Sound.valueOf(
                        override.getString("open-sound").toUpperCase(Locale.ROOT)
                );
            } catch (IllegalArgumentException e) {
                merged.openSound = base.openSound;
            }
        } else {
            merged.openSound = base.openSound;
        }

        // components: start with base, then apply overrides
        merged.components = new HashMap<>(base.components);
        if (override.contains("components")) {
            ConfigurationSection compSec = override.getConfigurationSection("components");
            for (String key : compSec.getKeys(false)) {
                if (key.length() != 1) continue;
                Component comp = ConfigHelper.load(Component.class, compSec.getConfigurationSection(key));
                merged.components.put(key.charAt(0), comp);
            }
        }

        // roomLoreTrailer
        merged.roomLoreTrailer = override.contains("room-lore-trailer")
                ? loadStageListMap(override.getConfigurationSection("room-lore-trailer"))
                : base.roomLoreTrailer;

        // roomLockedTrailer
        merged.roomLockedTrailer = override.contains("room-locked-trailer")
                ? loadStageListMap(override.getConfigurationSection("room-locked-trailer"))
                : base.roomLockedTrailer;

        // roomLoreTrailerOverfull
        merged.roomLoreTrailerOverfull = override.contains("room-lore-trailer-overfull")
                ? override.getStringList("room-lore-trailer-overfull")
                : base.roomLoreTrailerOverfull;

        return merged;
    }

    private static Map<Stage, List<String>> loadStageListMap(ConfigurationSection section) {
        if (section == null) return Collections.emptyMap();
        Map<Stage, List<String>> result = new EnumMap<>(Stage.class);
        for (String key : section.getKeys(false)) {
            try {
                Stage stage = Stage.valueOf(key.toUpperCase(Locale.ROOT));
                result.put(stage, section.getStringList(key));
            } catch (IllegalArgumentException ignored) {
                // skip unknown stage keys
            }
        }
        return result;
    }
}
