package com.qjd.cc;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    public static void startFloatWindowPermissionErrorToast(Context context) {
        if (context != null)
            Toast.makeText(context, "进入设置页面失败,请手动开启悬浮窗权限", Toast.LENGTH_SHORT).show();
    }

    /**
     * 跳转界面
     *
     * @param context
     * @param intent
     * @return
     */
    private static boolean startSafely(Context context, Intent intent) {
        List<ResolveInfo> resolveInfos = null;
        try {
            resolveInfos = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfos != null && resolveInfos.size() > 0) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean manageDrawOverlaysForMiui(Context context) {
        Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
        intent.putExtra("extra_pkgname", context.getPackageName());
        intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity");
        if (startSafely(context, intent)) {
            return true;
        }
        intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity");
        if (startSafely(context, intent)) {
            return true;
        }
        // miui v5 的支持的android版本最高 4.x
        // http://www.romzj.com/list/search?keyword=MIUI%20V5#search_result
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Intent intent1 = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent1.setData(Uri.fromParts("package", context.getPackageName(), null));
            return startSafely(context, intent1);
        }
        return false;
    }

    public static void manageDrawOverlays(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                startFloatWindowPermissionErrorToast(context);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (!manageDrawOverlaysForMiui(context)) {
                startFloatWindowPermissionErrorToast(context);
            }
        }
    }

    private static boolean checkOp(Context context, int op) {
        AppOpsManager manager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        try {
            Method method = AppOpsManager.class.getDeclaredMethod("checkOp", int.class, int.class, String.class);
            return AppOpsManager.MODE_ALLOWED == (int) method.invoke(manager, op, Binder.getCallingUid(), context.getPackageName());
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * 检查悬浮窗权限  当没有权限，跳转到权限设置界面
     *
     * @param context          上下文
     * @param isShowDialog     没有权限，是否弹框提示跳转到权限设置界面
     * @param isShowPermission 是否跳转权限开启界面
     * @return true 有权限   false 没有权限（跳转权限界面、权限失败 提示用户手动设置权限）
     * @by 腾讯云直播 悬浮框判断逻辑
     */

    private static final int OP_SYSTEM_ALERT_WINDOW = 24;
    public static boolean canDrawOverlays(Context context, boolean isShowDialog, boolean isShowPermission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                if (isShowDialog) {
                    //去授权
                    manageDrawOverlays(context);
                } else if (isShowPermission) {
                    manageDrawOverlays(context);
                }
                return false;
            }
            return true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (checkOp(context, OP_SYSTEM_ALERT_WINDOW)) {
                return true;
            } else {
                if (isShowPermission)
                    startFloatWindowPermissionErrorToast(context);
                return false;
            }
        } else {
            return true;
        }
    }
    /**
     * 是否有悬浮框权限
     *
     * @return
     */
    public boolean requestPermission(Context context) {
        return canDrawOverlays(context, true, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context context = this;
        requestPermission(context);
        final SmallWindowsView smWin = new SmallWindowsView(getApplicationContext(), this);
        Button btn = findViewById(R.id.btn_showfloatwin);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                    /*if (!(Settings.canDrawOverlays(MainActivity.this))) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        return;
                    }*/

                smWin.show();
            }
        });
    }
}