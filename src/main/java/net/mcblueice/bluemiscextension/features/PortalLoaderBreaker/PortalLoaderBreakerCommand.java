package net.mcblueice.bluemiscextension.features.PortalLoaderBreaker;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.commands.CommandManager;
import net.mcblueice.bluemiscextension.commands.SubCommand;
import net.mcblueice.bluemiscextension.utils.ConfigManager;

public class PortalLoaderBreakerCommand implements SubCommand {
    private final BlueMiscExtension plugin;
    private final CommandManager commandManager;
    private final ConfigManager lang;
    private final PortalLoaderBreaker feature;

    public PortalLoaderBreakerCommand(BlueMiscExtension plugin, PortalLoaderBreaker feature) {
        this.plugin = plugin;
        this.commandManager = plugin.getCommandManager();
        this.lang = plugin.getLanguageManager();
        this.feature = feature;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bluemiscextension.portalbreak")) return commandManager.NoPermission(sender);

        long durationMillis;
        if (args.length > 2) return commandManager.UsageError(sender);
        if (args.length == 1) {
            durationMillis = Math.max(1, plugin.getConfig().getLong("Features.PortalLoaderBreaker.DelayTime", 180000));
        } else {
            try {
                durationMillis = Long.parseLong(args[1]) * 1000;
            } catch (NumberFormatException e) {
                return commandManager.UsageError(sender);
            }
            if (durationMillis <= 0) return commandManager.UsageError(sender);
        }

        feature.stopPortalLoading(durationMillis);
        sender.sendMessage(lang.get("Prefix.Default") + lang.get("PortalLoaderBreaker.Stop", String.valueOf(durationMillis/1000)));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!sender.hasPermission("bluemiscextension.portalbreak")) return Collections.emptyList();
            return List.of("30", "60", "300", "600");
        }
        return Collections.emptyList();
    }
}