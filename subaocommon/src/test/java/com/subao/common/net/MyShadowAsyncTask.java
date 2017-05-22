package com.subao.common.net;

import android.os.AsyncTask;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadows.ShadowAsyncTask;

import java.util.concurrent.Executor;

/**
 * Created by hujd on 16-7-21.
 */
@Implements(AsyncTask.class)
public class MyShadowAsyncTask<Params, Progress, Result> extends ShadowAsyncTask<Params, Progress, Result> {
    @RealObject
    private AsyncTask<Params, Progress, Result> realAsyncTask;

    @Implementation
    public AsyncTask<Params, Progress, Result> executeOnExecutor(Executor executor, Params... params) {
        return super.execute(params);
    }
}
