package net.mcblueice.bluemiscextension.features.DamageIndicatorLimiter;

import org.bukkit.Particle;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedParticle;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.Feature;


public class DamageIndicatorLimiter implements Feature {
	private final BlueMiscExtension plugin;

	public DamageIndicatorLimiter(BlueMiscExtension plugin) {
		this.plugin = plugin;
	}

    @Override
    public void register() {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL,PacketType.Play.Server.WORLD_PARTICLES) {
            int maxParticles = plugin.getConfig().getInt("Features.DamageIndicatorLimiter.max_amount", 20);

            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket().deepClone();
                WrappedParticle<?> wrappedParticle = packet.getNewParticles().readSafely(0);
                Particle particle = wrappedParticle.getParticle();
                int count = packet.getIntegers().readSafely(0);
                if (particle == Particle.DAMAGE_INDICATOR && count > maxParticles) {
                    packet.getIntegers().writeSafely(0, maxParticles);
                    event.setPacket(packet);
                    DamageIndicatorLimiter.this.plugin.sendDebug("§e已將 §b" + event.getPlayer().getName() + " §e的 §6" + particle.name() + " §e粒子數量由 §6" + count + " §e調整為 §6" + maxParticles);
                }
            }
        });
    }
    
    @Override
    public void unregister() { ProtocolLibrary.getProtocolManager().removePacketListeners(plugin); }
}