package net.mcblueice.bluemiscextension.commands.subcommands;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.commands.CommandManager;
import net.mcblueice.bluemiscextension.commands.SubCommand;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;
import net.mcblueice.bluemiscextension.utils.StatusUtil;
import net.mcblueice.bluelib.utils.TaskScheduler;

public class StatusCommand implements SubCommand {
    private final BlueMiscExtension plugin;
    private final CommandManager commandManager;
    private final ConfigManager lang;
    private final DatabaseUtil databaseUtil;

    public StatusCommand(BlueMiscExtension plugin, CommandManager commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.lang = plugin.getLanguageManager();
        this.databaseUtil = plugin.getDatabaseUtil();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bluemiscextension.status")) return commandManager.NoPermission(sender);

        TaskScheduler.runGlobalTask(plugin, () -> sender.sendMessage(StatusUtil.getServerStatus(sender)));
        TaskScheduler.runAsync(plugin, () -> sender.sendMessage(databaseUtil.getDatabaseStatus()));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
