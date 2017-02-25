package com.monadpad.sketchatune2;

import android.graphics.Path;
import android.util.Log;
import com.monadpad.sketchatune2.dsp.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MonadChannel {

    private boolean looping = false;

    private long finishInitiatedTime = 0;
    private long finishLength = 0;
    private float fadeLevel = 0;

    private boolean addedTime = false;

    private float[] scale;
    private int octaves;
    private float base;

    private boolean envActive = false;
    private final WtOsc ugOscA1 = new WtOsc();
    private final ExpEnv ugEnvA = new ExpEnv();
    public final Dac ugDac = new Dac();

    private final Delay ugDelay = new Delay(UGen.SAMPLE_RATE / 2);
    private final Flange ugFlange = new Flange(UGen.SAMPLE_RATE / 64, 0.25f);

    private final ArrayList<Float> panHistory = new ArrayList<Float>();
    private final ArrayList<Float> xpHistory = new ArrayList<Float>();
    private final ArrayList<Float> ypHistory = new ArrayList<Float>();
    private final ArrayList<Long> tHistory = new ArrayList<Long>();
    private final List<Float> fHistory =  new ArrayList<Float>();
    private int currentI = 0;

    private int instrument;
    private boolean virgin = true;

    boolean free = true;

    private boolean hasPans = false;

    MonadChannelViewInfo mInfo;

    private float gain;

    int continuedAt = 0;

    private boolean autoTime;

    private MonadSettings mSettings;

    MonadChannelViewInfo getViewInfo(){
        return mInfo;
    }

    int mode = 0;

    boolean animatingAutoTime = false;
    int overlaps = 0;
    int overlapped = 0;
    long loopDuration = 0;


    public MonadChannel(int pinstrument, MonadSettings settings){
        mSettings = settings;
        mode = settings.testMode;
        this.autoTime = settings.autoTime;

        mInfo = new MonadChannelViewInfo(pinstrument);

        boolean delay, flange, softTimbre, softEnvelope, softe, softt;
        boolean saw = false;
        instrument = pinstrument;
        if (pinstrument == 0)
        {
            delay = true; flange = false; softt = true; softe = true;
        }
        else if (pinstrument == 1)
        {
            delay = false; flange = false; softt = true; softe = false;
        }
        else if (pinstrument == 2)
        {
            delay = true; flange = false; softt = false; softe = true;
        }
        else if (pinstrument == 3)
        {
            delay = true; flange = false; softt = false; softe = false;
        }
        else if (pinstrument == 4)
        {
            delay = false; flange = true; softt = false; softe = true;
        }
        else if (pinstrument == 5)
        {
            delay = false; flange = false; softt = false; softe = false;
        }
        else if (pinstrument == 6)
        {
            delay = false; flange = false; softt = false; softe = true;
        }
        else if (pinstrument == 7)
        {
            delay = false; flange = false; softt = false; softe = true;
            saw = true;
        }
        else if (pinstrument == 8)
        {
            delay = false; flange = false; softt = false; softe = false;
            saw = true;
        }
        else if (pinstrument == 9)
        {
            delay = true; flange = false; softt = false; softe = true;
            saw = true;
        }
        else if (pinstrument == 10)
        {
            delay = true; flange = false; softt = false; softe = false;
            saw = true;
        }
        else
        {
            delay = false; flange = false; softt = true; softe = true;
        }
        softEnvelope = softe;
        softTimbre = softt;

        scale = buildScale(settings.scale);
        octaves = settings.octaves;
        base = (float) settings.base;

        if (saw)
            ugOscA1.fillWithSaw();
        else if (softTimbre) {
            ugOscA1.fillWithHardSin(7.0f);
        } else {
            ugOscA1.fillWithSqrDuty(0.6f);
        }

        if (delay) {

            ugEnvA.chuck(ugDelay);

            if (flange) {
                ugDelay.chuck(ugFlange).chuck(ugDac);
            } else {
                ugDelay.chuck(ugDac);
            }
        } else {
            if (flange) {
                ugEnvA.chuck(ugFlange);
                ugFlange.chuck(ugDac);
            } else {
                ugEnvA.chuck(ugDac);
            }
        }


        ugOscA1.chuck(ugEnvA);
        if (!softEnvelope) {
            ugEnvA.setFactor(ExpEnv.hardFactor);
        }

        ugEnvA.setActive(true);
        envActive = true;

        setGain(0.75f);
    }

    public void setFreq(float freq){
        ugOscA1.setFreq(freq);
    }

    public void update(long nowInLoop){
        // ugDac.open();
        if (looping){
            if (virgin){
                // TODO find out Index Out Of Bounds as per crash reports
                if (tHistory.size() == 0)
                    return;

                //find where we are in the loop and set the right I
                if (autoTime && tHistory.get(0) < nowInLoop) {
                    currentI = tHistory.size();
                    overlapped = overlaps;
                }
                else if (autoTime && tHistory.get(0) >= nowInLoop) {
                    currentI = 0;
                    overlapped = 0;
                }
                else if (tHistory.get(tHistory.size() - 1) < nowInLoop) {
                    currentI = tHistory.size();
                    overlapped = overlaps;
                }
                else {
                    for (int ii=0; ii<tHistory.size(); ii++){
                        if (tHistory.get(ii) >= nowInLoop){
                            currentI = ii;
                            break;
                        }
                    }
                }
                virgin =false;

            }
            if (tHistory.size() > currentI) {
                try {
                    float freq = fHistory.get(currentI);
                    long currentT = tHistory.get(currentI);
                    if (freq == -1 ) {
                        mute();
                        currentI++;
                    }
                    else if (currentT < 0) {
                        currentI++;
                    }
                    else {
                        long overlappedTime = overlapped * loopDuration;
                        if (currentT - overlappedTime <= nowInLoop) {
                            if (!envActive) {
                                unmute();
                            }
                            ugOscA1.setFreq(freq);
                            if (hasPans && panHistory.size() > currentI)
                                ugDac.pan(panHistory.get(currentI));
                            currentI++;
                        }

                    }
                }
                catch (IndexOutOfBoundsException e){ //e.printStackTrace();
                }

                if (currentI == tHistory.size()) {
                    if (overlapped > 0 && tHistory.get(0) > nowInLoop) {
                        overlapped = 0;
                        currentI = 0;
                    }
                }
            }
            else{
                mute();
            }
        }

        if (finishLength > 0){
            float newfl = (float)(System.currentTimeMillis() - finishInitiatedTime) / (float)finishLength;
            if (newfl <= 1.0f && newfl != fadeLevel){
                fadeLevel = newfl;
                ugDac.fade(fadeLevel);
            }
        }

        if (mode == 0)
            ugDac.tick();

    }

    public void tick() {
        ugDac.tick();
    }



    public void addXY(long time, float pan, float x, float y){

        float freq;
        if (y < 0){
            looping = false;
            virgin = true;
            freq = -1;
        }
        else{

            //set the freq
            freq = buildFrequency(scale, octaves, y, base);
            setFreq(freq);
            if (pan != -2f){
                ugDac.pan(pan);

            }
        }

        if (x > -1){
            if (time > -1 || addedTime){
                tHistory.add(time);
                addedTime = true;

                //Log.d("MGH added time", Long.toString(time)) ;
            }
            if (pan != -2f){
                panHistory.add(pan);
                hasPans = true;
            }
            xpHistory.add(x);
            ypHistory.add(y);
            fHistory.add(freq);
        }

    }

    public float getHigh(){
        float high = 0;
        if (xpHistory.size() > 0){
            high = xpHistory.get(0);
            float newNumber;
            for (Float aXpHistory : xpHistory) {
                newNumber = aXpHistory;
                if (newNumber > high) high = newNumber;
            }
        }
        return high;
    }

    public float getLow(){
        float low = 0;
        if (xpHistory.size() > 0){
            low = xpHistory.get(0);
            float newNumber;
            for (Float aXpHistory : xpHistory) {
                newNumber = aXpHistory;
                if (newNumber < low) low = newNumber;
            }
        }
        return low;
    }


    public void prepareLoop(long loopDuration, float lineStart, float lineLength){


        if (!addedTime){
            tHistory.clear();
            for (Float aXpHistory : xpHistory) {

                tHistory.add((long) (((aXpHistory - lineStart) / lineLength) * (float) loopDuration));
            }
        }

        this.loopDuration = loopDuration;
        mute();
        currentI = 0;
        looping = true;

    }

    public void quantizeBeats(float bpm) {
        if (mSettings.libenizMode != MonadSettings.LibenizMode.OFF) {

            int bars = Math.max(1, (int)(loopDuration / (60000.0f / bpm) / 4));

            int divisions = 16 * bars;

            int beatDuration = (int)(loopDuration / divisions);
            int lastBeat = -1;
            int thisBeat;


            //Log.d("MGH beat quantizer", Long.toString(loopDuration));
            float time;
            for (int i = 0; i < tHistory.size(); i++) {

                thisBeat = Math.round((float)Math.abs(tHistory.get(i)) / beatDuration);
                time = beatDuration * thisBeat;

                if (thisBeat == lastBeat) {
                    time = -1 * time;
                }

                //Log.d("MGH beat quantizer ", Long.toString(tHistory.get(i)) + " >>> " + Float.toString(time));

                tHistory.set(i, (long)time);

                lastBeat = thisBeat;
            }

        }

    }

    public void popCherry(){
        virgin = false;
    }
    public void reset(){

        if (overlaps > overlapped){
            if (looping) {
                overlapped++;
            }
        }
        else {
            overlapped = 0;
            if(looping){
                mute();
            }
            currentI = 0;
        }
    }

    public void slowFinish(){
        finish(2500);
    }
    public void finish(){
        finish(500);
    }

    public void finish(int ftime){
        looping = false;
        mute();
        finishInitiatedTime = System.currentTimeMillis();
        finishLength = ftime;
    }

    public void fade(int length){
        finishInitiatedTime = System.currentTimeMillis();
        finishLength = length;
    }

    public long getFinishTime(){
        return finishInitiatedTime + finishLength;
    }

    public void mute(){
        ugEnvA.setActive(false);
        envActive = false;
    }
    public void unmute(){
        ugEnvA.setActive(true);
        envActive = true;
    }
    public void unloop(){
        unmute();
        looping = false;
    }
    public boolean isLooping() {
        return looping;
    }

    static float[] buildScale(String quantizerString) {
        if (quantizerString != null && quantizerString.length() > 0) {
            String[] parts = quantizerString.split(",");
            float[] scale = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                scale[i] = Float.parseFloat(parts[i]);
            }
            return scale;
        } else {
            return null;
        }
    }

    static float buildFrequency(final float[] scale, final int octaves, float input, float base) {
        input = Math.min(Math.max(input, 0.0f), 1.0f);
        //final float base = 24;        //final float base = 48;

        float mapped;
        if (scale == null) {
            mapped = base + input * octaves * 12.0f;
        } else {
            int idx = (int) ((scale.length * octaves + 1) * input);
            mapped = base + scale[idx % scale.length] + 12 * (idx / scale.length);
        }
        return (float) Math.pow(2, (mapped - 69.0f) / 12.0f) * 440.0f;

    }

    public String getChannelInfo(boolean autoTime) {

        //TODO gain and pan

        StringBuilder info = new StringBuilder();
        info.append("{\"instrument\":");
        info.append(instrument);
        info.append(", \"volume\":");
        info.append(getGain());
        info.append(", \"pan\":");
        info.append(getPan());
        info.append(", \"data\":[");

        boolean addComma = false;
        float y;

        for (int ix = 0; ix<xpHistory.size(); ix++){
            if (addComma)
                info.append(",");
            else
                addComma = true;

            info.append("[");
            info.append(xpHistory.get(ix));
            info.append(",");
            y = ypHistory.get(ix);
            if (y != -1.0f) y = 1.0f - y;
            info.append(y);
            if (autoTime) {
                info.append(",");
                // TODO find why this crashes as per reports
                if (tHistory.size() > ix)
                    info.append(tHistory.get(ix));
                else
                    info.append(0);

                if (panHistory.size() > ix){
                    info.append(",");
                    info.append(panHistory.get(ix));
                }
            }

            info.append("]");
        }
        info.append("]}");
        return info.toString();
    }

    public void close(){
        ugDac.close();
    }


    public void bend(float amt, float amtP){
        mInfo.path.offset(0, -1 * amt);
        for (int ii = 0; ii < ypHistory.size(); ii++){
            if (fHistory.get(ii) != -1){
                ypHistory.set(ii, ypHistory.get(ii) + amtP);
                fHistory.set(ii, buildFrequency(scale, octaves, ypHistory.get(ii), base));
            }
        }

    }

    public boolean hitTest(float x, float y){
        float radius = 20;
        return Math.abs(Math.max(mInfo.originX, radius) - x) < 35 && Math.abs(mInfo.originY - y) < 35;
    }

    public float[] getXYs(){
        int size = xpHistory.size();
        float[] ret = new float[size * 2];
        int i = 0;
        int j = 0;
        for (int ii = 0; ii < size; ii++) {
            ret[i] = xpHistory.get(ii);
            ret[i + 1] = ypHistory.get(j);
            j = j +1;
            i = i + 2;
        }
        return ret;
    }

    public int getXSize() {
        return xpHistory.size();
    }

    public void makeAutoTimePath(long loopDuration, float lineLength, float screenWidth, float screenHeight) {
        Path newPath = new Path();
        float yh, xh;
        boolean moved = false;
        if (xpHistory.size() > 0){
            mInfo.originX = tHistory.get(0) / (float) loopDuration * lineLength * screenWidth;
        }
        for (int ix = 0; ix < xpHistory.size(); ix++) {
            xh = tHistory.get(ix) / (float) loopDuration * lineLength * screenWidth;
            xpHistory.set(ix, xh);
            yh = ypHistory.get(ix);
            if (yh < 0) {
                moved = false;
            }
            else {
                yh = (1 - yh) * screenHeight;
                if (!moved) {
                    newPath.moveTo(xh, yh);
                    moved = true;
                }
                newPath.lineTo(xh, yh);
                newPath.moveTo(xh, yh);
            }
        }
        mInfo.path = newPath;
    }

    public void animateAutoTimePath(long loopDuration, float lineLength,
                                    float screenWidth, float screenHeight,
                                    float[] original, float percent,
                                    int size) {
        Path newPath = new Path();
        float yh, xh;
        boolean moved = false;
        if (xpHistory.size() > 0){
            mInfo.originX = tHistory.get(0) / (float) loopDuration * lineLength * screenWidth;
        }

        for (int ix = 0; ix < xpHistory.size(); ix++) {
            if (ix * 2 < original.length) {
                float originalX = original[ix * 2] * screenWidth;
                xh = tHistory.get(ix) / (float) loopDuration * lineLength * screenWidth;
                xh = originalX + (xh - originalX) * percent;
                xpHistory.set(ix, xh / screenWidth);
            }
            else {
                xh = xpHistory.get(ix) * screenWidth;
            }

            if (ypHistory.size() <= ix)
                continue;
            // lots of reported errors on the line below
            // maybe a timeing issue?
            // added the lines above to avoid it
            yh = ypHistory.get(ix);

            if (yh < 0) {
                moved = false;
            }
            else {
                yh = (1 - yh) * screenHeight;
                if (!moved) {
                    newPath.moveTo(xh, yh);
                    moved = true;
                }
                newPath.lineTo(xh, yh);
                newPath.moveTo(xh, yh);
            }
        }
        mInfo.path = newPath;
    }

    void rebuildFrequencies(String scale, int octaves, int base){
        this.scale = buildScale(scale);
        this.octaves = octaves;
        this.base = (float) base;

        fHistory.clear();
        float freq;
        for (Float y : ypHistory){
            if (y == -1){
                freq = -1;
            }
            else {
                freq = buildFrequency(this.scale, this.octaves, y, this.base);
            }
            fHistory.add(freq);
        }
    }

    public void pan(float v) {
        ugDac.pan(v);
    }

    public float getPan() {
        return ugDac.getLastPan();
    }

    public void setGain(float v) {
        gain = v;
        ugEnvA.setGain(2*v*v);
    }

    public float getGain(){
        return gain;
    }

    public void cancelRecordedPans() {
        hasPans = false;
        panHistory.clear();
    }

    public float[] getCurrentXY() {
        int i = currentI;
        float[] ret = {-1.0f, -1.0f};

        if (envActive && xpHistory.size() > i && ypHistory.size() > i){

            ret[0] = xpHistory.get(i) ;
            ret[1] = ypHistory.get(i);
        }

        return ret;
    }
}
