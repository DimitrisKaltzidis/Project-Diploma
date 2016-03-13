package com.jim.robotos_v2;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

public class VoiceOverride extends AppCompatActivity implements RecognitionListener {

    /* Named searches allow to quickly reconfigure the decoder */
    private static final String FORWARD_COMMAND = "forward";
    private static final String BACKWARD_COMMAND = "backward";
    private static final String RIGHT_COMMAND = "right";
    private static final String LEFT_COMMAND = "left";
    private static final String STOP_COMMAND = "stop";

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "robot";

    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_override);

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
                    switchSearch(FORWARD_COMMAND);
                }
            }
        }.execute();
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(FORWARD_COMMAND))
            switchSearch(FORWARD_COMMAND);
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE))
            switchSearch(STOP_COMMAND);
        else if (text.equals(RIGHT_COMMAND))
            switchSearch(RIGHT_COMMAND);
        else if (text.equals(LEFT_COMMAND))
            switchSearch(LEFT_COMMAND);
        else if (text.equals(BACKWARD_COMMAND))
            switchSearch(BACKWARD_COMMAND);
        else
            ((TextView) findViewById(R.id.result_text)).setText(text);
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        ((TextView) findViewById(R.id.result_text)).setText("");
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onError(Exception e) {
        ((TextView) findViewById(R.id.caption_text)).setText(e.getMessage());
    }

    @Override
    public void onTimeout() {
        switchSearch(FORWARD_COMMAND);
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

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

        /** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(FORWARD_COMMAND, KEYPHRASE);

        // Create grammar-based search for selection between demos
        File menuGrammar = new File(assetsDir, "menu.gram");
        recognizer.addGrammarSearch(STOP_COMMAND, menuGrammar);

        // Create grammar-based search for digit recognition
        File digitsGrammar = new File(assetsDir, "digits.gram");
        recognizer.addGrammarSearch(RIGHT_COMMAND, digitsGrammar);

        // Create language model search
        File languageModel = new File(assetsDir, "weather.dmp");
        recognizer.addNgramSearch(BACKWARD_COMMAND, languageModel);

        // Phonetic search
        File phoneticModel = new File(assetsDir, "en-phone.dmp");
        recognizer.addAllphoneSearch(LEFT_COMMAND, phoneticModel);
    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(FORWARD_COMMAND))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);
        try {
            String caption = getResources().getString(captions.get(searchName));
            ((TextView) findViewById(R.id.caption_text)).setText(caption);
        } catch (Exception vc) {
            vc.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            recognizer.cancel();
            recognizer.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
