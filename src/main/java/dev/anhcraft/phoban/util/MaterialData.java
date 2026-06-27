package dev.anhcraft.phoban.util;

import dev.anhcraft.config.bukkit.utils.ItemBuilder;
import org.bukkit.Material;

public record MaterialData(Material material, int customModelData) {

    public void applyTo(ItemBuilder builder) {
        builder.material(material);
        if (customModelData > 0) {
            builder.customModelData(customModelData);
        }
    }
}
