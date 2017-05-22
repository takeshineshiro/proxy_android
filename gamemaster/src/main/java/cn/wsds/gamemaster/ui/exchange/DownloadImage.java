package cn.wsds.gamemaster.ui.exchange;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

/**
 * Created by hujd on 16-3-28.
 */
public class DownloadImage {

    public static void displayImage(ImageView imageView, int imageResource, String url) {
        new DownloadImageTask(imageView, imageResource).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
    }

    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> mImageView;
        private final int mImageResource;

        private DownloadImageTask(ImageView imageView, int imageResource) {
            mImageResource = imageResource;
            if(imageView == null ) {
                throw new IllegalArgumentException();
            }
            mImageView = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String urlDiplay = params[0];
            Bitmap icon = null;
            try {
                InputStream inputStream = new URL(urlDiplay).openStream();
                icon = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return icon;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ImageView imageView = mImageView.get();
            if(imageView == null) {
                return;
            }
            if(bitmap !=null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(mImageResource);
            }
        }
    }




}
