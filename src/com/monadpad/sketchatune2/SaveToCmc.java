package com.monadpad.sketchatune2;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: m
 * Date: 7/4/13
 * Time: 11:35 PM
 */
public class SaveToCmc {

    public String desc = "";
    public String responseString = "";

    private Context context;

    public SaveToCmc(Context context) {
        this.context = context;
    }

    public boolean execute(String saveUrl, String data, String name, String artist, boolean isPublic) {

        boolean saved = false;
        HttpClient httpclientup = new DefaultHttpClient();
        try {
            HttpPost hPost = new HttpPost(saveUrl);
            List<NameValuePair> postParams = new ArrayList<NameValuePair>();
            postParams.add(new BasicNameValuePair("json", data));
            postParams.add(new BasicNameValuePair("name", name));
            postParams.add(new BasicNameValuePair("artist", artist));
            postParams.add(new BasicNameValuePair("longid", "true"));
            postParams.add(new BasicNameValuePair("private", isPublic ? "false" : "true"));
            hPost.setEntity(new UrlEncodedFormEntity(postParams));

            HttpResponse response = httpclientup.execute(hPost);
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                responseString = out.toString();
                if (!responseString.equals("bad")){
                    saved = true;
                }   else{
                    desc = responseString;
                }
            }

        } catch (ClientProtocolException ee) {
            desc = ee.getMessage();

        } catch (IOException ee) {
            desc = ee.getMessage();
        }
        return saved;
    }

    public void executeUpdate(MonadJamSourceInfo source, boolean isPublic) {

        String url = context.getString(R.string.url_main) +
                context.getString(R.string.url_grooveupdate);

        UpdateAsyncTask task = new UpdateAsyncTask();
        task.source = source;
        task.isPublic = isPublic;
        task.execute(url);
    }

    private class UpdateAsyncTask extends AsyncTask<String, Void, Void> {

        MonadJamSourceInfo source;
        boolean isPublic;

        @Override
        protected Void doInBackground(String... urls) {
            HttpClient httpclientup = new DefaultHttpClient();
            try {
                String url = urls[0];
                HttpPost hPost = new HttpPost(url);
                List<NameValuePair> postParams = new ArrayList<NameValuePair>();
                postParams.add(new BasicNameValuePair("id", Long.toString(source.id)));
                postParams.add(new BasicNameValuePair("name", source.title));
                postParams.add(new BasicNameValuePair("artist", source.artist));
                postParams.add(new BasicNameValuePair("authcode", source.authCode));
                postParams.add(new BasicNameValuePair("private", isPublic ? "false" : "true"));
                hPost.setEntity(new UrlEncodedFormEntity(postParams));

                HttpResponse response = httpclientup.execute(hPost);
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    responseString = out.toString();
                    Log.d("MGH", responseString);
                    if (!responseString.equals("bad")){
                        // must be good
                    }
                }

            } catch (ClientProtocolException ee) {
                desc = ee.getMessage();

            } catch (IOException ee) {
                desc = ee.getMessage();
            }

            return null;
        }
    }
}
