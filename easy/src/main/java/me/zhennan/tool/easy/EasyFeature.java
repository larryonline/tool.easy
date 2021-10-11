package me.zhennan.tool.easy;

import android.content.Context;

/**
 * Easy模块的功能包接口
 *
 * 开发者可以为 Easy 模块开发功能包.
 * 使得 Easy 模块可以变得更强大
 */
public interface EasyFeature {

    /**
     * 检查当前 Feature 是否提供目标服务
     * @param serviceClass 服务类型
     * @param <EasyService> 服务类定义
     * @return
     */
    <EasyService> boolean contains(Class<EasyService> serviceClass);

    /**
     * 构造目标类型的服务
     * @param serviceClass 服务类型
     * @param context 上下文
     * @param <EasyService> 服务类型定义
     * @return 获取通过 EasyProxy 代理的目标服务实例
     */
    <EasyService> EasyServiceProxy<EasyService> create(Class<EasyService> serviceClass, Context context);
}
