package net.mcblueice.bluemiscextension.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.PlayerInventory;

/**
 * 玩家物品欄更新事件
 * 玩家登入或 HuskSync 載入完成後由 BME 拋出 監聽者可修改物品
 * 也可用 {@link #call(Player)} 觸發
 */
public class PlayerInventoryUpdateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PlayerInventory inventory;

    public PlayerInventoryUpdateEvent(Player player) {
        this.player = player;
        this.inventory = player.getInventory();
    }

    /**
     * 需要更新物品欄的玩家
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * 玩家的物品欄 (與實際物品欄同一參照 可直接修改)
     */
    public PlayerInventory getInventory() {
        return inventory;
    }

    /**
     * 手動觸發一次玩家物品欄更新
     *
     * @param player 目標玩家
     */
    public static void call(Player player) {
        if (player == null) return;
        Bukkit.getPluginManager().callEvent(new PlayerInventoryUpdateEvent(player));
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
