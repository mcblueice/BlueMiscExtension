package net.mcblueice.bluemiscextension.features.PlaceholderAPI;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.Feature;
import net.mcblueice.bluemiscextension.features.PlaceholderAPI.Placeholder.BlueMiscPlaceholder;

public class PlaceholderFeature implements Feature {
    private final BlueMiscExtension plugin;
    private BlueMiscPlaceholder placeholderExpansion;

    public PlaceholderFeature(BlueMiscExtension plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
        if (placeholderExpansion == null) {
            placeholderExpansion = new BlueMiscPlaceholder(plugin);
            placeholderExpansion.register();
        }
    }

    @Override
    public void unregister() {
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
    }
}
