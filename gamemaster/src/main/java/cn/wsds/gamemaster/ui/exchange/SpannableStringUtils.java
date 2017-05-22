package cn.wsds.gamemaster.ui.exchange;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;

/**
 * Created by hujd on 16-3-2.
 */
public final class SpannableStringUtils {
    /**
     *
     * @param title
     * @param desc
     * @param offset
     * @param color
     * @param textSize
     * @return
     */
    public static SpannableStringBuilder getTextBuilder(String title, String desc, int offset, int color, int textSize) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(title);
        builder.append(desc);
        int start = title.length();
        int end = builder.length();
        builder.setSpan(
                new ForegroundColorSpan(color), start, end - offset,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        builder.setSpan(new AbsoluteSizeSpan(textSize), start, end -offset,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return builder;
    }

    /**
     * 设置文本带下划线
     * @param str
     * @return
     */
    public static SpannableStringBuilder getTextBuilderUnderline(String str) {
        if(TextUtils.isEmpty(str)) {
            return null;
        }
        SpannableStringBuilder builder = new SpannableStringBuilder(str);
        builder.setSpan(new UnderlineSpan(), 0, str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return builder;
    }
}
