package net.mcblueice.bluemiscextension.features.ItemSignature;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.commands.CommandManager;
import net.mcblueice.bluemiscextension.commands.SubCommand;
import net.mcblueice.bluelib.utils.TaskScheduler;

public class ItemSignatureCommand implements SubCommand {
    private final BlueMiscExtension plugin;
    private final CommandManager commandManager;
    private final ItemSignature itemSignature;

    public ItemSignatureCommand(BlueMiscExtension plugin, ItemSignature itemSignature) {
        this.plugin = plugin;
        this.commandManager = plugin.getCommandManager();
        this.itemSignature = itemSignature;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bluemiscextension.itemsign")) return commandManager.NoPermission(sender);
        if (!(sender instanceof Player player)) return commandManager.OnlyPlayer(sender);
        if (args.length != 1) return commandManager.UsageError(sender);

        TaskScheduler.runTask(player, plugin, () -> {
            itemSignature.signMainHandItem(player);
        });
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}