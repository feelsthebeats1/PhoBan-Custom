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
import dev.anhcraft.phoban.util.MiniMessageUtil;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
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

    public void msg(CommandSender sender, String str) {
        if (str == null) {
            sender.sendMessage(MiniMessageUtil.parse(messageConfig.prefix + "&c<Empty message>"));
            return;
        }
        sender.sendMessage(MiniMessageUtil.parse(messageConfig.prefix + str));
    }

    public void rawMsg(CommandSender sender, String str) {
        if (str == null) {
            sender.sendMessage(MiniMessageUtil.parse("&c<Empty message>"));
            return;
        }
        sender.sendMessage(MiniMessageUtil.parse(str));
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

        new GuiRefreshTask().runTaskTimer(this, 0L, 20L);
        new GameTickingTask().runTaskTimerAsynchronously(this, 0L, 20L);
        (freeTicketTask = new FreeTicketTask(this)).runTaskTimerAsynchronously(this, 0L, mainConfig.freeTicketEvery*20L);
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
