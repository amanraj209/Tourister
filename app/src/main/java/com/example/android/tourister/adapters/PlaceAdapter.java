package com.example.android.tourister.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.android.tourister.models.PlaceItem;
import com.example.android.tourister.R;

import java.util.List;

public class PlaceAdapter extends ArrayAdapter<PlaceItem> {

    int resource;

    public PlaceAdapter(Context context, int resource, List<PlaceItem> items) {
        super(context, resource, items);
        this.resource=resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LinearLayout placeItemView;
        PlaceItem placeItem = getItem(position);
        if (convertView == null) {
            placeItemView = new LinearLayout(getContext());
            String inflater = Context.LAYOUT_INFLATER_SERVICE;
            LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(inflater);
            layoutInflater.inflate(resource, placeItemView, true);
        }
        else {
            placeItemView = (LinearLayout) convertView;
        }

        TextView titleView = (TextView) placeItemView.findViewById(R.id.titleView);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        titleView.setText(placeItem.getTitle());

        TextView valueView = (TextView) placeItemView.findViewById(R.id.valueView);
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        valueView.setText(placeItem.getValue());
        return placeItemView;
    }
}
