package me.zhennan.tool.easy.impl;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

class EasyComponentClient extends EasyComponent {

    private final Map<Class<Object>, List<BiConsumer<Object, Class<Object>>>> mConsumerMap = new HashMap<>();
    private final BiConsumer<Object, Class<Object>> mComponentConsumer = (service, serviceClass) -> {
        List<BiConsumer<Object, Class<Object>>> list = mConsumerMap.get(serviceClass);
        if (null != list && 0 < list.size()) {
            for (BiConsumer<Object, Class<Object>> consumer : list) {
                consumer.accept(service, serviceClass);
            }
        }
    };

    private Context mContext = null;
    private EasyComponentHost mHostComponent = null;

    private boolean startupAlready = false;
    private boolean shutdownAlready = false;


    public EasyComponentClient(Context context, EasyComponentHost host) {
        if (null == context || context == context.getApplicationContext()) {
            throw new IllegalArgumentException("invalid context");
        }

        this.mContext = context;

        if (null == host) {
            throw new IllegalArgumentException("Given host is Null");
        }

        this.mHostComponent = host;
    }

    private EasyComponentHost host() {
        return mHostComponent;
    }

    public boolean isStartup() {
        return startupAlready;
    }

    @Override
    public void startup() {
        if (!startupAlready && !shutdownAlready) {
            startupAlready = true;

            // 父组件先 startup 一下
            host().startup();

            // 如果这时候已经准备好了就处理一下依赖关系
            Set<Class<Object>> dependencies = mConsumerMap.keySet();
            for (Class<Object> dependency : dependencies) {
                host().resolve(dependency, mComponentConsumer);
            }
        }
    }

    public boolean isShutdown() {
        return shutdownAlready;
    }

    @Override
    public void shutdown() {
        if (startupAlready && !shutdownAlready) {
            shutdownAlready = true;
            startupAlready = false;

            // 解除依赖
            host().unregister(mComponentConsumer);

            // 清除需要清除的组件
            mConsumerMap.clear();
            mHostComponent = null;
            mContext = null;
        }
    }

    public void register(Class<? super Object> serviceClass, BiConsumer consumer) {
        if (shutdownAlready) {
            throw new IllegalStateException("Component is shutdown already");
        }

        List<BiConsumer<Object, Class<Object>>> list = mConsumerMap.get(serviceClass);
        if (null == list) {
            list = new ArrayList<>();
            mConsumerMap.put(serviceClass, list);
        }

        if (!list.contains(consumer)) {
            list.add(consumer);
        }

        host().register(serviceClass, mComponentConsumer);

        if (isStartup()) {
            host().resolve(serviceClass, mComponentConsumer);
        }
    }

    @Override
    public <EasyService> void require(Class<EasyService> serviceClass, BiConsumer<EasyService, Class<EasyService>> consumer) {
        register((Class<Object>) serviceClass, consumer);
    }
}
