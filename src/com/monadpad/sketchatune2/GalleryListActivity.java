package com.monadpad.sketchatune2;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GalleryListActivity extends ListActivity
{
    private String galleryUrl;
    private int page;

    private TextView foot;
    private TextView head;

    private int headerOffset = 0;

    private String lastJSON = "";


    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        galleryUrl = getString(R.string.url_main) + getString(R.string.url_gallery);

        setTitle(getString(R.string.gallery_list_title));
        page = 1;

        head = new TextView(this);
        final int savedGrooves = SdListManager.getSavedGrooveCount();
        if (savedGrooves > 0) {
            head.setText(getString(R.string.saved_count_on_sd));
            head.setTextAppearance(this, android.R.style.TextAppearance_Medium);
            head.setPadding(20, 20, 20, 20);
            head.setGravity(0x11);
            getListView().addHeaderView(head, -7, true);

            headerOffset = 1;
        }

        foot = new TextView(this);
        foot.setText(R.string.loading_please_wait);
        foot.setTextAppearance(this, android.R.style.TextAppearance_Medium);
        foot.setPadding(20, 20, 20, 20);
        foot.setGravity(0x11);
        getListView().addFooterView(foot, -7, true);

        setupAdapter();

        if (savedInstanceState != null && savedInstanceState.get("last_json") != null){
            lastJSON = savedInstanceState.getString("last_json");
            page = savedInstanceState.getInt("page");
            foot.setText(R.string.get_more);

            try{
                JSONArray jsA = new JSONArray(lastJSON);
                addToListFromJson(jsA);
            } catch (JSONException ej){}
        }
        else {
            new DownloadGallery().execute();
        }
    }


    public void onListItemClick(ListView l, View v, int position, long id){

        if (v == head) {
            startActivity(new Intent(this, SdListActivity.class));
            return;
        }

        if (v == foot){
            if (((TextView)v).getText().equals(getString(R.string.get_more))){
                page++;
                new DownloadGallery().execute();
            }
            return;
        }

        GalleryRow row = (GalleryRow) getListAdapter().getItem(position - headerOffset);

        Intent intent = new Intent(this, MainActivity.class);
        String json = row.json;
        if (!json.startsWith("{")){
            json = "good;" + json;
        }
        intent.putExtra("grooveInfo", json);
        intent.putExtra("galleryId", Long.parseLong(row.id));
        intent.putExtra("galleryTitle", row.name);
        intent.putExtra("galleryArtist", row.artist);

        startActivity(intent);

    }

    private class DownloadGallery extends AsyncTask<Void, Void, Void>{

        String responseString = "";

        private boolean downloaded = false;
        private boolean parsed = false;

        private JSONArray jsonArray;

        @Override
        public void onPreExecute(){

            foot.setText(R.string.loading_please_wait);

        }

        @Override
        public void onPostExecute(Void v){

            if (downloaded && parsed && addToListFromJson(jsonArray) > 0) {
                lastJSON = responseString;
                foot.setText(R.string.get_more);
            }
            else {
                foot.setText(getString(R.string.no_more_to_download));
            }

        }


        @Override
        protected Void doInBackground(Void... voids) {

            // get a list of grooves in the gallery
            String cUrl= galleryUrl + Integer.toString(page);

            HttpClient httpclientup = new DefaultHttpClient();
            try {
                HttpResponse response = httpclientup.execute(new HttpGet(cUrl));
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    responseString = out.toString();

                    downloaded = true;

                }else{
                    responseString = statusLine.getReasonPhrase();
                }

            } catch (ClientProtocolException e) {
                responseString =
                        e.getMessage();
                e.printStackTrace();
            } catch (IOException e) {
                responseString =
                        e.getMessage();
                e.printStackTrace();
            }

            if (downloaded) {

                try {
                    jsonArray = new JSONArray(responseString);
                    parsed = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }


    }


    private int addToListFromJson(JSONArray jsA) {

        ArrayAdapter apdapter = (ArrayAdapter)getListAdapter();

        GalleryRow row;

        int added = 0;

        for (int iii = 0; iii < jsA.length(); iii++){


            row = new GalleryRow();

            try {
                row.id = jsA.getJSONObject(iii).getString("id");
                row.name = jsA.getJSONObject(iii).getString("name");
                row.artist = jsA.getJSONObject(iii).getString("artist");

                //                rb.add(jsA.getJSONObject(iii).getString("date"));
                //rb.add("date");

                row.json = jsA.getJSONObject(iii).getString("json");
                row.ratingCount = jsA.getJSONObject(iii).optDouble("ratingCount", 0);
                row.ratingAverage = jsA.getJSONObject(iii).optDouble("ratingAverage", 0);

                apdapter.add(row);
                added++;
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return added;

    }


    @Override
    public void onSaveInstanceState(Bundle saveInstanceState){
        super.onSaveInstanceState(saveInstanceState);
        saveInstanceState.putString("last_json", lastJSON);
        saveInstanceState.putInt("page", page);
    }

    private void setupAdapter(){

        ArrayList<GalleryRow> arrayList = new ArrayList<GalleryRow>();
        GalleryArrayAdapter adapter = new GalleryArrayAdapter(this, R.layout.gallery_row, 0, arrayList);
        setListAdapter(adapter);

    }

}
