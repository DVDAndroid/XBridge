package daylemk.xposed.xbridge.action;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import daylemk.xposed.xbridge.data.IntegerBox;
import daylemk.xposed.xbridge.data.MainPreferences;
import daylemk.xposed.xbridge.hook.AppInfoHook;
import daylemk.xposed.xbridge.hook.Hook;
import daylemk.xposed.xbridge.hook.StatusBarHook;
import daylemk.xposed.xbridge.utils.Log;
import de.robv.android.xposed.XSharedPreferences;

/**
 * @author DayLemK
 * @version 1.0
 *          28-四月-2015 9:16:48
 */
public class PlayAction extends Action {
    public static final String TAG = "PlayAction";
    public static final String STR_VIEW_IN_PLAY_STORE = "View in Play Store";
    public static final String PKG_NAME_PLAY_STORE = "com.android.vending";

    public static final String CLASS_NAME = PlayAction.class.getCanonicalName();
    public static final String PREF_SHOW_IN_RECENT_TASK = MainPreferences.PREF_SHOW_IN_RECENT_TASK +
            CLASS_NAME;
    public static final boolean PREF_SHOW_IN_RECENT_TASK_DEFAULT = true;
    public static final String PREF_SHOW_IN_STATUS_BAR = MainPreferences.PREF_SHOW_IN_STATUS_BAR +
            CLASS_NAME;
    public static final boolean PREF_SHOW_IN_STATUS_BAR_DEFAULT = true;
    public static final String PREF_SHOW_IN_APP_INFO = MainPreferences.PREF_SHOW_IN_APP_INFO +
            CLASS_NAME;
    public static final boolean PREF_SHOW_IN_APP_INFO_DEFAULT = true;
    public static boolean isShowInRecentTask = true;
    public static boolean isShowInStatusBar = true;
    public static boolean isShowInAppInfo = true;
    // just need to init the icon and listener once
    // EDIT: maybe the icon is already one instance in the system
    public static Drawable sIcon = null;
    //public static View.OnClickListener sOnClickListener = null;
    // EDIT: the on click listener should be different

    public static void loadPreference(XSharedPreferences preferences) {
        isShowInRecentTask = preferences.getBoolean(PREF_SHOW_IN_RECENT_TASK,
                PREF_SHOW_IN_RECENT_TASK_DEFAULT);
        isShowInStatusBar = preferences.getBoolean(PREF_SHOW_IN_STATUS_BAR,
                PREF_SHOW_IN_STATUS_BAR_DEFAULT);
        isShowInAppInfo = preferences.getBoolean(PREF_SHOW_IN_APP_INFO,
                PREF_SHOW_IN_APP_INFO_DEFAULT);
    }

    @Override
    protected Drawable getIcon(PackageManager packageManager) {
        // check the icon. if good, just return.
        // TODO: check if the app is just install or upgrade and the icon should be changed
        if (sIcon == null) {
//        Drawable pkgIcon = null;
            try {
                sIcon = packageManager.getApplicationIcon(PKG_NAME_PLAY_STORE);
                Log.d(TAG, "play store icon is found: " + sIcon);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "play store icon can't be found, use generic icon");
                // app is gone, just show package name and generic icon
                sIcon = packageManager.getDefaultActivityIcon();
            }
        } else {
            Log.d(TAG, "icon is ok, no need to create again");
        }
        return sIcon;
    }

    @Override
    public String getMenuTitle() {
        return STR_VIEW_IN_PLAY_STORE;
    }

    @Override
    protected Intent getIntent(Hook hook, String pkgName) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://play.google.com/store/apps/details?id="
                + pkgName));
        Log.d(TAG, "set intent finished: " + intent);
        return intent;
    }
}