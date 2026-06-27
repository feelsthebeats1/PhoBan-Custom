# Custom Model Data & Per-Category Room Selector — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add custom model data support to all material configs and allow per-category room selector GUI using base + override YAML files.

**Architecture:** Add `customModelData` int field alongside existing Material fields (RoomConfig.icon, CategorySelector.CategoryItem.material, palette GUI components). Create `MaterialData` helper record for unified apply-to-ItemBuilder. Add `ConfigMerger` utility to merge per-category GUI overrides with base config. Store per-category RoomSelectorGui in GuiRegistry map.

**Tech Stack:** Paper 1.20.4, anhcraft config library, anhcraft palette library, Bukkit Material, ItemBuilder

---

## File Structure

### New Files
| File | Responsibility |
|------|---------------|
| `src/main/java/dev/anhcraft/phoban/util/MaterialData.java` | Helper record: Material + customModelData, applyTo(ItemBuilder) |
| `src/main/java/dev/anhcraft/phoban/util/ConfigMerger.java` | Merge per-category GUI YAML with base RoomSelectorGui |

### Modified Files
| File | Change |
|------|--------|
| `src/main/java/dev/anhcraft/phoban/config/RoomConfig.java` | Add `int customModelData` field + `applyIconTo(ItemBuilder)` method |
| `src/main/java/dev/anhcraft/phoban/gui/CategorySelectorGui.java` | Add `int customModelData` to `CategoryItem` |
| `src/main/java/dev/anhcraft/phoban/gui/RoomSelectorGuiHandler.java` | Use `roomConfig.applyIconTo(builder)` in both locked & available branches |
| `src/main/java/dev/anhcraft/phoban/gui/CategorySelectorGuiHandler.java` | Apply `catItem.customModelData` when building item |
| `src/main/java/dev/anhcraft/phoban/gui/GuiRegistry.java` | Add `CATEGORY_ROOM_SELECTORS` map, 2 new methods |
| `src/main/java/dev/anhcraft/phoban/PhoBan.java` | Load per-category room selectors in `reload()` |
| `src/main/java/dev/anhcraft/phoban/util/Placeholder.java` | (check if any placeholder-related) |

---

### Task 1: Create MaterialData helper

**Files:**
- Create: `src/main/java/dev/anhcraft/phoban/util/MaterialData.java`

- [ ] **Step 1: Write the helper record**

```java
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
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/dev/anhcraft/phoban/util/MaterialData.java
git commit -m "feat: add MaterialData helper record for material + custom model data"
```

---

### Task 2: Add customModelData to RoomConfig

**Files:**
- Modify: `src/main/java/dev/anhcraft/phoban/config/RoomConfig.java`

- [ ] **Step 1: Add field + helper method**

Add after `private Material icon;`:

```java
private int customModelData;
```

Add before existing getters:

```java
public void applyIconTo(ItemBuilder builder) {
    builder.material(icon);
    if (customModelData > 0) builder.customModelData(customModelData);
}
```

Add new getter:

```java
public int getCustomModelData() {
    return customModelData;
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/dev/anhcraft/phoban/config/RoomConfig.java
git commit -m "feat: add customModelData field to RoomConfig"
```

---

### Task 3: Add customModelData to CategorySelectorGui.CategoryItem

**Files:**
- Modify: `src/main/java/dev/anhcraft/phoban/gui/CategorySelectorGui.java`

- [ ] **Step 1: Add field**

In `CategoryItem` class, add after `public String material;`:

```java
public int customModelData;
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/dev/anhcraft/phoban/gui/CategorySelectorGui.java
git commit -m "feat: add customModelData field to CategoryItem"
```

---

### Task 4: Update RoomSelectorGuiHandler to use applyIconTo

**Files:**
- Modify: `src/main/java/dev/anhcraft/phoban/gui/RoomSelectorGuiHandler.java`

- [ ] **Step 1: Replace `itemBuilder.material(roomConfig.getIcon())` calls**

Line 175 (locked room):
```java
// Replace:
itemBuilder.material(roomConfig.getIcon());
// With:
roomConfig.applyIconTo(itemBuilder);
```

Line 187 (available room):
```java
// Replace:
itemBuilder.material(roomConfig.getIcon());
// With:
roomConfig.applyIconTo(itemBuilder);
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/dev/anhcraft/phoban/gui/RoomSelectorGuiHandler.java
git commit -m "feat: use RoomConfig.applyIconTo in room selector GUI handler"
```

---

### Task 5: Update CategorySelectorGuiHandler

**Files:**
- Modify: `src/main/java/dev/anhcraft/phoban/gui/CategorySelectorGuiHandler.java`

- [ ] **Step 1: Update item building to apply custom model data**

Replace the material-setting block (lines 40-52) with:

```java
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
```

> Only change is the `if (catItem.customModelData > 0)` block added after setting material.

- [ ] **Step 2: Commit**

```bash
git add src/main/java/dev/anhcraft/phoban/gui/CategorySelectorGuiHandler.java
git commit -m "feat: apply customModelData in category selector GUI"
```

---

### Task 6: Implement ConfigMerger for per-category GUI

**Files:**
- Create: `src/main/java/dev/anhcraft/phoban/util/ConfigMerger.java`

- [ ] **Step 1: Write the merge utility**

```java
package dev.anhcraft.phoban.util;

import dev.anhcraft.config.bukkit.utils.Component;
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
        merged.title = override.contains("title") ? override.getString("title") : base.title;

        // layout
        merged.layout = override.contains("layout")
                ? override.getStringList("layout")
                : base.layout;

        // openSound
        if (override.contains("open-sound")) {
            try {
                merged.openSound = Sound.valueOf(override.getString("open-sound").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                merged.openSound = base.openSound;
            }
        } else {
            merged.openSound = base.openSound;
        }

        // components: base copy + override
        merged.components = new HashMap<>(base.components);
        if (override.contains("components")) {
            ConfigurationSection compSec = override.getConfigurationSection("components");
            for (String key : compSec.getKeys(false)) {
                Component comp = ConfigHelper.load(Component.class, compSec.getConfigurationSection(key));
                merged.components.put(key.charAt(0), comp);
            }
        }

        // roomLoreTrailer
        merged.roomLoreTrailer = override.contains("room-lore-trailer")
                ? loadStageListMap(override, "room-lore-trailer")
                : base.roomLoreTrailer;

        // roomLockedTrailer
        merged.roomLockedTrailer = override.contains("room-locked-trailer")
                ? loadStageListMap(override, "room-locked-trailer")
                : base.roomLockedTrailer;

        // roomLoreTrailerOverfull
        merged.roomLoreTrailerOverfull = override.contains("room-lore-trailer-overfull")
                ? override.getStringList("room-lore-trailer-overfull")
                : base.roomLoreTrailerOverfull;

        return merged;
    }

    private static Map<Stage, List<String>> loadStageListMap(YamlConfiguration config, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) return Collections.emptyMap();
        Map<Stage, List<String>> result = new EnumMap<>(Stage.class);
        for (String key : section.getKeys(false)) {
            try {
                Stage stage = Stage.valueOf(key.toUpperCase(Locale.ROOT));
                result.put(stage, section.getStringList(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }
}
```

Requires import: `import dev.anhcraft.palette.ui.element.Component;` — confirm exact palette Component path. If palette Component class is not public or doesn't have a public constructor, use a different approach:

**Alternative (if Component can't be loaded via ConfigHelper):** Load components manually from override section:

```java
// Instead of ConfigHelper.load(Component.class, ...), manually create Component:
Component comp = new Component();
comp.material(...); // If material field is accessible
```

We'll verify the exact palette Component API during implementation.

- [ ] **Step 2: Commit**

```bash
git add src/main/java/dev/anhcraft/phoban/util/ConfigMerger.java
git commit -m "feat: add ConfigMerger for per-category GUI override merging"
```

---

### Task 7: Update GuiRegistry

**Files:**
- Modify: `src/main/java/dev/anhcraft/phoban/gui/GuiRegistry.java`

- [ ] **Step 1: Add per-category map and update methods**

```java
package dev.anhcraft.phoban.gui;

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
        DIFFICULTY_SELECTOR.open(player, new DifficultySelectorGuiHandler(roomId, categoryFilter));
    }

    public static void openSoundExplorer(Player player) {
        SOUND_EXPLORER.open(player, new SoundExplorerGuiHandler());
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/dev/anhcraft/phoban/gui/GuiRegistry.java
git commit -m "feat: add per-category RoomSelectorGui map to GuiRegistry"
```

---

### Task 8: Update PhoBan.reload() to load per-category GUI overrides

**Files:**
- Modify: `src/main/java/dev/anhcraft/phoban/PhoBan.java`

- [ ] **Step 1: Add import and loading logic**

Add import:
```java
import dev.anhcraft.phoban.util.ConfigMerger;
```

In `reload()`, after loading `GuiRegistry.CATEGORY_SELECTOR` and before `GuiRegistry.SOUND_EXPLORER`, add:

```java
// Load per-category room selector overrides
GuiRegistry.CATEGORY_ROOM_SELECTORS.clear();
for (String category : gameManager.getCategories()) {
    String fileName = "gui/room-selector-" + category + ".yml";
    File catFile = new File(getDataFolder(), fileName);
    if (!catFile.exists()) continue;
    YamlConfiguration catConfig = YamlConfiguration.loadConfiguration(catFile);
    RoomSelectorGui merged = ConfigMerger.mergeRoomSelector(GuiRegistry.ROOM_SELECTOR, catConfig);
    GuiRegistry.CATEGORY_ROOM_SELECTORS.put(category, merged);
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/dev/anhcraft/phoban/PhoBan.java
git commit -m "feat: load per-category room selector overrides on reload"
```

---

### Task 9: Create example per-category override YAML

**Files:**
- Create: `src/main/resources/config/gui/room-selector-boss.yml` (bundled example)

- [ ] **Step 1: Write example override file**

```yaml
# Per-category override for "boss" category rooms
# Chỉ ghi các section muốn thay đổi so với room-selector.yml
title: "&0&lBoss Raid"
layout:
  - "---------"
  - "--xxxxx--"
  - "--xxxxx--"
  - "b-------I"
components:
  "b":
    type: back
    material: nether_star
    name: "&eQuay về danh mục"
    lore:
      - ""
      - "&fQuay về danh mục phó bản"
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/config/gui/room-selector-boss.yml
git commit -m "feat: add example per-category room selector override for boss category"
```

---

### Task 10: Build and verify

**Files:** (no new file changes)

- [ ] **Step 1: Run Maven package**

```bash
mvn package
```

Expected: BUILD SUCCESS, no compilation errors, no Aikar references.

- [ ] **Step 2: Quick check for import issues**

Verify palette Component class path is accessible. If `dev.anhcraft.palette.ui.element.Component` is not public or has no accessible constructor, update `ConfigMerger` accordingly:

```bash
# Check Component class accessibility
cd /tmp && jar xf /c/Users/DFK/.m2/repository/com/github/anhcraft/palette/v1.0.9/palette-v1.0.9.jar
javap -p dev/anhcraft/palette/ui/element/Component.class
```

If Component is not publicly constructable, replace the `ConfigHelper.load(Component.class, ...)` line with:
```java
// Manual Component creation
dev.anhcraft.config.bukkit.utils.ItemBuilder fallback = new dev.anhcraft.config.bukkit.utils.ItemBuilder();
// Since Component extends ItemBuilder, and we just need to set material/name/lore,
// we can use ItemBuilder directly and the palette library will handle it via the base class
```

- [ ] **Step 3: Commit final build fix if needed**

```bash
git add -A
git commit -m "fix: adjust ConfigMerger Component loading for palette library API"
```
