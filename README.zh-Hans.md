# Cream - Android 上的 Klipper / Kalico

Cream 是一个 Android 端的 Klipper / Kalico 宿主机应用，可以让你在任意一台支持 OTG 的 Android 5.0+ 设备上运行 [Klipper](https://github.com/KevinOConnor/klipper)（或 [Kalico](https://github.com/KalicoCrew/kalico)）宿主机软件（Klippy）。

Cream 是 [Beam Klipper](https://github.com/ProtonKicker/BeamKlipper) 的硬分叉（hardfork），主要变化包括：

- **全量 Kotlin 迁移**：核心应用包（`ru.ytkab0bp.beamklipper`）已从 Java 迁移到 Kotlin（71 个 Kotlin 源文件取代 34 个 Java 文件）。借助空安全、协程、不可变数据类等语言特性，消除了大量只能在运行时发现的崩溃类型，显著提升稳定性。详见 [KOTLIN_MIGRATION.md](KOTLIN_MIGRATION.md)。
- **内置 Kalico 引擎**：捆绑 [Kalico](https://github.com/KalicoCrew/kalico)（Klipper 的社区维护分支），可在设置中一键切换 Klipper / Kalico 固件引擎，无需重新安装。
- **UI 全面升级**：全新的「奶油」主题界面，重新设计的启动/停止按钮、设置面板、实例卡片与 Web 快捷卡片。
- **多语言支持**：内置简体中文、繁体中文、英语与俄语，并支持跟随系统。

# 快速开始

1. 从[这里](https://github.com/utkabobr/klipper/tree/prebuilt-v0.12.0)下载并安装 `firmware.bin`（或从[此仓库](https://github.com/utkabobr/klipper)自行构建以确保版本兼容）
2. 从 [Releases 页面](https://github.com/ProtonKicker/Cream/releases/latest) 安装 APK
3. 允许所有需要的权限
4. 添加打印机实例（列表中没有你的打印机时，选择 generic-***.cfg）
5. 点击启动按钮
6. 访问 Web 服务器地址 `http://IP:8888/`
7. 在 Web 编辑器的「设备」标签页中配置串口（单打印机设置下会自动配置）
8. 大功告成！

> 提示：若要使用 Kalico 引擎，请在「设置 → 引擎与界面」中把固件引擎切换为 Kalico，并使用 Kalico 固件树为你的主控板编译对应固件。

# 安装 Cream 之后，设备还能当普通设备用吗？

**当然可以！**

Cream 不会对 Android 系统做任何改动，它以普通 Android 应用的形式运行在用户空间。

# IP:端口是什么？

任意实例运行时，主页面都会显示该地址。

Web 服务器地址：`http://IP:8888/`

摄像头地址：
- /webcam/?action=stream => `http://IP:8889/`
- /webcam/?action=snapshot => `http://IP:8889/snapshot`

Fluidd 推荐使用 mjpeg-**stream**（非 adaptive mjpeg）摄像头配置，Mainsail 推荐 UV4L-MJPEG。

# 内置了什么？

Cream 内置了：
- [Klipper](https://github.com/KevinOConnor/klipper)
- [Kalico](https://github.com/KalicoCrew/kalico)
- [Moonraker](https://github.com/Arksine/moonraker)
- [Fluidd](https://github.com/fluidd-core/fluidd)
- [Mainsail](https://github.com/mainsail-crew/mainsail)
- [Happy Hare](https://github.com/moggieuk/Happy-Hare)
- [Klipper TMC Autotune](https://github.com/andrewmcgr/klipper_tmc_autotune)
- [Moonraker-timelapse](https://github.com/mainsail-crew/moonraker-timelapse)

# Android 扩展

Cream 提供了一些附加扩展，用于控制内置功能。

### 摄像头

在 printer.cfg 中加入 `[beam_camera]`

`SET_CAMERA_FLASHLIGHT ENABLED=true/false` - 开关闪光灯

`SET_CAMERA_FOCUS AUTOFOCUS=true/false FOCUS_DISTANCE=0...?` - 设置摄像头自动对焦状态；关闭自动对焦时可设置焦距。`FOCUS_DISTANCE` 单位为屈光度，因设备而异。

### 蜂鸣器

在 printer.cfg 中加入 `[include beam_beeper.cfg]`

使用 [文档中定义](https://marlinfw.org/docs/gcode/M300.html) 的 `M300` 宏。

# 自动启动

将需要的打印机设置为自动启动，**并将应用设为默认桌面**，即可实现开机自启。

如果设备已加密（大多数设备默认开启），你**必须**移除锁屏 PIN 码。

# 后台活动说明

部分厂商可能会限制应用的后台进程或性能。
可以将应用设为默认桌面并允许所有后台任务来规避。

# 支持 Android TV 吗？

支持，应该可以正常工作。但请注意，部分廉价电视盒子不支持直接设置 Cream 为桌面，需要先用 ADB 或 root 禁用系统桌面。

# 用哪种 USB 集线器？

作者使用的是绿联（UGREEN）Type-C 集线器（非广告，只是在等绿联来合作:D），只要能同时充电且与你的设备兼容，任何集线器都可以。

# 限制

- Web 服务器无法使用默认端口，因为 Android/Linux 不允许用户空间应用绑定 1024 以下的端口，而默认的 `http://IP` 需要 80 端口
- 同时最多只能运行 4 个实例，因为 Android 要求开发者为每个服务单独声明不同的进程（应该也没人需要更多吧 ¯\\\_(ツ)\_/¯）
- 部分设备在固件重启后会重置设备路径，这种情况下请使用 VID/PID 命名
- 不支持 SSH（也因此无法在设备上编译固件或运行额外的自启服务）
- 部分设备不支持同时 OTG 和充电，这种情况只能直接焊接到电池引脚（或者换一台设备，随你）
- 仅支持 250000 波特率（不想把这个设置转发到 Android USB 驱动，几乎所有配置都用 250000 而已）

# 构建

- 先拉取全部子模块！（使用 `git clone --recursive`，不要以压缩包形式下载）
- 用 Android Studio 导入项目并点击运行

# 贡献

欢迎提交 Pull Request。Cream 已经全面转向 Kotlin，新代码请使用 Kotlin 编写。
