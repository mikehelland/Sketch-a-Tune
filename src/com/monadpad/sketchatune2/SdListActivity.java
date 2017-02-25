package com.monadpad.sketchatune2;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class SdListActivity extends ListActivity  implements SdListManager.IHasAdapter
{

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setTitle("SD Card");
        new SdListManager(this, this, SdListManager.getPath11(), true);
    }


    public void onListItemClick(ListView l, View v, int position, long id){

        Intent intent = new Intent(this, MainActivity.class);
        String json = ((MiniMonadView)v.findViewById(R.id.mini_view)).getInfo();
        if (!json.startsWith("{")){
            json = "good;" + json;
        }
        intent.putExtra("grooveInfo", json);
        intent.putExtra("galleryTitle", ((TextView)v.findViewById(R.id.gallery_name)).getText());
        intent.putExtra("sd", true);
        startActivity(intent);

    }
}
