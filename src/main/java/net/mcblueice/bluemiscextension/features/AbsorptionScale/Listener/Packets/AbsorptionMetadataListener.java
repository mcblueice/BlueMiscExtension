package net.mcblueice.bluemiscextension.features.AbsorptionScale.Listener.Packets;

import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.AbsorptionScale.AbsorptionScale;
import net.mcblueice.bluelib.utils.ServerUtil;

public class AbsorptionMetadataListener extends PacketListenerAbstract {
    private final BlueMiscExtension plugin;
    private final AbsorptionScale feature;

    private static final int ABSORPTION_INDEX = ServerUtil.isAtLeast(1, 21, 11) ? 17 : 15;
    private final float MAX_ABSORPTION;

    public AbsorptionMetadataListener(BlueMiscExtension plugin, AbsorptionScale feature) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
        this.feature = feature;
        this.MAX_ABSORPTION = (float) plugin.getConfig().getDouble("Features.AbsorptionScale.max_amount", 20D);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) return;

        WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);
        if (!(event.getPlayer() instanceof Player viewer)) return;
        if (wrapper.getEntityId() != viewer.getEntityId()) return;

        List<EntityData<?>> metadataList = wrapper.getEntityMetadata();
        if (metadataList == null || metadataList.isEmpty()) return;

        EntityData<Float> absorptionData = getAbsorptionData(metadataList);
        if (absorptionData == null) return;

        float realAbsorption = absorptionData.getValue();
        boolean debug = plugin.getConfig().getBoolean("Features.AbsorptionScale.debug", false);
        UUID uuid = viewer.getUniqueId();

        if (realAbsorption <= 0F) {
            if (debug) plugin.sendDebug("§e已重置 §b" + viewer.getName() + " §e的吸收血量縮放資料");
            feature.getPeakAbsorption().remove(uuid);
            feature.getLastDisplayed().remove(uuid);
            return;
        }

        float peak = feature.getPeakAbsorption().compute(uuid, (k, currentPeak) -> (currentPeak == null || realAbsorption > currentPeak) ? realAbsorption : currentPeak);
        if (debug && realAbsorption >= peak) plugin.sendDebug("§e更新 §b" + viewer.getName() + " §e最高吸收血量至 §6" + peak);

        float displayValue = realAbsorption;
        if (peak > MAX_ABSORPTION) displayValue = realAbsorption * (MAX_ABSORPTION / peak);

        Float prevDisplayed = feature.getLastDisplayed().get(uuid);
        if (prevDisplayed != null && Math.abs(prevDisplayed - displayValue) < 1.0f) displayValue = prevDisplayed;
        feature.getLastDisplayed().put(uuid, displayValue);
        absorptionData.setValue(displayValue);

        if (debug) plugin.sendDebug("§e更新 §b" + viewer.getName() + " §e吸收血量至 §6" + displayValue + "§7(§6" + realAbsorption + "/" + peak + "§7)");
    }

    @SuppressWarnings("unchecked")
    private EntityData<Float> getAbsorptionData(List<EntityData<?>> metadataList) {
        for (EntityData<?> data : metadataList) {
            if (data.getIndex() == ABSORPTION_INDEX && data.getType() == EntityDataTypes.FLOAT) return (EntityData<Float>) data;
        }
        return null;
    }
}