package com.monadpad.sketchatune2;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
* User: m
* Date: 7/4/13
* Time: 11:34 PM
*/
class SaveToSd extends AsyncTask<String, Void, Void> {

    boolean saved = false;
    String desc = "";
    String fileName;
    boolean hasSdCard = false;
    private Context context;

    public SaveToSd(MainActivity context) {
        this.context = context;
    }

    protected Void doInBackground(String... data) {

        hasSdCard = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (!hasSdCard)
            return null;

        try{
//            String name = (new Date()).toString();
            String name = Long.toString(System.currentTimeMillis());
            File path = SdListManager.getPath11();
            fileName = path + "/" + name;
            if (path.exists() || path.mkdirs()){
                File file = new File(path, name + ".jam");
                FileWriter fw = new FileWriter(file);
                fw.append(data[0]);
                fw.flush();
                fw.close();
                saved = true;
            }
        }
        catch (IOException e){
            desc = e.getMessage();
        }
        return null;
    }

/*  who cares
    protected void onPostExecute(Void v) {

        if (hasSdCard) {
            String msg;
            if (saved){
                msg = context.getString(R.string.saved_to_sd_card) + " \n\n" + fileName;
            }
            else
                msg = context.getString(R.string.not_saved_to_sd_card) + " \n\n" +  desc;
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
    }
    */
}
