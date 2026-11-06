# Aurora # — Xperia Clear All root edition

沿用 [非 root 版 Aurora](https://github.com/justinlin099/Xperia_Clear_All/tree/main/branding) 的三條線與配色，在左上加入深色底的 `#` 標記，代表 root 版本。圖示名稱為 **Xperia Clear All #**。

- 彩色：青 `#00AFC8`、藍 `#3767E8`、紫 `#7544D8`；背景 `#EDF5FF`；root 徽章 `#18263D`。
- 主題圖示：白色三條線與 `#`，不使用實心徽章，避免單色時遮住符號；色彩由 Launcher 決定。
- Android 使用原生 VectorDrawable 與 adaptive icon，不載入 PNG；前景位於中央直徑 66dp 安全區。
- `aurora-root/` 提供可編輯 SVG、README 用 PNG 與彩色／單色／小尺寸預覽。

重建：在 repo 根目錄執行 `python branding/build_icons.py`。Android XML／SVG 只用 Python 標準函式庫；PNG 預覽另需 Pillow。正常 Gradle 建置不需 Python 或 Pillow。
