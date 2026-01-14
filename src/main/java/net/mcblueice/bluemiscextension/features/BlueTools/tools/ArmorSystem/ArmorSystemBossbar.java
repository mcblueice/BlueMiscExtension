package net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.utils.ConfigManager;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArmorSystemBossbar {

    private final BlueMiscExtension plugin;
    private final ConfigManager lang;
    private final ArmorSystem armorSystem;
    private final Map<UUID, BossBar> bossBars;
    public final NamespacedKey showBossBarKey;

    public ArmorSystemBossbar(ArmorSystem armorSystem) {
        this.plugin = BlueMiscExtension.getInstance();
        this.lang = plugin.getLanguageManager();
        this.armorSystem = armorSystem;
        this.bossBars = new HashMap<>();
        this.showBossBarKey = new NamespacedKey("bluetools", "show_boss_bar");
    }

    public void update(Player player, long currentEnergy, int totalCost) {
        if (isShowBossBar(player) && armorSystem.isWearingAnyPowerArmor(player)) {
            BossBar bar = bossBars.computeIfAbsent(player.getUniqueId(), k -> 
                Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID));
            
            if (!bar.getPlayers().contains(player)) bar.addPlayer(player);

            String timeStr;
            if (totalCost > 0) {
                long durationSeconds = currentEnergy / totalCost;
                timeStr = formatDuration(durationSeconds);
            } else {
                timeStr = lang.get("ArmorSystem.BossBar.Time.Infinity");
            }

            String title = lang.get("ArmorSystem.BossBar.Format", currentEnergy, totalCost, timeStr);
            bar.setTitle(title);
            bar.setProgress(1.0);
            bar.setVisible(true);
        } else {
            removeBossBar(player);
        }
    }

    private String formatDuration(long seconds) {
        Duration d = Duration.ofSeconds(seconds);
        long days = d.toDays();
        
        if (days >= 30) {
            long months = days / 30;
            long remainingDays = days % 30;
            return lang.get("ArmorSystem.BossBar.Time.Months", months, remainingDays);
        } else if (days >= 1) {
            return lang.get("ArmorSystem.BossBar.Time.Days", days, d.toHoursPart());
        } else if (d.toHours() > 0) {
            return lang.get("ArmorSystem.BossBar.Time.Hours", d.toHours(), d.toMinutesPart());
        } else if (d.toMinutes() > 0) {
            return lang.get("ArmorSystem.BossBar.Time.Minutes", d.toMinutes(), d.toSecondsPart());
        } else {
            return lang.get("ArmorSystem.BossBar.Time.Seconds", d.toSeconds());
        }
    }

    public boolean isShowBossBar(Player player) {
        return player.getPersistentDataContainer().getOrDefault(showBossBarKey, PersistentDataType.BOOLEAN, false);
    }

    public void setShowBossBar(Player player, boolean show) {
        player.getPersistentDataContainer().set(showBossBarKey, PersistentDataType.BOOLEAN, show);
        if (!show) removeBossBar(player);
    }

    public void removeBossBar(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) bar.removePlayer(player);
    }
}
