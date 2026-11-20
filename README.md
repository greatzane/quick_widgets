# 快捷宫格

适用于 Android 8.0 及以上版本的桌面小组件。支持单行或双行，每行 4、5 或 6 个位置。桌面只显示图标。图标大小可在 32–64 dp 之间调节，默认 48 dp。桌面空间较小时，图标会缩放以适应格子。

## 使用

1. 打开「快捷宫格」，设置行数、每行图标数和图标大小。背景可点选 11 种预设色或棋盘格透明选项，也可点「更多颜色」调节色调、饱和度、亮度和不透明度。
2. 点按预览里的图标或空位，选择常用功能或已安装应用。长按可测试打开、更换图标、移动或移除。
3. 点「添加到桌面」，在系统提示中确认。也可以从桌面的「小组件」列表选择快捷宫格的单行或双行版本。
4. 添加后，打开应用右上角的「管理」即可编辑对应的小组件。每个小组件独立保存。

常用功能包含微信的扫一扫和付款码，支付宝和云闪付的扫一扫、付款码与乘车码，以及飞书、美团和滴滴的扫一扫。

扫码、付款码和乘车码图标统一采用圆角底形与描边，用配色区分应用。重绘参考了应用公开的图标和功能入口。 [支付宝功能入口](https://opendocs.alipay.com/p/03oc9z) · [中国银联云闪付](https://www.unionpay.com/upowhtml/cn/templates/quickPass/quickPass.html) · [美团应用图标](https://play.google.com/store/apps/details?id=com.sankuai.meituan) · [滴滴骑行扫码](https://www6.didiglobal.com/travel-service/bike)

## 应用快捷方式

选择已安装应用后，可添加该应用允许外部调用的静态快捷方式、导入兼容的快捷方式创建结果，或粘贴应用提供的功能链接。Android 将动态快捷方式的查询权限授予系统桌面等角色。 [Android LauncherApps 文档](https://developer.android.com/reference/android/content/pm/LauncherApps#hasShortcutHostPermission())

微信扫一扫和付款码使用微信 Android 桌面快捷方式的原生入口。飞书扫一扫使用飞书的官方 AppLink。预设的直达效果以设备上的应用版本及开通状态为准。可以长按预览图标测试打开。 [飞书扫一扫 AppLink 文档](https://open.feishu.cn/document/uAjLw4CM/uYjL24iN/applink-protocol/supported-protocol/open-scan-function)

## 构建

使用 JDK 21 和 Android SDK Platform 36。设置 `JAVA_HOME` 和 `ANDROID_HOME` 后，在项目根目录运行以下命令。

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

调试安装包位于 `app/build/outputs/apk/debug/app-debug.apk`。
