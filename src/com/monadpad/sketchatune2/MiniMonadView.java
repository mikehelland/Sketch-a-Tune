package com.monadpad.sketchatune2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MiniMonadView extends View {

    private String mInfo;
    public final Paint[] colors;
    public List<MonadFakeChannel> channels = new ArrayList<MonadFakeChannel>();
    private boolean readyToDraw = false;
    public int position;

    public MiniMonadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        colors = InstrumentFactory.getColors();
        setBackgroundColor(0xFF000000);

    }

    @Override
    public void onDraw(Canvas canvas) {
        if (readyToDraw){
            for (MonadFakeChannel c : channels)
                c.draw(canvas );
        }
        else {
            canvas.drawText("loading...", 0, getHeight() / 2, colors[0]);
        }
    }

    public void setupFromGallery(String params, int position){
        readyToDraw = false;
        SetupAsync async = new SetupAsync();
        async.mPosition = position;
        async.execute(params);
    }

    class SetupAsync extends AsyncTask<String, Void, Void>{

        public List<MonadFakeChannel> newChannels = new ArrayList<MonadFakeChannel>();
        private int mPosition;

        @Override
        protected Void doInBackground(String... params){

            if (position != mPosition)
                return null;

            mInfo = params[0];
//            channels.clear();
            try {
                if (mInfo.startsWith("{")){
                    JSONArray jsChans = new JSONObject(mInfo).getJSONArray("channels");
                    for (int ils=0; ils<jsChans.length(); ils++ ){
                        setupChannelFromGallery(jsChans.getJSONObject(ils), 1);
                    }
                }
                else{
                    String[] sLines = mInfo.split(":");
                    float zoom = 1;

                    //the next lines should have X and Y coords
                    for (int ils=1; ils<sLines.length; ils++ ){

                        setupChannelFromGallery(sLines[ils], zoom);
                    }
                }
            }
            catch (Exception e){
                setupChannelFromGallery("1;0;0;1;1", 1);
            }
            if (mPosition == position){
                channels = newChannels;
                readyToDraw = true;
                postInvalidate();
            }
            return null;
        }

        private void setupChannelFromGallery(String channelData, float zoom){
            MonadFakeChannel chan ;

            if (!channelData.contentEquals("0")){

                String[] sCoords = channelData.split(";");

                if (sCoords.length > 1){
                    chan = new MonadFakeChannel(colors[Integer.parseInt(sCoords[0])]);
                    newChannels.add(chan);

                    // the first value should be the instrument setting
                    float width = getWidth();
                    float height = getHeight();
                    float ojX;
                    float ojY;
                    float ojEY;
                    for (int iss=1; iss<sCoords.length - 1; iss =iss+ 2 ){

                        ojX = zoom * Float.valueOf(sCoords[iss]);
                        ojY = Float.valueOf(sCoords[iss+1]);
                        if (ojY == -1){
                            //see if there's a next digit to steal
                            if (sCoords.length >= iss+3){
                                ojEY = Float.valueOf(sCoords[iss+3]);
                            }  else {
                                ojEY = 0;
                            }
                        }
                        else{
                            ojEY = ojY;
                        }
                        chan.addXY(ojX * width, (1 - ojEY) * height, ojX, ojY);
                    }

                }
            }
        }

        private void setupChannelFromGallery(JSONObject channelData, float zoom) throws JSONException {
            MonadFakeChannel chan ;
            JSONArray sCoords = channelData.getJSONArray("data");

            chan = new MonadFakeChannel(colors[channelData.getInt("instrument")]);
            newChannels.add(chan);

            // the first value should be the instrument setting
            float width = getWidth();
            float height = getHeight();
            float ojX, ojEX;
            float ojY, ojEY;
            for (int iss=0; iss<sCoords.length(); iss++ ){

                ojX = zoom * (float)sCoords.getJSONArray(iss).getDouble(0);
                ojEX = ojX;
                ojY = (float)sCoords.getJSONArray(iss).getDouble(1);
                if (ojY == -1.0f){
                    //see if there's a next digit to steal
                    if (iss + 1 < sCoords.length()){
                        ojEX = (float)sCoords.getJSONArray(iss+1).getDouble(0);
                        ojEY = (float)sCoords.getJSONArray(iss+1).getDouble(1);
                    }  else {
                        break;
                    }
                }
                else {
                    ojEY = ojY;
                }

                chan.addXY(ojEX * width, ojEY  * height, ojX, ojY);
            }
        }
    }

    public String getInfo(){
        return mInfo;
    }
}
