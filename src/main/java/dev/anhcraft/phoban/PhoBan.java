package dev.anhcraft.phoban;

import com.google.common.base.Preconditions;
import dev.anhcraft.jvmkit.utils.FileUtil;
import dev.anhcraft.jvmkit.utils.IOUtil;
import dev.anhcraft.palette.listener.GuiEventListener;
import dev.anhcraft.phoban.cmd.Command;
import dev.anhcraft.phoban.config.MainConfig;
import dev.anhcraft.phoban.config.MessageConfig;
import dev.anhcraft.phoban.game.Room;
import dev.anhcraft.phoban.integration.PlaceholderBridge;
import dev.anhcraft.phoban.listener.GameListener;
import dev.anhcraft.phoban.game.GameManager;
import dev.anhcraft.phoban.gui.CategorySelectorGui;
import dev.anhcraft.phoban.gui.DifficultySelectorGui;
import dev.anhcraft.phoban.gui.GuiRefreshTask;
import dev.anhcraft.phoban.gui.GuiRegistry;
import dev.anhcraft.phoban.gui.RoomSelectorGui;
import dev.anhcraft.phoban.storage.PlayerDataManager;
import dev.anhcraft.phoban.tasks.FreeTicketTask;
import dev.anhcraft.phoban.tasks.GameTickingTask;
import dev.anhcraft.phoban.util.ConfigHelper;
import dev.anhcraft.phoban.util.ConfigMerger;
import dev.anhcraft.phoban.util.MiniMessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public final class PhoBan extends JavaPlugin {
    public static final GameMode SPECTATOR_GAMEMODE = GameMode.SPECTATOR;
    public static PhoBan instance;
    public PlayerDataManager playerDataManager;
    public GameManager gameManager;
    public MainConfig mainConfig;
    public MessageConfig messageConfig;
    public FreeTicketTask freeTicketTask;

    @Override
    public void onEnable() {
        instance = this;
        playerDataManager = new PlayerDataManager(this);
        gameManager = new GameManager(this);
        new PlaceholderBridge(this);

        reload();

        getServer().getPluginManager().registerEvents(new GuiEventListener(), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        Command command = new Command(this);
        org.bukkit.command.PluginCommand pluginCommand = getCommand("phoban");
        if (pluginCommand == null) {
            getLogger().severe("Command 'phoban' is not defined in plugin.yml");
            return;
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }

    public void debug(@NotNull String format, @NotNull Object... args) {
        debug(1, format, args);
    }

    public void debug(int level, @NotNull String format, @NotNull Object... args) {
        if (mainConfig != null && mainConfig.debugLevel >= level) {
            getServer().getConsoleSender().sendMessage(org.bukkit.ChatColor.GOLD + "[PhoBan#DEBUG] " + String.format(format, args));
        }
    }

    private void sendMessage(CommandSender sender, String str, boolean usePrefix) {
        if (str == null) {
            String errorMsg = usePrefix ? 
                messageConfig.prefix + "&c<Empty message>" : 
                "&c<Empty message>";
            sender.sendMessage(MiniMessageUtil.parse(errorMsg));
            return;
        }
        
        Component prefixComponent = usePrefix ? 
            LegacyComponentSerializer.legacyAmpersand().deserialize(messageConfig.prefix) : 
            Component.empty();
        
        Component messageComponent = MiniMessageUtil.deserialize(str);
        
        Component combined = prefixComponent.append(messageComponent);
        sender.sendMessage(combined);
    }

    public void msg(CommandSender sender, String str) {
        sendMessage(sender, str, true);
    }

    public void rawMsg(CommandSender sender, String str) {
        sendMessage(sender, str, false);
    }

    public void sync(Runnable runnable) {
        getServer().getScheduler().runTask(this, runnable);
    }

    public void sync(Runnable runnable, int delay) {
        getServer().getScheduler().runTaskLater(this, runnable, delay);
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        for (Room room : gameManager.getActiveRooms()) {
            room.syncTerminate();
        }
        playerDataManager.terminate();
    }

    public void reload() {
        getServer().getScheduler().cancelTasks(this);

        getDataFolder().mkdir();
        mainConfig = ConfigHelper.load(MainConfig.class, requestConfig("config.yml"));
        messageConfig = ConfigHelper.load(MessageConfig.class, requestConfig("messages.yml"));

        new File(getDataFolder(), "gui").mkdir();
        GuiRegistry.ROOM_SELECTOR = ConfigHelper.load(RoomSelectorGui.class, requestConfig("gui/room-selector.yml"));
        GuiRegistry.DIFFICULTY_SELECTOR = ConfigHelper.load(DifficultySelectorGui.class, requestConfig("gui/difficulty-selector.yml"));
        GuiRegistry.CATEGORY_SELECTOR = ConfigHelper.load(CategorySelectorGui.class, requestConfig("gui/category-selector.yml"));
        GuiRegistry.SOUND_EXPLORER = ConfigHelper.load(DifficultySelectorGui.class, requestConfig("gui/sound-explorer.yml"));

        playerDataManager.reload();
        gameManager.reload();

        // Load per-category room selector overrides (after gameManager.reload so categories are ready)
        GuiRegistry.CATEGORY_ROOM_SELECTORS.clear();
        for (String category : gameManager.getCategories()) {
            String fileName = "gui/room-selector-" + category + ".yml";
            File catFile = new File(getDataFolder(), fileName);
            if (!catFile.exists()) continue;
            YamlConfiguration catConfig = YamlConfiguration.loadConfiguration(catFile);
            RoomSelectorGui merged = ConfigMerger.mergeRoomSelector(GuiRegistry.ROOM_SELECTOR, catConfig);
            GuiRegistry.CATEGORY_ROOM_SELECTORS.put(category, merged);
        }

        // Load per-room difficulty selector overrides
        GuiRegistry.ROOM_DIFFICULTY_SELECTORS.clear();
        getLogger().info("Loading per-room difficulty selectors. Rooms: " + gameManager.getRoomIds());
        for (String roomId : gameManager.getRoomIds()) {
            String fileName = "gui/difficulty-selector-" + roomId + ".yml";
            File diffFile = new File(getDataFolder(), fileName);
            getLogger().info("  Checking " + fileName + " exists=" + diffFile.exists());
            if (!diffFile.exists()) continue;

            // Load override YAML
            YamlConfiguration override = YamlConfiguration.loadConfiguration(diffFile);

            // Load base YAML, deep-merge bằng ConfigurationSection (tránh dot notation với key đặc biệt như "-")
            YamlConfiguration baseConfig = requestConfig("gui/difficulty-selector.yml");
            deepMerge(baseConfig, override);

            DifficultySelectorGui gui = ConfigHelper.load(DifficultySelectorGui.class, baseConfig);
            GuiRegistry.ROOM_DIFFICULTY_SELECTORS.put(roomId, gui);
            getLogger().info("  -> Loaded override for room: " + roomId);
        }
        getLogger().info("ROOM_DIFFICULTY_SELECTORS keys: " + GuiRegistry.ROOM_DIFFICULTY_SELECTORS.keySet());

        new GuiRefreshTask().runTaskTimer(this, 0L, 20L);
        new GameTickingTask().runTaskTimerAsynchronously(this, 0L, 20L);
        (freeTicketTask = new FreeTicketTask(this)).runTaskTimerAsynchronously(this, 0L, mainConfig.freeTicketEvery*20L);
    }

    private void deepMerge(ConfigurationSection base, ConfigurationSection override) {
        for (String key : override.getKeys(false)) {
            if (override.isConfigurationSection(key)) {
                ConfigurationSection baseSub = base.contains(key)
                        ? base.getConfigurationSection(key)
                        : base.createSection(key);
                deepMerge(baseSub, override.getConfigurationSection(key));
            } else {
                base.set(key, override.get(key));
            }
        }
    }

    public YamlConfiguration requestConfig(String path) {
        File f = new File(getDataFolder(), path);
        Preconditions.checkArgument(f.getParentFile().exists());

        if (!f.exists()) {
            try {
                FileUtil.write(f, IOUtil.readResource(PhoBan.class, "/config/" + path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return YamlConfiguration.loadConfiguration(f);
    }
}
