package me.zhennan.tool.easy.tool;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.concurrent.Executor;

/**
 * 简单服务连接器
 *
 * 1. 支持配置化连接参数 {@link #newConnectionIntent(Context)} {@link #newConnectionFlags()}
 * 2. 支持定制化连接 {@link #makeConnectionManually(Context, ServiceConnection, Handler)} {@link #resetConnectionManually(Context, ServiceConnection)}
 * 3. 支持服务断开后的重连策略
 *
 * @param <Service> 目标服务类型
 */
public class EasyConnection<Service> {

    /**
     * 外部服务调用
     * @param <Service>
     */
    public interface Callback<Service> {
        void onConnectionMade(Service service);
        void onConnectionReset();
    }

    private final Handler mServiceHandler;
    private final ConnectionScheduler scheduler;
    private final ServiceConnection mRealConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            scheduler.reset();

            isServiceConnecting = false;
            isServiceConnected = true;

            // 执行连接业务
            internalConnectionMade(newServiceFromBinder(binder));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceConnecting = false;
            isServiceConnected = false;

            // 调度重连指令
            scheduler.scheduleRetryConnection();

            // 执行断开业务
            internalConnectionReset();
        }
    };


    private Context mContext = null;
    private boolean isServiceConnecting = false, isServiceConnected = false;
    private Service mCache = null;
    private Callback<Service> mCallback = null;


    public EasyConnection() {
        this(null);
    }

    public EasyConnection(Handler handler) {
        this.mServiceHandler = new Handler(null == handler ? Looper.getMainLooper() : handler.getLooper());
        this.scheduler = new ConnectionScheduler(this::makeConnection, this::resetConnection, this::makeConnection);
    }

    /**
     * @return 服务连接中标记
     */
    public final boolean isConnecting() {
        return isServiceConnecting;
    }

    /**
     * @return 服务已连接标记
     */
    public final boolean isConnected() {
        return isServiceConnected;
    }

    /**
     * @return 获取服务接口
     */
    public final Service service() {
        return mCache;
    }

    /**
     * 设置服务连接状态回调
     * @param callback
     */
    public final void callback(Callback<Service> callback) {
        this.mCallback = callback;
    }

    /**
     * 发起连接接口
     */
    public final void make(Context context) {
        if (null == context) {
            throw new NullPointerException("Given context is Null");
        }

        if (!isServiceConnecting && !isServiceConnected) {
            isServiceConnecting = true;

            this.mContext = context;

            // 调度连接服务指令
            scheduler.scheduleMakeConnection();
        }
    }

    /**
     * 重置连接接口
     */
    public final void reset() {
        if (isServiceConnecting || isServiceConnected) {

            // 调度断开服务指令
            scheduler.scheduleResetConnection();
        }
    }

    private void makeConnection() {

        try {
            isServiceConnecting = true;
            Intent intent = newConnectionIntent(mContext);
            if (null == intent) {
                makeConnectionManually(mContext, mRealConnection, mServiceHandler);
            } else {
                boolean serviceFound = mContext.bindService(intent, mRealConnection, newConnectionFlags());
                if (!serviceFound) {
                    throw new RuntimeException("Given intent["+intent+"] can not connect to service");
                }
            }
        } catch (Exception e) {
            isServiceConnecting = false;
            throw new IllegalStateException("Trying to make connection failed.", e);
        }

    }

    private void resetConnection() {
        boolean cacheConnected = isServiceConnected;
        boolean cacheConnecting = isServiceConnecting;

        try {
            isServiceConnecting = false;
            isServiceConnected = false;
            Intent intent = newConnectionIntent(mContext);
            if (null == intent) {
                resetConnectionManually(mContext, mRealConnection);
            } else {
                mContext.unbindService(mRealConnection);
            }

        } catch (Exception e) {
            isServiceConnecting = cacheConnecting;
            isServiceConnected = cacheConnected;
            throw new IllegalStateException("Trying to reset connection failed.", e);
        }

        // call connection to reset
        if (!isServiceConnecting && !isServiceConnected) {
            internalConnectionReset();
        }

        this.mContext = null;
    }


    private void internalConnectionMade(Service service) {
        mCache = service;

        // 内部回调
        onConnectionMade(service);

        // 外部回调
        if (null != mCallback) {
            mCallback.onConnectionMade(mCache);
        }
    }

    private void internalConnectionReset() {
        mCache = null;

        // 内部回调
        onConnectionReset();

        // 外部回调
        if (null != mCallback) {
            mCallback.onConnectionReset();
        }
    }

    @Override
    public String toString() {
        return "EasyConnection{isConnecting:"+isServiceConnecting+", isConnected:"+isServiceConnected+"}";
    }


    // -----------------------------------------------------------------------------------------
    //
    // 子类实现
    //
    //

    /**
     * 构造连接意图
     * @param context 上下文
     * @return 连接意图组件
     */
    protected Intent newConnectionIntent(Context context) {
        return null;
    }

    /**
     * @return 构造连接参数
     *
     * @see Context#BIND_AUTO_CREATE
     * @see Context#BIND_DEBUG_UNBIND
     * @see Context#BIND_NOT_FOREGROUND
     * @see Context#BIND_ABOVE_CLIENT
     * @see Context#BIND_ALLOW_OOM_MANAGEMENT
     * @see Context#BIND_WAIVE_PRIORITY
     * @see Context#BIND_IMPORTANT
     * @see Context#BIND_ADJUST_WITH_ACTIVITY
     * @see Context#BIND_NOT_PERCEPTIBLE
     * @see Context#BIND_INCLUDE_CAPABILITIES
     * @see Context#BIND_EXTERNAL_SERVICE
     */
    protected int newConnectionFlags() {
        return Context.BIND_AUTO_CREATE;
    }

    /**
     * 自定义服务连接入口. 如果不使用标准连流程 {@link Context#bindService(Intent, int, Executor, ServiceConnection)}
     * 需要在该方法内部构建连接.
     * @param context 上下文
     * @param connection 连接描述
     * @param handler 线程调度句柄
     */
    protected void makeConnectionManually(Context context, ServiceConnection connection, Handler handler) {
        throw new IllegalStateException("Looks like you haven't chose to use standard \"bind/unbind service\" Workflow. " +
                "so you need to override AndroidServiceConnection#makeConnectionManually(Context, ServiceConnection, Handler) and " +
                "AndroidServiceConnection#resetConnectionManually(Context, ServiceConnection) to apply your customized \"bind/unbind service\" workflow. ");
    }

    /**
     * 重置服务连接. 如果不使用标准流程 {@link Context#unbindService(ServiceConnection)}
     * 需要在该方法内部重置连接
     *
     * @param context 上下文
     * @param connection 连接描述
     */
    protected void resetConnectionManually(Context context, ServiceConnection connection) {
        throw new IllegalStateException("Looks like you haven't chose to use standard \"bind/unbind service\" Workflow. " +
                "so you need to override AndroidServiceConnection#makeConnectionManually(Context, ServiceConnection, Handler) and " +
                "AndroidServiceConnection#resetConnectionManually(Context, ServiceConnection) to apply your customized \"bind/unbind service\" workflow. ");
    }

    /**
     * 手动触发连接方法.
     *
     * 如果手动连接过程不使用给出的 ServiceConnection 的话可以通过该方法手动触发流程
     *
     * @param name
     * @param binder
     */
    protected final void triggerConnectionMadeManually(ComponentName name, IBinder binder) {
        mRealConnection.onServiceConnected(name, binder);
    }

    protected final void triggerConnectionMadeManually() {
        triggerConnectionMadeManually(null, null);
    }

    /**
     * 把Binder实例转换成服务接口
     * @param binder 目标 Binder 实例
     * @return 服务接口实例
     */
    @SuppressWarnings("unchecked")
    protected Service newServiceFromBinder(IBinder binder){
        try {
            return (Service) binder;
        } catch (ClassCastException e) {
            throw new IllegalStateException("cast binder to Service failed. you should override this method to return correct Service your want.", e);
        }
    }


    /**
     * 连接建立后的内部回调
     * @param service
     */
    protected void onConnectionMade(Service service) {}

    /**
     * 连接断开后的内部回调
     */
    protected void onConnectionReset() {}

    /**
     * 连接调度器 (包内部类)
     */
    private static class ConnectionScheduler {

        private final Handler scheduler;
        private final Runnable makeCommand, resetCommand, retryCommand;

        ConnectionScheduler(Runnable makeCommand, Runnable resetCommand, Runnable retryCommand) {

            if (null == makeCommand) {
                throw new NullPointerException("Given makeCommand is Null");
            }

            if (null == resetCommand) {
                throw new NullPointerException("Given resetCommand is Null");
            }

            this.makeCommand = makeCommand;
            this.resetCommand = resetCommand;
            this.retryCommand = retryCommand;
            this.scheduler = new Handler(Looper.getMainLooper());
        }

        private boolean isRunOnMainThread() {
            return scheduler.getLooper().isCurrentThread();
        }


        public void scheduleMakeConnection() {
            reset();

            if (isRunOnMainThread()) {
                makeCommand.run();
            } else {
                scheduler.post(makeCommand);
            }
        }

        public void scheduleResetConnection() {
            reset();

            if (isRunOnMainThread()) {
                resetCommand.run();
            } else {
                scheduler.post(resetCommand);
            }
        }

        public void scheduleRetryConnection() {
            reset();

            scheduler.postDelayed(retryCommand, 1000);
        }


        public void reset() {
            scheduler.removeCallbacks(makeCommand);
            scheduler.removeCallbacks(resetCommand);
            scheduler.removeCallbacks(retryCommand);
        }
    }
}
