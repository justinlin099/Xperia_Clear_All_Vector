# 自動編譯、簽名與 GitHub Release

本專案的 [workflow](../.github/workflows/android.yml) 沿用非 root 版的操作方式，使用 Ubuntu 24.04、JDK 21、Gradle Wrapper 8.13 與 Android SDK 36。

| 操作 | 結果 |
| --- | --- |
| 推送至 `main` 或提出 PR | 跑 Android 單元測試、lint、發布打包測試，並上傳 `debug-apk` |
| 在 `main` 手動執行 | 通過檢查後簽名 APK，上傳 `signed-release-apk`，不建立公開 Release |
| 推送 `v` 開頭的 tag | 通過檢查後簽名，建立 GitHub Release，附上 APK、SHA-256 與簽名憑證資訊 |

## 第一次設定：四個 Secrets

到 **Vector 這個 repo** 的 **Settings → Secrets and variables → Actions → Secrets → New repository secret**，新增：

| 名稱 | 內容 |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | `.jks`／`.keystore` 檔案完整內容的 Base64 |
| `ANDROID_KEYSTORE_PASSWORD` | keystore 密碼 |
| `ANDROID_KEY_ALIAS` | keystore 中使用的 key alias |
| `ANDROID_KEY_PASSWORD` | 該 key 的密碼；若與 keystore 密碼相同，填相同值 |

另一個 repo 的 Repository Secrets 不會自動出現在這裡，需要在 Vector repo 另外設定。建立 Release 使用 Actions 自帶的 `GITHUB_TOKEN`，不需額外的 Personal Access Token。

如果要讓已安裝的 **Vector 舊版**直接覆蓋更新，請沿用簽過 Vector 舊版 APK 的私鑰；兩個版本的套件名稱不同，不要求與非 root 版使用相同金鑰。公開的憑證指紋無法代替私鑰。若尚未有正式金鑰，可在 Android Studio 的 **Build → Generate Signed App Bundle or APK → APK → Create new** 建立，放在 repo 外並備份。

PowerShell 將金鑰 Base64 直接複製到剪貼簿（路徑自行替換）：

```powershell
$vectorKeystorePath = 'D:\Signing\xperia-vector-release.jks'
[Convert]::ToBase64String([IO.File]::ReadAllBytes($vectorKeystorePath)) | Set-Clipboard
```

貼到 `ANDROID_KEYSTORE_BASE64`，完成後清空剪貼簿。不要將金鑰、Base64 或密碼提交至 Git。workflow 只在簽名工作讀取 Secrets；一般 PR／debug 建置不需要這些資料。

## 推送程式與 workflow

在 GitHub Desktop 檢查並提交本次變更，再 **Push origin** 到 `main`。內容需包含 `.github/`、Gradle 簽名設定、文件，以及先前的程式／圖示變更與 `gradle/wrapper/gradle-wrapper.jar`。`artifacts/`、`dist/` 與私鑰檔已排除。

先確認 repository 的 **Actions** 已啟用。若有 actions 使用政策限制，需允許 workflow 引用的 `actions/*` 與 `gradle/actions/*`。

## 先手動產出簽名 APK

1. 開啟 Vector repo 的 **Actions → Android CI and release**。
2. 按 **Run workflow**，分支選 **main**。
3. 等待 `Test, lint and build debug APK` 與 `Sign APK and publish tagged release` 完成。
4. 在該次執行的 **Artifacts** 下載 **signed-release-apk**。

壓縮檔包含：

- `Xperia-Clear-All-Vector-版本.apk`：固定金鑰簽名的 APK。
- `SHA256SUMS.txt`：APK 檔案的 SHA-256。
- `signing-certificate.txt`：簽名驗證結果與公開憑證指紋。

`debug-apk` 為測試簽章，與正式簽名檔用途不同。若檢查失敗，可下載 `test-and-lint-reports`。

## 自動發布 Release

tag 必須是 **`v` 加上 APK 的完整 `versionName`**，且 tag 指向的 commit 已包含於 `main`。

目前設定為：

```kotlin
versionCode = 8
versionName = "1.1.6-standalone"
```

第一次發布這個版本，在程式已提交並推送後執行：

```powershell
git tag v1.1.6-standalone
git push origin v1.1.6-standalone
```

Actions 成功後，前往 [Vector Releases](https://github.com/justinlin099/Xperia_Clear_All_Vector/releases) 下載。後續發布需遞增 `versionCode`、更新 `versionName`，使用新的對應 tag。

`-standalone` 代表獨立模組版本，單獨使用此後綴仍是正式 Release。`1.1.7-beta1-standalone`／`1.1.7-rc1-standalone` 等含預覽後綴的版本則自動標示為 pre-release。

workflow 會拒絕版本不符、錯誤套件名稱、未簽名的輸出檔名或混有舊檔的打包目錄。上傳前另以 `apksigner` 驗證實際 APK 簽章。已存在的 Release 不會被覆蓋；重跑已發佈的 tag 會停止並提示使用新版本。

## 本機驗證與簽名

不用正式私鑰也能跑檢查並建立 debug／unsigned release：

```powershell
python -m unittest discover -s .github/scripts -p 'test_*.py' -v
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

本機要簽 release 時，設定下列四個環境變數，再執行 `.\gradlew.bat --no-daemon --no-configuration-cache :app:assembleRelease`：

- `ANDROID_KEYSTORE_PATH`：金鑰檔的完整路徑（本機使用路徑，不是 Base64）。
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

未設定時保留 unsigned release 功能；只設定部分欄位時會直接報錯，避免誤以為已簽名。GitHub 的簽名工作則會先檢查 Secrets，缺任一項就停止。

發布輔助程式只用 Python 標準函式庫。正常 CI 不需重建圖示，也不需安裝 Pillow。自動化可驗證測試、建置與簽章，Sony 實機上的 Hook 行為仍需另外確認。

## 常見問題

| 問題 | 處理方式 |
| --- | --- |
| 沒有 Run workflow | 先把 workflow 推送到預設分支 `main`，並確認有寫入權限 |
| `Missing repository secret` | 在 Vector repo 的 Secrets 新增對應值，不能放在 Variables |
| keystore／alias／密碼錯誤 | 用 `keytool -list -keystore 金鑰路徑` 核對，再更新 Secrets |
| `Tag must be ...` | tag 要完整對應 `versionName`，包含 `-standalone` |
| `Require release commit to be on main` 失敗 | 先將該 commit 合併／推送至 `main` |
| APK 無法覆蓋舊版 | 比較套件名稱、簽名憑證與 versionCode；沿用原有 Vector 簽名金鑰 |
| `dist must be empty` | 本機打包前先移走上次的 `dist` 產物；CI 使用全新 checkout |

參考：[GitHub Secrets 官方說明](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets)、[手動執行 workflow](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow)。
