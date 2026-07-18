package net.mcblueice.bluemiscextension.commands.subcommands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.commands.CommandManager;
import net.mcblueice.bluemiscextension.commands.SubCommand;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.mcblueice.bluelib.utils.TextUtil;

public class InfoCommand implements SubCommand {
    private static final PersistentDataType<?, ?>[] SUPPORTED_PDC_TYPES = {
        PersistentDataType.STRING,
        PersistentDataType.INTEGER,
        PersistentDataType.DOUBLE,
        PersistentDataType.BYTE,
        PersistentDataType.SHORT,
        PersistentDataType.LONG,
        PersistentDataType.FLOAT,
        PersistentDataType.BYTE_ARRAY,
        PersistentDataType.INTEGER_ARRAY,
        PersistentDataType.LONG_ARRAY,
        PersistentDataType.BOOLEAN,
        PersistentDataType.LIST.strings()
    };

    private final BlueMiscExtension plugin;
    private final CommandManager commandManager;
    private final ConfigManager lang;
    private final DatabaseUtil databaseUtil;

    public InfoCommand(BlueMiscExtension plugin, CommandManager commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.lang = plugin.getLanguageManager();
        this.databaseUtil = plugin.getDatabaseUtil();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bluemiscextension.info")) return commandManager.NoPermission(sender);

        String targetName;
        if (args.length < 2) {
            if (sender instanceof Player player) {
                targetName = player.getName();
            } else {
                return commandManager.OnlyPlayer(sender);
            }
        } else {
            targetName = args[1];
        }

        Player target = plugin.getServer().getPlayerExact(targetName);
        if (target != null) {
            sendOnlineInfo(sender, target);
        } else {
            sender.sendMessage(TextUtil.parse(lang.get("Prefix.Default") + lang.get("SearchingPlayer", targetName), true, true));
            final String lookupName = targetName;
            databaseUtil.getOfflinePlayerData(lookupName).thenAccept(playerData -> {
                if (playerData == null) {
                    sender.sendMessage(TextUtil.parse(lang.get("Prefix.Default") + lang.get("PlayerNotFound", lookupName), true, true));
                    return;
                }
                sendOfflineInfo(sender, playerData);
            });
        }
        return true;
    }

    private void sendOnlineInfo(CommandSender sender, Player target) {
        UUID uuid = target.getUniqueId();
        PlayerData playerData = databaseUtil.getPlayerData(uuid);

        String nameStr = target.getName();
        Component nameComponent = TextUtil.parse(nameStr, true, false).clickEvent(ClickEvent.copyToClipboard(nameStr));

        String displayNameStr = MiniMessage.miniMessage().serialize(target.displayName());
        Component displayName = target.displayName().clickEvent(ClickEvent.copyToClipboard(displayNameStr));

        String uuidStr = uuid.toString();
        Component uuidComponent = TextUtil.parse(uuidStr, true, false).clickEvent(ClickEvent.copyToClipboard(uuidStr));

        String nicknameStr = playerData.nickname();
        Component nickname = (nicknameStr != null && !nicknameStr.isEmpty()) ? lang.getComponent("Info.NicknamePrefix", nicknameStr).clickEvent(ClickEvent.copyToClipboard(nicknameStr)) : Component.empty();

        Component hiddenArmor = playerData.hiddenArmor() ? lang.getComponent("Info.HiddenArmorTrue") : lang.getComponent("Info.HiddenArmorFalse");

        String hostnameStr = playerData.hostname();
        Component hostnameComponent = TextUtil.parse(hostnameStr, true, false).clickEvent(ClickEvent.copyToClipboard(hostnameStr));

        String ipStr = playerData.ip();
        Component ipComponent = TextUtil.parse(ipStr, true, false).clickEvent(ClickEvent.copyToClipboard(ipStr));

        Component pdcBlock = buildPdcBlock(target.getPersistentDataContainer());

        List<Component> lines = lang.getComponentList("Info.Online",
            nameComponent,
            displayName,
            uuidComponent,
            nickname,
            hiddenArmor,
            hostnameComponent,
            ipComponent,
            pdcBlock
        );
        for (Component line : lines) {
            sender.sendMessage(line);
        }
    }

    private void sendOfflineInfo(CommandSender sender, PlayerData playerData) {
        String nameStr = playerData.playerName();
        Component nameComponent = TextUtil.parse(nameStr, true, false).clickEvent(ClickEvent.copyToClipboard(nameStr));

        String uuidStr = playerData.uuid().toString();
        Component uuidComponent = TextUtil.parse(uuidStr, true, false).clickEvent(ClickEvent.copyToClipboard(uuidStr));

        String nicknameStr = playerData.nickname();
        Component nickname = (nicknameStr != null && !nicknameStr.isEmpty())
            ? lang.getComponent("Info.NicknamePrefix", nicknameStr).clickEvent(ClickEvent.copyToClipboard(nicknameStr))
            : Component.empty();

        Component hiddenArmor = playerData.hiddenArmor() ? lang.getComponent("Info.HiddenArmorTrue") : lang.getComponent("Info.HiddenArmorFalse");

        String hostnameStr = playerData.hostname();
        Component hostnameComponent = TextUtil.parse(hostnameStr, true, false).clickEvent(ClickEvent.copyToClipboard(hostnameStr));

        String ipStr = playerData.ip();
        Component ipComponent = TextUtil.parse(ipStr, true, false).clickEvent(ClickEvent.copyToClipboard(ipStr));

        List<Component> lines = lang.getComponentList("Info.Offline",
            nameComponent,
            uuidComponent,
            nickname,
            hiddenArmor,
            hostnameComponent,
            ipComponent
        );
        for (Component line : lines) {
            sender.sendMessage(line);
        }
    }

    private Component buildPdcBlock(PersistentDataContainer pdc) {
        Component pdcBlock = Component.empty().append(lang.getComponent("Info.PdcHeader"));
        Set<NamespacedKey> keys = pdc.getKeys();

        if (keys.isEmpty()) {
            return pdcBlock.append(Component.text("\n")).append(lang.getComponent("Info.PdcEmpty"));
        }

        for (NamespacedKey key : keys) {
            String keyName = key.toString();
            String valueStr = null;
            for (PersistentDataType<?, ?> type : SUPPORTED_PDC_TYPES) {
                if (pdc.has(key, type)) {
                    Object rawValue = pdc.get(key, type);

                    if (rawValue instanceof byte[]) {
                        valueStr = Arrays.toString((byte[]) rawValue);
                    } else if (rawValue instanceof int[]) {
                        valueStr = Arrays.toString((int[]) rawValue);
                    } else if (rawValue instanceof long[]) {
                        valueStr = Arrays.toString((long[]) rawValue);
                    } else if (rawValue instanceof List) {
                        List<?> list = (List<?>) rawValue;
                        valueStr = list.isEmpty() ? "[]" : list.stream().map(String::valueOf).collect(Collectors.joining(", "));
                    } else {
                        valueStr = String.valueOf(rawValue);
                    }
                    break;
                }
            }
            pdcBlock = pdcBlock.append(Component.text("\n")).append(lang.getComponent("Info.PdcEntry", keyName, (valueStr == null) ? lang.get("Info.PdcUnparseable") : valueStr));
        }
        return pdcBlock;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            if (!sender.hasPermission("bluemiscextension.info.other")) return Collections.emptyList();
            return null;
        }
        return Collections.emptyList();
    }
}
