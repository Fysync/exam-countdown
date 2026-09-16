# 研途倒计时 · 原生 Android 小组件

一个真正的桌面小组件（AppWidget）：绿色金属外壳、琥珀色液晶数字，显示「距离研究生考试还有 X 天」。可在桌面调整大小，长按可更改考试日期（默认 2027-12-25）。

```
android/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/example/examcountdown/
│   │   ├── CountdownWidgetProvider.kt    小组件核心：渲染 + 尺寸自适应
│   │   ├── CountdownWidgetConfigActivity.kt  选考试日期（DatePicker）
│   │   ├── CountdownEngine.kt            日期计算 + 每小组件独立存储
│   │   ├── UpdateScheduler.kt            午夜刷新调度（避开精确闹钟权限）
│   │   ├── BootReceiver.kt               开机/升级后恢复刷新
│   │   └── MainActivity.kt               安装说明页
│   └── res/
│       ├── layout/  widget_countdown.xml（大）、widget_countdown_small.xml（小）
│       ├── drawable/ 金属壳、液晶窗、扫描线、螺丝、图标
│       └── xml/countdown_widget_info.xml
└── build.gradle 等 Gradle 配置
```

## 构建方式：GitHub Actions 云端编译（推荐，本机零安装）

1. 仓库根目录已有 `.github/workflows/build-apk.yml`，push 到 `main` 或手动运行即触发。
2. 编译完成后：
   - 打开仓库 → **Actions** 标签 → 最新一次运行
   - 页面底部 **Artifacts** → 下载 `exam-countdown-apk`
   - 解压得到 `app-debug.apk`

## 装到一加平板（Android 16）

1. 平板开启 **USB 调试**（设置 → 关于本机 → 连点版本号 7 次开开发者选项 → 开发者选项 → USB 调试）
2. 数据线连电脑，运行：

   ```powershell
   D:\platform-tools\adb install E:\vibecoding\exam-countdown\android\app\build\outputs\apk\debug\app-debug.apk
   ```

   或把 APK 用微信/网盘传到平板，在文件管理器中点击安装。
3. 首次安装非商店应用：弹窗允许「安装未知应用」（给文件管理器/微信开权限）。
4. 回到主屏幕：**长按空白处 → 小组件 → 研途倒计时 → 拖到桌面**
5. 长按小组件可**调整大小**或点「配置」**更改考试日期**

## 技术要点

- **无需精确闹钟权限**：用 `AlarmManager.setAndAllowWhileIdle` + 小组件 30 分钟轮询保底，Android 12+ 也不弹权限。
- **自动跨天刷新**：午夜刷新调度 + 开机/日期/时区变更广播。
- **尺寸自适应**：桌面格子较小时自动切到紧凑布局。
- **离线可用**：纯本地渲染，无网络依赖。