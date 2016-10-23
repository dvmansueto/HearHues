package net.dvmansueto.hearhues;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility methods.
 * @author Dave
 * Based on http://stackoverflow.com/a/28427963
 */
class Utils {

    private static final String PREFERENCE_NAME = "net.dvmansueto.hearhues_preferences";

    /**
     * Sets the specified key to the specified value in the default Shared Preferences file.
     * @param context application context.
     * @param key the name of the shared preference to change.
     * @param value the new value to apply.
     */
    static void setStringPreference( Context context, String key, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences( PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString( key, value);
        editor.apply();
    }

    /**
     * Fetches the specified string from the default Shared Preferences file.
     * @param context application context.
     * @param key the name of the shared preference to fetch the value of.
     * @param defaultValue the value to return if this shared preference does not exist.
     * @return the value of the shared preference if it exists, else the default value.
     */
    static String getStringPreference( Context context, String key, String defaultValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences( PREFERENCE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString( key, defaultValue);
    }

}