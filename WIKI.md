# PhoBan-Custom Wiki

## Mục lục

1. [Hướng dẫn cài đặt](#hướng-dẫn-cài-đặt)
2. [Cấu hình](#cấu-hình)
   - [config.yml](#configyml)
   - [messages.yml](#messagesyml)
   - [Room Config](#room-config)
3. [GUI System](#gui-system)
   - [Category Selector](#category-selector)
   - [Room Selector](#room-selector)
   - [Difficulty Selector](#difficulty-selector)
   - [Per-category Room Selector](#per-category-room-selector)
4. [Custom Model Data](#custom-model-data)
5. [Color Codes](#color-codes)
6. [Permissions](#permissions)
7. [Commands](#commands)
8. [Room Mechanics](#room-mechanics)
9. [Troubleshooting](#troubleshooting)

---

## Hướng dẫn cài đặt

### Bước 1: Cài đặt dependencies

Server cần các plugin sau:

| Plugin | Version | Ghi chú |
|--------|---------|---------|
| Paper | 1.20.4 | Hoặc fork tương thích (Purpur, Pufferfish) |
| PlaceholderAPI | 2.11+ | Bắt buộc |
| MythicMobs | 5.3.5+ | Bắt buộc (dùng để spawn boss/mob) |
| WorldGuard | 7.0.9+ | Bắt buộc (quản lý vùng chơi) |

### Bước 2: Cài plugin

1. Tải file `.jar` PhoBan
2. Đặt vào `plugins/`
3. Khởi động server
4. Plugin sẽ tạo thư mục `plugins/PhoBan/` với cấu hình mẫu

### Bước 3: Thiết lập phòng đầu tiên

1. Đứng tại vị trí spawn của phòng, dùng `/pb getpos` để lấy tọa độ
2. Chỉnh sửa `plugins/PhoBan/rooms/room-1.yml`:
   - Set `enabled: true`
   - Sửa `queue-location`, `spawn-location` theo tọa độ
   - Đặt region WorldGuard
   - Cấu hình mobs, rewards
3. Dùng `/pb reload` để tải lại
4. Vào game dùng `/pb` để xem phòng

### Bước 4: Tạo region WorldGuard

```bash
/rg create room1
/rg define room1
# Mở rộng region bao phủ map phòng
```

---

## Cấu hình

### `config.yml`

```yaml
# Tặng vé miễn phí
free-ticket:
  enabled: false
  amount: 5
  every: 86400  # giây (86400 = 1 ngày)

# Thiết lập chung phòng
room-settings:
  waiting-time: 15     # giây chờ trước khi bắt đầu
  intermission-time: 20  # giây chờ sau khi kết thúc

# Giới hạn tạo phòng
room-create-cooldown: 240  # giây

# Spawn mặc định
spawn-location: world 0 128 0

# Lệnh khi nhấn nút "Thông tin" trong GUI
info-item-cmd: dm open shopphoban {player}

# Các lệnh được phép dùng trong phòng
allowed-commands:
  - heal
  - feed
  - phoban
  - pb
  - tell
  - msg

# Debug level (0-2)
debug-level: 0
```

### `messages.yml`

Tất cả messages hỗ trợ **cả** `&` codes và MiniMessage tags.

```yaml
prefix: "&e[PhoBan] &f"

stage:
  available: "&aPhòng trống"
  waiting: "&eĐang chờ"
  playing: "&bĐang chơi"
  ending: "&6Sắp kết thúc"

difficulty:
  easy: "&aDễ"
  medium: "&eTrung Bình"
  hard: "&cKhó"
  extreme: "&5Vô Cực"
  challenge: "&4Thách Đấu"

# Các messages khác...
win-message:
  - ""
  - "  &a&lVƯỢT THÀNH CÔNG:&f&l Phó bản {dungeon}"
  - "  &eĐộ khó: {difficulty} {challengeLevel}"
  - "  &eThời gian hoàn thành: &f{completeTime}"
  - ""

command-blocked: "&cBạn không thể dùng lệnh này trong phó bản."
full-inventory-warning: "&cHành trang đã đầy, quà rơi xuống đất!"
```

### Room Config

Mỗi file `.yml` trong `rooms/` là một phòng. Tên file = ID phòng.

#### Cấu trúc cơ bản

```yaml
enabled: false
name: "&aGod Skeleton"
category: "boss"                    # Danh mục (khớp với category-selector.yml)
icon: bone                          # Material
custom-model-data: 1234            # Custom model data (tuỳ chọn)
permission: "phoban.room.1"         # Quyền truy cập (tuỳ chọn)
display-order: 0                    # Thứ tự hiển thị
description:
  - "&7Mô tả phòng"

# Vị trí
queue-location: world 0 128 0       # Khu vực chờ
spawn-location: world 0 128 0       # Khu vực chơi
end-teleport-location: world 0 128 0  # Điểm tele sau khi kết thúc (tuỳ chọn)
region: "room1"                     # WorldGuard region

# Yêu cầu (tuỳ chọn)
#requirement: "room-1:hard"         # Cần vượt room-1 độ hard+

# Khoá thời tiết, thời gian (tuỳ chọn)
#weather-lock: downfall
#time-lock: 13000
```

#### Levels (cấp độ)

```yaml
levels:
  easy:
    ticket-cost: 1
    playing-time: 1800
    min-players: 1
    max-players: 5
    respawn-time: 10
    respawn-chances: 3
    
    # Mục tiêu (MythicMob: số lượng)
    objectives:
      "SkeletonKing": 1
    
    # Hoặc shorthand cho 1 boss
    #boss-id: "SkeletonKing"
    
    # Mob spawn
    mobs:
      - zombie @amount=3 @every=10 @delay=3
      - SkeletonKing world 0 128 0 @delay=10
    
    # Sound
    sounds:
      - ambient.basalt_deltas.loop @every=200 @delay=20
    
    # Rewards (console commands)
    start-rewards:
      - eco give {player} 5000
    win-rewards:
      - eco give {player} 10000
    first-win-rewards:
      - eco give {player} 20000
    boss-kill-rewards:
      - eco give {player} 20000
```

#### Mob Spawn Syntax

```
# Tại mỗi người chơi:
<entity> [@option=value ...]

# Tại vị trí cố định:
<entity> <world> <x> <y> <z> [@option=value ...]

Options:
  @amount=<số>        Số lượng spawn 1 đợt (mặc định: 1)
  @delay=<số>         Delay trước khi spawn lần đầu (giây)
  @every=<số>         Lặp lại mỗi N giây (0 = chỉ 1 lần)
  @times=<số>         Số lần spawn tối đa (0 = không giới hạn)

Vanilla options:
  @health=<số>        @followRange=<số>
  @attackDamage=<số>  @movementSpeed=<số>
  @armor=<số>         @silent  @glowing
```

Ví dụ:
```yaml
mobs:
  - zombie @amount=3 @every=10 @delay=3
  - SkeletonKing:2 world 0 128 0 @delay=10 @times=3 @every=30
```

---

## GUI System

### Category Selector

File: `gui/category-selector.yml`

```yaml
categories:
  boss:
    material: egg
    custom-model-data: 1234
    name: "&c&lBOSS"
    lore:
      - ""
      - "&7Phó bản tiêu diệt Boss"
      - "&e> Nhấn để chọn"
  dungeon:
    material: stone_bricks
    name: "&8&lDUNGEON"
```

### Room Selector

File: `gui/room-selector.yml`

Components:
- `-`: Filler item
- `x`: Room slot (tự động điền)
- `b`: Back button (ẩn khi ở mục chính)
- `I`: Info button

### Per-category Room Selector

Tạo file `gui/room-selector-<category>.yml` để override giao diện riêng cho từng danh mục:

```yaml
# gui/room-selector-boss.yml
title: "&0&lBoss Raid"
layout:
  - "---------"
  - "--xxxxx--"
  - "--xxxxx--"
  - "b-------I"
components:
  "b":
    type: back
    material: nether_star
    name: "&eQuay về danh mục"
```

Chỉ cần ghi các section muốn override — các section không ghi sẽ kế thừa từ `room-selector.yml`.

### Difficulty Selector

File: `gui/difficulty-selector.yml`

Hiển thị các cấp độ khó của phòng. Tự động khoá các độ khó chưa mở.

---

## Custom Model Data

Hỗ trợ field `custom-model-data` trong mọi item config.

**Room icon:**
```yaml
icon: bone
custom-model-data: 1234
```

**Category item:**
```yaml
boss:
  material: egg
  custom-model-data: 5678
```

**GUI component:**
```yaml
"x":
  material: black_stained_glass_pane
  custom-model-data: 9012
```

ItemBuilder tự động apply custom model data khi build ItemStack.

---

## Color Codes

Plugin hỗ trợ **3 format** màu sắc:

### 1. Legacy `&` codes

```yaml
"&aXanh lá"      "&cĐỏ"       "&eVàng"
"&lĐậm"          "&oNghiêng"  "&nGạch chân"
```

### 2. Hex color

```yaml
"&#ff0000Text đỏ"    "&#00ff00Text xanh"
```

### 3. MiniMessage tags

```yaml
"<red>Đỏ"                       "<gradient:red:blue>Gradient"
"<bold>Đậm"                     "<underlined>Gạch chân"
"<click:run_cmd:/help>Click</click>"
"<hover:show_text:'Hover text'>Hover</hover>"
```

Tất cả messages trong `messages.yml` đều hỗ trợ cả 3 format.

---

## Permissions

### Admin commands

| Permission | Mô tả |
|------------|-------|
| `phoban.admin` | Toàn quyền (bao gồm bypass tất cả) |
| `phoban.use` | Dùng lệnh `/pb` cơ bản |
| `phoban.reload` | Reload cấu hình |
| `phoban.list` | Xem danh sách phòng |
| `phoban.enable` | Bật phòng |
| `phoban.disable` | Tắt phòng |
| `phoban.join` | Force join phòng |
| `phoban.start` | Bắt đầu phòng |
| `phoban.end` | Kết thúc phòng |
| `phoban.terminate` | Hủy phòng |
| `phoban.tp` | Teleport đến phòng |
| `phoban.profile` | Xem profile |
| `phoban.categories` | Mở danh mục |
| `phoban.sound` | Mở sound explorer |
| `phoban.getpos` | Lấy tọa độ |
| `phoban.reset.respawn` | Reset hồi sinh |
| `phoban.reset.data` | Xóa data player |
| `phoban.ticket.add` | Thêm vé |
| `phoban.ticket.set` | Đặt vé |
| `phoban.admin.create` | Tạo phòng admin |
| `phoban.admin.openCategories` | Mở danh mục cho player |

### Room permissions

Đặt trong room config:
```yaml
permission: "phoban.room.1"
```

### Placeholders

| Placeholder | Mô tả |
|-------------|-------|
| `%phoban_tickets%` | Số vé hiện tại |
| `%phoban_total_matches%` | Tổng số trận |
| `%phoban_total_wins%` | Tổng số thắng |
| `%phoban_total_losses%` | Tổng số thua |

---

## Room Mechanics

### Flow

```
Player chọn phòng → Chọn độ khó → Phòng tạo (WAITING)
    → Đủ người / Hết giờ chờ → BẮT ĐẦU (PLAYING)
    → Hoàn thành mục tiêu / Hết giờ → TRAO THƯỞNG (ENDING)
    → Kết thúc
```

### Respawn System

- Người chơi có số lần hồi sinh giới hạn (`respawn-chances`)
- Thời gian hồi sinh (`respawn-time`)
- Hết lượt → chỉ có thể xem (spectator)

### Reward System

| Reward | Thời điểm | Placeholders |
|--------|-----------|-------------|
| `start-rewards` | Khi game bắt đầu | `{player}` |
| `win-rewards` | Khi thắng | `{player}` |
| `first-win-rewards` | Thắng lần đầu ở độ khó này | `{player}` |
| `boss-kill-rewards` | Người last-hit boss | `{player}`, `{boss}`, `{killer}` |

Rewards là console commands, có thể dùng:
```yaml
win-rewards:
  - eco give {player} 10000
  - lp user {player} permission add some.perm
  - minecraft:give {player} diamond 1
```

---

## Troubleshooting

### Phòng không hiện trong GUI

1. Kiểm tra `enabled: true` trong room config
2. Kiểm tra `permission` — player có permission không?
3. Kiểm tra WorldGuard region đã được tạo
4. Dùng `/pb reload`

### Lỗi "Room not found"

Kiểm tra tên file trong `rooms/` — tên file = ID phòng.

### MythicMob không spawn

1. Kiểm tra ID MythicMob đúng
2. Kiểm tra region WorldGuard
3. Kiểm tra log lỗi khi start phòng

### Custom model data không hiện

1. Đảm bảo resource pack đã cài đúng
2. Kiểm tra `custom-model-data` viết đúng (train-case)
3. Thử với `custom-model-data: 1` trước

### MiniMessage không hoạt động

1. Kiểm tra cú pháp MM đúng: `<tag>nội dung</tag>`
2. Có thể dùng `&` codes fallback — MM parse lỗi sẽ tự động fallback
3. Xem MiniMessage docs: https://docs.advntr.dev/minimessage/format.html
