package me.zhennan.tool.easy.impl;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.function.BiConsumer;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class EasyComponentClientTest {



    /**
     * EasyComponentClient的实例 依赖所在Android组件(Activity / Service)的 Context 和 EasyComponentHost
     */
    @Test
    public void testInstantiation() {
        EasyComponentHost MOCK_HOST = mock(EasyComponentHost.class);
        Context MOCK_CONTEXT = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context MOCK_APP_CONTEXT = MOCK_CONTEXT.getApplicationContext();

        try {
            new EasyComponentClient(null, MOCK_HOST);
            fail("实例化时, context 参数不允许空");
        } catch (IllegalArgumentException ignored) {}

        try {
            new EasyComponentClient(MOCK_APP_CONTEXT, MOCK_HOST);
            fail("实例化时, context 不允许为 Application Context");
        } catch (IllegalArgumentException ignored) {}

        try {
            new EasyComponentClient(MOCK_CONTEXT, null);
            fail("实例化时, host 参数不允许空");
        } catch (IllegalArgumentException ignored) {}


        try {
            new EasyComponentClient(MOCK_CONTEXT, MOCK_HOST);
        } catch (Exception e) {
            fail("正常实例化不能抛出任何异常: " + e.toString());
            e.printStackTrace();
        }
    }

    @Test
    public void testLifeCycle() {
        Context MOCK_CONTEXT = InstrumentationRegistry.getInstrumentation().getTargetContext();
        EasyComponentHost MOCK_HOST = mock(EasyComponentHost.class);

        EasyComponentClient target = new EasyComponentClient(MOCK_CONTEXT, MOCK_HOST);
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

    @Test
    public void testChildrenManagementWithLifeCycle() {
        Context MOCK_CONTEXT = InstrumentationRegistry.getInstrumentation().getTargetContext();
        EasyComponentHost MOCK_HOST = mock(EasyComponentHost.class);
        BiConsumer<MockService, Class<MockService>> MOCK_CONSUMER = mock(BiConsumer.class);

        EasyComponentClient target = new EasyComponentClient(MOCK_CONTEXT, MOCK_HOST);
        target.require(MockService.class, MOCK_CONSUMER);

        verify(MOCK_HOST, times(1)).register(same(MockService.class), any(BiConsumer.class));

        verify(MOCK_HOST, never()).unregister(any(BiConsumer.class));
        target.startup();

        target.shutdown();
        verify(MOCK_HOST, times(1)).unregister(any(BiConsumer.class));
    }

    @Test
    public void testRequireWithLifeCycle() {
        Context MOCK_CONTEXT = InstrumentationRegistry.getInstrumentation().getTargetContext();
        EasyComponentHost MOCK_HOST = mock(EasyComponentHost.class);
        BiConsumer<MockService, Class<MockService>> MOCK_CONSUMER = mock(BiConsumer.class);

        EasyComponentClient target = new EasyComponentClient(MOCK_CONTEXT, MOCK_HOST);
        target.require(MockService.class, MOCK_CONSUMER);

        verify(MOCK_HOST, never()).resolve(same(MockService.class), any(BiConsumer.class));
        verify(MOCK_HOST, times(1)).register(same(MockService.class), any(BiConsumer.class));

        target.startup();
        verify(MOCK_HOST, times(1)).resolve(same(MockService.class), any(BiConsumer.class));
    }




    class MockService {}

}
