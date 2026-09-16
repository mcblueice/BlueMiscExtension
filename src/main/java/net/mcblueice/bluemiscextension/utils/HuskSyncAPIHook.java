package net.mcblueice.bluemiscextension.utils;

import net.william278.husksync.api.HuskSyncAPI;

public class HuskSyncAPIHook {
    private final HuskSyncAPI huskSyncAPI;
    public HuskSyncAPIHook() {
        this.huskSyncAPI = HuskSyncAPI.getInstance();
    }

    public HuskSyncAPI getHuskSyncAPI() {
        return this.huskSyncAPI;
    }
}