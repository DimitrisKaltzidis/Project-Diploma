package com.jim.robotos_v2;

import android.bluetooth.BluetoothAdapter;
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
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jim.robotos_v2.Utilities.Bluetooth;
import com.jim.robotos_v2.Utilities.CustomRecognitionListener;
import com.jim.robotos_v2.Utilities.Utilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

public class VoiceOverride extends AppCompatActivity implements RecognitionListener, CustomRecognitionListener {

    private ImageView ivBluetooth, ivDirection;
    private TextView tvDistance;
    private Bluetooth bt;
    private static StringBuilder sb = new StringBuilder();
    private int distanceToObstacle = 2000;
    private TextToSpeech textToSpeech;
    private CoordinatorLayout coordinatorLayout;
    private Intent recognizerIntent;
    private android.speech.SpeechRecognizer speech = null;
    private ProgressBar progressBar;
    private String command = "STOP";

    private static final String KWS_SEARCH = "wakeup";
    private static final String KEYPHRASE = "robot move";

    private edu.cmu.pocketsphinx.SpeechRecognizer recognizer;
    private String LOG_TAG = "LOG";

    private ArrayList<String> commands = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_override);

        commands.add("forward");
        commands.add("backward");
        commands.add("left");
        commands.add("right");
        commands.add("stop");

        ivBluetooth = (ImageView) findViewById(R.id.ivBluetooth);
        ivDirection = (ImageView) findViewById(R.id.ivDirection);
        tvDistance = (TextView) findViewById(R.id.tvDistance);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        progressBar = (ProgressBar) findViewById(R.id.progressBarVoice);

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

        // textToSpeech.speak("Hello", TextToSpeech.QUEUE_FLUSH, null);

        initializeSpeechRecognizer();

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

                } else {
                    startListeningSphinx(KWS_SEARCH);
                }
            }
        }.execute();

    }

    private void setupRecognizer(File assetsDir) {
        try {
            recognizer = defaultSetup()
                    .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                    .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                            // .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                    .setKeywordThreshold(1e-45f)  // Threshold to tune for keyphrase to balance between false alarms and misses
                    .setBoolean("-allphone_ci", true)
                    .getRecognizer();
            recognizer.addListener(this);

            // Create keyword-activation search.
            recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        } catch (Exception e) {
            e.printStackTrace();
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
                            if (distanceToObstacle < 40 && !(command.equals("STOP"))) {
                                Utilities.setDirectionImage("STOP", ivDirection, bt);
                                textToSpeech.speak("YOU ARE ABOUT TO HIT AN OBSTACLE STOPPING", TextToSpeech.QUEUE_FLUSH, null);
                                ((TextView) findViewById(R.id.tvResults))
                                        .setText("STOP");
                                command = "STOP";
                            }
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

        if (speech != null) {
            speech.destroy();
        }

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
        showSnackbar("Begin of speech Sphinx", Snackbar.LENGTH_LONG);

        progressBar.setIndeterminate(false);
        progressBar.setMax(10);
    }


    @Override
    public void onEndOfSpeech() {

        showSnackbar("End of speech Sphinx", Snackbar.LENGTH_LONG);

        try {
            if (!recognizer.getSearchName().equals(KWS_SEARCH))
                startListeningSphinx(KWS_SEARCH);
        } catch (Exception r) {
            r.printStackTrace();
        }
        progressBar.setIndeterminate(true);
    }


    @Override
    public void onPartialResult(Hypothesis hypothesis) { /// change recogniser Sphinx -> Android

        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setIndeterminate(true);
            recognizer.cancel(); //cancel Sphinx speech recognizer
            speech.startListening(recognizerIntent); // start android speech recognizer
            ((TextView) findViewById(R.id.user_help)).setText(R.string.speech_prompt);
        }

    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            showSnackbar("Sphinx onResult: " + hypothesis.getHypstr(), Snackbar.LENGTH_LONG);
        }
    }

    @Override
    public void onError(Exception e) {
        showSnackbar("Sphinx error", Snackbar.LENGTH_LONG);
    }

    @Override
    public void onTimeout() {
        startListeningSphinx(KWS_SEARCH);
    }


    /*---------------------------------------------------------------------------------------------------------------------------------------*/

    private void startListeningSphinx(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            // recognizer.startListening(searchName);
            recognizer.startListening(searchName, 10000);

        //String caption = getResources().getString(searchName);
        ((TextView) findViewById(R.id.user_help)).setText(R.string.robot_move);
    }

    private void initializeSpeechRecognizer() {
        speech = android.speech.SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(VoiceOverride.this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "en-us");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.i(LOG_TAG, "onReadyForSpeech Android");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        Log.i(LOG_TAG, "onRmsChanged Android: " + rmsdB);
        progressBar.setProgress((int) rmsdB);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: Android" + buffer);
    }

    @Override
    public void onError(int error) {/// Change Listener because something went wrong Android -> Sphinx
        String errorMessage = getErrorText(error);
        Log.d(LOG_TAG, "FAILED " + errorMessage);
        speech.cancel();
        startListeningSphinx(KWS_SEARCH);
        showSnackbar(errorMessage, Snackbar.LENGTH_LONG);
    }

    @Override
    public void onResults(Bundle results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> matches = results
                .getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
        String text = "";
        for (String result : matches)
            text += result + "\n";

        decideDirection(matches);

        speech.cancel();
        startListeningSphinx(KWS_SEARCH);

    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.i(LOG_TAG, "onPartialResults");
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.i(LOG_TAG, "onEvent");
    }

    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case android.speech.SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case android.speech.SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case android.speech.SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case android.speech.SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case android.speech.SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

    private void showSnackbar(String text, int duration) {
        Snackbar snackbar = Snackbar
                .make(coordinatorLayout, text, duration);

        snackbar.show();
    }


    private void decideDirection(ArrayList<String> matches) {
        int position = -1;

        for (String phrase : matches) {

            position = commands.indexOf(phrase.toLowerCase());
            if (position != -1) {
                break;
            }
        }

        if (position != -1) {
            Utilities.setDirectionImage(commands.get(position).toUpperCase(), ivDirection, bt);
            textToSpeech.speak("EXECUTING COMMAND " + commands.get(position).toUpperCase(), TextToSpeech.QUEUE_FLUSH, null);
            ((TextView) findViewById(R.id.tvResults))
                    .setText(commands.get(position).toUpperCase());
            command = commands.get(position).toUpperCase();
        } else {
            Utilities.setDirectionImage("STOP", ivDirection, bt);
            textToSpeech.speak("NO COMMAND DETECTED STOPPING", TextToSpeech.QUEUE_FLUSH, null);
            ((TextView) findViewById(R.id.tvResults))
                    .setText("STOP");
            command = "STOP";
        }


    }
}
