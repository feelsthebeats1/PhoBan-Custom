# Custom Model Data & Per-Category Room Selector

**Date:** 2026-06-27
**Author:** Phoban Dev
**Status:** Draft

## Overview

Two related improvements to the PhoBan plugin:
1. Support custom model data (1.20.4) via config field `custom-model-data` alongside material fields
2. Allow per-category room selector GUI configurations with base + override model

---

## Part 1: Custom Model Data

### Motivation

Bukkit Material enum alone cannot express custom model data. Servers using resource packs need items with custom model data (e.g. `bone` + `custom-model-data: 1234`) to show custom textures.

### Implementation

#### 1.1 Strategy — Separate Fields

Không dùng format `material:id`. Thay vào đó, **thêm field `customModelData` riêng** kế bên mỗi material field:

```yaml
# RoomConfig icon
icon: bone
custom-model-data: 1234
```

**Lý do:**
- Config library (anhcraft) tự động deserialize `custom-model-data` → `customModelData` (train-case naming)
- ItemBuilder trong palette **đã có sẵn** field `customModelData` + setter
- Không cần custom type adapter hay parser phức tạp
- Backward compatible: file cũ không có `custom-model-data` thì mặc định = 0 (bỏ qua)

#### 1.2 Config Changes

**`RoomConfig.java`:**
- Giữ nguyên field `icon: Material` (config library auto-parse từ YAML string)
- **Thêm** field `@Optional int customModelData` (default = 0)
- Getter mới hoặc method helper:

```java
// New field
@Optional
private int customModelData;

// New method
public void applyIconTo(ItemBuilder builder) {
    builder.material(icon);
    if (customModelData > 0) builder.customModelData(customModelData);
}
```

YAML:
```yaml
icon: bone
custom-model-data: 1234
```

**`CategorySelectorGui.CategoryItem.java`:**
- Giữ `material: String`
- **Thêm** field `int customModelData`
- Update getter/handler để dùng cả hai

**Palette GUI components (`room-selector.yml`, `difficulty-selector.yml`, `category-selector.yml`):**
- Component extends ItemBuilder → **đã có sẵn** `customModelData` field
- YAML hiện tại có thể thêm trực tiếp:
```yaml
"x":
    type: room
    material: bone
    custom-model-data: 1234
```
- Config library tự động map `custom-model-data` → `customModelData` trên Component
- **Không cần sửa code palette**

#### 1.3 MaterialData Helper

Vẫn giữ `MaterialData` nhưng đơn giản hơn — là helper để apply cả material + CMD:

```java
// dev.anhcraft.phoban.util.MaterialData
public record MaterialData(Material material, int customModelData) {
    public void applyTo(ItemBuilder builder) {
        builder.material(material);
        if (customModelData > 0) builder.customModelData(customModelData);
    }
}
```


#### 1.3 Handler Updates

**`RoomSelectorGuiHandler.java`:**
```java
// Line 175 (locked room):
roomConfig.getIcon().applyTo(itemBuilder);

// Line 187 (available room):
roomConfig.getIcon().applyTo(itemBuilder);
```

**`CategorySelectorGuiHandler.java`:**
```java
// Lines 40-52 — replace Material.valueOf block:
if (catItem == null) {
    itemBuilder.material(Material.CHEST);
    itemBuilder.name("&a" + category);
} else {
    catItem.material.applyTo(itemBuilder);
    itemBuilder.name(catItem.name);
    if (catItem.lore != null) itemBuilder.lore().addAll(catItem.lore);
}
```

#### 1.4 Palette GUI Components (No Code Change)

Palette's `Component` extends `ItemBuilder`, which already has:
```java
private int customModelData;
public void customModelData(int);
```

YAML files (`room-selector.yml`, `difficulty-selector.yml`, `category-selector.yml`) can already use:
```yaml
material: bone
custom-model-data: 1234
```

The config library deserializes `custom-model-data` → `customModelData` automatically due to `TRAIN_CASE` naming. No palette code changes needed.

#### 1.5 Config Library Adapter

If `BukkitConfigDeserializer` supports custom type adapters, register:

```java
// In ConfigHelper or PhoBan static init:
deserializer.registerAdapter(MaterialData.class, (node, schema) -> {
    String raw = node.asScalar(String.class);
    return MaterialData.parse(raw);
});
```

If not possible, use `@Exclude` raw String + `@PostHandler`:

```java
// RoomConfig.java
@Exclude private String iconRaw;

@PostHandler
private void postHandler() {
    // ... existing code ...
    if (iconRaw != null) icon = MaterialData.parse(iconRaw);
}
```

---

## Part 2: Per-Category Room Selector

### Motivation

Different categories (boss, dungeon, etc.) may want different GUI looks — different titles, layouts, button items.

### Implementation

#### 2.1 Loading Strategy — Base + Override

1. `room-selector.yml` = base/default config (unchanged)
2. Category-specific files: `gui/room-selector-<category>.yml`

**Loading in `PhoBan.reload()`:**

```java
// Load base
GuiRegistry.ROOM_SELECTOR = ConfigHelper.load(RoomSelectorGui.class, requestConfig("gui/room-selector.yml"));

// Load per-category overrides
GuiRegistry.CATEGORY_ROOM_SELECTORS.clear();
for (String category : gameManager.getCategories()) {
    String fileName = "gui/room-selector-" + category + ".yml";
    File f = new File(getDataFolder(), fileName);
    if (!f.exists()) continue;  // skip if no override file
    
    YamlConfiguration overrideSection = requestConfig(fileName);
    RoomSelectorGui merged = mergeRoomSelector(GuiRegistry.ROOM_SELECTOR, overrideSection);
    GuiRegistry.CATEGORY_ROOM_SELECTORS.put(category, merged);
}
```

#### 2.2 Merge Logic — `ConfigMerger`

New utility: `dev.anhcraft.phoban.util.ConfigMerger`

RoomSelectorGui extends Gui. Gui has fields: `title`, `layout`, `components`, `openSound`.
RoomSelectorGui adds: `roomLoreTrailer`, `roomLoreTrailerOverfull`, `roomLockedTrailer`.

**Merge approach:** Load override file as `YamlConfiguration`, apply to a **fresh copy** of base's field values. RoomSelectorGui không có deepClone method, nên merge bằng cách:

1. Load override config thành `YamlConfiguration override`
2. Tạo `RoomSelectorGui merged = new RoomSelectorGui()`
3. Copy từng field từ base:
   ```java
   merged.title = override.contains("title") ? override.getString("title") : base.title;
   merged.layout = override.contains("layout") ? override.getStringList("layout") : base.layout;
   merged.openSound = override.contains("open-sound") ? ... : base.openSound;
   // Components: merge
   merged.components = new HashMap<>(base.components);
   if (override.contains("components")) {
       ConfigurationSection compSec = override.getConfigurationSection("components");
       for (String key : compSec.getKeys(false)) {
           Component comp = ConfigHelper.load(Component.class, compSec.getConfigurationSection(key));
           merged.components.put(key.charAt(0), comp);
       }
   }
   // Trailers: kế thừa base, chỉ override nếu có
   merged.roomLoreTrailer = override.contains("room-lore-trailer") ? loadStageMap(override, "room-lore-trailer") : base.roomLoreTrailer;
   merged.roomLockedTrailer = override.contains("room-locked-trailer") ? loadStageMap(override, "room-locked-trailer") : base.roomLockedTrailer;
   merged.roomLoreTrailerOverfull = override.contains("room-lore-trailer-overfull") ? override.getStringList("room-lore-trailer-overfull") : base.roomLoreTrailerOverfull;
   ```
4. Helper `loadStageMap` parse YAML section thành `Map<Stage, List<String>>`

#### 2.3 GuiRegistry Updates

```java
public class GuiRegistry {
    public static RoomSelectorGui ROOM_SELECTOR;
    public static Map<String, RoomSelectorGui> CATEGORY_ROOM_SELECTORS = new HashMap<>();
    // ... existing fields ...

    public static void openRoomSelector(Player player, String category) {
        RoomSelectorGui gui = CATEGORY_ROOM_SELECTORS.getOrDefault(category, ROOM_SELECTOR);
        gui.open(player, new RoomSelectorGuiHandler(category));
    }
}
```

#### 2.4 Example Override File

`gui/room-selector-boss.yml`:
```yaml
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
# roomLoreTrailer, roomLockedTrailer kế thừa từ base
```

---

## Files Changed

### New Files
| File | Purpose |
|------|---------|
| `src/main/java/dev/anhcraft/phoban/util/MaterialData.java` | Material + custom model data container |
| `src/main/java/dev/anhcraft/phoban/util/ConfigMerger.java` | Per-category GUI merge logic |

### Modified Files
| File | Change |
|------|--------|
| `src/main/java/dev/anhcraft/phoban/config/RoomConfig.java` | `icon` field: `Material` → `MaterialData` + post-handler |
| `src/main/java/dev/anhcraft/phoban/gui/CategorySelectorGui.java` | `CategoryItem.material`: `String` → `MaterialData` |
| `src/main/java/dev/anhcraft/phoban/gui/RoomSelectorGuiHandler.java` | Use `materialData.applyTo()` instead of `itemBuilder.material(Material)` |
| `src/main/java/dev/anhcraft/phoban/gui/CategorySelectorGuiHandler.java` | Use `materialData.applyTo()` instead of `Material.valueOf()` |
| `src/main/java/dev/anhcraft/phoban/gui/GuiRegistry.java` | Add `CATEGORY_ROOM_SELECTORS` map, update `openRoomSelector` |
| `src/main/java/dev/anhcraft/phoban/PhoBan.java` | Load per-category room selectors in `reload()` |
| `src/main/resources/config/gui/room-selector.yml` | (optional) Example usage of `custom-model-data` |

### No Changes
- Palette library (Component extends ItemBuilder, already has `customModelData`)
- palette GUI YAML files (can already use `custom-model-data` field)
- DifficultySelectorGui, SoundExplorerGui
- Game logic (Room, GameManager, etc.)

---

## Migration Notes

1. **Backward compatibility**: `icon: bone` (không có custom model data) vẫn hoạt động bình thường
2. **Existing room YAML files**: Không cần sửa — `MaterialData.parse("bone")` trả về `customModelData=0`, `applyTo()` không set CMD
3. **Existing category-selector.yml**: `material: egg` vẫn hoạt động qua `MaterialData.parse`
4. **Fallback**: Nếu `MaterialData.parse` gặp material không hợp lệ, trả về `null` và handler fallback sang `Material.BARRIER` / `Material.CHEST`
