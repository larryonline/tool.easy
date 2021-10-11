package me.zhennan.tool.easy;

import java.util.function.BiConsumer;

/**
 * 简单接口依赖解决器
 */
public interface EasyResolver {

    /**
     * 服务依赖解决接口
     * @param serviceClass 服务类型
     * @param consumer 依赖解决回调接口
     * @param <EasyService> 服务类型定义
     */
    <EasyService> void require(Class<EasyService> serviceClass, BiConsumer<EasyService, Class<EasyService>> consumer);
}
