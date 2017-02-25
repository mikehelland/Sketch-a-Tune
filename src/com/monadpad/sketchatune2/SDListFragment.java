package com.monadpad.sketchatune2;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class SDListFragment extends ListFragment implements SdListManager.IHasAdapter {

    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedSate){
        View ret = inflater.inflate(R.layout.sd_list, container);
        refresh();
        return ret;
    }

    public void refresh(){
        new SdListManager(getActivity(), this, SdListManager.getPath11(), false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
    }

    public void onListItemClick(ListView l, View v, int position, long id){
        ((MainActivity)getActivity()).loadGroove("good;" + ((MiniMonadView)v.findViewById(R.id.mini_view)).getInfo());
    }
}
