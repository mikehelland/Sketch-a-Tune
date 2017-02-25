package com.monadpad.sketchatune2.record;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.monadpad.sketchatune2.R;

import java.io.FileNotFoundException;

//import com.monadpad.fingergroove.fingergrooves.record.R;

public class Main extends Activity {

	boolean firstTime = true;
    private PcmWriter pcmW ;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.main_record);

        //pcmW = new PcmWriter(getApplicationContext(), "temp.pcm");

        ((MonadaphoneView) findViewById(R.id.mpad)).setActivity(this);
        //((MonadaphoneView) findViewById(R.id.mpad)).setPcmWriter(pcmW);


		findViewById(R.id.clearButton).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ((MonadaphoneView) findViewById(R.id.mpad)).resetClear();
            }
        });
		
		Spinner spinner = (Spinner) findViewById(R.id.instrumentSpinner);
	    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
	            this, R.array.instruments, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    spinner.setOnItemSelectedListener(new OnItemSelectedListener(){
	    
	    	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
	    		((MonadaphoneView) findViewById(R.id.mpad)).instrumentSettings(pos);
	    	}

	        public void onNothingSelected(AdapterView<?> parent) {
	       }
	    });
	    
	}



    @Override
	public void onResume(){
		super.onResume();

		if (firstTime){
			firstTime = false;
		}
		else {
			((MonadaphoneView) findViewById(R.id.mpad)).reset();
		}

    }

    @Override
	public void onPause(){
		super.onPause();
		((MonadaphoneView) findViewById(R.id.mpad)).hardReset();
        if (!isFinishing()){
            finish();
        }
    }

    private String galleryData = "";
    public void setGalleryData(String data){
        galleryData = data;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
      // Save UI state changes to the savedInstanceState.
      // This bundle will be passed to onCreate if the process is
      // killed and restarted.
      savedInstanceState.putString("LoadFromGallery", galleryData);
      super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      String myString = savedInstanceState.getString("LoadFromGallery");
        if (myString.length() > 0){
            final String fdata = myString;
            final MonadaphoneView mpv =  (MonadaphoneView)findViewById(R.id.mpad);
            final ViewTreeObserver obs = mpv.getViewTreeObserver();

            obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

                public boolean onPreDraw () {

                    mpv.setupFromGalleryPreDraw(fdata, this);

                    return true;
               }
            });



        }
        galleryData = "";
    }



} 
