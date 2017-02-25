package com.monadpad.sketchatune2;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.os.AsyncTask;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MonadJam {
    private MonadThread mThread;

    public static int FADE_OUT_LENGTH = 5000;

    private long lastUp = 0;
    private long loopLengthCounter = 0;
    private long loopDuration = 0;

    private boolean loopIsSetup = false;
    private boolean threadRunning = false;

    private MonadChannel currentChannel = null;
    private MonadChannel lastChannel = null;

//    private String scale;
//    private int octaves;
//    private int autoremove;
//    private int base;
    private int scaleLength;

    private boolean currentChannelIsLive = false;

    private float lineStart = 0;
    private float lineLength = 0;

    private List<MonadChannel> channels = new CopyOnWriteArrayList<MonadChannel>();
    private List<MonadChannel> qChannels = new ArrayList<MonadChannel>();

    //boolean mGalleryState = true;

    private int nextInstrument = 0;

    private boolean cleanFromUndo = false;

    private boolean continuedLastChannel = false;

    private MonadJamSourceInfo mSourceInfo = new MonadJamSourceInfo();

    private Context context;

    private boolean lockScreen;
    private boolean isScreenLocked = false;

    private int testMode = 0;

//    boolean autoTime = false;
    boolean moveLinesInAutoTime = false;


    private long touchCounter = 0;

    private MonadSettings mSettings = new MonadSettings();

    long showTip = 0;

    private String clientVersion = "";


    public MonadJam(Context ctx){
        setup(ctx);
        useDefaultSettings();
        currentChannel = newChannel();

    }

    void setSettingsToDefault(){

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = settings.edit();
        edit.putString("quantizer", "0,2,4,5,7,9,11");
        edit.putString("octaves", "4");
        edit.putString("base", "36");
        edit.putString("autoremove", "10");
        edit.putBoolean("autotime", true);
        edit.commit();

        currentChannel = newChannel();
    }

    int getOctaves() {
        return mSettings.octaves;
    }
    int getScaleLength() {
        return scaleLength;
    }
    boolean getMoveLinesInAutoTime() {
        return moveLinesInAutoTime;
    }


    private void useDefaultSettings() {
        final SharedPreferences synthPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        moveLinesInAutoTime = synthPrefs.getBoolean("movelines", false);
        mSettings.autoTime = synthPrefs.getBoolean("autotime", true);
        mSettings.scale = synthPrefs.getString("quantizer", "0,2,4,5,7,9,11");
        mSettings.octaves = Integer.parseInt(synthPrefs.getString("octaves", "4"));
        String ar = synthPrefs.getString("autoremove", "10");
        if (ar.equalsIgnoreCase("OFF") || ar.startsWith("0"))
            mSettings.autoRemove = 0;
        else
            mSettings.autoRemove = Integer.parseInt(ar);
        mSettings.base = Integer.parseInt(synthPrefs.getString("base", "36"));
        if (MonadChannel.buildScale(mSettings.scale) == null) {
            scaleLength = 0;
        }
        else {
            scaleLength = MonadChannel.buildScale(mSettings.scale).length;
        }
    }

    public MonadJam(Context ctx, String info){
        setup(ctx);
        setupFromString(info, false);
    }

    public MonadJam(Context ctx, String info, boolean useDefaults){
        setup(ctx);
        setupFromString(info, useDefaults);
    }

    private void setup(Context ctx){
        context = ctx;

        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            clientVersion = pInfo.versionName;
        }
        catch (PackageManager.NameNotFoundException e) {
        }

        final SharedPreferences synthPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        lockScreen = synthPrefs.getBoolean("lockscreen", false);
        testMode = synthPrefs.getBoolean("testmode2", false) ? 1 : 0;

        mThread = new MonadThread(this);
        mThread.mode = testMode;
        currentChannel = newChannel();

    }

    private void setupFromString(String info, boolean useDefaults){
        if (info.startsWith("{")){
            try {

                JSONObject jsO = new JSONObject(info);

                loopDuration = jsO.getInt("duration");

                if (jsO.has("startLine"))
                    lineStart = (float)jsO.getDouble("startLine");
                else {
                    lineStart = 0;
                }

                if (jsO.has("lineLength"))
                    lineLength = (float)jsO.getDouble("lineLength");
                else {
                    lineLength = 0.85f;
                }
                mSettings.autoTime = jsO.has("autoTime") && jsO.getBoolean("autoTime");
                moveLinesInAutoTime = !jsO.has("moveLines") || jsO.getBoolean("moveLines");
                if (!useDefaults && jsO.has("ascale")){
                    //JSONArray jsScale = jsO.getJSONArray("sca")
                    mSettings.scale = jsO.getString("ascale").replace("[","").replace("]","");
                    mSettings.octaves = jsO.getInt("octaves");
                    mSettings.base= jsO.getInt("base");
                    //autoremove = Integer.valueOf(line1[7]);
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor edit = settings.edit();
                    edit.putString("quantizer", mSettings.scale);
                    edit.putString("octaves", Integer.toString(mSettings.octaves));
                    edit.putString("base", Integer.toString(mSettings.base));
                    edit.putBoolean("autotime", mSettings.autoTime);
                    edit.putBoolean("movelines", moveLinesInAutoTime);
                    //edit.putString("autoremove", line1[7]);
                    edit.commit();

                    ((MainActivity)context).updatePrefFragment(mSettings.autoTime, moveLinesInAutoTime);

                }
                else {
                    useDefaultSettings();
                }
                float[] ascale = MonadChannel.buildScale(mSettings.scale);
                if (ascale != null)
                    scaleLength = ascale.length;
                else
                    scaleLength = 0;


                if (jsO.has("channels")) {
                    JSONArray jsChans = jsO.getJSONArray("channels");
                    for (int ils=0; ils<jsChans.length(); ils++ ){

                        MonadChannel mpc = newChannel(jsChans.getJSONObject(ils));

                        if (mpc != null) {
                            mpc.popCherry();
                            mpc.prepareLoop(loopDuration, lineStart, lineLength);
                        }
                        channels.add(mpc);
                    }
                }

                if (channels.size() == 0){
                    cleanFromUndo = true;
                }

                loopIsSetup = true;
                mThread.setupLoop(loopDuration);
                if (jsO.has("started")) {
                    mThread.startLoop(jsO.getLong("started"));
                }
                else {
                    mThread.startLoop();
                }
                mThread.start();
                if (lockScreen) lockTheScreen();
                threadRunning = true;
            }
            catch (JSONException e) {
                String msg = context.getString(R.string.failed_to_load_string);
                if (e.getMessage() != null)
                    msg = msg + "\n\n" + e.getMessage();
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
        else{
            try{

                String[] sLines = info.split(":");

                //the first line should have the duration, linestart, and linelength
                String[] line1 = sLines[0].split(";");

                loopDuration = Long.valueOf(line1[1]);
                lineStart = Float.valueOf(line1[2]);
                lineLength = Float.valueOf(line1[3]);

                if (!useDefaults){
                    mSettings.scale = line1[4];
                    mSettings.octaves = Integer.valueOf(line1[5]);
                    mSettings.base= Integer.valueOf(line1[6]);
                    mSettings.autoRemove = Integer.valueOf(line1[7]);
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor edit = settings.edit();
                    edit.putString("quantizer", line1[4]);
                    edit.putString("octaves", line1[5]);
                    edit.putString("base", line1[6]);
                    edit.putString("autoremove", line1[7]);
                    edit.commit();
                }
                else {
                    useDefaultSettings();
                }

                //the next lines should have X and Y coords
                for (int ils=1; ils<sLines.length; ils++ ){

                    MonadChannel mpc = newChannel(sLines[ils]);

                    if (line1.length > 17 + 1 + (ils - 1) * 2){
                        float gain = Float.parseFloat(line1[17 + (ils - 1) * 2]);
                        float pan = Float.parseFloat(line1[17 + 1 + (ils - 1) * 2]);
                        mpc.setGain(gain);
                        mpc.pan(pan);
                    }

                    if (mpc != null) {
                        mpc.popCherry();
                        mpc.prepareLoop(loopDuration, lineStart, lineLength);
                    }
                    channels.add(mpc);
                }

                if (channels.size() > 0){
                    loopIsSetup = true;
                    mThread.setupLoop(loopDuration);
                    mThread.startLoop();
                    mThread.start();
                    if (lockScreen) lockTheScreen();
                    threadRunning = true;
                    //            fresh = false;
                }

            }
            catch (Exception e) {
                String msg = context.getString(R.string.failed_to_load_string);
                if (e.getMessage() != null)
                    msg = msg + "\n\n" + e.getMessage();
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

        }
    }


    public void stop(){
        loopIsSetup = false;
        mThread.interrupt();
    }
    public void niceStop(){
        stop(3000);
    }
    public void stop(int stopTime){
        if (mThread.isAlive()){
            mThread.reset(stopTime);

            if (isScreenLocked){
                ((Activity)context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                isScreenLocked = false;
            }
        }
        loopIsSetup = false;
    }

    public boolean isSetup(){
        return loopIsSetup;
    }

    public void addXY(float x, float y){
        addXYTP(x, y, -1, -2);

    }
    public void addXYTP(float x, float y, long time, float pan){
        if (!threadRunning) {
            mThread.start();
            threadRunning = true;
            lockTheScreen();
        }

        MonadChannel chan = currentChannel;
        if (y > -1){
            long nBreakThreshold = 2000;
            if (lastUp > 0 && System.currentTimeMillis() - lastUp < nBreakThreshold
                    && lastChannel != null) {
                if (showTip > 0) {
                    showTip = 0;
                }

                continuedLastChannel = true;
                currentChannel = lastChannel;
                chan = currentChannel;
                //currentChannel.animatingAutoTime = false;
                currentChannel.continuedAt = currentChannel.getXSize();
                currentChannel.unloop();

                // if this is the first track, make sure to keep counting
                if (channels.size() == 1 && !cleanFromUndo)
                    loopIsSetup = false;

            } else {
                if (x == -1f || currentChannelIsLive ||
                        (currentChannel.free && loopIsSetup && x > getBoundary())){
                    currentChannelIsLive = true;
                    currentChannel.unmute();
                    x = -1f;
                } else{
    //                onModify();
                    if (!loopIsSetup && loopLengthCounter == 0){
                        loopLengthCounter = System.currentTimeMillis();
                    }
                    else {

                    }

                    currentChannelIsLive = false;

                    if (currentChannel.free){
                        channels.add(currentChannel);

                        if (mSettings.autoRemove > 0 && channels.size() > mSettings.autoRemove){
                            doAutoRemove();
                        }

                        currentChannel.free = false;
                    }
                }
            }
            // -2 is mousedown
            if (time == -2) {
                if (continuedChannel()) {
                    time = System.currentTimeMillis() - touchCounter;
                }
                else if (isSetup()) {
                    touchCounter = getLoopStartTime();
                    time = System.currentTimeMillis() - touchCounter;
                }
                else {
                    touchCounter = System.currentTimeMillis();
                    time = 0;
                }
            }
            else if (time == -3) {
                time = System.currentTimeMillis() - touchCounter;
            }
            chan.addXY(time, pan, x, y);
            lastUp = 0;
        }
        else {
            if (currentChannelIsLive){
                lastUp = 0;
                currentChannelIsLive = false;
                currentChannel.getViewInfo().path = null;
                qChannels.add(currentChannel);
                currentChannel.slowFinish();
                currentChannel = newChannel();
            }
            else {
                continuedLastChannel = false;
                if (time == -3) {
                    time = System.currentTimeMillis() - touchCounter;
                }

                chan.addXY(time, pan, x, -1);
                if (y > -2) {
                    lastUp = System.currentTimeMillis();

                    lastChannel = currentChannel;
                    currentChannel = newChannel();

                    boolean okToStart = false;
                    if (!loopIsSetup) {
                        loopDuration = System.currentTimeMillis() - loopLengthCounter;
                        if (loopDuration < 500) {
                            showTip = System.currentTimeMillis();
                        }
                        okToStart = mThread.setupLoop(loopDuration);
                        if (mSettings.autoTime) {
                            lineStart =  0.0f;
                            lineLength = 0.85f;
                        }
                        else {
                            lineStart =  chan.getLow();
                            lineLength = chan.getHigh() - lineStart;
                        }

                        chan.popCherry();
                    }
                    else {
                        chan.overlaps = (int)Math.floor((double)time / (double)loopDuration);
                    }

                    chan.prepareLoop(loopDuration, lineStart, lineLength);

                    // if move lines is on, don't quantize the beats, it gets messed up
                    if (!(moveLinesInAutoTime && mSettings.autoTime))
                        chan.quantizeBeats(getBPM());

                    if (!loopIsSetup && okToStart){
                        mThread.startLoop();
                        loopIsSetup = true;
                    }
                }
                else {
                    chan.mute();
                }
            }
        }
    }

/*    public void makeAutoTimePath(float width, float height) {
        if (lastChannel != null) {
            lastChannel.makeAutoTimePath(loopDuration, lineLength, width, height);
        }
    }
*/
    public void animateAutoTimePath(float width, float height) {
        if (lastChannel != null) {
            lastChannel.animatingAutoTime = true;
            (new AnimateAutoTime()).execute(width, height);
        }
    }

    public boolean isCurrentChannelAnimating() {
        return currentChannel != null && currentChannel.animatingAutoTime;
    }

    public void resetLastUp() {
        lastUp = 0l;
    }

    class AnimateAutoTime extends AsyncTask<Float, Void, Void> {

        @Override
        protected Void doInBackground(Float... objects) {
            MonadChannel chan = lastChannel;
            float percentThruChange;
            long now;
            long startTime = 0;
            long animationLength = 700l;
            float[] original = new float[0];
            int xsize = 0;
            boolean finished = false;
            boolean restart = true;
            boolean chanLooping;
            while (!chan.isLooping() || (!finished && chan.animatingAutoTime)) {
                now = System.currentTimeMillis();
                chanLooping = chan.isLooping();
                if (chanLooping && restart) {
                    original = chan.getXYs();
                    xsize = chan.getXSize();
                    startTime = now;
                    restart = false;
                }

                percentThruChange = Math.min(1f, (float)(now - startTime) / (float) animationLength);

                chan.animateAutoTimePath(loopDuration, lineLength,
                        objects[0], objects[1], original, percentThruChange, xsize);

                if (!chanLooping) {
                    restart = true;
                    finished = false;
                }
                else if (percentThruChange == 1.0f) {
                    finished = true;
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            chan.animatingAutoTime = false;
            return null;
        }
    }


    public float getBoundary(){
        return lineStart + lineLength;
    }

/*  public void finishAllChannelsHard(){
        while( channels.size() > 0){
            channels.get(0).finish();
            channels.remove(0);
        }
    }

    public int getChannelCount(){
        return channels.size();
    }*/

    public void setLoopBack(){
        mThread.setLoopBack();
    }


    public String getGrooveInfo(){
        //duration,linestart,linelength
        if (!loopIsSetup)
            return "";

        StringBuilder info = new StringBuilder();
        info.append("{\"id\":");
        info.append(mSourceInfo.id);
        info.append(",\"channels\":[");
        boolean addComma = false;
        for (MonadChannel channel : channels) {
            if (addComma)
                info.append(",");
            else
                addComma = true;

            info.append(channel.getChannelInfo(mSettings.autoTime));
        }

        info.append("], \"autoTime\": ");
        info.append(mSettings.autoTime);
        info.append(", \"moveLines\": ");
        info.append(moveLinesInAutoTime);
        info.append(", \"ascale\": [");
        info.append(mSettings.scale);
        info.append("], \"octaves\": ");
        info.append(mSettings.octaves);
        info.append(", \"base\": ");
        info.append(mSettings.base);
        info.append(", \"artist\": ");
        info.append("\"").append(mSourceInfo.artist).append("\"");
        info.append(", \"duration\" : ");
        info.append(loopDuration);
        info.append(", \"startLine\" : ");
        info.append(lineStart);
        info.append(", \"lineLength\" : ");
        info.append(lineLength);
        info.append(", \"autoRemove\" : ");
        info.append(mSettings.autoRemove);
        info.append(", \"client\" : \"");
        info.append(context.getResources().getString(R.string.app_name));
        info.append("\", \"clientVersion\" : \"");
        info.append(clientVersion);
        info.append("\" }");


        return info.toString();
    }

    public void bend(float amt, float amtP){
        for (MonadChannel mc : channels){
            mc.bend(amt, amtP);
        }
    }

/*    public long getDuration(long duration) {
        long ret = 0;
        if (duration > 0){
            float bpm = 240000f / (float)duration;
            int bmp2 = Math.round(bpm);
            ret = (long)(240000f / bmp2);
            //    ret = duration;
        }
        return ret;
    }        */

    MonadChannel newChannel(){
        return new MonadChannel(nextInstrument, mSettings);
    }

    MonadChannel newChannel(String channelData){

        MonadChannel chan = null;

        if (!channelData.contentEquals("0")){

            String[] sCoords = channelData.split(";");

            if (sCoords.length > 1){

                chan = new MonadChannel(Integer.valueOf(sCoords[0]), mSettings);
                chan.mute();

                float ojX;
                float ojY;
                for (int iss=1; iss<sCoords.length - 1; iss =iss+ 2 ){

                    ojX = Float.valueOf(sCoords[iss]);
                    ojY = Float.valueOf(sCoords[iss+1]);
                    chan.addXY(-1, -2, ojX, ojY);
                }
            }
        }
        return chan;
    }

    MonadChannel newChannel(JSONObject channelData) throws JSONException {

        JSONArray jsA = channelData.getJSONArray("data");

        MonadChannel chan = new MonadChannel(channelData.getInt("instrument"), mSettings);
        chan.mute();

        try {
            double gain = channelData.getDouble("volume");
            double pan = channelData.getDouble("pan");
            chan.setGain((float)gain);
            chan.pan((float)pan);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }


        float ojX;
        float ojX2;
        float ojY;
        long ojT;
        float pan;
        for (int iss=0; iss<jsA.length(); iss++ ){

            ojX = (float)jsA.getJSONArray(iss).getDouble(0);
            ojY = (float)jsA.getJSONArray(iss).getDouble(1);
            if (ojY > -1){
                ojY = 1 - ojY;
            }
            if (mSettings.autoTime){
                pan = (float)jsA.getJSONArray(iss).optDouble(3, -2);
                ojT = jsA.getJSONArray(iss).getLong(2);
                ojX2 = lineLength * (float)ojT / (float)loopDuration;
                if (moveLinesInAutoTime) {
                    ojX = ojX2;
                }
                chan.overlaps = (int)Math.floor(ojX2);
                chan.addXY(ojT, pan, ojX, ojY);
            }
            else {
                chan.addXY(-1, -2, ojX, ojY);
            }
        }
        return chan;
    }


    List<MonadChannel> getChannels(){
        return channels;
    }
    List<MonadChannel> getQChannels(){
        return qChannels;
    }

    public float getLineStart(){
        return lineStart;
    }

    public float getLineLength(){
        return lineLength;
    }

    void addPath(Path p, float originX, float originY){
        if (currentChannel != null){
            currentChannel.getViewInfo().path = p;
            currentChannel.getViewInfo().originX = originX;
            currentChannel.getViewInfo().originY = originY;
        }
    }
    void addTouchList(MonadPad3DView.TouchList tl){
        if (currentChannel != null){
            currentChannel.getViewInfo().setTouchList( tl );
        }
    }

    void changeNextInstrument(int nextInstrument){
        this.nextInstrument = nextInstrument;
        currentChannel = newChannel();
    }

    MonadChannel getLiveChannel(){
        if (currentChannelIsLive){
            return currentChannel;
        }
        else
            return null;
    }

    public void undo(){
        if (channels.size() > 0){
            MonadChannel chan = channels.get(channels.size() - 1);
            undoChannel(chan);
            lastUp = 0;
        }
    }

    void undoChannel(MonadChannel chan){
        chan.finish();
        channels.remove(chan);
        qChannels.add(chan);

        if (channels.size() == 0){
            cleanFromUndo = true;
        }
    }

    void doAutoRemove(){
        //TODO glithcy performance here? maybe to do with fade calculations? maybe not

        if (callbackForAutoRemove != null) {
            callbackForAutoRemove.run();
        }

        undoChannel(channels.get(0));
    }

    public void refreshPreferences() {
        useDefaultSettings();

        for (MonadChannel mpc : channels){
            mpc.rebuildFrequencies(mSettings.scale, mSettings.octaves, mSettings.base);
        }

    }

    public void refreshPreference(Preference preference, SharedPreferences synthPrefs) {

        String key = preference.getKey();
        if (key.equals("autotime")){
            mSettings.autoTime = synthPrefs.getBoolean("autotime", true);
        }

    }

    public void refreshPreference(Preference preference, Object o) {

        String key = preference.getKey();
        if (key.equals("autoremove")){
            String ar = o.toString();
            if (ar.equalsIgnoreCase("OFF"))
                mSettings.autoRemove = 0;
            else
                mSettings.autoRemove = Integer.parseInt(ar);
        }
        else {
            if (key.equals("quantizer")){
                mSettings.scale = o.toString();
                if (MonadChannel.buildScale(mSettings.scale) == null) {
                    scaleLength = 0;
                }
                else {
                    scaleLength = MonadChannel.buildScale(mSettings.scale).length;
                }
            }
            else if (key.equals("octaves")){
                mSettings.octaves = Integer.parseInt(o.toString());
            }
            else if (key.equals("base")){
                mSettings.base = Integer.parseInt(o.toString());
            }

            // the  basic settings of the music have changed
            // RECALCULATING!!!
            for (MonadChannel mpc : channels){
                mpc.rebuildFrequencies(mSettings.scale, mSettings.octaves, mSettings.base);
            }
        }

        currentChannel = newChannel();

    }


/*    public MonadChannel loadChannel(String sLine) {
        MonadChannel mpc = newChannel(sLine);
        channels.add(mpc);
        mpc.prepareLoop(loopDuration, lineStart, lineLength);
        return mpc;
    }
*/

    public MonadChannel loadChannel(JSONObject channelData) throws JSONException {
        MonadChannel mpc = newChannel(channelData);
        channels.add(mpc);
        mpc.prepareLoop(loopDuration, lineStart, lineLength);
        return mpc;
    }

    public void finishAllChannels(){
        while( channels.size() > 0){
            channels.get(0).finish();
            qChannels.add(channels.get(0));
            channels.remove(0);
        }
        cleanFromUndo = true;
    }

    public void adjustBounds(float lineStart, float lineLength){
        this.lineStart = lineStart;
        this.lineLength = lineLength;
    }

    public float getNowInLoop() {
        if (isSetup())
            return (float)(System.currentTimeMillis() - mThread.getLoopCounter()) / (float)loopDuration;
        else
            return 0;
    }

    public float getBPM(){
        float ret = 0f;

        if (isSetup()){
            ret = 240000f / ((float)loopDuration);
        }

        while (ret < 68.0f) {
            ret = Math.max(1.0f, ret) * 2.0f;
        }
        while (ret > 208.0f) {
            ret = ret / 2.0f;
        }

        return ret;
    }

    public int getDuration() {
        return (int)loopDuration;
    }

    public void fadeOut() {
        for (MonadChannel channel : channels)
            channel.fade(FADE_OUT_LENGTH);
        mThread.resetHardIn(FADE_OUT_LENGTH);
    }

    public boolean continuedChannel(){
        return continuedLastChannel;
    }

    public MonadJamSourceInfo getSourceInfo(){
        return mSourceInfo;
    }

    public void setSourceInfo(MonadJamSourceInfo info){
        mSourceInfo = info;

    }

    public void cancelCurrentChannel() {
        if (currentChannelIsLive){
            addXY(0, -1);
        }
        else if (!currentChannel.free){
            addXY(0, -1);
            undo();
        }
    }

    private void lockTheScreen(){
        if (lockScreen){

            Activity activity = (Activity) context;
            Display getO = activity.getWindowManager().getDefaultDisplay();
            if (getO.getWidth() > getO.getHeight()){
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
            else {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            isScreenLocked = true;
        }
    }

    long getLoopStartTime() {
        return mThread.getLoopCounter();
    }

    private Runnable callbackForAutoRemove = null;
    void setAutoRemoveCallback(Runnable callback) {
        callbackForAutoRemove = callback;
    }

    public void unmuteCurrentChannel() {
        if (currentChannel != null) {
            currentChannel.unmute();
        }

    }

    public void setAutoTime(boolean setting) {
        mSettings.autoTime = setting;
    }

    public boolean getAutoTime() {
        return mSettings.autoTime;
    }

}
