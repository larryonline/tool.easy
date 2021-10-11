package me.zhennan.tool.easy.impl;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import me.zhennan.tool.easy.EasyFeature;
import me.zhennan.tool.easy.EasyServiceProxy;

class EasyFeatureManager implements EasyFeature {

    private final Map<String, EasyFeature> mFeatureMap = new HashMap<>();

    public void use(EasyFeature feature) {
        if (null == feature) {
            throw new IllegalArgumentException("Given EasyFeature is Null");
        }

        String token = feature.getClass().getName();
        if (!mFeatureMap.containsKey(token)) {
            mFeatureMap.put(token, feature);
        } else {
            // warning to coder that he use same feature for twice
        }
    }

    private EasyFeature match(Class<?> def) {
        for (EasyFeature feature : mFeatureMap.values()) {
            if (feature.contains(def)) {
                return feature;
            }
        }
        return null;
    }

    @Override
    public <T> boolean contains(Class<T> theClass) {
        return null != match(theClass);
    }

    @Override
    public <T> EasyServiceProxy<T> create(Class<T> serviceClass, Context context) {
        EasyFeature feature = match(serviceClass);
        if (null == feature) {
            throw new IllegalArgumentException("Given Service["+serviceClass+"] not exist");
        }
        return feature.create(serviceClass, context);
    }
}
