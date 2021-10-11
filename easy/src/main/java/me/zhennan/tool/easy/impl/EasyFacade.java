package me.zhennan.tool.easy.impl;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import me.zhennan.tool.easy.EasyFeature;
import me.zhennan.tool.easy.EasyResolver;

public class EasyFacade {

    private final EasyFeatureManager mFeatureManager = new EasyFeatureManager();
    private final Map<Context, EasyComponent> mComponentMap = new HashMap<>();

    public void use(EasyFeature feature) {
        mFeatureManager.use(feature);
    }

    public void startup(Context context) {
        if (null == context) {
            throw new IllegalArgumentException("Given context is Null");
        }

        EasyComponent component = mComponentMap.get(context);
        if (null == component) {
            EasyComponentHost host = (EasyComponentHost) mComponentMap.get(context.getApplicationContext());
            if (null == host) {
                host = new EasyComponentHost(context, mFeatureManager);
                mComponentMap.put(context.getApplicationContext(), host);
            }

            if (context != context.getApplicationContext()) {
                component = new EasyComponentClient(context, host);
                mComponentMap.put(context, component);
            } else {
                component = host;
            }
        }

        component.startup();
    }

    public void shutdown(Context context) {
        if (null == context) {
            throw new IllegalArgumentException("Given context is Null");
        }

        EasyComponent component = mComponentMap.remove(context);

        if (null != component) {
            component.shutdown();
        }
    }

    public EasyResolver from(Context context) {
        if (null == context) {
            throw new IllegalArgumentException("Given context is Null");
        }

        if (!mComponentMap.containsKey(context)) {
            throw new IllegalStateException("You must invoke Easy.start("+context+") before invoke this method");
        }

        return mComponentMap.get(context);
    }
}
