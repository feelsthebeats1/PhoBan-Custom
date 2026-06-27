package dev.anhcraft.phoban.config;

import dev.anhcraft.config.annotations.Configurable;
import dev.anhcraft.config.annotations.Exclude;
import dev.anhcraft.config.annotations.PostHandler;
import dev.anhcraft.config.annotations.Validation;
import dev.anhcraft.phoban.game.Difficulty;
import dev.anhcraft.phoban.game.RoomRequirement;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Configurable(keyNamingStyle = Configurable.NamingStyle.TRAIN_CASE)
public class RoomConfig {
    private boolean enabled;

    @Validation(notNull = true)
    private String name;

    private String category;

    @Validation(notNull = true)
    private Material icon;

    private int customModelData;

    private String permission;

    private String requirement;

    private WeatherType weatherLock;

    private int timeLock = -1;

    private int displayOrder;

    @Validation(notNull = true)
    private List<String> description;

    @Validation(notNull = true)
    private Location spawnLocation;

    private Location endTeleportLocation;

    @Validation(notNull = true)
    private Location queueLocation;

    @Validation(notNull = true)
    private String region;

    @Validation(notNull = true)
    private Map<Difficulty, LevelConfig> levels;

    private Integer waitingTime;

    private Integer intermissionTime;

    @Exclude
    private RoomRequirement roomRequirement;

    @PostHandler
    private void postHandler() {
        roomRequirement = requirement == null ? null : RoomRequirement.parse(requirement);
    }

    public void applyIconTo(dev.anhcraft.config.bukkit.utils.ItemBuilder builder) {
        builder.material(icon);
        if (customModelData > 0) builder.customModelData(customModelData);
    }

    public boolean isEnabled() {
        return enabled;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public Material getIcon() {
        return icon;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    @NotNull
    public List<String> getDescription() {
        return description;
    }

    @Nullable
    public String getPermission() {
        return permission;
    }

    @Nullable
    public String getCategory() {
        return category;
    }

    @Nullable
    public Integer getWaitingTime() {
        return waitingTime;
    }

    @Nullable
    public Integer getIntermissionTime() {
        return intermissionTime;
    }

    @Nullable
    public RoomRequirement getRoomRequirement() {
        return roomRequirement;
    }

    @Nullable
    public WeatherType getWeatherLock() {
        return weatherLock;
    }

    public int getTimeLock() {
        return timeLock;
    }

    @NotNull
    public Location getSpawnLocation() {
        return spawnLocation;
    }

    @Nullable
    public Location getEndTeleportLocation() {
        return endTeleportLocation;
    }

    @NotNull
    public Location getQueueLocation() {
        return queueLocation;
    }

    @NotNull
    public World getWorld() {
        return spawnLocation.getWorld();
    }

    @NotNull
    public String getRegion() {
        return region;
    }

    @NotNull
    public Map<Difficulty, LevelConfig> getLevels() {
        return levels;
    }

    @Nullable
    public LevelConfig getLevel(Difficulty difficulty) {
        return levels.get(difficulty);
    }

    public void setEnabled(boolean b) {
        enabled = b;
    }
}
