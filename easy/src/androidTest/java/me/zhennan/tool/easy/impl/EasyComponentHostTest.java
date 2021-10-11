package me.zhennan.tool.easy.impl;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.function.BiConsumer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import me.zhennan.tool.easy.EasyServiceProxy;

@RunWith(AndroidJUnit4.class)
public class EasyComponentHostTest {

    /**
     * EasyComponentHost的实例 依赖 APK 的 ApplicationContext 和 一个 EasyFeatureManager
     */
    @Test
    public void testInstantiation() {
        Context MOCK_CONTEXT = InstrumentationRegistry.getInstrumentation().getTargetContext();
        EasyFeatureManager MOCK_MANAGER = Mockito.mock(EasyFeatureManager.class);


        // TEST START ---------------------------------------------

        try {
            // 不传 context 报错
            new EasyComponentHost(null, MOCK_MANAGER);
            fail("创建 EasyComponentHost 的时候应该报错. 因为传入了空的 ApplicationContext");
        } catch (IllegalArgumentException ignored) { }

        try {
            // 传入的 Context 不是 application context 也报错
            new EasyComponentHost(MOCK_CONTEXT, MOCK_MANAGER);
            fail("创建 EasyComponentHost 的时候应该报错. 因为传入的Context不是ApplicationContext");
        } catch (IllegalArgumentException ignored) { }

        try {
            // 传入空的 EasyFeatureManager 报错
            new EasyComponentHost(MOCK_CONTEXT, null);
            fail("创建 EasyComponentHost 的时候应该报错. 因为传入了空的 EasyFeatureManager 参数");
        } catch (IllegalArgumentException ignored) {}

        // ------------------------------------------------ TEST END
    }

    /**
     * EasyComponentHost 会维护好自己的生命周期
     */
    @Test
    public void testLifeCycle() {
        Context MOCK_CONTEXT = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context MOCK_APP_CONTEXT = MOCK_CONTEXT.getApplicationContext();
        EasyFeatureManager MOCK_FEATURE_MANAGER = mock(EasyFeatureManager.class);

        EasyComponentHost target = new EasyComponentHost(MOCK_APP_CONTEXT, MOCK_FEATURE_MANAGER);
        assertFalse(target.isStartup());
        assertFalse(target.isShutdown());

        target.startup();
        assertTrue(target.isStartup());
        assertFalse(target.isShutdown());

        target.shutdown();
        assertFalse(target.isStartup());
        assertTrue(target.isShutdown());


        // 再次启动会失败. 因为之前已经 shutdown 过了
        target.startup();
        assertFalse(target.isStartup());
        assertTrue(target.isShutdown());
    }

    /**
     * EasyComponentHost 会同步所有 EasyServiceProxy 实例的生命周期
     */
    @Test
    public void testRequireWithLifecycle() {
        Context MOCK_CONTEXT = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context MOCK_APP_CONTEXT = MOCK_CONTEXT.getApplicationContext();
        MockService MOCK_SERVICE = Mockito.mock(MockService.class);
        EasyServiceProxy<MockService> MOCK_PROXY = (EasyServiceProxy<MockService>) Mockito.mock(EasyServiceProxy.class);
        Mockito.when(MOCK_PROXY.provide()).thenReturn(MOCK_SERVICE);

        EasyFeatureManager MOCK_MANAGER = Mockito.mock(EasyFeatureManager.class);
        Mockito.when(MOCK_MANAGER.create(MockService.class, MOCK_APP_CONTEXT)).thenReturn(MOCK_PROXY);
        Mockito.when(MOCK_MANAGER.contains(MockService.class)).thenReturn(true);


        // TEST START ---------------------------------------------
        EasyComponentHost target = new EasyComponentHost(MOCK_APP_CONTEXT, MOCK_MANAGER);
        target.startup();
        target.shutdown();
        // ------------------------------------------------ TEST END

        // 如果没有需要处理的依赖. 那么 EasyFeatureManager 的方法不会被调用
        Mockito.verify(MOCK_MANAGER, Mockito.never()).contains(Mockito.any());
        Mockito.verify(MOCK_MANAGER, Mockito.never()).create(Mockito.any(), Mockito.any());
        Mockito.verify(MOCK_MANAGER, Mockito.never()).use(Mockito.any());

        // TEST START ---------------------------------------------
        target = new EasyComponentHost(MOCK_APP_CONTEXT, MOCK_MANAGER);
        target.require(MockService.class, (obj, clazz) -> {
            // 注册依赖
        });

        target.startup(); // 启动 host
        // ------------------------------------------------ TEST END

        // 被注册的 MOCK_PROXY 的生命周期与 EasyComponentHost 同步
        Mockito.verify(MOCK_PROXY, Mockito.atLeast(1)).isStartup();
        Mockito.verify(MOCK_PROXY, Mockito.times(1)).startup();


        Mockito.when(MOCK_PROXY.isStartup()).thenReturn(true); // 已经 startup 过了

        // TEST START ---------------------------------------------
        target.shutdown(); // 关闭 host
        // ------------------------------------------------ TEST END

        Mockito.verify(MOCK_PROXY, Mockito.atLeast(1)).isShutdown();
        Mockito.verify(MOCK_PROXY, Mockito.times(1)).shutdown();
    }

    @Test
    public void testRegisterUnregister()  {
        Context MOCK_CONTEXT = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context MOCK_APP_CONTEXT = MOCK_CONTEXT.getApplicationContext();
        MockService MOCK_SERVICE = Mockito.mock(MockService.class);
        EasyServiceProxy<MockService> MOCK_PROXY = (EasyServiceProxy<MockService>) Mockito.mock(EasyServiceProxy.class);
        Mockito.when(MOCK_PROXY.provide()).thenReturn(MOCK_SERVICE);

        EasyFeatureManager MOCK_MANAGER = Mockito.mock(EasyFeatureManager.class);
        Mockito.when(MOCK_MANAGER.create(MockService.class, MOCK_APP_CONTEXT)).thenReturn(MOCK_PROXY);
        Mockito.when(MOCK_MANAGER.contains(MockService.class)).thenReturn(true);

        BiConsumer<MockService, Class<MockService>> MOCK_CONSUMER = Mockito.mock(BiConsumer.class);


        // TEST START ---------------------------------------------
        EasyComponentHost target = new EasyComponentHost(MOCK_APP_CONTEXT, MOCK_MANAGER);

        target.register(MockService.class, MOCK_CONSUMER);
        // ------------------------------------------------ TEST END

        Mockito.verify(MOCK_PROXY, Mockito.never()).startup();
        Mockito.verify(MOCK_PROXY, Mockito.never()).provide();


//        Mockito.when(MOCK_PROXY.isStartup()).thenReturn(true);
        // TEST START ---------------------------------------------
        target = new EasyComponentHost(MOCK_APP_CONTEXT, MOCK_MANAGER);
        target.startup();

        target.register(MockService.class, MOCK_CONSUMER);
        // ------------------------------------------------ TEST END

        Mockito.verify(MOCK_PROXY, Mockito.times(1)).startup();
//        Mockito.verify(MOCK_PROXY, Mockito.times(1)).provide();

        Mockito.reset(MOCK_PROXY, MOCK_CONSUMER);
        // TEST START ---------------------------------------------
        target = new EasyComponentHost(MOCK_APP_CONTEXT, MOCK_MANAGER);
        target.startup();

        target.register(MockService.class, MOCK_CONSUMER);
        target.unregister(MOCK_CONSUMER);
        // ------------------------------------------------ TEST END

//        Mockito.verify(MOCK_PROXY, Mockito.times(1)).provide();
        Mockito.verify(MOCK_PROXY, Mockito.times(1)).asyncProvide(Mockito.any(BiConsumer.class));
        Mockito.verify(MOCK_PROXY, Mockito.times(1)).startup();
//        Mockito.verify(MOCK_PROXY, Mockito.times(1)).shutdown();


    }

    @Test
    public void testRequire() {
        Context MOCK_CONTEXT = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context MOCK_APP_CONTEXT = MOCK_CONTEXT.getApplicationContext();
        MockService MOCK_SERVICE = Mockito.mock(MockService.class);
        EasyServiceProxy<MockService> MOCK_PROXY = (EasyServiceProxy<MockService>) Mockito.mock(EasyServiceProxy.class);
        Mockito.when(MOCK_PROXY.provide()).thenReturn(MOCK_SERVICE);

        EasyFeatureManager MOCK_MANAGER = Mockito.mock(EasyFeatureManager.class);
        Mockito.when(MOCK_MANAGER.create(MockService.class, MOCK_APP_CONTEXT)).thenReturn(MOCK_PROXY);
        Mockito.when(MOCK_MANAGER.contains(MockService.class)).thenReturn(true);

        BiConsumer<MockService, Class<MockService>> MOCK_CONSUMER = Mockito.mock(BiConsumer.class);

        // TEST START ---------------------------------------------
        EasyComponentHost target = new EasyComponentHost(MOCK_APP_CONTEXT, MOCK_MANAGER);

        target.require(MockService.class, MOCK_CONSUMER);
        // ------------------------------------------------ TEST END

        // 并没有被调用过
        Mockito.verify(MOCK_CONSUMER, Mockito.never()).accept(Mockito.same(MOCK_SERVICE), Mockito.same(MockService.class));
        Mockito.when(MOCK_PROXY.isStartup()).thenReturn(true); // PROXY 启动状态


        // TEST START ---------------------------------------------
        target = new EasyComponentHost(MOCK_APP_CONTEXT, MOCK_MANAGER);
        target.startup();


        target.require(MockService.class, MOCK_CONSUMER);
        // ------------------------------------------------ TEST END

        Mockito.verify(MOCK_CONSUMER, Mockito.times(1)).accept(Mockito.same(MOCK_SERVICE), Mockito.same(MockService.class));
    }

    class MockService { }
}
