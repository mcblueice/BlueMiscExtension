package net.mcblueice.bluemiscextension.features.PlayerNick;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.commands.CommandManager;
import net.mcblueice.bluemiscextension.commands.SubCommand;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;
import net.mcblueice.bluelib.utils.TextUtil;

public class PlayerNickCommand implements SubCommand {
    private final BlueMiscExtension plugin;
    private final DatabaseUtil databaseUtil;
    private final CommandManager commandManager;
    private final PlayerNick playerNick;
    private final ConfigManager lang;

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("(?i)\\{#[0-9a-f]{6}\\}|&#[0-9a-f]{6}|&x(?:&[0-9a-f]){6}|</?(?:#[0-9a-f]{6}|color:#[0-9a-f]{6}|c:#[0-9a-f]{6}|gradient[^>]*|rainbow[^>]*|transition[^>]*)[>]");

    public PlayerNickCommand(BlueMiscExtension plugin, PlayerNick playerNick) {
        this.plugin = plugin;
        this.databaseUtil = plugin.getDatabaseUtil();
        this.commandManager = plugin.getCommandManager();
        this.playerNick = playerNick;
        this.lang = plugin.getLanguageManager();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bluemiscextension.nick")) return commandManager.NoPermission(sender);
        if (args.length < 2 || args.length > 3) return commandManager.UsageError(sender);

        String rawInput = args[1].trim();
        if (rawInput.equals("-clear")) rawInput = null;

        Player target;

        if (args.length == 3) {
            if (!sender.hasPermission("bluemiscextension.nick.other")) return commandManager.NoPermission(sender);
            target = plugin.getServer().getPlayerExact(args[2]);
            if (target == null) return commandManager.PlayerNotFound(sender, args[2]);
        } else {
            if (sender instanceof Player player) {
                target = player;
            } else {
                return commandManager.OnlyPlayer(sender);
            }
        }

        if (rawInput == null) {
            if (!databaseUtil.isPlayerDataLoaded(target.getUniqueId())) return commandManager.PlayerDataNotLoaded(sender, target.getName());
            playerNick.setPlayerNick(target, "");

            if (sender.equals(target)) {
                sender.sendMessage(lang.getComponent("Prefix.Default").append(lang.getComponent("PlayerNick.ClearSelf")));
            } else {
                sender.sendMessage(lang.getComponent("Prefix.Default").append(lang.getComponent("PlayerNick.ClearOther", target.getName())));
                target.sendMessage(lang.getComponent("Prefix.Default").append(lang.getComponent("PlayerNick.ClearByOther")));
            }
            return true;
        }

        boolean hasBasicPerm = sender.hasPermission("bluemiscextension.nick.basiccolor");
        boolean hasHexPerm = sender.hasPermission("bluemiscextension.nick.hexcolor");

        if (rawInput.length() > 200) {
            sender.sendMessage(lang.getComponent("Prefix.Default").append(lang.getComponent("PlayerNick.RawInputTooLong")));
            return true;
        }

        if (hasHexPerm) {
            rawInput = TextUtil.parseToString(rawInput, true, false);
        } else if (hasBasicPerm) {
            rawInput = HEX_COLOR_PATTERN.matcher(rawInput).replaceAll("");
            rawInput = TextUtil.parseToString(rawInput, true, false);
        } else {
            rawInput = TextUtil.parseToString(rawInput, false, false);
        }

        int realLength = TextUtil.getLength(rawInput);

        if (realLength == 0) {
            sender.sendMessage(lang.getComponent("Prefix.Default").append(lang.getComponent("PlayerNick.InvalidInput")));
            return true;
        }

        if (realLength > plugin.getConfig().getInt("Features.PlayerNick.max_display_name_length", 16)) {
            sender.sendMessage(lang.getComponent("Prefix.Default").append(lang.getComponent("PlayerNick.NickTooLong", String.valueOf(plugin.getConfig().getInt("Features.PlayerNick.max_display_name_length", 16)), String.valueOf(realLength))));
            return true;
        }

        if (!databaseUtil.isPlayerDataLoaded(target.getUniqueId())) return commandManager.PlayerDataNotLoaded(sender, target.getName());

        playerNick.setPlayerNick(target, rawInput);

        if (sender.equals(target)) {
            sender.sendMessage(lang.getComponent("Prefix.Default").append(lang.getComponent("PlayerNick.SetSelf", rawInput)));
        } else {
            sender.sendMessage(lang.getComponent("Prefix.Default").append(lang.getComponent("PlayerNick.SetOther", target.getName(), rawInput)));
            target.sendMessage(lang.getComponent("Prefix.Default").append(lang.getComponent("PlayerNick.SetByOther", rawInput)));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            List<String> subs = new ArrayList<>();
            subs.add("-clear");
            if (sender.hasPermission("bluemiscextension.nick.basiccolor")) subs.add("&9基礎色碼");
            if (sender.hasPermission("bluemiscextension.nick.hexcolor")) subs.add("{#FF0000}進階色碼");
            StringUtil.copyPartialMatches(args[1], subs, completions);
            return completions;
        }

        if (args.length == 3) {
            if (sender.hasPermission("bluemiscextension.nick.other")) return null;
        }
        return Collections.emptyList();
    }
}
