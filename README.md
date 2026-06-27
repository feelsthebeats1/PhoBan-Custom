# PhoBan-Custom

> Plugin phó bản (dungeon/instance) tùy chỉnh cho Minecraft Paper 1.20.4 — hỗ trợ MythicMobs, WorldGuard, PlaceholderAPI.

## Tính năng

- 🏰 **Hệ thống phó bản đa cấp độ**: EASY, MEDIUM, HARD, EXTREME, CHALLENGE
- 📂 **Phân loại danh mục**: Boss, Dungeon, v.v. — mỗi danh mục có GUI riêng
- 🎮 **Tích hợp MythicMobs**: Boss, minion, spawn theo thời gian
- 🌍 **WorldGuard regions**: Tự động quản lý vùng chơi
- 🎟️ **Hệ thống vé**: Vé phó bản, nhận vé miễn phí định kỳ
- 📊 **Thống kê**: Lịch sử chơi, tỉ lệ thắng, thời gian hoàn thành
- 🎨 **Item custom model data**: Hỗ trợ `custom-model-data: 1234` cho resource packs
- 💬 **MiniMessage + legacy color**: Hỗ trợ cả `&a` và `<gradient>` tags
- 🖼️ **GUI tùy chỉnh**: room-selector.yml, category-selector.yml, difficulty-selector.yml

## Yêu cầu

- Paper **1.20.4** (hoặc fork tương thích)
- Java **16+**
- **PlaceholderAPI**
- **MythicMobs** (5.3.5+)
- **WorldGuard** (7.0.9+)

## Cài đặt

1. Tải file `.jar` từ [releases](https://github.com/feelsthebeats1/PhoBan-Custom/releases)
2. Đặt vào thư mục `plugins/`
3. Khởi động server — plugin sẽ tạo cấu hình mẫu
4. Chỉnh sửa cấu hình trong `plugins/PhoBan/`
5. Dùng `/pb reload` để tải lại

## Lệnh

| Lệnh | Mô tả | Quyền |
|------|-------|-------|
| `/pb` | Mở menu chọn phó bản | `phoban.use` |
| `/pb help` | Xem hướng dẫn | — |
| `/pb profile <player>` | Xem thông tin người chơi | `phoban.profile` |
| `/pb quit` | Rời phó bản hiện tại | — |
| `/pb list` | Danh sách phòng | `phoban.list` |
| `/pb join <room> [player]` | Vào phòng | `phoban.join` |
| `/pb start <room>` | Bắt đầu phòng | `phoban.start` |
| `/pb end <room>` | Kết thúc phòng | `phoban.end` |
| `/pb terminate <room>` | Hủy phòng | `phoban.terminate` |
| `/pb enable <room>` | Bật phòng | `phoban.enable` |
| `/pb disable <room>` | Tắt phòng | `phoban.disable` |
| `/pb reload` | Tải lại cấu hình | `phoban.reload` |
| `/pb reset respawn <room> [player]` | Reset hồi sinh | `phoban.reset.respawn` |
| `/pb reset data <player>` | Xóa dữ liệu người chơi | `phoban.reset.data` |
| `/pb tp <room> [player]` | Teleport đến phòng | `phoban.tp` |
| `/pb categories [category]` | Mở danh mục | `phoban.categories` |
| `/pb sound` | Mở sound explorer | `phoban.sound` |
| `/pb ticket add <player> <amount>` | Thêm vé | `phoban.ticket.add` |
| `/pb ticket set <player> <amount>` | Đặt vé | `phoban.ticket.set` |
| `/pb admin create <room> <difficulty> [level]` | Tạo phòng admin | `phoban.admin.create` |
| `/pb admin openCategories <category> <player>` | Mở danh mục cho player | `phoban.admin.openCategories` |
| `/pb getpos` | Lấy tọa độ hiện tại | `phoban.getpos` |

## Cấu hình

### `config.yml` — Cấu hình chính

```yaml
free-ticket:
  enabled: false
  amount: 5
  every: 86400  # giây

room-settings:
  waiting-time: 15     # giây chờ trước khi bắt đầu
  intermission-time: 20  # giây chờ sau khi kết thúc

room-create-cooldown: 240  # giây
spawn-location: world 0 128 0
info-item-cmd: dm open shopphoban {player}
allowed-commands:
  - heal
  - feed
  - phoban
  - pb
  - tell
  - msg
```

### `messages.yml` — Tin nhắn

Sử dụng `&` codes (legacy) và MiniMessage tags (`<gradient>`, `<rainbow>`, v.v.):

```yaml
prefix: "&e[PhoBan] &f"
difficulty:
  easy: "&aDễ"
  hard: "&cKhó"
  challenge: "&4Thách Đấu"
win-message:
  - ""
  - "  &a&lVƯỢT THÀNH CÔNG:&f&l Phó bản {dungeon}"
```

### `gui/room-selector.yml` — GUI chọn phòng

Có thể override theo từng category:

- **Base**: `gui/room-selector.yml`
- **Override boss**: `gui/room-selector-boss.yml`
- **Override dungeon**: `gui/room-selector-dungeon.yml`

### `gui/category-selector.yml` — GUI danh mục

```yaml
categories:
  boss:
    material: egg
    custom-model-data: 1234  # custom model data
    name: "&c&lBOSS"
    lore:
      - ""
      - "&7Phó bản tiêu diệt Boss"
```

### Cấu hình phòng (`rooms/room-1.yml`)

```yaml
enabled: false
name: "&aGod Skeleton"
category: "boss"
icon: bone
custom-model-data: 1234  # tuỳ chọn
permission: "phoban.room.1"
display-order: 0
description:
  - "&7God Skeleton là con boss cực mạnh."
queue-location: world 0 128 0
spawn-location: world 0 128 0
region: "room1"

levels:
  easy:
    ticket-cost: 1
    playing-time: 1800
    min-players: 1
    max-players: 5
    respawn-time: 10
    respawn-chances: 3
    objectives:
      "SkeletonKing": 1
    win-rewards:
      - eco give {player} 10000
    mobs:
      - zombie @amount=3 @every=10 @delay=3
      - SkeletonKing world 0 128 0 @delay=10
```

### Custom Model Data

Hỗ trợ `custom-model-data` trong:

- `rooms/<file>.yml` → `icon:` + `custom-model-data:`
- `gui/category-selector.yml` → `material:` + `custom-model-data:`
- `gui/room-selector.yml` → component `material:` + `custom-model-data:`

## Phát triển

### Build

```bash
mvn clean package
```

Output: `target/PhoBan-<version>-Custom.jar`

### Cấu trúc project

```
src/main/java/dev/anhcraft/phoban/
├── cmd/          # Lệnh (CommandExecutor + TabCompleter)
├── config/       # Cấu hình POJO (RoomConfig, MainConfig, ...)
├── game/         # Logic game (GameManager, Room, Difficulty, ...)
├── gui/          # GUI handlers + config models
├── integration/  # PlaceholderAPI bridge
├── listener/     # Bukkit events
├── storage/      # Player data
├── tasks/        # BukkitRunnable tasks
└── util/         # Utilities (MiniMessageUtil, MaterialData, ConfigMerger, ...)
```

## License

Private project — all rights reserved.
