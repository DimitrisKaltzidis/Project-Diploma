package com.jim.robotos_v2;

import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.jim.robotos_v2.Utilities.Bluetooth;
import com.jim.robotos_v2.Utilities.Utilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

public class VoiceOverride extends AppCompatActivity implements RecognitionListener {

    private ImageView ivBluetooth, ivDirection;
    private TextView tvDistance;
    private Bluetooth bt;
    private static StringBuilder sb = new StringBuilder();
    private int distanceToObstacle = 2000;
    private TextToSpeech textToSpeech;
    private CoordinatorLayout coordinatorLayout;
    private final int REQ_CODE_SPEECH_INPUT = 100;


    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "robot move";
    //private static final String MENU_SEARCH = "menu";

    private SpeechRecognizer recognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_override);

        ivBluetooth = (ImageView) findViewById(R.id.ivBluetooth);
        ivDirection = (ImageView) findViewById(R.id.ivDirection);
        tvDistance = (TextView) findViewById(R.id.tvDistance);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        bt = new Bluetooth(this, mHandler);
        connectService();

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }
        });

        textToSpeech.speak("Hello", TextToSpeech.QUEUE_FLUSH, null);

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(VoiceOverride.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    ((TextView) findViewById(R.id.caption_text))
                            .setText("Failed to init recognizer " + result);
                } else {
                    switchSearch(KWS_SEARCH);
                }
            }
        }.execute();

    }

    private void setupRecognizer(File assetsDir) {

        try {

            recognizer = defaultSetup()
                    .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                    .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

                            // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                    .setRawLogDir(assetsDir)

                            // Threshold to tune for keyphrase to balance between false alarms and misses
                    .setKeywordThreshold(1e-45f)

                            // Use context-independent phonetic search, context-dependent is too slow for mobile
                    .setBoolean("-allphone_ci", true)

                    .getRecognizer();
            recognizer.addListener(this);

            // Create keyword-activation search.
            recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

         /*   // Create grammar-based searches.
            File menuGrammar = new File(assetsDir, "menu.gram");
            recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);
*/
            // recognizer.startListening(MENU_SEARCH);
        } catch (Exception e) {

        }

    }

    public void connectService() {
        try {
            ivBluetooth.setImageResource(R.drawable.connecting);
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter.isEnabled()) {
                bt.start();
                bt.connectDevice("HC-05");///device name
                Log.d("BLUETOOTH", "Btservice started - listening");
                ivBluetooth.setImageResource(R.drawable.connected);
            } else {
                Log.w("BLUETOOTH", "Btservice started - bluetooth is not enabled");
                ivBluetooth.setImageResource(R.drawable.disabled);
            }
        } catch (Exception e) {
            Log.e("BLUETOOTH", "Unable to start bt ", e);
            ivBluetooth.setImageResource(R.drawable.disconnected);
        }
    }

    static String TAG = "HANDLER";

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case Bluetooth.MESSAGE_STATE_CHANGE:
                    Log.d(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    break;
                case Bluetooth.MESSAGE_WRITE:
                    Log.d(TAG, "MESSAGE_WRITE ");
                    break;
                case Bluetooth.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String strIncom = new String(readBuf, 0, msg.arg1);
                    sb.append(strIncom);
                    int endOfLineIndex = sb.indexOf("\r\n");                            // determine the end-of-line
                    if (endOfLineIndex > 0) {                                            // if end-of-line,
                        String sbprint = sb.substring(0, endOfLineIndex);               // extract string
                        sb.delete(0, sb.length());   // and clear


                        Log.d("READ_FROM_ARDUINO", sbprint + "");
                        try {

                            distanceToObstacle = Utilities.normalizeReadingsFromDistanceSensor(Integer.parseInt(sbprint), distanceToObstacle);

                            Log.d("READ_FROM_ARDUINO_NORM", distanceToObstacle + "");
                            tvDistance.setText(distanceToObstacle + "cm");
                            //distanceToObstacle = Integer.parseInt(sbprint);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "handleMessage: CRASH CONVERSION");
                        }
                    }
                    break;
                case Bluetooth.MESSAGE_DEVICE_NAME:
                    Log.d(TAG, "MESSAGE_DEVICE_NAME " + msg);
                    break;
                case Bluetooth.MESSAGE_TOAST:
                    Log.d(TAG, "MESSAGE_TOAST " + msg);
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onPause() {
        super.onPause();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        Utilities.setDirectionImage("STOP", ivDirection, bt);
        bt.stop();
        recognizer.cancel();
        //finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Utilities.setDirectionImage("STOP", ivDirection, bt);
        bt.stop();

    }

    public void onDestroy() {
        super.onDestroy();

        Utilities.setDirectionImage("STOP", ivDirection, bt);
        bt.stop();

        recognizer.cancel();
        recognizer.shutdown();
    }

    /*-------------------------------------------------------------------------------------------------------------------------------------------------*/
    @Override
    public void onBeginningOfSpeech() {
        Snackbar snackbar = Snackbar
                .make(coordinatorLayout, "onBeginningOfSpeech", Snackbar.LENGTH_LONG);

        snackbar.show();
    }

    @Override
    public void onEndOfSpeech() {
        Snackbar snackbar = Snackbar
                .make(coordinatorLayout, "onEndOfSpeech", Snackbar.LENGTH_LONG);

        snackbar.show();

        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
        //recognizer.stop();
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {

        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE))
            promptSpeechInput();
        //  switchSearch(MENU_SEARCH);

        Snackbar snackbar = Snackbar
                .make(coordinatorLayout, "onPartialResult text: " + text, Snackbar.LENGTH_LONG);

        snackbar.show();
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            Snackbar snackbar = Snackbar
                    .make(coordinatorLayout, hypothesis.getHypstr(), Snackbar.LENGTH_LONG);

            snackbar.show();
            Log.d(TAG, "onResult: works" + hypothesis.getHypstr());
        }
    }

    @Override
    public void onError(Exception e) {
        Snackbar snackbar = Snackbar
                .make(coordinatorLayout, "onError", Snackbar.LENGTH_INDEFINITE);

        snackbar.show();
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }


    /*---------------------------------------------------------------------------------------------------------------------------------------*/

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);

        //String caption = getResources().getString(searchName);
        ((TextView) findViewById(R.id.caption_text)).setText(searchName);
    }

    /**
     * Showing google speech input dialog
     */
    private void promptSpeechInput() {
        recognizer.cancel();

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Snackbar snackbar = Snackbar
                    .make(coordinatorLayout, "Speech is not supported", Snackbar.LENGTH_INDEFINITE);

            snackbar.show();
        }
    }

    /**
     * Receiving speech input
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    //txtSpeechInput.setText(result.get(0));
                    Snackbar snackbar = Snackbar
                            .make(coordinatorLayout, "You said: " + result.get(0), Snackbar.LENGTH_INDEFINITE);

                    snackbar.show();

                    switchSearch(KWS_SEARCH);

                }
                break;
            }

        }
    }
}
