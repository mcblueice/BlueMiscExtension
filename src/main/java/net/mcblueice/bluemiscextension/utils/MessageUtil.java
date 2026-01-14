package net.mcblueice.bluemiscextension.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MessageUtil {
    private MessageUtil() {
        throw new IllegalStateException("Utility class should not be instantiated");
    }

    public static String legacyToMiniMessage(String legacyText) {
        String text = legacyText.replace('§', '&');
        Component component = LegacyComponentSerializer.builder()
                .character('&')
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build()
                .deserialize(text);
        return MiniMessage.miniMessage().serialize(component);
    }

    public static String sectionToMiniMessage(String legacyText) {
        Component component = LegacyComponentSerializer.legacySection().deserialize(legacyText);
        return MiniMessage.miniMessage().serialize(component);
    }
}
