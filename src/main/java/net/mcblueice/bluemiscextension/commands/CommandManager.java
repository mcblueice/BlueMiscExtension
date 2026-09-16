package net.mcblueice.bluemiscextension.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.commands.subcommands.DebugCommand;
import net.mcblueice.bluemiscextension.commands.subcommands.InfoCommand;
import net.mcblueice.bluemiscextension.commands.subcommands.ReloadCommand;
import net.mcblueice.bluemiscextension.commands.subcommands.StatusCommand;
import net.mcblueice.bluemiscextension.commands.subcommands.UnlockDataCommand;
import net.mcblueice.bluemiscextension.commands.subcommands.UpdateInvCommand;
import net.mcblueice.bluemiscextension.utils.ConfigManager;

public class CommandManager implements CommandExecutor, TabCompleter {
    private final ConfigManager lang;

    public record CommandNode(SubCommand command, String permission) {}

    private final Map<String, CommandNode> subCommands = new HashMap<>();

    public CommandManager(BlueMiscExtension plugin) {
        this.lang = plugin.getLanguageManager();

        this.register(new ReloadCommand(plugin, this), "bluemiscextension.reload", new String[]{"reload"});
        this.register(new DebugCommand(plugin, this), "bluemiscextension.debug", new String[]{"debug"});
        this.register(new StatusCommand(plugin, this), "bluemiscextension.status", new String[]{"status"});
        this.register(new UnlockDataCommand(plugin, this), "bluemiscextension.unlockdata", new String[]{"unlockdata"});
        this.register(new InfoCommand(plugin, this), "bluemiscextension.info", new String[]{"info"});
        this.register(new UpdateInvCommand(plugin, this), "bluemiscextension.updateinv", new String[]{"updateinv"});
    }

    public void register(SubCommand subCommand, String permission, String[] names) {
        CommandNode node = new CommandNode(subCommand, permission);
        for (String name : names) {
            subCommands.put(name.toLowerCase(), node);
        }
    }
    public void register(SubCommand subCommand, String[] names) {
        register(subCommand, null, names);
    }

    public void unregister(String name) {
        subCommands.remove(name.toLowerCase());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            String subCmdName = args[0].toLowerCase();
            if (subCommands.containsKey(subCmdName)) {
                CommandNode node = subCommands.get(subCmdName);
                if (node.permission() != null && !sender.hasPermission(node.permission())) return NoPermission(sender);
                return node.command().execute(sender, args);
            }
        }
        return UsageError(sender);
    }

    public boolean UsageError(CommandSender sender) {
        sender.sendMessage(lang.get("Prefix.Default") + lang.get("UsageError"));
        return true;
    }
    public boolean OnlyPlayer(CommandSender sender) {
        sender.sendMessage(lang.get("Prefix.Default") + lang.get("OnlyPlayer"));
        return true;
    }
    public boolean PlayerNotFound(CommandSender sender, String playerName) {
        sender.sendMessage(lang.get("Prefix.Default") + lang.get("PlayerNotFound", playerName));
        return true;
    }
    public boolean PlayerDataNotLoaded(CommandSender sender, String playerName) {
        sender.sendMessage(lang.get("Prefix.Default") + lang.get("PlayerDataNotLoaded", playerName));
        return true;
    }

    public boolean NoPermission(CommandSender sender) {
        sender.sendMessage(lang.get("Prefix.Default") + lang.get("NoPermission"));
        return true;
    }
    public boolean NotEnabled(CommandSender sender, String featureName) {
        sender.sendMessage(lang.get("Prefix.Default") + lang.get(featureName + ".NotEnabled"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            for (Map.Entry<String, CommandNode> entry : subCommands.entrySet()) {
                CommandNode node = entry.getValue();
                if (node.permission() == null || sender.hasPermission(node.permission())) subs.add(entry.getKey());
            }
            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], subs, completions);
            Collections.sort(completions);
            return completions;
        }

        if (args.length >= 2) {
            CommandNode node = subCommands.get(args[0].toLowerCase());
            if (node != null && (node.permission() == null || sender.hasPermission(node.permission()))) return node.command().tabComplete(sender, args);
        }

        return Collections.emptyList();
    }
}
