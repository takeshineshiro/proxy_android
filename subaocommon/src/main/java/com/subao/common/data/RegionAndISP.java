package com.subao.common.data;


import android.annotation.SuppressLint;

public class RegionAndISP {

    public final int region;
    public final int isp;

    public RegionAndISP(int region, int isp) {
        this.region = region;
        this.isp = isp;
    }

    /**
     * 将给定的{@link RegionAndISP}转换成人工易读的文本
     *
     * @param regionAndISP {@link RegionAndISP}
     * @return 如果给定的参数为null则返回null，否则返回转换后的文本
     */
    public static String toText(RegionAndISP regionAndISP) {
        if (regionAndISP == null) {
            return null;
        } else {
            return regionAndISP.toText();
        }
    }

    public String toSimpleText() {
        return this.region + "." + this.isp;
    }

    /**
     * 转换成人工易读的文本
     */
    public String toText() {
        ChinaRegion region = null;
        for (ChinaRegion cr : ChinaRegion.values()) {
            if (this.region == cr.num) {
                region = cr;
                break;
            }
        }
        ChinaISP isp = null;
        for (ChinaISP exists : ChinaISP.values()) {
            if (this.isp == exists.num) {
                isp = exists;
                break;
            }
        }
        StringBuilder sb = new StringBuilder(128);
        if (region == null) {
            sb.append(this.region);
        } else {
            sb.append(region.pinyin);
        }
        sb.append('.');
        if (isp == null) {
            sb.append(this.isp);
        } else {
            sb.append(isp.code);
        }
        return sb.toString();
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("[region=%d, isp=%d]", region, isp);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (!(o instanceof RegionAndISP)) {
            return false;
        }
        RegionAndISP other = (RegionAndISP) o;
        return this.region == other.region
            && this.isp == other.isp;
    }

    @Override
    public int hashCode() {
        return this.region * 100 + this.isp;
    }
}
