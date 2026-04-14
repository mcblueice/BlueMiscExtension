package net.mcblueice.bluemiscextension.commands;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import net.mcblueice.bluelib.utils.TaskScheduler;
import net.mcblueice.bluemiscextension.BlueMiscExtension;

public class AliasCommand {
    private static final int MAX_DISPATCH_DEPTH = 5;
    private static final ThreadLocal<Integer> DISPATCH_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> TAB_COMPLETE_DEPTH = ThreadLocal.withInitial(() -> 0);

    private final BlueMiscExtension plugin;
    private final File aliasFile;
    private final String fallbackPrefix;

    private final Map<String, String> aliasTargets = new ConcurrentHashMap<>();
    private final Map<String, Command> aliasTabTargets = new ConcurrentHashMap<>();
    private final Map<String, DynamicAliasCommand> registeredCommands = new ConcurrentHashMap<>();

    private CommandMap commandMap;
    private Map<String, Command> knownCommands;

    public AliasCommand(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.aliasFile = new File(plugin.getDataFolder(), "alias.yml");
        this.fallbackPrefix = plugin.getName().toLowerCase(Locale.ROOT);
    }

    public void reload() {
        unregisterAll();
        registerAll();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            TaskScheduler.runTask(player, plugin, player::updateCommands);
        }
    }

    public void registerAll() {
        if (!aliasFile.exists()) plugin.saveResource("alias.yml", false);

        if (!initCommandMap()) {
            plugin.getLogger().warning("無法初始化 CommandMap Alias 指令不會被載入");
            return;
        }

        aliasTargets.clear();
        aliasTabTargets.clear();

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(aliasFile);
        int loaded = 0;
        int skipped = 0;

        for (String featureName : yml.getKeys(false)) {
            ConfigurationSection featureSection = yml.getConfigurationSection(featureName);
            if (featureSection == null) {
                skipped++;
                continue;
            }

            if (!plugin.getFeatureManager().isFeatureEnabled(featureName)) {
                skipped += featureSection.getKeys(false).size();
                continue;
            }

            for (String alias : featureSection.getKeys(false)) {
                ConfigurationSection aliasSection = featureSection.getConfigurationSection(alias);
                if (aliasSection == null) {
                    skipped++;
                    continue;
                }

                if (!aliasSection.getBoolean("enabled", true)) {
                    skipped++;
                    continue;
                }

                String target = aliasSection.getString("target", "").trim();
                if (target.isEmpty()) {
                    plugin.getLogger().warning("Alias target 為空 已跳過: " + featureName + "." + alias);
                    skipped++;
                    continue;
                }

                String normalizedAlias = alias.toLowerCase(Locale.ROOT);
                if (aliasTargets.containsKey(normalizedAlias)) {
                    plugin.getLogger().warning("發現重複 alias 已跳過: " + normalizedAlias);
                    skipped++;
                    continue;
                }

                if (isCommandOccupied(normalizedAlias)) {
                    plugin.getLogger().warning("Alias 與現有指令衝突 已跳過: " + normalizedAlias);
                    skipped++;
                    continue;
                }

                aliasTargets.put(normalizedAlias, target);
                loaded++;
            }
        }

        // 將 alias 註冊為 Bukkit Command
        for (Map.Entry<String, String> entry : aliasTargets.entrySet()) {
            String alias = entry.getKey();
            DynamicAliasCommand dynamicCommand = new DynamicAliasCommand(this, alias);
            commandMap.register(fallbackPrefix, dynamicCommand);
            registeredCommands.put(alias, dynamicCommand);

            String targetLabel = parseTargetLabel(entry.getValue());
            if (targetLabel != null) {
                Command targetCommand = knownCommands.get(targetLabel);
                if (targetCommand != null) aliasTabTargets.put(alias, targetCommand);
            }
        }
        plugin.getLogger().info("Alias 載入完成: 註冊 " + loaded + " 筆 跳過 " + skipped + " 筆");
    }

    public void unregisterAll() {
        if (!initCommandMap()) {
            registeredCommands.clear();
            aliasTargets.clear();
            aliasTabTargets.clear();
            return;
        }

        // 同時從 knownCommands 移除短名與 namespaced 名稱
        for (Map.Entry<String, DynamicAliasCommand> entry : registeredCommands.entrySet()) {
            String alias = entry.getKey();
            DynamicAliasCommand cmd = entry.getValue();

            cmd.unregister(commandMap);
            knownCommands.remove(alias);
            knownCommands.remove(fallbackPrefix + ":" + alias);
        }

        registeredCommands.clear();
        aliasTargets.clear();
        aliasTabTargets.clear();
    }

    private boolean executeAlias(CommandSender sender, String alias, String[] args) {
        String target = aliasTargets.get(alias.toLowerCase(Locale.ROOT));
        if (target == null || target.isBlank()) return false;

        // 避免 alias 無限遞迴
        int depth = DISPATCH_DEPTH.get();
        if (depth >= MAX_DISPATCH_DEPTH) {
            sender.sendMessage("§cAlias 呼叫層級過深 已中止執行");
            return true;
        }

        DISPATCH_DEPTH.set(depth + 1);
        try {
            StringBuilder commandLine = new StringBuilder(target);
            for (String arg : args) {
                if (arg == null || arg.isBlank()) continue;
                commandLine.append(' ').append(arg);
            }
            return Bukkit.dispatchCommand(sender, commandLine.toString());
        } finally {
            DISPATCH_DEPTH.set(depth);
        }
    }

    private List<String> tabCompleteAlias(CommandSender sender, String alias, String[] args) {
        String target = aliasTargets.get(alias.toLowerCase(Locale.ROOT));
        if (target == null || target.isBlank()) return Collections.emptyList();

        int depth = TAB_COMPLETE_DEPTH.get();
        if (depth >= MAX_DISPATCH_DEPTH) return Collections.emptyList();

        TAB_COMPLETE_DEPTH.set(depth + 1);
        try {
            String[] targetParts = target.trim().split("\\s+");
            if (targetParts.length == 0) return Collections.emptyList();

            String targetLabel = targetParts[0];
            if (targetLabel.startsWith("/")) targetLabel = targetLabel.substring(1);
            targetLabel = targetLabel.toLowerCase(Locale.ROOT);
            if (targetLabel.isEmpty()) return Collections.emptyList();

            Command targetCommand = aliasTabTargets.get(alias.toLowerCase(Locale.ROOT));
            if (targetCommand == null) return Collections.emptyList();

            int fixedArgCount = Math.max(0, targetParts.length - 1);
            String[] mergedArgs = new String[fixedArgCount + args.length];

            if (fixedArgCount > 0) System.arraycopy(targetParts, 1, mergedArgs, 0, fixedArgCount);
            if (args.length > 0) System.arraycopy(args, 0, mergedArgs, fixedArgCount, args.length);

            List<String> completions = targetCommand.tabComplete(sender, targetLabel, mergedArgs);
            return completions != null ? completions : Collections.emptyList();
        } finally {
            TAB_COMPLETE_DEPTH.set(depth);
        }
    }

    private boolean initCommandMap() {
        if (commandMap != null && knownCommands != null) return true;

        this.commandMap = Bukkit.getCommandMap();
        if (this.commandMap == null) {
            plugin.getLogger().warning("取得 CommandMap 失敗: Bukkit.getCommandMap() 回傳 null");
            return false;
        }

        this.knownCommands = commandMap.getKnownCommands();
        if (this.knownCommands == null) {
            plugin.getLogger().warning("取得 knownCommands 失敗: CommandMap.getKnownCommands() 回傳 null");
            this.commandMap = null;
            return false;
        }

        return true;
    }

    private boolean isCommandOccupied(String alias) {
        return knownCommands.containsKey(alias) || knownCommands.containsKey(fallbackPrefix + ":" + alias);
    }

    private String parseTargetLabel(String target) {
        if (target == null || target.isBlank()) return null;

        String[] parts = target.trim().split("\\s+");
        if (parts.length == 0) return null;

        String label = parts[0];
        if (label.startsWith("/")) label = label.substring(1);
        label = label.toLowerCase(Locale.ROOT);
        return label.isEmpty() ? null : label;
    }

    private static final class DynamicAliasCommand extends Command {
        private final AliasCommand owner;
        private final String alias;

        private DynamicAliasCommand(AliasCommand owner, String alias) {
            super(alias);
            this.owner = owner;
            this.alias = alias;
            this.setDescription("BlueMiscExtension alias command");
            this.setUsage("/" + alias);
            this.setAliases(Collections.emptyList());
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            return owner.executeAlias(sender, alias, args);
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String commandAlias, String[] args) {
            return owner.tabCompleteAlias(sender, alias, args);
        }
    }
}