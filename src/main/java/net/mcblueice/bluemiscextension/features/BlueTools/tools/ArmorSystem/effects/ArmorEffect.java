package net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.effects;

import org.bukkit.entity.Player;

public interface ArmorEffect {
    
    /**
     * 獲取效果的唯一標識符 (例如 "flight", "night_vision")
     * 用於 PDC 綁定和狀態追蹤
     */
    String getId();

    /**
     * 計算此效果每秒消耗的能量
     * @param player 玩家
     * @return 消耗量
     */
    int calculateCost(Player player, float value);

    /**
     * 應用效果
     * @param player 玩家
     * @param value 效果強度/數值 (對於無需數值的效果可忽略)
     */
    void apply(Player player, float value);

    /**
     * 移除效果 (當能量不足或脫下裝備時)
     * @param player 玩家
     */
    void remove(Player player);
}