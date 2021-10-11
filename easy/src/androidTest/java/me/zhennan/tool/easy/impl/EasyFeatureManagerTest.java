package me.zhennan.tool.easy.impl;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import me.zhennan.tool.easy.EasyFeature;
import me.zhennan.tool.easy.EasyServiceProxy;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class EasyFeatureManagerTest {


    @Test
    public void testManager() {

        MockService MOCK_SERVICE = mock(MockService.class);

        EasyServiceProxy<MockService> MOCK_PROXY = mock(EasyServiceProxy.class);
        when(MOCK_PROXY.provide()).thenReturn(MOCK_SERVICE);

        EasyFeature MOCK_FEATURE = mock(EasyFeature.class);
        when(MOCK_FEATURE.contains(same(MockService.class))).thenReturn(true);
        when(MOCK_FEATURE.create(same(MockService.class), any())).thenReturn(MOCK_PROXY);

        EasyFeatureManager target = new EasyFeatureManager();
        target.use(MOCK_FEATURE);

        assertTrue(target.contains(MockService.class));

        EasyServiceProxy<MockService> proxy = target.create(MockService.class, null);
        assertEquals(proxy, MOCK_PROXY);

        verify(MOCK_FEATURE, atLeast(1)).contains(same(MockService.class));
        verify(MOCK_FEATURE, times(1)).create(same(MockService.class), any());
    }

    class MockService {}


}

