package com.vecolsoft.deligo_conductor.Utils;

import android.content.SharedPreferences;

public class Utils {

    public static boolean getValuePreference(SharedPreferences prefs) {
        return prefs.getBoolean("estado_switch", false);
    }

    public static void removesharedpreferencies(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("estado_switch");
        editor.apply();
    }

}
