package me.zhennan.tool.easy.mock;

import android.os.Binder;

public class SimpleServiceAPI extends Binder {


    public String ping() {
        return "PONG";
    }
}
