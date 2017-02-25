package com.monadpad.sketchatune2;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.RatingBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.List;

public class GalleryArrayAdapter extends ArrayAdapter<GalleryRow> {
    private final Context context;

    public GalleryArrayAdapter(Context context, int layout, int view, List<GalleryRow> list){
        super(context, layout, view, list);
        this.context = context;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent){

        final GalleryRow row = getItem(position);

        boolean loaded = false;

        final View rowView;
        final ViewHolder holder;
        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.gallery_row, parent, false);
            holder = new ViewHolder();

            holder.name= (TextView)rowView.findViewById(R.id.gallery_name);
            holder.artist = (TextView)rowView.findViewById(R.id.gallery_artist);
            holder.mini = (MiniMonadView)rowView.findViewById(R.id.mini_view);
            holder.rating = (RatingBar)rowView.findViewById(R.id.gallery_ratingbar);
            holder.id = (TextView)rowView.findViewById(R.id.gallery_id);
            rowView.setTag(holder);

       }
        else {
            if (((ViewHolder)convertView.getTag()).id.getText().equals(row.id)) {
                return convertView;
            }
            rowView = convertView;
            holder = (ViewHolder)convertView.getTag();
        }

        holder.mini.position = position;


        holder.id.setText(row.id);

        String name = row.name;
        String artist = row.artist;
        holder.name.setText(name);
        holder.artist.setText(artist);
        double ratingI = row.ratingAverage;
        if (ratingI >= 0)
            holder.rating.setRating((float)ratingI);

        final String json = row.json;

        final MiniMonadView mini = holder.mini;
        final ViewTreeObserver obs = mini.getViewTreeObserver();

        obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

            public boolean onPreDraw () {

                try {
                    mini.setupFromGallery(json, position);
                }
                catch (Exception e){

                }
                mini.getViewTreeObserver().removeOnPreDrawListener(this);

                return true;
            }
        });


        return rowView;
    }

    static class ViewHolder {
        TextView name;
        TextView artist;
        TextView id;
        MiniMonadView mini;
        RatingBar rating;

    }

}

