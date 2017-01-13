package com.mcc.ocr;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by jacopobufalino on 01/12/16.
 */

public class CustomListAdapter extends ArrayAdapter<String> {

    public List<OCRecord> records;

    public CustomListAdapter(Activity context, int resource, List<OCRecord> records) {
        super(context, resource);

        this.records = new LinkedList<>(records);
    }

    public CustomListAdapter(Activity context, int resource) {

        super(context, resource);

    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.activity_image_list, null);
        }
        TextView tv = (TextView) v.findViewById(R.id.decodedText);
        tv.setText(records.get(position).getText().replace("\n", ""));
        ImageView iv = (ImageView) v.findViewById(R.id.thumbnail);
        iv.setImageBitmap(records.get(position).getThumbnail());
        return v;
    }

    @Override
    public int getCount() {
        return records.size();
    }
}
