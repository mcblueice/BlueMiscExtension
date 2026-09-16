# BlueMiscExtension

一個藍冰伺服器用於擴展雜項功能的插件

## 功能特色

- 減少生物受擊時噴出的粒子效果 (DamageIndicatorLimiter)
- 縮放吸收生命值 (AbsorptionScale)
- 手持／背包 Shift 右鍵開啟界伏盒 (ShulkerBox)
- 開啟虛擬工作台 (VirtualWorkbench)
- 生存模式放置／破壞／調整光源方塊 (LightBlock)
- 電梯功能 (Elevator)
- 盔甲隱身功能 (ArmorHide)
- 基岩版玩家虛擬鞘翅顯示 (BedrockGlideElytra)
- 物品署名與保護 (ItemSignature)
- 玩家暱稱 (PlayerNick)
- 鞘翅合併胸甲 (ElytraArmor): 在鐵砧上將鞘翅合入胸甲取得滑翔效果
- 穿透點擊 (ClickThrough): 懸浮文字等介面允許穿透點擊 並自動略過 QuickShop-Hikari 商店告示牌
- 開服地獄門實體冷卻 (PortalLoaderBreaker): 延遲非玩家實體進入傳送門 並可用指令手動開啟封鎖
- 夜魅生成限制 (PhantomSpawnLimiter): 可開關玩家夜魅生成
- 物品更新 (ItemUpdater): 玩家資料與 HuskSync 同步完成後統一刷新物品欄 並開放 `PlayerInventoryUpdateEvent` 供其他插件調整物品
- PlaceholderAPI 支援

## 指令列表

### 主指令 `/bluemiscextension` (簡寫 `/bme`)

| 指令 | 用途 | 權限節點 |
|------|------|------|
| `/bme reload` | 重新載入 `config.yml` 與 `lang.yml` | `bluemiscextension.reload` |
| `/bme debug` | 切換偵錯訊息顯示 (玩家或主控台) | `bluemiscextension.debug` |
| `/bme status` | 查看伺服器效能與資料庫連線狀態 | `bluemiscextension.status` |
| `/bme info` | 查看插件版本與功能清單 | `bluemiscextension.info` |
| `/bme unlockdata <player>` | 強制解鎖玩家資料 | `bluemiscextension.unlockdata` |
| `/bme armorhide [player]` | 切換盔甲隱身狀態 | `bluemiscextension.armorhide` |
| `/bme workbench <type> [player]` | 開啟虛擬工作台 | `bluemiscextension.workbench` (子權限見下方) |
| `/bme nick <暱稱> [player]` | 設定玩家暱稱，`-clear` 清除 | `bluemiscextension.nick` |
| `/bme sign` | 對主手物品署名／解除署名 | `bluemiscextension.itemsign` |
| `/bme phantomspawn <player>` | 切換玩家夜魅生成開關 | `bluemiscextension.phantomspawn` |
| `/bme updateinv [player]` | 立即觸發物品欄更新 (未指定玩家時對所有在線玩家觸發) | `bluemiscextension.updateinv` |
| `/bme portalbreak [秒數]` | 手動阻止非玩家實體進入傳送門 (未指定秒數時使用 `Features.PortalLoaderBreaker.DelayTime`) | `bluemiscextension.portalbreak` |

### workbench 類型與權限

| Type | 用途 | 權限節點 |
|------|------|---------|
| WORKBENCH | 工作台 | `bluemiscextension.workbench.workbench` |
| ANVIL | 鐵砧 | `bluemiscextension.workbench.anvil` |
| GRINDSTONE | 砂輪 | `bluemiscextension.workbench.grindstone` |
| SMITHING | 鍛造台 | `bluemiscextension.workbench.smithing` |
| CARTOGRAPHY | 製圖台 | `bluemiscextension.workbench.cartography` |
| LOOM | 紡織機 | `bluemiscextension.workbench.loom` |
| ENDERCHEST | 終界箱 | `bluemiscextension.workbench.enderchest` |

### 暱稱權限

| 權限節點 | 用途 |
|------|------|
| `bluemiscextension.nick.basiccolor` | 允許基礎色碼 (`&1`～`&f`) |
| `bluemiscextension.nick.hexcolor` | 允許 HEX 與漸層色碼 (`{#FF0000}`、`<gradient>` 等) |
| `bluemiscextension.nick.other` | 允許修改他人暱稱 |

### 物品署名權限

| 權限節點 | 用途 |
|------|------|
| `bluemiscextension.itemsign.override` | 允許覆蓋／解除他人署名 |

## PlaceholderAPI 變量

| 變量 | 用途 |
|------|------|
| `%bluemiscextension_armorhidden%` | 顯示玩家是否隱藏盔甲 |
| `%bluemiscextension_phantomspawn%` | 顯示玩家是否允許生成夜魅 |
| `%bluemiscextension_ip%` | 顯示玩家 IP 地址 |
| `%bluemiscextension_hostname%` | 顯示玩家連線的主機名稱 |
| `%bluemiscextension_absorption%` | 顯示玩家目前吸收血量 |
| `%bluemiscextension_maxabsorption%` | 顯示玩家目前縮放的吸收上限血量 |
| `%bluemiscextension_hasnickname%` | 顯示玩家是否有暱稱 |
| `%bluemiscextension_nickname%` | 顯示玩家暱稱 |
| `%bluemiscextension_displayname%` | 顯示玩家顯示名稱 |
| `%bluemiscextension_tps%` | 顯示玩家所在區域 TPS |
| `%bluemiscextension_minimessage_<內容>%` | 將內容轉換為保留顏色的純文字 |

## 物品更新事件 (API)

`ItemUpdater` 功能會在玩家資料載入完成後(若伺服器安裝 HuskSync 則會等待同步完成)拋出 `PlayerInventoryUpdateEvent` 監聽者可直接修改 `event.getInventory()` 內的物品 (與玩家實際物品欄為同一參照 修改後自動生效):

```java
@EventHandler(priority = EventPriority.HIGH)
public void onInventoryUpdate(PlayerInventoryUpdateEvent event) {
    event.getInventory().forEach(item -> {
        // 修改物品名稱、lore 或 PDC
    });
}
```

也可呼叫 `PlayerInventoryUpdateEvent.call(player)` 手動觸發一次更新 (等效於 `/bme updateinv <player>`)

## 軟依賴

| 插件 | 用途 |
|------|------|
| PacketEvents | 封包處理 (吸收縮放、裝備隱形、基岩版虛擬鞘翅、受擊粒子限制) |
| PlaceholderAPI | 提供佔位符 |
| Floodgate | 基岩版玩家判斷 |
| HuskSync | 玩家資料同步完成後才觸發物品更新事件 |

## 授權 License

本專案採用 MIT License

## TODO

- 新增告示牌顏色功能
- 新增盔甲架修改功能