package net.mcblueice.bluemiscextension.features.DamageIndicatorLimiter.Listener.Packets;

import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;

import net.mcblueice.bluemiscextension.BlueMiscExtension;

public class DamageIndicatorListener extends PacketListenerAbstract {
    private final BlueMiscExtension plugin;

    private final int MAX_PARTICLES;

    public DamageIndicatorListener(BlueMiscExtension plugin) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
        this.MAX_PARTICLES = plugin.getConfig().getInt("Features.DamageIndicatorLimiter.max_amount", 20);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.PARTICLE) return;

        WrapperPlayServerParticle particle = new WrapperPlayServerParticle(event);
        if (particle.getParticle().getType() != ParticleTypes.DAMAGE_INDICATOR) return;

        int count = particle.getParticleCount();
        if (count > MAX_PARTICLES) {
            particle.setParticleCount(MAX_PARTICLES);
            boolean debug = plugin.getConfig().getBoolean("Features.DamageIndicatorLimiter.debug", false);
            if (debug && event.getPlayer() instanceof Player player) plugin.sendDebug("§e已將 §b" + player.getName() + " §e的 §6DAMAGE_INDICATOR §e粒子數量由 §6" + count + " §e調整為 §6" + MAX_PARTICLES);
        }
    }
}