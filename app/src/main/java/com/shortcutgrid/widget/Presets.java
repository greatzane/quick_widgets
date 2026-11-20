package com.shortcutgrid.widget;

import static com.shortcutgrid.widget.ShortcutEntry.Glyph.*;

enum Presets {
    WECHAT_SCAN("wechat_scan", "微信扫一扫", "com.tencent.mm", SCAN,
            wechat("launch_type_scan_qrcode")),
    WECHAT_PAY("wechat_pay", "微信付款码", "com.tencent.mm", PAY,
            wechat("launch_type_offline_wallet")),
    ALIPAY_SCAN("alipay_scan", "支付宝扫一扫", "com.eg.android.AlipayGphone", SCAN,
            alipay("10000007")),
    ALIPAY_PAY("alipay_pay", "支付宝付款码", "com.eg.android.AlipayGphone", PAY,
            alipay("20000056")),
    ALIPAY_TRANSIT("alipay_transit", "支付宝乘车码", "com.eg.android.AlipayGphone", TRANSIT,
            alipay("200011235")),
    UNIONPAY_SCAN("unionpay_scan", "云闪付扫一扫", "com.unionpay", SCAN,
            "upwallet://native/scanCode"),
    UNIONPAY_PAY("unionpay_pay", "云闪付付款码", "com.unionpay", PAY,
            "upwallet://native/codepay", "upwallet://pay"),
    UNIONPAY_TRANSIT("unionpay_transit", "云闪付乘车码", "com.unionpay", TRANSIT,
            "upwallet://rn/rnhtmlridingcode"),
    FEISHU_SCAN("feishu_scan", "飞书扫一扫", "com.ss.android.lark", SCAN,
            "https://applink.feishu.cn/client/qrcode/main"),
    MEITUAN_SCAN("meituan_scan", "美团扫一扫", "com.sankuai.meituan", SCAN,
            "imeituan://www.meituan.com/scanQRCode"),
    DIDI_SCAN("didi_scan", "滴滴扫一扫", "com.sdu.didi.psnger", SCAN,
            "OneTravel://shortcuts/open_scan", "onetravel://router/page/open_scan");

    final ShortcutEntry entry;
    final String[] targets;

    Presets(String key, String title, String packageName, ShortcutEntry.Glyph glyph,
            String... targets) {
        entry = new ShortcutEntry(ShortcutEntry.Kind.PRESET, title, packageName, key, glyph, "");
        this.targets = targets;
    }

    private static String wechat(String launchType) {
        // Matches the Android launcher shortcuts published by WeChat.
        return "intent:#Intent;action=com.tencent.mm.ui.ShortCutDispatchAction;"
                + "S.LauncherUI.Shortcut.LaunchType=" + launchType + ";end";
    }

    private static String[] alipay(String appId) {
        return new String[] {
                "alipays://platformapi/startapp?appId=" + appId,
                "alipayqr://platformapi/startapp?saId=" + appId,
                "alipay://platformapi/startapp?appId=" + appId
        };
    }

    static Presets find(String key) {
        for (Presets preset : values()) {
            if (preset.entry.target().equals(key)) return preset;
        }
        return null;
    }
}
