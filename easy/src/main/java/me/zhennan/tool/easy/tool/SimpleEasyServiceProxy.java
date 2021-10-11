package me.zhennan.tool.easy.tool;

import android.content.Context;

import java.util.function.BiConsumer;

import me.zhennan.tool.easy.EasyServiceProxy;

public class SimpleEasyServiceProxy<T> implements EasyServiceProxy<T> {

    private final Context context;
    private final EasyConnection<T> connection;
    private final Class<T> serviceClass;

    public SimpleEasyServiceProxy(Context context, Class<T> serviceClass, EasyConnection<T> connection){
        if (null == connection) {
            throw new IllegalArgumentException("Given connection is null");
        }

        if (null == context) {
            throw new IllegalArgumentException("Given context is Null");
        }

        if (null == serviceClass) {
            throw new IllegalArgumentException("Given service class is Null");
        }

        this.connection = connection;
        this.context = context;
        this.serviceClass = serviceClass;
    }

    @Override
    public T provide() {
        return connection.service();
    }

    @Override
    public void asyncProvide(BiConsumer<T, Class<T>> consumer) {
        connection.callback(null == consumer ? null : new EasyConnection.Callback<T>() {
            @Override
            public void onConnectionMade(T service) {
                consumer.accept(service, serviceClass);
            }

            @Override
            public void onConnectionReset() {
                consumer.accept(null, serviceClass);
            }
        });
    }

    public boolean isStartup() {
        return connection.isConnected() || connection.isConnecting();
    }

    public boolean isShutdown() {
        return !connection.isConnected() && !connection.isConnecting();
    }

    public void startup() {
        if (!connection.isConnected() && !connection.isConnecting()) {
            connection.make(context);
        }
    }

    public void shutdown() {
        if (connection.isConnecting() || connection.isConnected()) {
            connection.reset();
        }
    }

}
