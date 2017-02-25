package com.monadpad.sketchatune2;

import android.animation.ObjectAnimator;
import android.app.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.monadpad.sketchatune2.dsp.SampleRate;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity extends Activity
{

    private static final int DIALOG_SAVE = 1;

    private Fragment settings;

    private boolean ratingsOpen = false;
    private boolean settingsOpen = false;
    private String last = "";
    private String intentString = "";
    private String saveString = "";

    private MonadJamSourceInfo intentSource;
    private MonadView mpad;
    private MonadJam mJam;

    private boolean isLoaded;

    private int instrument = 0;
    private String lastClear = "";

    private boolean drumMachine = false;
    private boolean drumMachineStarted = false;

    private boolean wentToSettings = false;

    private String shareUrl;
    private String saveUrl;

    boolean showAppBar = false;
    boolean isShowingAppBar = false;

    private boolean usedFakeActionBar = false;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        shareUrl = getString(R.string.url_main) +
                getString(R.string.url_share);
        saveUrl = getString(R.string.url_main) +
                getString(R.string.url_grooves);


        if (Build.VERSION.SDK_INT < 11){

            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN|
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            Log.d("MGH screen height", Integer.toString(metrics.heightPixels));
            if (metrics.heightPixels >= 600) {
                usedFakeActionBar = true;
            }


        }

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("useLowSampleRate", false)) {
            SampleRate.setLow();
        }

        setContentView(R.layout.main);

        mpad = (MonadView)findViewById(R.id.mpad);

        if (Build.VERSION.SDK_INT >= 11){
            settings = getFragmentManager()
                    .findFragmentById(R.id.settings_fragment);
        }

        findViewById(R.id.settings_layout).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                mpad.closeMenus();
                if (settingsOpen){
                    closeMenus();
                }
                else {
                    if (Build.VERSION.SDK_INT >= 11) {
                        ObjectAnimator anim = ObjectAnimator.ofInt(MainActivity.this,
                                "panelLeft", 0, settings.getView().getWidth() * -1);
                        anim.setDuration(300);
                        anim.start();
                        settingsOpen = true;
                    }
                    else {
                        wentToSettings = true;
                        startActivity(new Intent(MainActivity.this, SynthPreferencesActivity.class));
                    }
                }
            }

        });

        findViewById(R.id.settings_layout).setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                DisplayMetrics dm = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(dm);
                Toast.makeText(MainActivity.this, "Screen size \n " + dm.widthPixels + " x " + dm.heightPixels,
                        Toast.LENGTH_LONG).show();

                return false;
            }

        });

        findViewById(R.id.clearButton).setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                closeMenus();
                clearButton();
            }
        });
        findViewById(R.id.clearButton).setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                closeMenus();
                longClearButton();
                return true;
            }
        });

        findViewById(R.id.clearAll).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                closeMenus();
                clearButton();

                sendClearAllBroadCast();
            }
        });

        findViewById(R.id.undoButton).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeMenus();
                if (!mpad.undo() && lastClear.length() > 0) {
                    loadLast(lastClear);
                    onSetupLoop();
                }
            }
        });
        findViewById(R.id.undoButton).setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                closeMenus();
                mpad.undoDrag();
                return true;
            }
        });
        Button inst = (Button)findViewById(R.id.instrument);
        inst.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                closeMenus();
                mpad.changeInstrument();
            }
        });


        findViewById(R.id.abc).setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                closeMenus();
                mpad.abc();
            }

        });

        findViewById(R.id.levels).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeMenus();
                mpad.levels();
            }
        });

        findViewById(R.id.finishButton).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                view.setVisibility(View.INVISIBLE);

                Log.d("MGH", "finishbuttonclicked");

                findViewById(R.id.instrument).setVisibility(View.VISIBLE);
                findViewById(R.id.abc).setVisibility(View.VISIBLE);
                findViewById(R.id.levels).setVisibility(View.VISIBLE);
                findViewById(R.id.undoButton).setVisibility(View.VISIBLE);
                findViewById(R.id.clearButton).setVisibility(View.VISIBLE);

                if (isShowingAppBar) {
                    findViewById(R.id.clearAll).setVisibility(View.VISIBLE);

                }

                View cap = findViewById(R.id.change_settings_caption);
                if (cap != null){
                    cap.setVisibility(View.VISIBLE);
                    findViewById(R.id.select_color_caption).setVisibility(View.VISIBLE);

                }

                findViewById(R.id.settings_layout).setVisibility(View.VISIBLE);
                mpad.levels();
            }
        });

        findViewById(R.id.drummachine).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent drumIntent = getPackageManager().getLaunchIntentForPackage("com.monadpad.omgdrums");
                if (drumIntent != null) {
                    drumIntent.putExtra("bpm", mJam.getBPM());
                    drumIntent.putExtra("started", mJam.getLoopStartTime());
                    startActivity(drumIntent);

                }
                else {
                    startActivity(new Intent(MainActivity.this, GetOMGDrumsActivity.class));
                }

            }
        });

        if (usedFakeActionBar) {

            findViewById(R.id.fakeactionbar).setVisibility(View.VISIBLE);
            findViewById(R.id.fab_open).setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    startActivity(new Intent(getApplicationContext(), GalleryListActivity.class));
                }
            });
            findViewById(R.id.fab_save).setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    share();
                }
            });

        }

        Intent intent = getIntent();
        boolean usedIntent = setupFromIntent(intent);

        // if we have an already running mJam, we need that foremost
        MonadJam jam = (MonadJam)getLastNonConfigurationInstance();
        if (jam != null) {
            if (usedIntent) {
                jam.niceStop();
            }
            else {
                mJam = jam;
                mpad.setJam(jam);
                isLoaded = true;
            }

        }

        if (!isLoaded && !usedIntent){
            if (savedInstanceState != null && savedInstanceState.getString("LoadLast") != null){
                if (savedInstanceState.getString("LoadLast").length() > 0){
                    last = savedInstanceState.getString("LoadLast");
                }
            }
        }


        updatePreference(("quantizer"), prefs.getString("quantizer", "0,2,4,5,7,9,11"));
        updatePreference(("base"), prefs.getString("base", "37"));
        updatePreference(("octaves"), prefs.getString("octaves", "4"));
        updatePreference(("autoremove"), prefs.getString("autoremove", "10"));



    }



    public void onBackPressed() {
        if (settingsOpen)
            closeMenus();
        else if (mpad.inTutorial())
            mpad.resetClear();
        else
            super.onBackPressed();
    }
    public boolean closeMenus(){

        if (ratingsOpen) {
            findViewById(R.id.rating_panel).setVisibility(View.GONE);
            ratingsOpen = false;
        }

        if (settingsOpen){
            ObjectAnimator anim = ObjectAnimator.ofInt(this,
                    "panelLeft", settings.getView().getWidth() * -1, 0);
            anim.setDuration(300);
            anim.start();
            settingsOpen = false;
            return true;
        }

        return false;
    }

    public void setPanelLeft(int paramInt){

        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)settings.getView().getLayoutParams();
        lp.leftMargin = paramInt;
        settings.getView().setLayoutParams(lp);
    }

    protected void onNewIntent(Intent intent){
        MonadJam jam = mJam;
        if (setupFromIntent(intent)) {
            isLoaded = false;
            if (jam != null && jam.isSetup()){
                jam.niceStop();
            }
        }
    }



    protected Dialog onCreateDialog(int dialog){

        switch (dialog){

            case DIALOG_SAVE:

                final Dialog dl = new Dialog(this);
                dl.setTitle(getString(R.string.save_dialog_title));
                dl.setContentView(R.layout.share);

                final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                final String artist = settings.getString("artist", "");
                ((EditText)dl.findViewById(R.id.txtArtist)).setText(artist);

                final EditText editText = (EditText) dl.findViewById(R.id.txtOjName);
                final Button okButton = (Button)dl.findViewById(R.id.okButton);

                okButton.setEnabled(false);

                editText.addTextChangedListener(new TextWatcher() {
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }
                    public void afterTextChanged(Editable editable) {
                        okButton.setEnabled(editable.length() > 0);

                    }
                });

                dl.findViewById(R.id.cancelButton).setOnClickListener( new View.OnClickListener() {
                    public void onClick(View v) {
                        removeDialog(DIALOG_SAVE);
                    }
                });


                okButton.setOnClickListener( new View.OnClickListener() {
                    public void onClick(View v) {

                    String name = editText.getText().toString();
                    String newArtist = ((EditText)dl.findViewById(R.id.txtArtist)).getText().toString();

                    if (!artist.equals(newArtist) ){
                        SharedPreferences.Editor edit = settings.edit();
                        edit.putString("artist", newArtist);
                        edit.commit();
                    }

                    removeDialog(DIALOG_SAVE);

                    MonadJamSourceInfo source = mJam.getSourceInfo();
                    source.artist = newArtist;
                    source.title = name;

                    boolean pub = ((CheckBox)dl.findViewById(R.id.chkGallery)).isChecked();
                    new SaveToCmc(MainActivity.this).executeUpdate(source, pub);

                    String grooveInfo = mJam.getGrooveInfo();
                    new SaveToSd(MainActivity.this).execute(grooveInfo);

                    SendJam sendJam = new SendJam();
                    sendJam.isPublic = pub;
                    sendJam.artist = source.artist;
                    sendJam.jamName = name;

                    sendJam.execute(grooveInfo);

                    }
                } );
                return dl;
        }
        return null;
    }


    @Override
    public void onResume(){
        super.onResume();
        if (!isLoaded){
            if (intentString.length() > 0){
                if (!intentString.equals("loading")) {
                    loadString(intentString);
                    if (intentSource != null){
                        mJam.setSourceInfo(intentSource);
                        intentSource = null;
                    }
                }
            }
            else if (last.length() > 0){
                loadLast(last);

                if (intentSource != null){

                    mJam.setSourceInfo(intentSource);
                    intentSource = null;
                }
            }
            else {
                mJam = new MonadJam(this);
                mpad.setJam(mJam);
            }
        }
        else {
            if (wentToSettings) {
                mJam.refreshPreferences();
                mpad.setPreferences();

                wentToSettings = false;
            }
        }
        intentString = "";
        last = "";
        if (mJam != null) {
            mpad.instrumentSettings(instrument);
            mpad.setPreferences();
        }
        isLoaded = true;

        refreshPreferences();

    }



    private void loadLast(String lastString){
        if (lastString != null && lastString.length() > 0){
            mJam = new MonadJam(this, lastString, true);
            mpad.setJam(mJam);

        }
        last = "";
    }

    private boolean loadString(String loadString){
        boolean ret = false;
        if (loadString.length() > 0){
            mJam = new MonadJam(this, loadString);
            mpad.setJam(mJam);
            ret = true;
        }

        return ret;
    }

    public void onPause(){
        super.onPause();

        if (mJam != null && mJam.isSetup()) {
            last = mJam.getGrooveInfo();
            intentSource = mJam.getSourceInfo();

            if (drumMachine && !isFinishing()) {
            }
            else {
                mJam.niceStop();
                isLoaded = false;
            }
        }
        else {
            isLoaded = false;
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        Object o = null;
        if (mJam != null && mJam.isSetup()) {
            o = mJam;
        }
        return o;
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {

        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("lastClear", lastClear);
        savedInstanceState.putInt("instrument", instrument);
        savedInstanceState.putString("saveString", saveString);
        savedInstanceState.putStringArray("groovelets", mpad.getGroovelets());

        MonadJamSourceInfo source;

        if (mJam != null && mJam.isSetup()) {
            last = mJam.getGrooveInfo();
            source = mJam.getSourceInfo();
        }
        else {
            source = intentSource;
        }

        if (source != null){
            savedInstanceState.putInt("gallerySource", source.source);
            savedInstanceState.putLong("galleryId", source.id);
            savedInstanceState.putString("galleryArtist", source.artist);
            savedInstanceState.putString("galleryTitle", source.title);
        }

        savedInstanceState.putString("LoadLast",  last);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null){
            if (savedInstanceState.getString("LoadLast") != null) {
                last = savedInstanceState.getString("LoadLast");
            }
            lastClear = savedInstanceState.getString("lastClear");
            instrument = savedInstanceState.getInt("instrument");
            saveString = savedInstanceState.getString("saveString");

            if (savedInstanceState.getInt("gallerySource") > 0){

                intentSource = new MonadJamSourceInfo();
                intentSource.title = savedInstanceState.getString("galleryTitle");
                intentSource.artist= savedInstanceState.getString("galleryArtist");
                intentSource.id = savedInstanceState.getLong("galleryId");
                intentSource.source = savedInstanceState.getInt("gallerySource");
            }

            String[] gs = savedInstanceState.getStringArray("groovelets");
            if (gs != null) {
                mpad.setGroovelets(gs);
            }
        }
    }

    public void onSetupLoop() {

        if (drumMachine) {

            float bpm = mJam.getBPM();

            Intent i = new Intent();
            i.setAction("com.androidinstrument.drum.SETBPMEXTERNAL");
            i.putExtra("bpmval", bpm);
            sendBroadcast(i);

            if (!drumMachineStarted) {
                i = new Intent();
                i.setAction("com.androidinstrument.drum.STARTPLAYBACK");
                sendBroadcast(i);
                drumMachineStarted = true;
            }
        }

    }

    public void updatePrefFragment(boolean autoTime, boolean moveLinesInAutoTime) {
        if (settings != null) {
            PreferenceFragment prefFrag = (PreferenceFragment)getFragmentManager()
                    .findFragmentById(R.id.pref_fragment);
            ((CheckBoxPreference)prefFrag.findPreference("autotime")).setChecked(autoTime);
            ((CheckBoxPreference)prefFrag.findPreference("movelines")).setChecked(moveLinesInAutoTime);
        }
    }

    private class SendJam extends AsyncTask<String, Void, String> {

        private boolean saved = false;
        private String desc = "";
        private ProgressDialog progress;

        boolean isPublic = false;
        String artist = "";
        String jamName = "";

        protected String doInBackground(String... urls) {

            SaveToCmc save = new SaveToCmc(MainActivity.this);
            saved = save.execute(saveUrl, urls[0], artist, jamName, isPublic);
            desc = save.desc;
            return save.responseString;
        }

        protected void onPreExecute(){

            progress = ProgressDialog.show(MainActivity.this, "",
                    getString(R.string.save_progress), true);
        }

        protected void onPostExecute(String result) {
            try { // reported erros on dismiss
                progress.dismiss();
            } catch (Exception e) {}

            if (saved) {
                String[] jamId = result.split(";");
                if (jamId.length > 1 && jamId[1].length() > 0 && result.startsWith("good")){
                    Log.d("MGH", jamId[1]) ;

                    try {
                        long id = Long.parseLong(jamId[1]);
                        shareId(id);

                        mJam.getSourceInfo().id = id;
                        if (jamId.length > 2)
                            mJam.getSourceInfo().authCode = jamId[2];

                    }
                    catch (Exception e) {
                        saved = false;
                        desc = "Couldn't parse response";
                    }

                }
                else {
                    saved = false;
                }
            }

            if (!saved ) {
                if (desc == null || desc.length() == 0)
                    desc = getString(R.string.something_went_wrong_connecting_to_cmc);

                Toast.makeText(MainActivity.this,
                        desc,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void shareId(long id) {
        String actionSend = Intent.ACTION_SEND;
        Intent shareIntent = new Intent(actionSend);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareUrl + Long.toString(id));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_groove_title)));
    }

    private void toast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private boolean setupFromIntent(Intent intent){
        boolean ret= false;
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String uri = intent.getData().toString().replace("cloudmusiccompany2", "cloudmusiccompany");
            //String uri = "http://10.0.2.2:8888/?id=177";
            download(uri);
            intentString = "loading";
            ret = true;
            intent.setAction(Intent.ACTION_MAIN);

        }
        else {
            if (intent.getExtras() != null && intent.getExtras().getString("grooveInfo") != null){

                intentSource = new MonadJamSourceInfo();
                intentSource.source = MonadJamSourceInfo.GALLERY_SOURCE_USER;
                intentSource.artist = intent.getStringExtra("galleryArtist");
                intentSource.id = intent.getLongExtra("galleryId", 0);
                intentSource.title = intent.getStringExtra("galleryTitle");
                if (intentSource.artist == null)
                    intentSource.artist = "";
                else
                    intentSource.source = MonadJamSourceInfo.GALLERY_SOURCE_GALLERY;
                if (intentSource.title == null)
                    intentSource.title = "";

                if (intent.getBooleanExtra("sd", false)){
                    intentSource.source = MonadJamSourceInfo.GALLERY_SOURCE_SD;
                }

                intentString = intent.getExtras().getString("grooveInfo");
                intent.removeExtra("grooveInfo");
                ret = true;

                if (intentSource.id > 0) {
                    final long id = intentSource.id;
                    final View panel = findViewById(R.id.rating_panel);
                    final RatingBar rbar = (RatingBar)findViewById(R.id.ratingbar);
                    rbar.setRating(0.0f);
                    panel.setVisibility(View.VISIBLE);
                    rbar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                        public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                            panel.setVisibility(View.GONE);
                            new PostRating(MainActivity.this, v, id);
                        }
                    });
                    ratingsOpen = true;
                }
            }
            else if (!isLoaded && intent.getExtras() != null && intent.getExtras().containsKey("bpm")) {
                int bpm = intent.getExtras().getInt("bpm");
                int duration = 8 * 60000 / bpm;

                findViewById(R.id.clearAll).setVisibility(View.VISIBLE);
                findViewById(R.id.drummachine).setVisibility(View.VISIBLE);

                long started = intent.getLongExtra("started", System.currentTimeMillis());

                intentString = "{duration: " + Integer.toString(duration) +
                        ", started: " + Long.toString(started)  +"}";

                if (!isShowingAppBar)
                    showAppBar = true;

                ret = true;
            }
        }
        return ret;
    }


    private void download(String cUrl){
        GetJam g = new GetJam(cUrl);
        g.execute();
    }

    private class GetJam extends AsyncTask<Void, Void, Void> {

        String url;
        public GetJam(String url) {

            // if it has share.htm redirect

            String u = Uri.parse(url).getQueryParameter("u");
            String substring = "share.htm?u=";
            int substringStart = url.indexOf(substring);
            if (u != null && substringStart > -1) {

                 url = url.substring(0, substringStart) + u;

            }

            if (url.startsWith("sketchatune://"))
                url = url.replace("sketchatune://", "http://");

            this.url = url + "&aclient=true";
        }

        private String ret;

        ProgressDialog dialog;

        @Override
        public void onPreExecute(){
            dialog = ProgressDialog.show(MainActivity.this, "",
                    getString(R.string.loading_please_wait), true);
        }


        protected Void doInBackground(Void... urls) {
            ret = "";
            HttpClient httpclientup = new DefaultHttpClient();
            try {
                HttpResponse response = httpclientup.execute(new HttpGet(url));
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    ret = out.toString();

                }else{
                    ret = statusLine.getReasonPhrase();
                }

            } catch (ClientProtocolException e) {
                ret = e.getMessage();
                e.printStackTrace();
            } catch (IOException e) {
                ret = e.getMessage();
                e.printStackTrace();
            }
            catch (Exception e){
                ret = e.getMessage();
            }
            return null;
        }

        protected void onPostExecute(Void v) {
            try {
                dialog.dismiss();
            } catch (Exception e) {}

            if (ret.startsWith("{") || ret.startsWith("good")){
                if (loadString(ret)) {
                    String sId = Uri.parse(url).getQueryParameter("id");
                    if (sId != null && sId.length() > 0) {
                        final int id = Integer.parseInt(sId);
                        mJam.getSourceInfo().id = id;
                        //todo get the artist and groove name?
                        final View bar = findViewById(R.id.rating_panel);
                        bar.setVisibility(View.VISIBLE);
                        ((RatingBar)findViewById(R.id.ratingbar)).
                                setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                                    public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                                        bar.setVisibility(View.GONE);
                                        new PostRating(MainActivity.this, v, id);
                                    }
                                });
                        ratingsOpen = true;
                    }
                }
            }
            else{
                Toast.makeText(MainActivity.this,
                        getString(R.string.something_went_wrong_connecting_to_cmc)
                                + "\n\n" + ret, Toast.LENGTH_LONG ).show();
            }
        }
    }

    public void loadGroove(String s) {
        mJam.stop();
        mJam = new MonadJam(this, s);
        mpad.setJam(mJam);
    }


    public void newColor(int p, int pos) {
        GradientDrawable sd = (GradientDrawable)findViewById(R.id.instrument).getBackground().mutate();
        int[] ccc = {mpad.colors[0].getColor(), p};
        try {
            //api 16, apparently
            sd.setColors(ccc);
        } catch (NoSuchMethodError o) {
            sd.setColor(p);
        }

        sd.invalidateSelf();

        instrument = pos;
    }

    public void changingState(int newState){
        if (newState == MonadView.STATE_CHANGE_LEVELS){
            findViewById(R.id.finishButton).setVisibility(View.VISIBLE);
            findViewById(R.id.instrument).setVisibility(View.GONE);
            findViewById(R.id.abc).setVisibility(View.GONE);
            findViewById(R.id.levels).setVisibility(View.GONE);
            findViewById(R.id.undoButton).setVisibility(View.GONE);
            findViewById(R.id.clearButton).setVisibility(View.GONE);
            findViewById(R.id.clearAll).setVisibility(View.GONE);

            View cap = findViewById(R.id.change_settings_caption);
            if (cap != null){
                cap.setVisibility(View.GONE);
                findViewById(R.id.select_color_caption).setVisibility(View.GONE   );

            }

            findViewById(R.id.settings_layout).setVisibility(View.GONE);
        }
        else if (newState == MonadView.STATE_MID_DRAW) {
//            findViewById(R.id.keepRollingButton).setVisibility(View.VISIBLE);
        }
        else if (newState == MonadView.STATE_DRAW) {
//            Button rollingButton = (Button)findViewById(R.id.keepRollingButton);
//            if (!rollingButton.isPressed()) {
//                rollingButton.setVisibility(View.GONE);
//            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (usedFakeActionBar) {
            return false;
        }

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            //TODO ??
/*            case R.id.background_menu :
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
                return true;
*/
            case R.id.share_menu:

                share();
                return true;

            case R.id.open_menu:

                startActivity(new Intent(getApplicationContext(), GalleryListActivity.class));
//                startActivity(new Intent(getApplicationContext(), SdListActivity.class));


        }
        return true;

    }


    private void share() {
        if (!mJam.isSetup()){
            toast(getString(R.string.nothing_to_share));
        } else {

            if (mJam.getSourceInfo().id > -1) {
                shareId(mJam.getSourceInfo().id);
            }
            else {

                showDialog(DIALOG_SAVE);

            }
        }
    }

    public void clearButton(){
        String gi = mJam.getGrooveInfo();
        if (gi.length() > 0)
            lastClear = gi;

        mJam.niceStop();
        mJam = new MonadJam(this);
//                mJam.changeNextInstrument(instrument);

        mpad.resetClear();

        /*if (drumMachine) {
            Intent i = new Intent();
            i.setAction("com.androidinstrument.drum.STOPPLAYBACK");
            sendBroadcast(i);
            drumMachineStarted = false;
        }*/


        mpad.setJam(mJam);

    }
    public void longClearButton(){
        mJam.fadeOut();
        mpad.fadeOut();
        mJam = new MonadJam(this);
        mpad.setJam(mJam);
    }





    private void updatePreference(String key, String value){
        if (key.equals("autoremove") &&
                findViewById(R.id.autoremove_value) != null){
            String caption;
            if (value.equalsIgnoreCase("OFF"))
                caption = "Off";
            else
                caption = "On";

            ((TextView)findViewById(R.id.autoremove_value)).
                    setText(caption);
        }

        if (key.equals("quantizer") &&
                findViewById(R.id.scale_value) != null){
            String[] scales = getResources().getStringArray(R.array.quantizer_values);
            for (int is = 0; is < scales.length; is++){
                if (value.equals(scales[is])){
                    ((TextView)findViewById(R.id.scale_value)).
                            setText(getResources().getStringArray(R.array.quantizer_entries)[is]);
                    break;
                }
            }
        }

        if (key.equals("octaves") &&
                findViewById(R.id.octaves_value) != null) {
            ((TextView)findViewById(R.id.octaves_value)).
                    setText(value);
        }

        if (key.equals("base") &&
                findViewById(R.id.bottom_note_value) != null) {
            String[] keys = getResources().getStringArray(R.array.base);
            for (int is = 0; is < keys.length; is++){
                if (value.equals(keys[is])){
                    ((TextView)findViewById(R.id.bottom_note_value)).
                            setText(getResources().getStringArray(R.array.base_captions)[is]);
                    break;
                }
            }
        }

    }

    public void refreshPreference(Preference preference, Object o) {

        mJam.refreshPreference(preference, o);
        mpad.setPreferences();
        mpad.invalidate();

        updatePreference(preference.getKey(), o.toString());
    }

    public void refreshPreference(Preference preference) {

        final SharedPreferences synthPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        mJam.refreshPreference(preference, synthPrefs);
        mpad.setPreferences();
        mpad.invalidate();

        if (preference.getKey().equals("drummachine_mode")) {
            drumMachine = synthPrefs.getBoolean("drummachine_mode", true);

            int visible = drumMachine ? View.VISIBLE : View.GONE;
            findViewById(R.id.clearAll).setVisibility(visible);
            findViewById(R.id.drummachine).setVisibility(visible);
        }

    }

    void refreshPreferences() {
        final SharedPreferences synthPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        boolean oldDrumMachine = drumMachine;

        drumMachine = synthPrefs.getBoolean("drummachine_mode", true);

        if (oldDrumMachine != drumMachine) {
            int visible = drumMachine ? View.VISIBLE : View.GONE;
            findViewById(R.id.clearAll).setVisibility(visible);
            findViewById(R.id.drummachine).setVisibility(visible);
        }

    }

    private void shrinkMpad() {
        //int height = mpad.getHeight();
        ViewGroup.LayoutParams params = mpad.getLayoutParams();
        params.height = mpad.getHeight() - 58;
        mpad.setLayoutParams(params);
    }

    private void sendClearAllBroadCast() {

        Intent statusIntent = new Intent("com.androidinstrument.drum.STOPPLAYBACK");
        sendBroadcast(statusIntent);


    }
}
