package net.mcblueice.bluemiscextension.commands.subcommands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.api.PlayerInventoryUpdateEvent;
import net.mcblueice.bluemiscextension.commands.CommandManager;
import net.mcblueice.bluemiscextension.commands.SubCommand;
import net.mcblueice.bluemiscextension.utils.ConfigManager;

public class UpdateInvCommand implements SubCommand {
    private final BlueMiscExtension plugin;
    private final CommandManager commandManager;
    private final ConfigManager lang;

    public UpdateInvCommand(BlueMiscExtension plugin, CommandManager commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.lang = plugin.getLanguageManager();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bluemiscextension.updateinv")) return commandManager.NoPermission(sender);

        if (args.length > 1) {
            Player target = plugin.getServer().getPlayerExact(args[1]);
            if (target == null) return commandManager.PlayerNotFound(sender, args[1]);
            PlayerInventoryUpdateEvent.call(target);
            sender.sendMessage(lang.get("Prefix.Default") + "已觸發 " + target.getName() + " 的物品欄更新");
        } else {
            int count = 0;
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                PlayerInventoryUpdateEvent.call(online);
                count++;
            }
            sender.sendMessage(lang.get("Prefix.Default") + "已觸發 " + count + " 位在線玩家的物品欄更新");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            if (!sender.hasPermission("bluemiscextension.updateinv")) return Collections.emptyList();
            List<String> completions = new ArrayList<>();
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                completions.add(online.getName());
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
