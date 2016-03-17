package com.jim.robotos_v2;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.jim.robotos_v2.Utilities.Preferences;

public class Settings extends AppCompatActivity {

    private EditText etMapErrorRange, etBearingRange, etAvoidAngleRange, etAvoidPointMapError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        etBearingRange = (EditText) findViewById(R.id.etBearingRange);
        etMapErrorRange = (EditText) findViewById(R.id.etMapsErrorRange);
        //Moires pou prostithontai stin gwnia apofigis empodiou se periptwsi lathous
        etAvoidAngleRange = (EditText) findViewById(R.id.etAvoidAngleRange);
        //metra pou prostithontai stin apostasi apofigis tou empodiou logo pithanis asimfwnias xarti pragmatikotitas
        etAvoidPointMapError = (EditText) findViewById(R.id.etAvoidPointMapError);


        etMapErrorRange.setText("" + Preferences.loadPrefsFloat("DISTANCE_ERROR_RANGE", 3, getApplicationContext()));
        etBearingRange.setText("" + Preferences.loadPrefsFloat("BEARING_RANGE", 20, getApplicationContext()));
        etAvoidAngleRange.setText("" + Preferences.loadPrefsFloat("OBSTACLE_AVOIDING_BEARING_ERROR_RANGE_DEGREES", 3f, getApplicationContext()));
        etAvoidPointMapError.setText("" + Preferences.loadPrefsFloat("OBSTACLE_AVOIDING_MAP_ERROR_METERS", 1f, getApplicationContext()));


    }

    public void saveSettings(View view) {

        try {
            Preferences.savePrefsFloat("DISTANCE_ERROR_RANGE", Float.parseFloat(etMapErrorRange.getText().toString()), getApplicationContext());
            Preferences.savePrefsFloat("BEARING_RANGE", Float.parseFloat(etBearingRange.getText().toString()), getApplicationContext());
            Preferences.savePrefsFloat("OBSTACLE_AVOIDING_BEARING_ERROR_RANGE_DEGREES", Float.parseFloat(etAvoidAngleRange.getText().toString()), getApplicationContext());
            Preferences.savePrefsFloat("OBSTACLE_AVOIDING_MAP_ERROR_METERS", Float.parseFloat(etAvoidPointMapError.getText().toString()), getApplicationContext());

            Toast.makeText(getApplicationContext(), R.string.saved, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), R.string.please_fill_correctly, Toast.LENGTH_SHORT).show();
        }

    }
}
