package dev.anhcraft.phoban.gui;

import dev.anhcraft.config.annotations.Configurable;
import dev.anhcraft.palette.ui.Gui;

import java.util.Map;

@Configurable(keyNamingStyle = Configurable.NamingStyle.TRAIN_CASE)
public class CategorySelectorGui extends Gui {
    public Map<String, CategoryItem> categories;

    @Configurable(keyNamingStyle = Configurable.NamingStyle.TRAIN_CASE)
    public static class CategoryItem {
        public String material;
        public int customModelData;
        public String name;
        public java.util.List<String> lore;
    }
}