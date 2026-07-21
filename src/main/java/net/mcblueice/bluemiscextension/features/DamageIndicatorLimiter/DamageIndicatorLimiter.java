package net.mcblueice.bluemiscextension.features.DamageIndicatorLimiter;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.DamageIndicatorLimiter.Listener.Packets.DamageIndicatorListener;
import net.mcblueice.bluemiscextension.features.Feature;

public class DamageIndicatorLimiter implements Feature {
    private final BlueMiscExtension plugin;
    private PacketListenerAbstract packetListener;

    public DamageIndicatorLimiter(BlueMiscExtension plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
        this.packetListener = new DamageIndicatorListener(plugin);
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);
    }

    @Override
    public void unregister() {
        if (packetListener != null) PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
    }
}
