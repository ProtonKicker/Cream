# Kream - Android 上的 Klipper / Kalico

Kream 是一個 Android 端的 Klipper / Kalico 主機應用程式，可以讓你在任何支援 OTG 的 Android 5.0+ 裝置上執行 [Klipper](https://github.com/KevinOConnor/klipper)（或 [Kalico](https://github.com/KalicoCrew/kalico)）主機軟體（Klippy）。

Kream 是 [Cream](https://github.com/ProtonKicker/Cream) 的繼任者，而 Cream 則是 [Beam Klipper](https://github.com/ProtonKicker/BeamKlipper) 的硬分叉。Beam Klipper 最初由 [ProtonKicker](https://github.com/ProtonKicker) 創建。主要變化包括：

- **全面 Kotlin 遷移**：核心應用程式套件（`ru.ytkab0bp.beamklipper`）已從 Java 遷移到 Kotlin（71 個 Kotlin 原始檔取代 34 個 Java 檔案）。藉助空安全、協程、不可變資料類別等語言特性，消除了大量只能在執行時發現的當機類型，大幅提升穩定性。詳見 [KOTLIN_MIGRATION.md](KOTLIN_MIGRATION.md)。
- **內建 Kalico 引擎**：捆綁 [Kalico](https://github.com/KalicoCrew/kalico)（Klipper 的社群維護分支），可在設定中一鍵切換 Klipper / Kalico 韌體引擎，無需重新安裝。
- **UI 全面升級**：全新的「奶油」主題介面，重新設計的啟動/停止按鈕、設定面板、執行個體卡片與 Web 快捷卡片。
- **多語言支援**：內建簡體中文、繁體中文、英語與俄語，並支援跟隨系統。

# 快速入門

1. 從[這裡](https://github.com/utkabobr/klipper/tree/prebuilt-v0.12.0)下載並安裝 `firmware.bin`（或從[此儲存庫](https://github.com/utkabobr/klipper)自行建置以確保版本相容）
2. 從 [Releases 頁面](https://github.com/ProtonKicker/Kream/releases/latest) 安裝 APK
3. 允許所有需要的權限
4. 新增印表機執行個體（清單中沒有你的印表機時，選擇 generic-***.cfg）
5. 點擊啟動按鈕
6. 存取 Web 伺服器位址 `http://IP:8888/`
7. 在 Web 編輯器的「裝置」分頁中設定序列埠（單一印表機設定下會自動設定）
8. 大功告成！

> 提示：若要使用 Kalico 引擎，請在「設定 → 引擎與介面」中把韌體引擎切換為 Kalico，並使用 Kalico 韌體樹為你的主控板編譯對應韌體。

# 安裝 Kream 之後，裝置還能當一般裝置用嗎？

**當然可以！**

Kream 不會對 Android 系統做任何更動，它以一般 Android 應用程式的形式執行在使用者空間。

# IP:連接埠是什麼？

任何執行個體執行時，主頁面都會顯示該位址。

Web 伺服器位址：`http://IP:8888/`

相機位址：
- /webcam/?action=stream => `http://IP:8889/`
- /webcam/?action=snapshot => `http://IP:8889/snapshot`

Fluidd 建議使用 mjpeg-**stream**（非 adaptive mjpeg）相機設定，Mainsail 建議使用 UV4L-MJPEG。

# 內建了什麼？

Kream 內建了：
- [Klipper](https://github.com/KevinOConnor/klipper)
- [Kalico](https://github.com/KalicoCrew/kalico)
- [Moonraker](https://github.com/Arksine/moonraker)
- [Fluidd](https://github.com/fluidd-core/fluidd)
- [Mainsail](https://github.com/mainsail-crew/mainsail)
- [Happy Hare](https://github.com/moggieuk/Happy-Hare)
- [Klipper TMC Autotune](https://github.com/andrewmcgr/klipper_tmc_autotune)
- [Moonraker-timelapse](https://github.com/mainsail-crew/moonraker-timelapse)

# Android 擴充功能

Kream 提供了一些附加擴充功能，用於控制內建功能。

### 相機

在 printer.cfg 中加入 `[beam_camera]`

`SET_CAMERA_FLASHLIGHT ENABLED=true/false` - 開關閃光燈

`SET_CAMERA_FOCUS AUTOFOCUS=true/false FOCUS_DISTANCE=0...?` - 設定相機自動對焦狀態；關閉自動對焦時可設定焦距。`FOCUS_DISTANCE` 單位為屈光度，因裝置而異。

### 蜂鳴器

在 printer.cfg 中加入 `[include beam_beeper.cfg]`

使用 [文件中定義](https://marlinfw.org/docs/gcode/M300.html) 的 `M300` 巨集。

# 自動啟動

將需要的印表機設定為自動啟動，**並將應用程式設為預設桌面**，即可實現開機自啟。

如果裝置已加密（大多數裝置預設開啟），你**必須**移除鎖定畫面 PIN 碼。

# 背景活動說明

部分廠商可能會限制應用程式的背景程序或效能。
可以將應用程式設為預設桌面並允許所有背景工作來規避。

# 支援 Android TV 嗎？

支援，應該可以正常運作。但請注意，部分廉價電視盒不支援直接將 Kream 設定為桌面，需要先用 ADB 或 root 停用系統桌面。

# 用哪種 USB 集線器？

作者使用的是綠聯（UGREEN）Type-C 集線器（非廣告，只是在等綠聯來合作:D），只要能同時充電且與你的裝置相容，任何集線器都可以。

# 限制

- Web 伺服器無法使用預設連接埠，因為 Android/Linux 不允許使用者空間應用程式繫結 1024 以下的連接埠，而預設的 `http://IP` 需要 80 連接埠
- 同時最多只能執行 4 個執行個體，因為 Android 要求開發者為每個服務單獨宣告不同的程序（應該也沒人需要更多吧 ¯\\\_(ツ)\_/¯）
- 部分裝置在韌體重新啟動後會重設裝置路徑，這種情況下請使用 VID/PID 命名
- 不支援 SSH（也因此無法在裝置上編譯韌體或執行額外的自啟服務）
- 部分裝置不支援同時 OTG 和充電，這種情況只能直接焊接到電池引腳（或者換一台裝置，隨你）
- 僅支援 250000 鮑率（不想把這個設定轉送到 Android USB 驅動程式，幾乎所有設定都用 250000 而已）

# 建置

- 先拉取全部子模組！（使用 `git clone --recursive`，不要以壓縮檔形式下載）
- 用 Android Studio 匯入專案並點擊執行

# 貢獻

歡迎提交 Pull Request。Kream 已經全面轉向 Kotlin，新程式碼請使用 Kotlin 撰寫。
