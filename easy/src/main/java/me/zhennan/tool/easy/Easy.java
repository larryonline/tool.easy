package me.zhennan.tool.easy;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Context;

import me.zhennan.tool.easy.impl.EasyFacade;

/**
 * Easy Singleton
 */
public class Easy {

    private static final EasyFacade facade = new EasyFacade();

    public static void use(EasyFeature feature) {
        facade.use(feature);
    }

    public static void startup(Application app) {
        facade.startup(app);
    }

    public static void startup(Activity activity) {
        facade.startup(activity);
    }

    public static void shutdown(Activity activity) {
        facade.shutdown(activity);
    }

    public static void startup(Service service) {
        facade.startup(service);
    }

    public static void shutdown(Service service) {
        facade.shutdown(service);
    }

    public static EasyResolver from(Context context) {
        return facade.from(context);
    }
}
