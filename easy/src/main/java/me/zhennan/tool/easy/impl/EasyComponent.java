package me.zhennan.tool.easy.impl;

import me.zhennan.tool.easy.EasyResolver;

abstract class EasyComponent implements EasyResolver {

    /**
     * Component Startup
     */
    public abstract void startup();

    /**
     * Component Shutdown
     */
    public abstract void shutdown();
}
