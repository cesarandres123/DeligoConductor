package com.vecolsoft.deligo_conductor.Utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;

import com.vecolsoft.deligo_conductor.R;
import com.vecolsoft.deligo_conductor.Servicio.MyServicio;

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
