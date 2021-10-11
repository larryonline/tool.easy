package me.zhennan.tool.easy;

import java.util.function.BiConsumer;

/**
 * Easy服务代理
 * @param <Service> 被代理类型
 */
public interface EasyServiceProxy<Service> {

    /**
     * @return 直接获取代理服务
     */
    Service provide();

    /**
     * 异步获取被代理的服务
     *
     * 因为有些服务可能会存在IPC通讯.
     * 存在断开链接的情况. 如果该情况发生
     * 这时候 consumer 会被调用. 被传入的参数分别是 consumer(null, Class<Service>)
     *
     * EasyServiceProxy在同一时间只接受一个 consumer
     *
     * @param consumer 服务消费者
     */
    void asyncProvide(BiConsumer<Service, Class<Service>> consumer);


    /**
     * @return 服务代理的启动状态: true(已启动) / false(未启动)
     */
    boolean isStartup();

    /**
     * 启动服务
     */
    void startup();

    /**
     * @return 服务代理的关闭状态: true(已关闭) / false(未关闭)
     */
    boolean isShutdown();

    /**
     * 关闭服务
     */
    void shutdown();
}
