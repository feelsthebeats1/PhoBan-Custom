package dev.anhcraft.phoban.cmd;

import dev.anhcraft.phoban.PhoBan;
import dev.anhcraft.phoban.config.RoomConfig;
import dev.anhcraft.phoban.game.Difficulty;
import dev.anhcraft.phoban.game.Room;
import dev.anhcraft.phoban.gui.GuiRegistry;
import dev.anhcraft.phoban.storage.GameHistory;
import dev.anhcraft.phoban.util.TimeUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.bukkit.ChatColor.*;

public class Command implements CommandExecutor, TabCompleter {
    private static final String USE_PERMISSION = "phoban.use";

    private final PhoBan plugin;

    public Command(PhoBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        if (!perm(sender, USE_PERMISSION)) return true;
        if (args.length == 0) {
            Player player = requirePlayer(sender);
            if (player != null) GuiRegistry.openRoomSelector(player);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> help(sender, label);
            case "profile" -> profile(sender, args);
            case "quit" -> quit(sender);
            case "reload" -> reload(sender);
            case "enable" -> setEnabled(sender, args, true);
            case "disable" -> setEnabled(sender, args, false);
            case "list" -> list(sender);
            case "join" -> join(sender, args);
            case "start", "end", "terminate" -> roomAction(sender, args, args[0].toLowerCase(Locale.ROOT));
            case "reset" -> reset(sender, args);
            case "tp" -> tp(sender, args);
            case "categories" -> categories(sender, args);
            case "sound" -> sound(sender);
            case "ticket" -> ticket(sender, args);
            case "admin" -> admin(sender, args);
            case "getpos" -> getpos(sender);
            default -> help(sender, label);
        }
        return true;
    }

    private void help(CommandSender sender, String label) {
        sender.sendMessage(GOLD + "PhoBan commands:");
        sender.sendMessage(YELLOW + "/" + label + GREEN + " - Open room selector");
        sender.sendMessage(YELLOW + "/" + label + " quit" + GREEN + " - Leave your current room");
        if (sender.hasPermission("phoban.profile")) sender.sendMessage(YELLOW + "/" + label + " profile <player>");
        if (sender.hasPermission("phoban.reload")) sender.sendMessage(YELLOW + "/" + label + " reload");
        if (sender.hasPermission("phoban.join")) sender.sendMessage(YELLOW + "/" + label + " join <room> [player]");
        if (sender.hasPermission("phoban.ticket.add") || sender.hasPermission("phoban.ticket.set")) sender.sendMessage(YELLOW + "/" + label + " ticket <add|set> <player> <amount>");
    }

    private boolean perm(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        sender.sendMessage(RED + "You don't have permission to use this command!");
        return false;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;
        sender.sendMessage(RED + "This command can only be used by players!");
        return null;
    }

    private Player target(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            Player player = Bukkit.getPlayerExact(args[index]);
            if (player == null) sender.sendMessage(RED + "Player not found: " + args[index]);
            return player;
        }
        if (sender instanceof Player player) return player;
        sender.sendMessage(RED + "You must specify a player!");
        return null;
    }

    private OfflinePlayer offline(CommandSender sender, String name) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(name);
        if (!player.isOnline() && !player.hasPlayedBefore()) {
            sender.sendMessage(RED + "This player has not played before!");
            return null;
        }
        if (!player.isOnline()) sender.sendMessage(YELLOW + "Fetching player data as he is currently offline...");
        return player;
    }

    private Integer integer(CommandSender sender, String value, String name) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            sender.sendMessage(RED + "Invalid " + name + ": " + value);
            return null;
        }
    }

    private void profile(CommandSender sender, String[] args) {
        if (!perm(sender, "phoban.profile")) return;
        if (args.length < 2) {
            sender.sendMessage(RED + "Usage: /pb profile <player>");
            return;
        }
        OfflinePlayer player = offline(sender, args[1]);
        if (player == null) return;
        plugin.playerDataManager.requireData(player.getUniqueId()).whenComplete((playerData, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(RED + throwable.getMessage());
                return;
            }
            sender.sendMessage(YELLOW + "This is the profile of " + WHITE + player.getName());
            sender.sendMessage(GREEN + "- Available ticket: " + WHITE + playerData.getTicket());
            playerData.streamPlayedRooms().forEach(room -> {
                GameHistory history = playerData.requireRoomHistory(room);
                sender.sendMessage(AQUA + "* Played room " + room);
                for (Difficulty value : Difficulty.values()) {
                    if (history.getPlayTimes(value) == 0) continue;
                    String msg = value == Difficulty.CHALLENGE
                            ? String.format("&7- %s %d:&7 &fWon %d, Lost %d, Total %d (Best: %s) (Win Ratio: %.2f%%)", plugin.messageConfig.difficulty.get(value), playerData.getChallengeLevel(room), history.getWinTimes(value), history.getLossTimes(value), history.getPlayTimes(value), TimeUtils.format(history.getBestCompleteTime(value)), ((double) history.getWinTimes(value)) / history.getPlayTimes(value) * 100)
                            : String.format("&7- %s:&7 &fWon %d, Lost %d, Total %d (Best: %s) (Win Ratio: %.2f%%)", plugin.messageConfig.difficulty.get(value), history.getWinTimes(value), history.getLossTimes(value), history.getPlayTimes(value), TimeUtils.format(history.getBestCompleteTime(value)), ((double) history.getWinTimes(value)) / history.getPlayTimes(value) * 100);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                }
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&7> Total:&7 &fWon %d, Lost %d, Total %d (Best: %s) (Win Ratio: %.2f%%)", history.getTotalWinTimes(), history.getTotalLossTimes(), history.getTotalPlayTimes(), TimeUtils.format(history.getBestCompleteOfAllTime()), ((double) history.getTotalWinTimes()) / history.getTotalPlayTimes() * 100)));
            });
        });
    }

    private void quit(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player != null) plugin.gameManager.attemptLeaveRoom(player);
    }

    private void reload(CommandSender sender) {
        if (!perm(sender, "phoban.reload")) return;
        if (!plugin.gameManager.getActiveRoomIds().isEmpty()) {
            sender.sendMessage(RED + "There are active rooms in playing!");
            return;
        }
        plugin.reload();
        sender.sendMessage(GREEN + "Reloaded the plugin!");
    }

    private void setEnabled(CommandSender sender, String[] args, boolean enabled) {
        if (!perm(sender, enabled ? "phoban.enable" : "phoban.disable")) return;
        if (args.length < 2) {
            sender.sendMessage(RED + "Usage: /pb " + args[0] + " <room>");
            return;
        }
        RoomConfig roomConfig = plugin.gameManager.getRoomConfig(args[1]);
        if (roomConfig == null) {
            sender.sendMessage(RED + "Room not found: " + args[1]);
            return;
        }
        roomConfig.setEnabled(enabled);
        sender.sendMessage((enabled ? GREEN + "Enabled " : YELLOW + "Disabled ") + args[1]);
    }

    private void list(CommandSender sender) {
        if (!perm(sender, "phoban.list")) return;
        sender.sendMessage(GOLD + "All: " + String.join(",", plugin.gameManager.getRoomIds()));
        sender.sendMessage(GREEN + "Active: " + String.join(",", plugin.gameManager.getActiveRoomIds()));
    }

    private void join(CommandSender sender, String[] args) {
        if (!perm(sender, "phoban.join")) return;
        if (args.length < 2) {
            sender.sendMessage(RED + "Usage: /pb join <room> [player]");
            return;
        }
        Player target = target(sender, args, 2);
        if (target == null) return;
        if (plugin.gameManager.getRoom(args[1]) == null) {
            sender.sendMessage(RED + "Room not created: " + args[1]);
            return;
        }
        plugin.gameManager.attemptJoinRoom(target, args[1], true);
    }

    private void roomAction(CommandSender sender, String[] args, String action) {
        if (!perm(sender, "phoban." + action)) return;
        if (args.length < 2) {
            sender.sendMessage(RED + "Usage: /pb " + action + " <room>");
            return;
        }
        if (action.equals("start")) plugin.gameManager.tryStart(args[1]);
        else if (action.equals("end")) plugin.gameManager.tryEnd(args[1]);
        else plugin.gameManager.tryTerminate(args[1]);
        sender.sendMessage(GREEN + action.substring(0, 1).toUpperCase(Locale.ROOT) + action.substring(1) + "d " + args[1]);
    }

    private void reset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(RED + "Usage: /pb reset <respawn|data> ...");
            return;
        }
        if (args[1].equalsIgnoreCase("respawn")) resetRespawn(sender, args);
        else if (args[1].equalsIgnoreCase("data")) resetData(sender, args);
        else sender.sendMessage(RED + "Usage: /pb reset <respawn|data> ...");
    }

    private void resetRespawn(CommandSender sender, String[] args) {
        if (!perm(sender, "phoban.reset.respawn")) return;
        if (args.length < 3) {
            sender.sendMessage(RED + "Usage: /pb reset respawn <room> [player]");
            return;
        }
        Player target = target(sender, args, 3);
        if (target == null) return;
        Room room = plugin.gameManager.getRoom(args[2]);
        if (room == null) {
            sender.sendMessage(RED + "Room not created: " + args[2]);
            return;
        }
        room.getRespawnChances().remove(target.getUniqueId());
        sender.sendMessage(GREEN + "Reset respawn chances for " + target.getName());
    }

    private void resetData(CommandSender sender, String[] args) {
        if (!perm(sender, "phoban.reset.data")) return;
        if (args.length < 3) {
            sender.sendMessage(RED + "Usage: /pb reset data <player>");
            return;
        }
        OfflinePlayer player = offline(sender, args[2]);
        if (player == null) return;
        plugin.playerDataManager.requireData(player.getUniqueId()).whenComplete((playerData, throwable) -> {
            if (throwable != null) sender.sendMessage(RED + throwable.getMessage());
            else {
                playerData.reset();
                sender.sendMessage(GREEN + "Reset player data: " + player.getName());
            }
        });
    }

    private void tp(CommandSender sender, String[] args) {
        if (!perm(sender, "phoban.tp")) return;
        if (args.length < 2) {
            sender.sendMessage(RED + "Usage: /pb tp <room> [player]");
            return;
        }
        Player target = target(sender, args, 2);
        if (target == null) return;
        RoomConfig roomConfig = plugin.gameManager.getRoomConfig(args[1]);
        if (roomConfig == null) {
            sender.sendMessage(RED + "Room not found: " + args[1]);
            return;
        }
        target.teleport(roomConfig.getSpawnLocation());
    }

    private void categories(CommandSender sender, String[] args) {
        if (!perm(sender, "phoban.categories")) return;
        Player player = requirePlayer(sender);
        if (player == null) return;
        if (args.length < 2) {
            if (plugin.gameManager.getCategories().isEmpty()) GuiRegistry.openRoomSelector(player);
            else GuiRegistry.openCategorySelector(player);
            return;
        }
        String category = category(args[1]);
        if (category == null) {
            sender.sendMessage(RED + "Category not found: " + args[1]);
            return;
        }
        GuiRegistry.openRoomSelector(player, category);
    }

    private void sound(CommandSender sender) {
        if (!perm(sender, "phoban.sound")) return;
        Player player = requirePlayer(sender);
        if (player != null) GuiRegistry.openSoundExplorer(player);
    }

    private void ticket(CommandSender sender, String[] args) {
        if (args.length < 4 || (!args[1].equalsIgnoreCase("add") && !args[1].equalsIgnoreCase("set"))) {
            sender.sendMessage(RED + "Usage: /pb ticket <add|set> <player> <amount>");
            return;
        }
        boolean add = args[1].equalsIgnoreCase("add");
        if (!perm(sender, add ? "phoban.ticket.add" : "phoban.ticket.set")) return;
        OfflinePlayer player = offline(sender, args[2]);
        Integer amount = integer(sender, args[3], "amount");
        if (player == null || amount == null) return;
        plugin.playerDataManager.requireData(player.getUniqueId()).whenComplete((playerData, throwable) -> {
            if (throwable != null) sender.sendMessage(RED + throwable.getMessage());
            else if (add) {
                playerData.addTicket(amount);
                sender.sendMessage(GREEN + "Added " + amount + " tickets for " + player.getName());
            } else {
                playerData.setTicket(amount);
                sender.sendMessage(GREEN + "Set " + amount + " tickets for " + player.getName());
            }
        });
    }

    private void admin(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(RED + "Usage: /pb admin <create|openCategories> ...");
            return;
        }
        if (args[1].equalsIgnoreCase("create")) adminCreate(sender, args);
        else if (args[1].equalsIgnoreCase("openCategories")) openCategories(sender, args);
        else sender.sendMessage(RED + "Usage: /pb admin <create|openCategories> ...");
    }

    private void adminCreate(CommandSender sender, String[] args) {
        if (!perm(sender, "phoban.admin.create")) return;
        Player player = requirePlayer(sender);
        if (player == null) return;
        if (args.length < 4) {
            sender.sendMessage(RED + "Usage: /pb admin create <room> <difficulty> [challengeLevel]");
            return;
        }
        Difficulty difficulty;
        try {
            difficulty = Difficulty.valueOf(args[3].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(RED + "Invalid difficulty. Valid: EASY, MEDIUM, HARD, EXTREME, CHALLENGE");
            return;
        }
        if (plugin.gameManager.getRoomConfig(args[2]) == null) {
            sender.sendMessage(RED + "Room not found: " + args[2]);
            return;
        }
        int challengeLevel = 0;
        if (difficulty == Difficulty.CHALLENGE && args.length > 4) {
            Integer parsed = integer(sender, args[4], "challenge level");
            if (parsed == null) return;
            challengeLevel = parsed - 1;
        }
        sender.sendMessage(GREEN + "Creating room " + args[2] + " at difficulty " + difficulty + "...");
        plugin.gameManager.attemptCreateRoom(player, args[2], difficulty, challengeLevel);
    }

    private void openCategories(CommandSender sender, String[] args) {
        if (!perm(sender, "phoban.admin.openCategories")) return;
        if (args.length < 4) {
            sender.sendMessage(RED + "Usage: /pb admin openCategories <category> <player>");
            return;
        }
        Player player = Bukkit.getPlayerExact(args[3]);
        if (player == null) {
            sender.sendMessage(RED + "Player not found: " + args[3]);
            return;
        }
        if (plugin.gameManager.getCategories().isEmpty()) GuiRegistry.openRoomSelector(player);
        else GuiRegistry.openCategorySelector(player);
    }

    private void getpos(CommandSender sender) {
        if (!perm(sender, "phoban.getpos")) return;
        Player player = requirePlayer(sender);
        if (player == null) return;
        var loc = player.getLocation();
        var locString = String.join(" ", loc.getWorld().getName(), Integer.toString(loc.getBlockX()), Integer.toString(loc.getBlockY()), Integer.toString(loc.getBlockZ()), Integer.toString(Math.round(loc.getYaw())), Integer.toString(Math.round(loc.getPitch())));
        var text = new ComponentBuilder("Your location is ").color(net.md_5.bungee.api.ChatColor.WHITE)
                .append(locString).color(net.md_5.bungee.api.ChatColor.BLUE).underlined(true)
                .event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, locString))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Yes click here")))
                .append(" (click the underlined text to copy)").reset().color(net.md_5.bungee.api.ChatColor.GRAY).italic(true)
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("NO NOT THIS TEXT!!1!"))).create();
        player.spigot().sendMessage(text);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(USE_PERMISSION)) return Collections.emptyList();
        if (args.length == 1) return match(args[0], top(sender));
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "profile" -> args.length == 2 && sender.hasPermission("phoban.profile") ? match(args[1], players()) : Collections.emptyList();
            case "enable", "disable" -> args.length == 2 && sender.hasPermission("phoban." + sub) ? match(args[1], rooms(sender)) : Collections.emptyList();
            case "tp" -> !sender.hasPermission("phoban.tp") ? Collections.emptyList() : args.length == 2 ? match(args[1], rooms(sender)) : args.length == 3 ? match(args[2], players()) : Collections.emptyList();
            case "join" -> !sender.hasPermission("phoban.join") ? Collections.emptyList() : args.length == 2 ? match(args[1], activeRooms(sender)) : args.length == 3 ? match(args[2], players()) : Collections.emptyList();
            case "start", "end", "terminate" -> args.length == 2 && sender.hasPermission("phoban." + sub) ? match(args[1], activeRooms(sender)) : Collections.emptyList();
            case "reset" -> completeReset(sender, args);
            case "categories" -> args.length == 2 && sender.hasPermission("phoban.categories") ? match(args[1], plugin.gameManager.getCategories()) : Collections.emptyList();
            case "ticket" -> completeTicket(sender, args);
            case "admin" -> completeAdmin(sender, args);
            default -> Collections.emptyList();
        };
    }

    private List<String> top(CommandSender sender) {
        List<String> list = new ArrayList<>(List.of("help", "quit"));
        add(list, sender, "phoban.profile", "profile");
        add(list, sender, "phoban.reload", "reload");
        add(list, sender, "phoban.enable", "enable");
        add(list, sender, "phoban.disable", "disable");
        add(list, sender, "phoban.list", "list");
        add(list, sender, "phoban.join", "join");
        add(list, sender, "phoban.start", "start");
        add(list, sender, "phoban.end", "end");
        add(list, sender, "phoban.terminate", "terminate");
        if (sender.hasPermission("phoban.reset.respawn") || sender.hasPermission("phoban.reset.data")) list.add("reset");
        add(list, sender, "phoban.tp", "tp");
        add(list, sender, "phoban.categories", "categories");
        add(list, sender, "phoban.sound", "sound");
        if (sender.hasPermission("phoban.ticket.add") || sender.hasPermission("phoban.ticket.set")) list.add("ticket");
        if (sender.hasPermission("phoban.admin.create") || sender.hasPermission("phoban.admin.openCategories")) list.add("admin");
        add(list, sender, "phoban.getpos", "getpos");
        return list;
    }

    private void add(List<String> list, CommandSender sender, String permission, String value) {
        if (sender.hasPermission(permission)) list.add(value);
    }

    private List<String> completeReset(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> list = new ArrayList<>();
            add(list, sender, "phoban.reset.respawn", "respawn");
            add(list, sender, "phoban.reset.data", "data");
            return match(args[1], list);
        }
        if (args[1].equalsIgnoreCase("respawn") && sender.hasPermission("phoban.reset.respawn")) return args.length == 3 ? match(args[2], activeRooms(sender)) : args.length == 4 ? match(args[3], players()) : Collections.emptyList();
        if (args[1].equalsIgnoreCase("data") && sender.hasPermission("phoban.reset.data") && args.length == 3) return match(args[2], players());
        return Collections.emptyList();
    }

    private List<String> completeTicket(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> list = new ArrayList<>();
            add(list, sender, "phoban.ticket.add", "add");
            add(list, sender, "phoban.ticket.set", "set");
            return match(args[1], list);
        }
        return args.length == 3 && (sender.hasPermission("phoban.ticket.add") || sender.hasPermission("phoban.ticket.set")) ? match(args[2], players()) : Collections.emptyList();
    }

    private List<String> completeAdmin(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> list = new ArrayList<>();
            add(list, sender, "phoban.admin.create", "create");
            add(list, sender, "phoban.admin.openCategories", "openCategories");
            return match(args[1], list);
        }
        if (args[1].equalsIgnoreCase("create") && sender.hasPermission("phoban.admin.create")) return args.length == 3 ? match(args[2], rooms(sender)) : args.length == 4 ? match(args[3], Arrays.stream(Difficulty.values()).map(Enum::name).toList()) : Collections.emptyList();
        if (args[1].equalsIgnoreCase("openCategories") && sender.hasPermission("phoban.admin.openCategories")) return args.length == 3 ? match(args[2], plugin.gameManager.getCategories()) : args.length == 4 ? match(args[3], players()) : Collections.emptyList();
        return Collections.emptyList();
    }

    private List<String> players() {
        List<String> list = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) list.add(player.getName());
        return list;
    }

    private List<String> rooms(CommandSender sender) {
        return plugin.gameManager.getRoomIds().stream().filter(id -> canAccess(sender, id)).toList();
    }

    private String category(String input) {
        for (String category : plugin.gameManager.getCategories()) {
            if (category.equalsIgnoreCase(input)) return category;
        }
        return null;
    }

    private List<String> activeRooms(CommandSender sender) {
        return plugin.gameManager.getActiveRoomIds().stream().filter(id -> canAccess(sender, id)).toList();
    }

    private boolean canAccess(CommandSender sender, String id) {
        RoomConfig roomConfig = plugin.gameManager.getRoomConfig(id);
        String permission = roomConfig == null ? null : roomConfig.getPermission();
        return permission == null || sender.hasPermission(permission);
    }

    private List<String> match(String input, Collection<String> options) {
        String lowerInput = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lowerInput)).toList();
    }
}
