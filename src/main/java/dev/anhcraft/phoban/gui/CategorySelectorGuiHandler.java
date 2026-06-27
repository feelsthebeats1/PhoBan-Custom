package dev.anhcraft.phoban.gui;

import dev.anhcraft.config.bukkit.utils.ItemBuilder;
import dev.anhcraft.palette.event.ClickEvent;
import dev.anhcraft.palette.ui.GuiHandler;
import dev.anhcraft.phoban.PhoBan;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategorySelectorGuiHandler extends GuiHandler {
    @Override
    public void onPreOpen(@NotNull Player player) {
        List<Integer> slots = new ArrayList<>(locateComponent("category"));
        Collections.sort(slots);
        List<String> categories = new ArrayList<>(PhoBan.instance.gameManager.getCategories());

        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);

            if (i >= categories.size()) {
                resetItem(slot);
                getSlot(slot).clearEvents();
                continue;
            }

            String category = categories.get(i);
            CategorySelectorGui.CategoryItem catItem = GuiRegistry.CATEGORY_SELECTOR.categories.get(category);
            replaceItem(slot, (index, itemBuilder) -> {
                if (catItem == null) {
                    itemBuilder.material(Material.CHEST);
                    itemBuilder.name("&a" + category);
                    return itemBuilder;
                }
                Material mat;
                try {
                    mat = Material.valueOf(catItem.material.toUpperCase());
                } catch (IllegalArgumentException e) {
                    mat = Material.BARRIER;
                }
                itemBuilder.material(mat);
                if (catItem.customModelData > 0) {
                    itemBuilder.customModelData(catItem.customModelData);
                }
                itemBuilder.name(catItem.name);
                if (catItem.lore != null) {
                    itemBuilder.lore().addAll(catItem.lore);
                }
                return itemBuilder;
            });

            getSlot(slot).setEvents((ClickEvent) (clickEvent, player1, slot1) -> {
                GuiRegistry.openRoomSelector(player, category);
            });
        }
    }
}