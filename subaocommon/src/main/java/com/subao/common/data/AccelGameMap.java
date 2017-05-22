package com.subao.common.data;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 管理{@link AccelGame}的特殊容器
 */
public class AccelGameMap {

    /**
     * 如果一个AppLabel包含下列关键字，则该APP不能被视为游戏
     */
    private final static String[] BAD_WORDS = new String[]{
        "notification", "pps", "pptv", "theme", "wallpaper", "wifi", "安装", "八门神器",
        "百宝箱", "伴侣", "宝典", "备份", "必备", "壁纸", "变速", "表情",
        "补丁", "插件", "查询", "查询", "出招表", "春节神器", "答题", "大全",
        "大师", "单机", "动态", "翻图", "辅助", "辅助", "改名", "工具",
        "攻略", "喊话", "合成表", "合集", "盒子", "红包神器", "画报", "集市",
        "计算", "技巧", "計算", "加速", "脚本", "解说", "精选", "剧场",
        "快问", "礼包", "连招表", "论坛", "漫画", "秘籍", "模拟器", "魔盒",
        "配装器", "拼图", "启动器", "全集", "社区", "视频", "视讯", "手册",
        "刷开局", "刷魔", "锁屏", "台词", "特辑", "头条", "图集", "图鉴",
        "圖鑑", "外挂", "系列", "下载", "小说", "小智", "修改", "一键",
        "英雄帮", "英雄榜", "游戏盒", "游戏通", "掌游宝", "照相", "直播", "指南",
        "制作器", "主题", "助理", "助手", "抓包", "追剧", "桌面", "资料",
        "资讯", "資料", "作弊",
    };

    /**
     * 特殊的AppLabel，不能视为游戏
     */
    private final static String[] BAD_APP_LABELS = new String[]{"掌上英雄联盟"};

    /**
     * 特殊的包名，不能视为游戏
     */
    private final static String[] BAD_PACKAGE_NAMES = new String[]{
        "com.kugou.android",
        "com.huluxia.mctool",
        "com.tencent.qt.sns",
    };

    private static final Locale locale = Locale.US;

    /**
     * 从游戏名到{@link AccelGame}对象的映射
     */
    private final HashMap<String, AccelGame> mapByLabel;

    static boolean isLabelBad(String appLabel) {
        for (String black : BAD_APP_LABELS) {
            if (black.equals(appLabel)) {
                return true;
            }
        }
        return false;
    }

    static boolean isPackageNameBad(String packageName) {
        if (!TextUtils.isEmpty(packageName)) {
            String lowerPackageName = packageName.toLowerCase(locale);
            for (String black : BAD_PACKAGE_NAMES) {
                if (black.equals(lowerPackageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public AccelGameMap(List<AccelGame> accelGameList) {
        if (accelGameList == null || accelGameList.isEmpty()) {
            this.mapByLabel = null;
        } else {
            this.mapByLabel = new HashMap<String, AccelGame>(accelGameList.size());
            for (AccelGame ag : accelGameList) {
                this.mapByLabel.put(ag.appLabel.toLowerCase(locale), ag);
            }
        }
    }

    /**
     * 根据游戏包名和名字查找相应的{@link AccelGame}
     */
    public AccelGame findAccelGame(String packageName, String appLabel) {
        if (appLabel == null || appLabel.length() == 0) {
            return null;
        }
        //
        // 过滤Label在黑名单里的
        if (isLabelBad(appLabel)) {
            return null;
        }
        //
        // 过滤PackageName在黑名单里的
        if (isPackageNameBad(packageName)) {
            return null;
        }
        //
        // Label转换为小写（便于匹配）
        String lowerAppLabel = appLabel.toLowerCase(locale);
        //
        // 先精确匹配
        if (mapByLabel != null) {
            AccelGame found = mapByLabel.get(lowerAppLabel);
            if (found != null) {
                return found;
            }
        }
        //
        // 如果关键字小于等于三个字符，不再进行模糊匹配了
        if (lowerAppLabel.length() <= 3) {
            return null;
        }
        //
        // 进行模糊匹配
        if (mapByLabel != null) {
            Iterator<Map.Entry<String, AccelGame>> it = this.mapByLabel.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, AccelGame> entry = it.next();
                String key = entry.getKey();
                int keyLen = key.length();
                if (keyLen <= 2) {
                    continue;
                }
                AccelGame accelGame = entry.getValue();
                // 游戏名是3个ASCII字符的不进行模糊匹配
                if (accelGame.isLabelThreeAsciiChar) {
                    continue;
                }
                // 只允许精确匹配的，跳过
                if (accelGame.needExactMatch()) {
                    continue;
                }
                if (lowerAppLabel.contains(key)) {
                    if (doesAppLabelIncludeBlackWord(lowerAppLabel)) {
                        return null;
                    } else {
                        return accelGame;
                    }
                }
            }
        }
        //
        // 没找到，返回null
        return null;
    }

    /**
     * 判断给定的游戏名称，是否含有“黑词”（即“助手”“攻略”一类的需要过滤的词）
     *
     * @param appLabel 游戏名字
     * @return true表示游戏名字里含有“黑词”，否则返回false
     */
    public boolean doesAppLabelIncludeBlackWord(String appLabel) {
        for (String black : BAD_WORDS) {
            if (appLabel.contains(black)) {
                return true;
            }
        }
        return false;
    }

}
