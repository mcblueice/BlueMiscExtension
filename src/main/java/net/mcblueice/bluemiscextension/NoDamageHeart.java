package net.mcblueice.bluemiscextension;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;


public class NoDamageHeart {
    private final JavaPlugin plugin;
    private int maxParticles;
    private boolean debuglog;

    public NoDamageHeart(JavaPlugin plugin) {
        this.plugin = plugin;
        this.maxParticles = plugin.getConfig().getInt("removeDamageheart.max-heart", 20);
        this.debuglog = plugin.getConfig().getBoolean("removeDamageheart.debug-log", false);
    }

    public void register() {
    ProtocolManager manager = ProtocolLibrary.getProtocolManager();
    manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.WORLD_PARTICLES) {
            @Override
            public void onPacketSending(PacketEvent event) {
                try {
                    Object param = event.getPacket().getModifier().read(9);
                    Player player = event.getPlayer();
                    if (param != null) {
                        if (param.getClass().getMethod("a").invoke(param).equals("minecraft:damage_indicator")) {
                            int count = event.getPacket().getIntegers().read(0);
                            int actualCount = (count == 0) ? 1 : count;
                            if (actualCount > maxParticles) {
                                event.getPacket().getIntegers().write(0, maxParticles);
                                if (debuglog) plugin.getServer().getConsoleSender().sendMessage("§r[BlueMiscExtension] §b已將 §a" + player.getName() + " §b的 §edamage_indicator §b粒子數量由 §6" + actualCount + " §b調整為 §6" + maxParticles);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }
    
    public void unregister() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(plugin);
    }
}