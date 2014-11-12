package com.geoblender.pollerizer;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private static String SERVICE_URL = "http://geoblender.com/android/android.php";
    private static final String SERVICE_URL_Y = "http://geoblender.com/android/yes.php";
    private static final String SERVICE_URL_N = "http://geoblender.com/android/no.php";
    // 1. Declare a service url:
    private static final String SERVICE_URL_SURVEYS = "http://www.geoblender.com/android/survey.php";

    private static final String LOG_TAG = "Pollerizer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button yesButton = (Button) findViewById(R.id.buttonYes);
        final Button noButton = (Button) findViewById(R.id.buttonNo);

        final GridLayout gridLayout = (GridLayout) findViewById(R.id.mainLayout);

        getData();

        View.OnClickListener yes = new View.OnClickListener() {
            int counter = 0;
            public void onClick(View view) {
                String myVote = "Yes";
                castVote(myVote);
                counter ++;
                Toast.makeText(getApplicationContext(), "Yes vote #" + counter + " sent to server",
                        Toast.LENGTH_SHORT).show();
            }
        };

        View.OnClickListener no = new View.OnClickListener() {
            int counter = 0;
            public void onClick(View view) {
                String myVote = "No";
                castVote(myVote);
                counter ++;
                Toast.makeText(getApplicationContext(), "No vote #" + counter + " sent to server",
                        Toast.LENGTH_SHORT).show();
            }
        };

        yesButton.setOnClickListener(yes);
        noButton.setOnClickListener(no);
    } // onCreate


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void vote(final String myVote) throws IOException {
        HttpURLConnection conn = null;
        final StringBuilder json = new StringBuilder();
        try {
            // Connect to the web service

            if(myVote == "Yes"){
                SERVICE_URL = SERVICE_URL_Y;
                Log.e(LOG_TAG, SERVICE_URL);
            } else {
                SERVICE_URL = SERVICE_URL_N;
                Log.e(LOG_TAG, SERVICE_URL);
            }

            URL url = new URL(SERVICE_URL);
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Read the JSON data into the StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                json.append(buff, 0, read);
            }

        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "No connection",
                    Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "Error connecting to service", e);
            throw new IOException("Error connecting to service", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    } // connect to web

    private void castVote(final String myVote) {
        // Worker thread cuz it's a network operation.
        new Thread(new Runnable() {
            public void run() {
                try {
                    vote(myVote);
                    getData();
                    Log.e(LOG_TAG, myVote);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Cannot vote", e);
                    return;
                }
            }
        }).start();
    }

    // 2. Create function to call the function to retrieve JSON:
    private void getData() {
        // Retrieve the city data from the web service
        // In a worker thread since it's a network operation.
        new Thread(new Runnable() {
            public void run() {
                try {
                    retrieveData();
                    Log.e(LOG_TAG, "Data retrived");
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Cannot retrive data", e);
                    return;
                }
            }
        }).start();
    }

    // 3. Create function to retrieve JSON:
    protected void retrieveData() throws IOException {
        HttpURLConnection conn = null;
        final StringBuilder json = new StringBuilder();
        try {
            // Connect to the web service
            URL url = new URL(SERVICE_URL_SURVEYS);
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Read the JSON data into the StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                json.append(buff, 0, read);
            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to service", e);
            throw new IOException("Error connecting to service", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // Do stuff with JSON
        // Must run this on the UI thread since it's a UI operation.
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    processJson(json.toString());
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error processing JSON", e);
                }
            }
        });
    }

    // 4. Create function to process JSON
    void processJson(String json) throws JSONException {
        // De-serialize the JSON string into an array of objects
        JSONArray jsonArray = new JSONArray(json);
        for (int i = 0; i < jsonArray.length(); i++) {

            JSONObject jsonObj = jsonArray.getJSONObject(i);

            double totalVotes = (int) jsonObj.getDouble("yes") + (int) jsonObj.getDouble("no");
            double percentYes = (int) jsonObj.getDouble("yes") / totalVotes * 100;
            double percentNo = (int) jsonObj.getDouble("no") / totalVotes * 100;
            percentYes = Math.round(percentYes*10.0)/10.0;
            percentNo = Math.round(percentNo*10.0)/10.0;

            //Log.e(LOG_TAG, "Stuff: " + percentYes);

            TextView labelVotesYes = (TextView) findViewById(R.id.votesYes);
            TextView labelVotesNo = (TextView) findViewById(R.id.votesNo);
            TextView labelPercentYes = (TextView) findViewById(R.id.percentYes);
            TextView labelPercentNo = (TextView) findViewById(R.id.percentNo);

            labelVotesYes.setText(jsonObj.getString("yes"));
            labelVotesNo.setText(jsonObj.getString("no"));
            labelPercentYes.setText(String.valueOf(percentYes));
            labelPercentNo.setText(String.valueOf(percentNo));

        } // loop
    }
}
