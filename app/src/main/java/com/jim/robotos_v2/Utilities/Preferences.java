package com.jim.robotos_v2.Utilities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jim on 25/1/2015.
 */
public class Preferences {

    /*AVAILABLE PREFERENCES
    *
    * //obstacle avoidance
    * OBSTACLE_AVOIDING_BEARING_ERROR_RANGE_DEGREES /// Moires pou prostithontai stin gwnia apofigis empodiou se periptwsi lathous
    * OBSTACLE_AVOIDING_MAP_ERROR_METERS /// metra pou prostithontai stin apostasi apofigis tou empodiou logo pithanis asimfwnias xarti pragmatikotitas
    * OBSTACLE_DETECTION_RANGE /// sta posa metra thewrw oti exw anixneusei ena empodio
    *
    * BEARING_RANGE /// Moires +/- tis gwnias stin opoia vriskete to epomeno simeio stin diadromi
    * DISTANCE_ERROR_RANGE /// Metra apoklisis tou xarti me tin pragmatikotita gia na theorisw oti exw episkeftei ena shmeio
    *
    * COMMUNICATION_LOOP_REPEAT_TIME /// xronos gia to pote to Thread pou apofasizei tin poreia prepei na ekteleite
    * BOTTOM_LINE_VALUE /// to ipsos tis grammis sto Color detection
    * DISTANCE_TO_STOP_FROM_OBSTACLE_CM /// Color detection activity distance to stop if an a object is in front of the sensor
    *
    * */






    @SuppressLint("LongLogTag")
    static public void savePrefsString(String toBeSaved, String valueToBeSaved,
                                       Context context) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString(toBeSaved, valueToBeSaved);
        edit.commit();
      //  Log.e("Execute Preference Command", "SAVE STRING PREFERENCE '" + toBeSaved + "' WITH VALUE '" + valueToBeSaved + "'");
    }

    @SuppressLint("LongLogTag")
    static public void savePrefsInt(String toBeSaved, int valueToBeSaved,
                                    Context context) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putInt(toBeSaved, valueToBeSaved);
        edit.commit();
     //   Log.e("Execute Preference Command", "SAVE INT PREFERENCE '" + toBeSaved + "' WITH VALUE '" + valueToBeSaved + "'");
    }

    @SuppressLint("LongLogTag")
    static public void savePrefsFloat(String toBeSaved, float valueToBeSaved,
                                      Context context) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putFloat(toBeSaved, valueToBeSaved);
        edit.commit();
      //  Log.e("Execute Preference Command", "SAVE FLOAT PREFERENCE '" + toBeSaved + "' WITH VALUE '" + valueToBeSaved + "'");
    }

    @SuppressLint("LongLogTag")
    static public float loadPrefsFloat(String name, float defaultValue, Context context) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        float value = sharedPreferences.getFloat(name, defaultValue);
      //  Log.e("Execute Preference Command", "LOAD FLOAT PREFERENCE '" + name + "' WITH VALUE '" + value + "'");
        return value;
    }

    @SuppressLint("LongLogTag")
    static public String loadPrefsString(String name, String defaultValue, Context context) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        String value = sharedPreferences.getString(name, defaultValue);
       // Log.e("Execute Preference Command", "LOAD STRING PREFERENCE '" + name + "' WITH VALUE '" + value + "'");
        return value;
    }

    @SuppressLint("LongLogTag")
    static public int loadPrefsInt(String name, int defaultValue, Context context) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        int value = sharedPreferences.getInt(name, defaultValue);
      //  Log.e("Execute Preference Command", "LOAD INT PREFERENCE '" + name + "' WITH VALUE '" + value + "'");
        return value;
    }

    @SuppressLint("LongLogTag")
    public static void writeList(Context context, List<String> list,
                                 String prefix) {
        SharedPreferences prefs = context.getSharedPreferences(
                "com.tardis.ordersamos", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        int size = prefs.getInt(prefix + "_size", 0);

        // clear the previous data if exists
        for (int i = 0; i < size; i++)
            editor.remove(prefix + "_" + i);

        // write the current list
        for (int i = 0; i < list.size(); i++)
            editor.putString(prefix + "_" + i, list.get(i));

        editor.putInt(prefix + "_size", list.size());
        editor.commit();
       // Log.e("Execute Preference Command", "WRITE LIST");
    }

    @SuppressLint("LongLogTag")
    public static List<String> readList(Context context, String prefix) {
        SharedPreferences prefs = context.getSharedPreferences(
                "com.tardis.ordersamos", Context.MODE_PRIVATE);

        int size = prefs.getInt(prefix + "_size", 0);

        List<String> data = new ArrayList<String>(size);
        for (int i = 0; i < size; i++)
            data.add(prefs.getString(prefix + "_" + i, null));
       // Log.e("Execute Preference Command", "READ LIST");
        return data;
    }

    @SuppressLint("LongLogTag")
    public static void saveListIntValuesToPreferencesWithPrefix(Context context, List<String> listToBeSaved, String prefix) {


        for (int i = 0; i < listToBeSaved.size(); i++) {
            try {
                if ((Preferences.loadPrefsInt(listToBeSaved.get(i) + prefix, -1,
                        context) == -1)) {
                    Preferences.savePrefsInt(listToBeSaved.get(i) + prefix, 0,
                            context);
                //    Log.e("Execute Preference Command", "WRITE NAME "+listToBeSaved.get(i)+" FROM LIST WITH VALUE '0'");
                }
            } catch (Exception e) {
              //  Log.e("ERROR","Execute Preference Command from 'saveListIntValuesToPreferencesWithPrefix' Method");

            }
        }

    }
}

