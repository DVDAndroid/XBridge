package daylemk.xposed.xbridge.action;

import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import daylemk.xposed.xbridge.data.IntegerBox;
import daylemk.xposed.xbridge.hook.AppInfoHook;
import daylemk.xposed.xbridge.hook.Hook;
import daylemk.xposed.xbridge.hook.StatusBarHook;
import daylemk.xposed.xbridge.utils.Log;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author DayLemK
 * @version 1.0
 *          28-四月-2015 9:16:48
 */
public abstract class Action {
    public static final String TAG = "Action";
    /**
     * The map contain the action view id *
     */
    private static Map<Class<? extends Action>, Integer> viewIdMap = new HashMap<>();

    /** load all action related preference */
    public static void loadPreference (XSharedPreferences preferences){
        PlayAction.loadPreference(preferences);
    }

    public static boolean isNeed2Add(ViewGroup viewGroup, Class<? extends Action> actionClass) {
        Log.d(TAG, "the check map class is: " + actionClass);
        if (viewIdMap.containsKey(actionClass)) {
            // the key is here
            int viewId = viewIdMap.get(actionClass);
            if (viewGroup.findViewById(viewId) != null) {
                // the view is here too
                // no need to add
                Log.d(TAG, "this view is already added");
                return false;
            } else {
                // this layout does not contain this view, need to add it
                return true;
            }
        } else {
            // the id is not here, so we generate it and put it in the map
            viewIdMap.put(actionClass, View.generateViewId());
            // --------- debug map content begin ---------
            Log.d(TAG, "map,keys: " + viewIdMap.keySet());
            Set<Class<? extends Action>> keySet = viewIdMap.keySet();
            Iterator<Class<? extends Action>> iterator = keySet.iterator();
            Class<? extends Action> key;
            int value;
            StringBuilder sb = new StringBuilder();
            while (iterator.hasNext()) {
                key = iterator.next();
                value = viewIdMap.get(key);
                sb.append(key).append(" - ").append(value).append(",");
            }
            Log.d(TAG, "map: " + sb.toString());
            // --------- debug map content end ---------
            return true;
        }
    }

    /**
     * get the view id of this action, we need make it public, so we can get the previous view
     * from layout
     */
    public static int getViewId(Class<? extends Action> actionClass) {
        if (!viewIdMap.containsKey(actionClass)) {
            Log.w(TAG, "the map doesn't contain: " + actionClass);
        }

        return viewIdMap.get(actionClass);
    }

    private Intent getFinalIntent(Hook hook, String pkgName) {
        Intent intent = getIntent(hook, pkgName);
        // this is just need for appInfo screen for now, not in status bar
        if (hook instanceof AppInfoHook) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        }
        return intent;
    }


    protected abstract Intent getIntent(Hook hook, String pkgName);

    /**
     * get the icon of this action
     */
    protected abstract Drawable getIcon(PackageManager packageManager);

    /**
     * get the String that represent this action, recommend overwrite this in the subclass
     */
    public String getMenuTitle() {
        return "";
    }

    public void setAction(final Hook hook, final Context context,
                          final String pkgName,
                          View view) {
//        super.setAction(hook, context, pkgName, imageButton);
        PackageManager packageManager = context.getPackageManager();
        if (view instanceof ImageButton) {
            ((ImageButton) view).setImageDrawable(getIcon(packageManager));
        } else if (view instanceof ImageView) {
            ((ImageView) view).setImageDrawable(getIcon(packageManager));
        }
        view.setContentDescription(getMenuTitle());
        // set the button id first
        // the button id can be the same within the different notification
        view.setId(getViewId(Action.this.getClass()));
        Log.d(TAG, "set click action, pkg: " + pkgName);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "on click play action, hook: " + hook);
                Intent intent = getFinalIntent(hook, pkgName);
                if (hook instanceof StatusBarHook) {
                    // dismiss the keyguard adn collapse panels
                    ((StatusBarHook) hook).dismissKeyguardAndStartIntent(Action.this, intent,
                            pkgName);
                } else {
                    // just start activity as user
                    startIntentAsUser(context, intent, pkgName);
//                    context.startActivity(intent);
                }

                Log.d(TAG, "play action done");
            }
        });
    }

    public void setAction(final Hook hook, final Context context,
                          final String pkgName,
                          MenuItem menuItem) {
        // set the show flag
        menuItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        // set the icon
        menuItem.setIcon(getIcon(context.getPackageManager()));
        Log.d(TAG, "set click action, pkg: " + pkgName);
        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Log.d(TAG, "on click play action, hook: " + hook);
                Intent intent = getFinalIntent(hook, pkgName);
                if (hook instanceof AppInfoHook) {
                    // no need to start as user, 'cause when the appInfo screen is called, it
                    // already signed to a user
                    context.startActivity(intent);
                }

                Log.d(TAG, "play action done");
                return true;
            }
        });
    }

    /**
     * start intent as user
     * @param context context
     * @param intent the Action intent
     * @param pkgName clicked package name
     */
    public void startIntentAsUser(Context context, Intent intent, String pkgName) {
        startIntentAsUser(context, intent, getUid(context, pkgName));
    }

    public void startIntentAsUser(Context context, Intent intent, int appUid) {
        Log.d(TAG, "start intent as user, appUid: " + appUid + ", " + intent);
        // TODO: get ride of TaskStackBuilder ???
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create
                (context).addNextIntentWithParentStack
                (intent);
        Log.d(TAG, "taskStackBuilder: " + taskStackBuilder);

        int userId = (int) XposedHelpers.callStaticMethod(UserHandle
                .class, "getUserId", appUid);
        Log.d(TAG, "userId: " + userId);

        Object userHandle = null;
        try {
            // here should use int.class
            Constructor<?> userHandleConstructor = UserHandle.class
                    .getDeclaredConstructor(int.class);
            userHandleConstructor.setAccessible(true);
            userHandle = userHandleConstructor.newInstance(userId);
        } catch (Exception e) {
            e.printStackTrace();
            XposedBridge.log(e);
        }
        Log.d(TAG, "userHandle: " + userHandle);

        // call startActivities method
        // FIXED: should use the empty Bundle object not null
        XposedHelpers.callMethod(taskStackBuilder, "startActivities",
                new Bundle(), userHandle);
        Log.d(TAG, "start activities");
    }

    private int getUid(Context context, String pkgName) {
        // get the appUid
        int appUid = -1;
        try {
            final ApplicationInfo info = context.getPackageManager().getApplicationInfo(pkgName,
                    PackageManager.GET_UNINSTALLED_PACKAGES
                            | PackageManager.GET_DISABLED_COMPONENTS);
            if (info != null) {
                appUid = info.uid;
                Log.d(TAG, "uid is: " + appUid + ", appInfo: " + info);
            }
        } catch (Exception e) {
            // output log
            XposedBridge.log(e);
        }
        return appUid;
    }
}