package net.mcblueice.bluemiscextension.commands.subcommands;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.commands.CommandManager;
import net.mcblueice.bluemiscextension.commands.SubCommand;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluelib.utils.TaskScheduler;

public class ReloadCommand implements SubCommand {
    private final BlueMiscExtension plugin;
    private final CommandManager commandManager;
    private final ConfigManager lang;

    public ReloadCommand(BlueMiscExtension plugin, CommandManager commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.lang = plugin.getLanguageManager();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bluemiscextension.reload")) return commandManager.NoPermission(sender);

        TaskScheduler.runGlobalTask(plugin, () -> {
            plugin.reloadConfig();
            lang.reload();
            plugin.getFeatureManager().reload();
            if (plugin.getAliasCommand() != null) plugin.getAliasCommand().reload();
            sender.sendMessage(lang.get("Prefix.Default") + lang.get("ReloadSuccess"));
        });
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
