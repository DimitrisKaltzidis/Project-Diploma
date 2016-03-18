package com.jim.robotos_v2;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.core.Scalar;

import java.util.ArrayList;

public class ScenarioMode extends AppCompatActivity {

    private Scalar mBlobColorRgba;
    private EditText etRed, etGreen, etBlue, etAlpha;
    private ImageView ivDetectionColor;
    private ArrayList<Double> colorValues = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scenario_mode);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mBlobColorRgba = new Scalar(255);


        etRed = (EditText) findViewById(R.id.etRed);
        etGreen = (EditText) findViewById(R.id.etGreen);
        etBlue = (EditText) findViewById(R.id.etBlue);
        etAlpha = (EditText) findViewById(R.id.etAlpha);
        ivDetectionColor = (ImageView) findViewById(R.id.ivDetectionColorDialog);


    }


    public void previewColorClicked(View view) {
        mBlobColorRgba.val[0] = Double.parseDouble(etRed.getText().toString());
        mBlobColorRgba.val[1] = Double.parseDouble(etGreen.getText().toString());
        mBlobColorRgba.val[2] = Double.parseDouble(etBlue.getText().toString());
        mBlobColorRgba.val[3] = Double.parseDouble(etAlpha.getText().toString());
        ivDetectionColor.setBackgroundColor(Color.argb((int) mBlobColorRgba.val[0], (int) mBlobColorRgba.val[0], (int) mBlobColorRgba.val[1], (int) mBlobColorRgba.val[2]));
    }

    public void nextActivityClicked(View view) {
        try {
            mBlobColorRgba.val[0] = Double.parseDouble(etRed.getText().toString());
            mBlobColorRgba.val[1] = Double.parseDouble(etGreen.getText().toString());
            mBlobColorRgba.val[2] = Double.parseDouble(etBlue.getText().toString());
            mBlobColorRgba.val[3] = Double.parseDouble(etAlpha.getText().toString());


            colorValues.add(mBlobColorRgba.val[0]);
            colorValues.add(mBlobColorRgba.val[1]);
            colorValues.add(mBlobColorRgba.val[2]);
            colorValues.add(mBlobColorRgba.val[3]);


            Intent intent = new Intent(ScenarioMode.this, ObstacleAvoidance.class);
            intent.putExtra("detectionColor", colorValues);
            startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Fill all the fields in the right manner(kai gamw ta agglika e?)", Toast.LENGTH_LONG).show();
        }
    }
}
