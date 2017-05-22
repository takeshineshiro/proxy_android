package com.subao.common.net;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.internal.bytecode.InstrumentationConfiguration;

/**
 * Created by hujd on 16-7-21.
 */
public class CustomShadowTestRunner extends RobolectricTestRunner {
    public CustomShadowTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    public InstrumentationConfiguration createClassLoaderConfig(Config config) {
        InstrumentationConfiguration.Builder builder = InstrumentationConfiguration.newBuilder();
        /**
         * 添加shadow的对象
         */
        builder.addInstrumentedClass(MyShadowAsyncTask.class.getName());
        return builder.build();
    }
}