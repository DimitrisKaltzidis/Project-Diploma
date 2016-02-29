package com.jim.robotos_v2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.jim.robotos_v2.Utilities.Preferences;

public class Settings extends AppCompatActivity {

    private EditText etMapErrorRange, etBearingRange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etBearingRange = (EditText) findViewById(R.id.etBearingRange);
        etMapErrorRange = (EditText) findViewById(R.id.etMapsErrorRange);

        etMapErrorRange.setText("" + Preferences.loadPrefsFloat("DISTANCE_ERROR_RANGE", 3, getApplicationContext()));
        etBearingRange.setText("" + Preferences.loadPrefsFloat("BEARING_RANGE", 20, getApplicationContext()));


    }

    public void saveSettings(View view) {

        try {
            Preferences.savePrefsFloat("DISTANCE_ERROR_RANGE", Float.parseFloat(etMapErrorRange.getText().toString()), getApplicationContext());
            Preferences.savePrefsFloat("BEARING_RANGE", Float.parseFloat(etBearingRange.getText().toString()), getApplicationContext());
            Toast.makeText(getApplicationContext(), R.string.saved,Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), R.string.please_fill_correctly,Toast.LENGTH_SHORT).show();
        }

    }
}
