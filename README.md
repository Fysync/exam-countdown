# 研途倒计时 · 2027 考研

像教室门口那块实体倒计时牌一样的考研倒计时：绿色金属外壳、琥珀色液晶数码管、扫描线质感。可离线使用，可"安装"到平板主屏幕。

```
E:\vibecoding\exam-countdown\
├── index.html           页面结构
├── styles.css           样式（全系统字体，不依赖网络）
├── app.js               逻辑（跨零点刷新 / 常亮 / 秒数 / 尺寸）
├── manifest.webmanifest PWA 清单（可安装到主屏）
├── sw.js                Service Worker（离线缓存）
├── icons\               应用图标（192 / 512 / maskable / apple）
├── tools\               图标 / 安卓素材生成脚本
├── android\             原生桌面小组件工程（AppWidget，见 android\README.md）
└── .github\workflows\   GitHub Actions 云端编译 APK
```

> **两套成品**：网页版（PWA）在根目录；原生安卓小组件在 `android\`，由 GitHub Actions 云端编译出 APK。

## 功能

- 距离研究生考试还有 **X 天**，每天 00:00 自动刷新，跨天不用手动开页面
- 可选**今日剩余 HH:MM:SS** 秒级倒计时
- 可选**屏幕常亮**（Wake Lock，保持牌子一直亮着）
- 三种桌面尺寸：小号 2×2 / 中号 4×2 / 大号 4×4（大号多显示周数、考试星期）
- **专注模式**：一键全屏铺满整块牌子，适合当桌面摆件
- 考试当天 / 已结束的自动状态文案
- 考试日期可改（默认 2027-12-25），设置自动保存在本设备

## 本地预览

```powershell
cd E:\vibecoding\exam-countdown
py -m http.server 4173
```

浏览器打开 <http://127.0.0.1:4173/>。
注意：Service Worker 只在 http/https 下工作，直接双击 index.html 无法离线缓存。

## 部署到公网（任选一个，免费）

| 平台 | 方式 |
|---|---|
| GitHub Pages | 建仓库 → 上传本目录 → Settings → Pages 选 main 分支 |
| Vercel | vercel.com 导入本目录，默认设置直接可用 |
| Netlify | netlify.com 拖拽本目录上传即可 |

## 装到一加平板（Android 16）主屏幕

1. 在平板上用 Chrome / 自带浏览器打开部署好的网址
2. 右上角菜单 →「**添加到主屏幕**」/「安装应用」
3. 桌面上会出现"研途倒计时"图标，点开即全屏显示，可离线使用
4. 想一直亮着：打开应用内「屏幕常亮」开关；想铺满整屏：点右下角「专注模式」

> 说明：这是 PWA（全屏应用），不是安卓原生桌面小组件。以后如果做原生 AppWidget，会放在 `android\` 子目录。

## 常见问题

- **为什么首页数字不动？** 页面挂着时会跨零点自动刷新；若用的是老浏览器不支持定时器常驻，手动刷新一次即可。
- **图标想换？** 改 `tools\make_icons.py` 里的配色/文字后，运行 `py tools\make_icons.py` 重新生成。
- **更新离线缓存？** 改 `sw.js` 顶部的 `VERSION` 并重新部署。