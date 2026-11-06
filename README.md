<p align="center"><img src="branding/aurora-root/icon-circle.png" width="112" alt="Xperia Clear All # root 版圖示"></p>

<h1 align="center">Xperia Clear All #</h1>

<p align="center"><strong>繁體中文</strong> · <a href="README.en.md">English</a></p>

<p align="center">透過 Vector／LSPosed，在 Xperia 原廠多工操作列加入「全部清除」。</p>

<p align="center">
  <a href="https://github.com/justinlin099/Xperia_Clear_All_Vector/releases">下載 APK</a> ·
  <a href="https://github.com/justinlin099/Xperia_Clear_All_Vector/issues">回報問題</a> ·
  <a href="https://github.com/justinlin099/Xperia_Clear_All">不需要 root？前往 Shizuku 版</a>
</p>

## 選擇版本

| | 本專案：Vector／LSPosed 版 | [非 root／Shizuku 版](https://github.com/justinlin099/Xperia_Clear_All) |
| --- | --- | --- |
| 使用需求 | root 與啟用中的 Vector／LSPosed | Shizuku 無線偵錯（ADB）模式 |
| 按鈕位置 | 直接加入 Sony Launcher 多工操作列 | 懸浮按鈕，可自訂位置與外觀 |
| 啟用方式 | 在框架中勾選模組與 Xperia Home | 在 App 中連接 Shizuku 並授權 |
| 圖示 | Aurora 三條線加上 `#` root 標記 | Aurora 三條線 |

請擇一啟用，避免多工畫面同時出現兩套清除按鈕。

## 功能

- 呼叫 Sony Launcher 原生的 `dismissAllTasks(View)`，沿用清除動畫與任務移除流程。
- 與截圖、多視窗按鈕共同排列，統一按鈕高度、圖示文字置中及圓角點擊回饋。
- 清單為空或正在選擇分割畫面時隱藏；滑動、轉場及清除期間防止重複操作。
- 文字使用 Launcher 自身的翻譯資源；外觀隨 Launcher 設定更新。
- 只有 Hook 模組，沒有獨立設定介面、常駐服務、懸浮視窗或輪詢。

## 使用需求與相容性

- 已 root 的 Sony Xperia，使用原廠 Xperia Home，並已安裝及啟用 [Vector](https://github.com/JingMatrix/Vector) 或相容的 LSPosed。
- APK 最低安裝版本為 **Android 16（API 36）**。
- 現行 Hook 針對 **Xperia Home 17，versionCode 37** 的內部類別與方法。最低 Android 版本不代表所有較新系統／Launcher 都相容；其他 Launcher 版本與第三方桌面尚未確認。
- 沿用 Legacy Xposed API，宣告最低框架 API 93；不要求遷移至 libxposed API 102。

作者已確認原有版本於實機正常運作。本次 1.1.6 的程式整理與資源優化另有自動化回歸測試；改版後的實機確認與測試範圍請見 [BUILD_VALIDATION.txt](BUILD_VALIDATION.txt)。

## 安裝與啟用

1. 從 [Releases](https://github.com/justinlin099/Xperia_Clear_All_Vector/releases) 下載已發佈的 APK，或依下方步驟自行編譯安裝。
2. 在 Vector／LSPosed 管理器啟用 **Xperia Clear All #**。
3. 作用範圍只勾選 **Xperia Home／`com.sonymobile.launcher`**。
4. 重新啟動手機，讓 Launcher 載入模組。
5. 開啟幾個應用程式，再進入多工畫面查看「全部清除」。

此模組沒有桌面啟動入口，安裝後從框架管理器操作即可。不需啟用 Shizuku、上層顯示或額外背景執行權限。

## 資源用量

模組僅在 Sony Launcher 主程序中運作，以原有 Launcher 事件更新按鈕。一般任務狀態回呼不再重做整排版面、資源名稱查找或圖示同步；版面與外觀工作分別由佈局／設定事件觸發。圖示只有在實際顏色改變時才要求狀態重繪。

已移除未使用的 AppCompat、Material 與 AndroidX Core 執行期依賴。這能減少 APK 體積；實際 CPU、PSS 記憶體與電池用量仍需實機量測，不能把 APK 縮小比例當作省電比例。

新版 Vector／libxposed API 的評估、優化細節與量測限制請見 [效能說明](docs/performance.md)。

## 常見問題

### 安裝後找不到 App 圖示？

這是沒有 Activity 的 Hook 模組，不會出現在桌面程式列表。圖示會顯示於系統應用程式資訊及框架管理器支援的模組列表。

### 按鈕沒有出現？

確認模組已啟用、作用範圍為 Xperia Home、已重新開機、最近使用清單非空，且未在分割畫面選擇流程。再檢查框架日誌中的 `XPERIA_RECENTS:` 訊息；若出現 `unsupported Launcher`，請附上 Xperia Home 版本回報。

### 如何停用？

在 Vector／LSPosed 取消啟用模組並重開機，或解除安裝 APK 後重開機。

## 自行編譯

需要 JDK 21 或 22、Android SDK Platform 36，以及可下載 Gradle／Maven 依賴的網路。專案包含 Gradle 8.13 Wrapper，不需另外安裝 Gradle。

```powershell
# Windows PowerShell；先設定 JAVA_HOME 與 ANDROID_HOME
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

macOS／Linux：

```sh
bash ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

輸出為 `app/build/outputs/apk/debug/app-debug.apk`。debug APK 使用本機 debug 金鑰，只有簽章與已安裝版本相同時才能直接覆蓋。發佈版本請使用自己的固定簽章金鑰。

圖示來源與重建方式見 [branding/README.md](branding/README.md)。

## GitHub 自動編譯與發布

已提供 GitHub Actions：`main`／PR 執行測試、lint 並產生 debug APK；在 `main` 手動執行可產生正式金鑰簽名的 APK；推送與 `versionName` 一致的 `v` tag 會自動建立 Release。

第一次需在 **Vector repo** 設定四個簽名 Secrets。完整步驟與目前版本的 tag 範例見 [自動編譯與發布說明](docs/RELEASING.md)。

## 回報問題

請在 [Issues](https://github.com/justinlin099/Xperia_Clear_All_Vector/issues) 附上機型、Android 版本、Xperia Home 版本、Vector／LSPosed 版本、模組版本、重現步驟及相關 `XPERIA_RECENTS:` 日誌。

## 授權與致謝

本專案採用 [GNU GPLv3](LICENSE)。感謝 [Vector](https://github.com/JingMatrix/Vector)、[LSPosed](https://github.com/LSPosed/LSPosed) 與 [XposedBridge](https://github.com/rovo89/XposedBridge)。Aurora 圖示延續自同作者的[非 root 專案](https://github.com/justinlin099/Xperia_Clear_All)。

本專案為社群工具，非 Sony 官方應用程式。
