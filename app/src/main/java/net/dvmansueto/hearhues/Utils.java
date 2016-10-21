package net.dvmansueto.hearhues;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by Dave on 21/10/16.
 * Based on http://stackoverflow.com/a/28427963
 */

public class Utils {

    private static final String TAG = "Utils";

    private static final String PREFERENCE_NAME = "net.dvmansueto.hearhues_preferences";

    public static void setStringPreference(Context context, String value, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString( key, value);
        editor.commit();
    }


    public static void setStringPreference(Context context, String value, String key, String prefName) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String getStringPreference(Context context, String defaultValue, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences( PREFERENCE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString( key, defaultValue);
    }

    public static String getStringPreference(Context context, String defaultValue, String key, String prefName) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        String temp = sharedPreferences.getString(key, defaultValue);
        return temp;
    }
}