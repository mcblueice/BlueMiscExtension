package net.mcblueice.bluemiscextension;

import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

public class BlueMiscExtension extends JavaPlugin {
    private static BlueMiscExtension instance;
    private Logger logger;
    private boolean enableremoveDamageheart;
    private NoDamageHeart noDamageHeart;

    public BlueMiscExtension() {
    }

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        saveDefaultConfig();
        refreshFeatures();

        getCommand("bluemiscextension").setExecutor(new Commands(this));

        logger.info("BlueMiscExtension 已啟動");
    }

    @Override
    public void onDisable() {
        logger.info("BlueMiscExtension 已卸載");
        if (noDamageHeart != null) {
            noDamageHeart.unregister();
        }
    }

    public static BlueMiscExtension getInstance() {
        return instance;
    }

    public boolean isSafariNetEnabled() {
        return enableremoveDamageheart;
    }

    public void refreshFeatures() {
        if (noDamageHeart != null) {
            noDamageHeart.unregister();
            noDamageHeart = null;
        }

        enableremoveDamageheart = getConfig().getBoolean("removeDamageheart.enable", true);

        if (enableremoveDamageheart) {
            if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
                getServer().getConsoleSender().sendMessage("§r[BlueMiscExtension] §aProtocolLib 已啟用 已開啟 移除受傷愛心 功能！");
                noDamageHeart = new NoDamageHeart(this);
                noDamageHeart.register();
            } else {
                getServer().getConsoleSender().sendMessage("§r[BlueMiscExtension] §cProtocolLib 未啟用 已關閉 移除受傷愛心 功能！");
            }
        } else {
            getServer().getConsoleSender().sendMessage("§r[BlueResExtension] §c移除受傷愛心 功能已關閉");
        }
    }
}
