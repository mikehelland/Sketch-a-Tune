package com.monadpad.sketchatune2;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.io.*;

public class SdListManager {

    final private Context mContext;
    final private IHasAdapter mHasAdapter;
    final private File sdPath;
    public boolean showProgressDialog = false;

    static public File getPath11(){
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "Jams/");
    }
    static public File getPath5(){
        return new File("/sdcard/monadpad/");
    }

    public SdListManager(Context context, IHasAdapter hasAdapter, File path, boolean showProgress){
        mContext = context;
        mHasAdapter = hasAdapter;
        this.showProgressDialog = showProgress;

        sdPath = path;

        new DownloadGallery().execute();
    }

    private class DownloadGallery extends AsyncTask<Void, Void, Cursor> {

        ProgressDialog dialog ;
        String message = "";

        @Override
        public void onPreExecute(){
            if (showProgressDialog){
                dialog = ProgressDialog.show(mContext, "",
                         mContext.getString(R.string.loading_please_wait, true));
            }
        }

        @Override
        public void onPostExecute(Cursor c){
            if (showProgressDialog){
                try {
                dialog.dismiss();
                } catch (Exception e) {}
            }

            if (c == null){
                if (showProgressDialog){
                    Toast.makeText(mContext,
                            mContext.getString(R.string.something_went_wrong) +
                            " \n\n " + message, Toast.LENGTH_LONG).show();
                }
            }
            else {
                Cursor galleryCursor = c;
                SimpleCursorAdapter curA = new GalleryAdapter(mContext,
                        R.layout.gallery_row,
                        galleryCursor, new String[]{"name", "artist"},
                        new int[]{R.id.gallery_name,R.id.gallery_artist});
                mHasAdapter.setListAdapter(curA);
            }
        }

        @Override
        protected Cursor doInBackground(Void... voids) {

            final String[] galleryProjection = {"_ID", "name", "artist", "date", "json"};
            MatrixCursor newCursor = new MatrixCursor(galleryProjection);

            File fDir = sdPath;
            if (!fDir.exists()){
                if (!fDir.mkdirs()){
                    message = mContext.getString(R.string.couldnt_find_make_dir) + " " + fDir.getPath();
                    return null;
                }
            }

            final String[] items = fDir.list(new FilenameFilter() {
                public boolean accept(File file, String s) {
                    return s.endsWith(".jam");
                }
            });

            if (showProgressDialog && items.length == 0){
                message = mContext.getString(R.string.no_saved_grooves);
                return null;
            }
            else {
                int id = 0;
                for (String sdName : items){
                    File fJam = new File(fDir, sdName);
                    StringBuilder resultsB = new StringBuilder();
                    String results = null;
                    try {
                        FileReader fileReader = new FileReader(fJam);
                        BufferedReader reader = new BufferedReader(fileReader);
                        String line;
                        line = reader.readLine();
                        while(line != null)
                        {
                            resultsB.append(line);
                            line = reader.readLine();
                        }
                        reader.close();
                        results = resultsB.toString();
                    }
                    catch (FileNotFoundException e){

                    }
                    catch (IOException e){
                    }
                    if (results != null){
                        MatrixCursor.RowBuilder rb = newCursor.newRow();
                        rb.add(id++);
                        rb.add(sdName.replace(".jam", ""));
                        rb.add("");
                        //rb.add(jsA.getJSONObject(iii).getString("date"));
                        rb.add("date");
                        rb.add(results);
                    }
                }
                return newCursor;
            }
        }
    }

    public interface IHasAdapter {
        public void setListAdapter(ListAdapter listAdapter);
    }

    public static boolean hasSdCard(){
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static int getSavedGrooveCount() {
        if (!hasSdCard())
            return 0;


        File fDir = getPath11();
        if (!fDir.exists())
            return 0;

        final String[] items = fDir.list(new FilenameFilter() {
            public boolean accept(File file, String s) {
                return s.endsWith(".jam");
            }
        });
        return items.length;
    }
}
