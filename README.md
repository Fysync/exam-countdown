# 研途倒计时 · 2027 考研

像教室门口那块实体倒计时牌一样的考研倒计时：绿色金属外壳、琥珀色数码管数字、扫描线质感。安卓原生 App + 桌面小组件，全部离线运行。

## 功能

- **桌面小组件**：数码管大数字「距离研究生考试还有 X 天」+ 底部日期 2027.12.25，跨零点自动刷新、随桌面格子自动伸缩，长按可单独改考试日期
- **主程序 · 管理台**：日历选择考试日期（默认 2027-12-25），改动同步到所有小组件；「立即刷新」一键重绘
- **主程序 · 桌面时钟**：横屏全屏 + 屏幕常亮、只显示时间（HH:MM:SS 数码管，冒号闪烁），按返回键回到管理台

## 项目结构

```
exam-countdown/
├── android/                 原生安卓工程（AppWidget + App）
│   └── app/src/main/        Kotlin 源码 / 布局 / drawable / 字体
├── web/                     网页版（PWA，浏览器预览用）
├── tools/                   图标与素材生成脚本
└── .github/workflows/       GitHub Actions 云端编译配置
```

## 构建（GitHub Actions 云端编译）

仓库：`https://github.com/Fysync/exam-countdown`（私有）

- 每次 `git push` 到 main 自动触发编译
- 编译产物在仓库 **Actions** 页 → 最新一次运行 → **Artifacts** → 下载 `exam-countdown-apk`，解压得 `app-debug.apk`
- 也可在 Actions 页手动触发（`workflow_dispatch`）

## 安装到一加平板（Android 16）

1. 平板开启 **USB 调试**（设置 → 关于本机 → 连点版本号 7 次 → 开发者选项 → USB 调试）
2. 数据线连电脑，运行：

   ```powershell
   D:\platform-tools\adb install E:\vibecoding\exam-countdown\dist\app-debug.apk
   ```

   或把 APK 用微信/网盘传到平板，在文件管理器中点击安装
3. 首次安装非商店应用：给文件管理器/微信开「安装未知应用」权限
4. 回到主屏幕：**长按空白处 → 小组件 → 研途倒计时 → 拖到桌面**
5. 打开 App：管理台设考试日期、桌面时钟看时间

## 更新

- 新代码 `git push` → Actions 自动编译 → 下载新 APK → `adb install -r` 覆盖安装即可
- 应用内的考试日期和小组件设置会保留

## 技术要点

- **数码管数字**：数字用开源 DSEG7 字体渲染成图片交给小组件，绕开部分桌面不显示自定义字体的问题
- **无需精确闹钟权限**：`AlarmManager.setAndAllowWhileIdle` + 30 分钟轮询保底，Android 12+ 不弹权限
- **自动跨天**：午夜刷新调度 + 开机/日期/时区变更广播
- **离线可用**：纯本地渲染，无网络依赖