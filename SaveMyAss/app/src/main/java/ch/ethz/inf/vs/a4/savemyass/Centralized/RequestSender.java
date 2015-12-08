package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jan on 07.12.15.
 *
 * sends a json object to the specified url via http post
 */
public class RequestSender {

    protected final static String TAG = "###RequestSender";

    private String url;

    private ResponseListener listener;

    public RequestSender(String url){
        this.url = url;
    }

    public class AsyncWorker extends AsyncTask<JSONObject, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(JSONObject... params) {
            // create a new HttpClient and post header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(url);
            // add the json to the post request
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("json", params[0].toString()));
            try {
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);
                BufferedReader buf = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String answerLine;
                StringBuilder sb = new StringBuilder();
                while((answerLine = buf.readLine()) != null)
                    sb.append(answerLine);

                Log.d(TAG, "response: " + sb.toString());
                return new JSONObject(sb.toString());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
            listener.onResponseReceive(jsonObject);
        }
    }

    /**
     * implementation for sending a request from the main-thread
     */
    public void sendRequest(JSONObject json, ResponseListener sender){
        listener = sender;
        new AsyncWorker().execute(json);
    }

    public JSONObject sendRequest(JSONObject json){
        // create a new HttpClient and post header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(url);
        // add the json to the post request
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
        nameValuePairs.add(new BasicNameValuePair("json", json.toString()));
        try {
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            BufferedReader buf = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String answerLine;
            StringBuilder sb = new StringBuilder();
            while((answerLine = buf.readLine()) != null)
                sb.append(answerLine);

            Log.d(TAG, "response: " + sb.toString());
            return new JSONObject(sb.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}