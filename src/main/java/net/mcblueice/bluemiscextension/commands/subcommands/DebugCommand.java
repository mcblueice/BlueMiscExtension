package net.mcblueice.bluemiscextension.commands.subcommands;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.commands.CommandManager;
import net.mcblueice.bluemiscextension.commands.SubCommand;
import net.mcblueice.bluemiscextension.utils.ConfigManager;

public class DebugCommand implements SubCommand {
    private final BlueMiscExtension plugin;
    private final CommandManager commandManager;
    private final ConfigManager lang;

    public DebugCommand(BlueMiscExtension plugin, CommandManager commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.lang = plugin.getLanguageManager();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bluemiscextension.debug")) return commandManager.NoPermission(sender);

        boolean enabled;
        if (sender instanceof Player player) {
            enabled = plugin.toggleDebugMode(player.getUniqueId());
        } else {
            enabled = plugin.toggleDebugMode(plugin.CONSOLE_UUID);
        }
        sender.sendMessage(lang.get("Prefix.Default") + lang.get(enabled ? "DebugEnabled" : "DebugDisabled"));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
