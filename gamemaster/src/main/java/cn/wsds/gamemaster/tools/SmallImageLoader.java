package cn.wsds.gamemaster.tools;

import android.content.Context;
import android.graphics.Bitmap;
import cn.wsds.gamemaster.R;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class SmallImageLoader extends ImageLoader {
	
	private volatile static SmallImageLoader instance;
	private volatile static ImageLoaderConfiguration imageLoaderConfiguration;
	private volatile static DisplayImageOptions displayImageOptions;

    public static SmallImageLoader getInstance(Context context) {
        if (instance == null) {
            synchronized (SmallImageLoader.class) {
                if (instance == null) {
                	instance = new SmallImageLoader();
                	context = context.getApplicationContext();
                	displayImageOptions = new DisplayImageOptions.Builder()
	        			.showImageOnLoading(R.drawable.icon_game)
	        			.showImageForEmptyUri(R.drawable.icon_game)
	        			.showImageOnFail(R.drawable.icon_game).cacheInMemory(true)
	        			.cacheOnDisk(true).considerExifParams(true)
	        			.bitmapConfig(Bitmap.Config.RGB_565).build();
                	
                	imageLoaderConfiguration = new ImageLoaderConfiguration.Builder(context)
                			.denyCacheImageMultipleSizesInMemory()
                			.memoryCacheExtraOptions(150, 150)
                			.diskCacheExtraOptions(150, 150, null)
                			.defaultDisplayImageOptions(displayImageOptions)
                			.build();
                	instance.init(imageLoaderConfiguration);
                }
            }
        }
        
        return instance;
    }

}
