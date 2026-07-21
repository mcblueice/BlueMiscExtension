package net.mcblueice.bluemiscextension.features.AbsorptionScale;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import com.github.retrooper.packetevents.PacketEvents;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.Feature;
import net.mcblueice.bluemiscextension.features.FeatureManager;
import net.mcblueice.bluemiscextension.features.AbsorptionScale.Listener.Packets.AbsorptionMetadataListener;

public class AbsorptionScale implements Listener, Feature {
    private final BlueMiscExtension plugin;
    private AbsorptionMetadataListener packetListener;

    private final Map<UUID, Float> peakAbsorption = new ConcurrentHashMap<>();
    private final Map<UUID, Float> lastDisplayed = new ConcurrentHashMap<>();

    public AbsorptionScale(BlueMiscExtension plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
        this.packetListener = new AbsorptionMetadataListener(plugin, this);
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);
    }

    @Override
    public void unregister() {
        if (packetListener != null) PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
    }

    public static Float getMaxAbsorption(UUID uuid) {
        FeatureManager featureManager = BlueMiscExtension.getInstance().getFeatureManager();
        AbsorptionScale absorptionScale = featureManager.getFeature(AbsorptionScale.class);
        if (absorptionScale == null) return 0F;
        Float max = absorptionScale.peakAbsorption.get(uuid);
        return max != null ? max : 0F;
    }
    public static Float getAbsorption(UUID uuid) {
        BlueMiscExtension plugin = BlueMiscExtension.getInstance();
        Player player = plugin.getServer().getPlayer(uuid);
        if (player == null) return 0F;
        return (float) player.getAbsorptionAmount();
    }

    public Map<UUID, Float> getPeakAbsorption() {
        return peakAbsorption;
    }
    public Map<UUID, Float> getLastDisplayed() {
        return lastDisplayed;
    }
}
