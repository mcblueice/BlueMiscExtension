package net.mcblueice.bluemiscextension.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.text.DecimalFormat;

import com.sun.management.OperatingSystemMXBean;

public class ServerUtil {

    public static final boolean isFolia = checkFolia();
    public static final String mcVersion = getMCVersion();
    public static final int majorVersion = getMCVersionParts(0);
    public static final int minorVersion = getMCVersionParts(1);
    public static final int patchVersion = getMCVersionParts(2);

    private static final DecimalFormat df = new DecimalFormat("#.##");

    private ServerUtil() {
        throw new IllegalStateException("Utility class should not be instantiated");
    }

    private static boolean checkFolia() {
        try {
            Bukkit.class.getMethod("getRegionTPS", Location.class);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static String getMCVersion() {
        String version = Bukkit.getBukkitVersion();
        return version.split("-")[0];
    }
    public static int getMCVersionParts(int index) {
        String version = Bukkit.getBukkitVersion();
        String[] parts = version.split("-")[0].split("\\.");
        if (index >= parts.length) return 0;
        return Integer.parseInt(parts[index]);
    }

    public static boolean isNewAttributeKey() {
        return (majorVersion >= 26) || (minorVersion == 21 && patchVersion >= 7);
    }
    public static int getAbsorptionIndex() {
        return (majorVersion >= 26) || (minorVersion == 21 && patchVersion >= 11) ? 17 : 15;
    }
    public static boolean isNewParticle() {
        return (majorVersion >= 26) || (minorVersion == 21 && patchVersion >= 10);
    }

    public static Component getServerStatus(CommandSender sender) {
        TextComponent.Builder builder = Component.text();

        builder.append(Component.text("§8§m---------§r§a伺服器狀態§8§m---------\n"));
        builder.append(Component.text("§6核心類型: " + (isFolia ? "§bFolia" : "§ePaper/Spigot") + " §e" +getMCVersion() + "\n"));
        builder.append(Component.text("§6Bukkit版本: §e" + Bukkit.getBukkitVersion() + "\n"));

        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        int processors = osBean.getAvailableProcessors();
        double systemLoad = osBean.getProcessCpuLoad() * 100;
        if (systemLoad < 0) systemLoad = 0;

        builder.append(Component.text("§6CPU數量: §e" + processors + "\n"));
        builder.append(Component.text("§6CPU使用率: " + formatCPU(systemLoad) + df.format(systemLoad) + "%\n"));

        long maxMem = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        long totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        long usedMem = totalMem - freeMem;

        builder.append(Component.text("§6RAM使用率: " + formatMemory(usedMem, maxMem) + usedMem + "MB §7/ §e" + maxMem + "MB\n"));

        builder.append(Component.text("§8§m----------§r§5全域數據§8§m----------\n"));
        
        double[] tps = Bukkit.getTPS();
        builder.append(Component.text("§6TPS: " + (tps[0]) + "§7, " + (tps[1]) + "§7, " + (tps[2]) + "\n"));

        int totalChunks = 0;
        int totalEntities = 0;

        for (World world : Bukkit.getWorlds()) {
            int wEntities = world.getEntityCount();
            int wChunks = world.getLoadedChunks().length;
            totalEntities += wEntities;
            totalChunks += wChunks;

            TextComponent.Builder worldLine = Component.text()
                .append(Component.text("§7• §f" + world.getName() + ": "))
                .append(Component.text("§e" + wChunks + " §7區塊, "))
                .append(Component.text("§e" + wEntities + " §7生物"));

            if (sender instanceof Player) {
                Location spawn = world.getSpawnLocation();
                String cmd = "/execute in " + world.getKey().toString() + " run tp " + spawn.getBlockX() + " " + spawn.getBlockY() + " " + spawn.getBlockZ();
                
                worldLine.hoverEvent(HoverEvent.showText(Component.text("§b點擊傳送到 " + world.getName() + " 重生點")))
                         .clickEvent(ClickEvent.runCommand(cmd));
            }
            
            builder.append(worldLine.append(Component.text("\n")));
        }

        builder.append(Component.text("§6總計: §e" + totalChunks + " §6區塊, §e" + totalEntities + " §6生物\n"));
        builder.append(Component.text("§8§m--------------------------"));

        return builder.build();
    }

    private static String formatCPU(double load) {
        if (load < 50) return "§a";
        if (load < 80) return "§e";
        return "§c";
    }

    private static String formatMemory(long used, long max) {
        double percent = (double) used / max;
        if (percent < 0.50) return "§a";
        if (percent < 0.80) return "§e";
        return "§c";
    }

    public static String formatTPS(double tps) {
        tps = Math.min(20.0, tps);
        String tpsStr = df.format(tps);
        if (tps >= 19.8) return "§a" + tpsStr;
        if (tps >= 18.0) return "§2" + tpsStr;
        if (tps >= 15.0) return "§e" + tpsStr;
        if (tps >= 10.0) return "§c" + tpsStr;
        return "§4" + tpsStr;
    }

    public static String formatMspt(double mspt) {
        mspt = Math.max(0, mspt);
        String msptStr = df.format(mspt);
        if (mspt <= 40) return "§a" + msptStr;
        if (mspt <= 50) return "§e" + msptStr;
        if (mspt <= 100) return "§c" + msptStr;
        return "§4" + msptStr;
    }


    public static double getTPS() {
        double[] tpsArray = Bukkit.getTPS();
        return Math.min(tpsArray[0], 20);
    }

    public static double getMSPT() {
        return isFolia ? Bukkit.getAverageTickTime() : 0.0;
    }

    public static double getRegionTPS(Location location) {
        if (!isFolia) return 0;
        try {
            Method method = Bukkit.class.getMethod("getRegionTPS", Location.class);
            double[] tpsArray = (double[]) method.invoke(null, location);
            return Math.min(tpsArray[0], 20);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}