package io.github.lczx.aml;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import io.github.lczx.aml.tunnel.AMLTunnelService;

public final class AML {

    private static Class<? extends AMLTunnelService> serviceClass = AMLTunnelService.class;

    private AML() { }

    public static void setServiceClass(final Class<AMLTunnelService> serviceClass) {
        AML.serviceClass = serviceClass;
    }

    public static void startTunnelService(final Context context) {
        startTunnelService(context, null);
    }

    public static void startTunnelService(final Context context, final String[] targetPackageNames) {
        context.startService(
                new Intent(AMLTunnelService.ACTION_START, null, context, serviceClass)
                        .putExtra(AMLTunnelService.EXTRA_TARGET_PACKAGES, targetPackageNames));
    }

    public static void stopTunnelService(final Context context) {
        context.startService(
                new Intent(AMLTunnelService.ACTION_STOP, null, context, serviceClass));
    }

    public static boolean isServiceRunning(final Context context) {
        final ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (final ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) return true;
        }
        return false;
    }

    public static void addStatusListener(/* ... */) {
        // TODO: Implement
    }

}
