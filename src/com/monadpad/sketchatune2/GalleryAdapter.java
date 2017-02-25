package com.monadpad.sketchatune2;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RatingBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class GalleryAdapter extends SimpleCursorAdapter {
    private final Context context;
    private final Cursor mCursor;

    public GalleryAdapter(Context context, int layout, Cursor c, String[] from, int[] to){
        super(context, layout, c, from, to);
        this.context = context;
        this.mCursor = c;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent){
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
            rowView.setTag(holder);

       }
        else {
            rowView = convertView;
            holder = (ViewHolder)convertView.getTag();
        }

        holder.mini.position = position;
        mCursor.moveToPosition(position);
        String name = mCursor.getString(mCursor.getColumnIndex("name"));
        String artist = mCursor.getString(mCursor.getColumnIndex("artist"));
        holder.name.setText(name);
        holder.artist.setText(artist);
        int ratingI = mCursor.getColumnIndex("ratingAverage");
        if (ratingI > -1)
            holder.rating.setRating(mCursor.getFloat(ratingI));
        final String json = mCursor.getString(mCursor.getColumnIndex("json"));

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
        MiniMonadView mini;
        RatingBar rating;
    }

}

