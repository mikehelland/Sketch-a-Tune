package com.monadpad.sketchatune2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * User: m
 * Date: 1/13/14
 * Time: 1:21 AM
 */
public class GetOMGDrumsActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);


        setContentView(R.layout.getomgdrums);

        findViewById(R.id.googleplay_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                Intent stopDrums = new Intent("com.androidinstrument.drum.STOPPLAYBACK");
                sendBroadcast(stopDrums);


                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setData(Uri.parse("market://details?id=com.monadpad.omgdrums"));
                    startActivity(intent);
                }
                catch (Exception e) {
                    Intent browser = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.monadpad.omgdrums"));
                    startActivity(browser);
                }

            }
        });

        findViewById(R.id.amazon_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent stopDrums = new Intent("com.androidinstrument.drum.STOPPLAYBACK");
                sendBroadcast(stopDrums);


                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setData(Uri.parse("amzn://apps/android?p=com.monadpad.omgdrums"));
                    startActivity(intent);

                }
                catch (Exception e) {
                    Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.amazon.com/gp/product/B00HTC596W"));
                    startActivity(browser);
                }

            }
        });

        String installer = getPackageManager().getInstallerPackageName(getPackageName());

        if ("com.amazon.venezia".equals(installer)) {
            findViewById(R.id.googleplay_button).setVisibility(View.GONE);
        }
        else if ("com.android.vending".equals(installer)) {
            findViewById(R.id.amazon_button).setVisibility(View.GONE);
        }


    }

    @Override
    public void onPause() {
        super.onPause();

        if (!isFinishing())
            finish();
    }

}
