package me.zhennan.tool.easy.impl;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import me.zhennan.tool.easy.EasyServiceProxy;

class EasyComponentHost extends EasyComponent {

    private final Map<Class<Object>, EasyServiceProxy<Object>> mProxyMap = new HashMap();
    private final Map<Class<Object>, List<BiConsumer<Object, Class<Object>>>> mConsumerMap = new HashMap<>();
    private final BiConsumer<Object, Class<Object>> mComponentConsumer = (service, serviceClass) -> {
        List<BiConsumer<Object, Class<Object>>> list = mConsumerMap.get(serviceClass);
        if (null != list && 0 < list.size()) {
            for (BiConsumer<Object, Class<Object>> consumer : list) {
                consumer.accept(service, serviceClass);
            }
        }
    };

    private Context mAppContext = null;
    private EasyFeatureManager mFeatureManager = null;


    private boolean startupAlready = false;
    private boolean shutdownAlready = false;

    public EasyComponentHost(Context applicationContext, EasyFeatureManager manager) {
        if (null == applicationContext || applicationContext != applicationContext.getApplicationContext()) {
            throw new IllegalArgumentException("invalid context");
        }

        mAppContext = applicationContext;

        if (null == manager) {
            throw new IllegalArgumentException("Given feature manager is Null");
        }

        mFeatureManager = manager;
    }

    public boolean isStartup() {
        return startupAlready;
    }

    @Override
    public void startup() {
        if (!startupAlready && !shutdownAlready) {
            startupAlready = true;

            // 试着处理所有的 proxy
            for (Class<Object> serviceClass : mConsumerMap.keySet()) {
                List<BiConsumer<Object, Class<Object>>> list = mConsumerMap.get(serviceClass);
                if (null != list && 0 < list.size()) {
                    for (BiConsumer<Object, Class<Object>> consumer : list) {
                        resolve(serviceClass, consumer);
                    }
                }
            }
        }
    }

    public boolean isShutdown() {
        return shutdownAlready;
    }

    @Override
    public void shutdown() {
        if (startupAlready && !shutdownAlready) {
            startupAlready = false;
            shutdownAlready = true;


            for (EasyServiceProxy<Object> proxy : mProxyMap.values()) {
                proxy.asyncProvide(null);
                if (proxy.isStartup() && !proxy.isShutdown()) {
                    proxy.shutdown();
                }
            }

            // proxy 和 consumer 全部清除
            mProxyMap.clear();
            mConsumerMap.clear();


        }
    }

    public void register(Class<? extends Object> serviceClass, BiConsumer consumer) {
        EasyServiceProxy<Object> proxy = mProxyMap.get(serviceClass);

        if (null == proxy) {
            if (!mFeatureManager.contains(serviceClass)) {
                throw new IllegalArgumentException("Given service["+serviceClass+"] have not in feature list");
            } else {
                proxy = mFeatureManager.create((Class<Object>) serviceClass, mAppContext);
                proxy.asyncProvide(mComponentConsumer);
                mProxyMap.put((Class<Object>) serviceClass, proxy);
            }
        }

        List<BiConsumer<Object, Class<Object>>> list = mConsumerMap.get(serviceClass);
        if (null == list) {
            list = new ArrayList<>();
            mConsumerMap.put((Class<Object>) serviceClass, list);
        }

        list.add(consumer);

        // 立即处理该依赖
        resolve((Class<Object>) serviceClass, consumer);
    }

    public void unregister(BiConsumer consumer) {

        Map<Class<Object>, List<BiConsumer<Object, Class<Object>>>> copy = new HashMap(mConsumerMap);
        for (Class<Object> serviceClass : mConsumerMap.keySet()) {
            List<BiConsumer<Object, Class<Object>>> list = copy.get(serviceClass);
            list.remove(consumer);

            if (0 == list.size()) {
                copy.remove(serviceClass);
                EasyServiceProxy<Object> proxy = mProxyMap.remove(serviceClass); // 这里删除了 service

                if (null != proxy) {
                    proxy.asyncProvide(null);
                    if (proxy.isStartup() && !proxy.isShutdown()) {
                        proxy.shutdown();
                    }
                }
            }
        }

        mConsumerMap.clear();
        mConsumerMap.putAll(copy); // 更新 consumer map
    }

    public void resolve(Class<? extends Object> serviceClass, BiConsumer consumer) {
        EasyServiceProxy<Object> proxy = mProxyMap.get(serviceClass);
        if (startupAlready && !proxy.isStartup()) {
            proxy.startup();
        }

        if (proxy.isStartup()) {
            Object service = proxy.provide();
            if (null != service) {
                consumer.accept(service, serviceClass);
            }
        }
    }

    @Override
    public <EasyService> void require(Class<EasyService> serviceClass, BiConsumer<EasyService, Class<EasyService>> consumer) {
        register((Class<Object>) serviceClass, consumer);
    }
}
