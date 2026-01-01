
# BlueMiscExtension

一個藍冰伺服器用於擴展雜項功能的插件

## 功能特色

- 減少生物受擊時噴出的粒子效果(damage_indicator)
- 縮放吸收生命值(absorption)
- 手持/背包Shift右鍵開啟界伏盒
- 開啟虛擬工作台
- 生存模式放置/破壞/調整光源方塊
- 電梯功能
- 盔甲隱身功能
- PlaceholderAPI 支援

## 指令列表

| 指令 | 用途 | 權限節點 |
|------|------|------|
| `/bluemiscextension reload` | 重新載入 `config.yml` 與 `lang.yml` | `bluemiscextension.reload` |
| `/bluemiscextension debug` | 切換偵錯訊息顯示（玩家或主控台） | `bluemiscextension.debug` |
| `/bluemiscextension status` | 查看伺服器效能與資料庫連線狀態 | `bluemiscextension.status` |
| `/bluemiscextension armorhide` | 切換盔甲隱身狀態 | `bluemiscextension.armorhide` |
| `/bluemiscextension armorhide <player>` | 為其他玩家切換盔甲隱身狀態 | `bluemiscextension.armorhide.other` |
| `/bluemiscextension workbench <type>` | 開啟虛擬工作台 | `bluemiscextension.workbench`（子權限見下方） |
| `/bluemiscextension workbench <type> <player>` | 為其他玩家開啟虛擬工作台 | `bluemiscextension.workbench.other`（子權限見下方） |

### workbench 類型與權限

| Type | 用途（簡短） | 權限節點 |
|------|-------------|---------|
| WORKBENCH | 工作台 | `bluemiscextension.workbench.workbench` |
| ANVIL | 鐵砧 | `bluemiscextension.workbench.anvil` |
| GRINDSTONE | 砂輪 | `bluemiscextension.workbench.grindstone` |
| SMITHING | 鍛造台 | `bluemiscextension.workbench.smithing` |
| CARTOGRAPHY | 製圖台 | `bluemiscextension.workbench.cartography` |
| LOOM | 紡織機 | `bluemiscextension.workbench.loom` |
| ENDERCHEST | 終界箱 | `bluemiscextension.workbench.enderchest` |

## PlaceholderAPI 變量

| 變量 | 用途 |
|------|------|
| `%bluemiscextension_armorhidden%` | 顯示玩家是否隱藏盔甲 |
| `%bluemiscextension_ip%` | 顯示玩家 IP 地址 |
| `%bluemiscextension_hostname%` | 顯示玩家連線的主機名稱 |

## 授權 License

本專案採用 MIT License  

## TODO

新增告示牌顏色功能
新增盔甲架修改功能