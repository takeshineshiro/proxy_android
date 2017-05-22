package cn.wsds.gamemaster.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.text.TextUtils;

public class BitmapUtil {
	
	/** 
     * 转换图片成圆形 
     *  
     * @param bitmap 
     *            传入Bitmap对象 
     * @return 
     */  
    public static Bitmap toRoundBitmap(Bitmap bitmap) {  
        int width = bitmap.getWidth();  
        int height = bitmap.getHeight();  
        float roundPx;  
        float left, top, right, bottom, dst_left, dst_top, dst_right, dst_bottom;  
        if (width <= height) {  
            roundPx = width >> 1;  
            left = 0;  
            top = 0;  
            right = width;  
            bottom = width;  
            height = width;  
            dst_left = 0;  
            dst_top = 0;  
            dst_right = width;  
            dst_bottom = width;  
        } else {  
            roundPx = height >> 1;  
            float clip = (width - height) >> 1;  
            left = clip;  
            right = width - clip;  
            top = 0;  
            bottom = height;  
            width = height;  
            dst_left = 0;  
            dst_top = 0;  
            dst_right = height;  
            dst_bottom = height;  
        }  
  
        Bitmap output = Bitmap.createBitmap(width, height, Config.ARGB_8888);  
        final Rect src = new Rect((int) left, (int) top, (int) right, (int) bottom);  
        final Rect dst = new Rect((int) dst_left, (int) dst_top, (int) dst_right, (int) dst_bottom);  
  
        final Paint paint = new Paint();  
        paint.setAntiAlias(true);// 设置画笔无锯齿  
        final int color = 0xff424242;  
        paint.setColor(color);  
  
        Canvas canvas = new Canvas(output);  
        canvas.drawARGB(0, 0, 0, 0); // 填充整个Canvas  
        canvas.drawCircle(roundPx, roundPx, roundPx, paint);  
  
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));// 设置两张图片相交时的模式,参考http://trylovecatch.iteye.com/blog/1189452  
        canvas.drawBitmap(bitmap, src, dst, paint); //以Mode.SRC_IN模式合并bitmap和已经draw了的Circle  
          
        return output;  
    }  

    public static byte[] bitmap2Bytes(Bitmap bm){  
        ByteArrayOutputStream baos = new ByteArrayOutputStream();    
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);    
        return baos.toByteArray();  
    }
    
    /**
	 * 通过网络取位图
	 *  这是一个耗时操作，请不要在主线程去取
	 * @param spec
	 * @return 
	 */
	public Bitmap getInputBitmap(String spec){
		if(TextUtils.isEmpty(spec)){
			return null;
		}
		InputStream inputStream = null;
		try {
			URL url = new URL(spec);
			HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
			inputStream = urlConn.getInputStream();
			Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
			return bitmap;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
}
