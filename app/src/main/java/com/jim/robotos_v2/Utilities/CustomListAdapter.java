package com.jim.robotos_v2.Utilities;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jim.robotos_v2.R;

public class CustomListAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final String[] itemName;
    private final Integer[] imageId;

    public CustomListAdapter(Activity context, String[] itemName, Integer[] imageId) {
        super(context, R.layout.row, itemName);

        this.context = context;
        this.itemName = itemName;
        this.imageId = imageId;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            convertView = inflater.inflate(R.layout.row, parent, false);
            holder = new ViewHolder();
            holder.tvTitle = (TextView) convertView.findViewById(R.id.title);
            holder.ivIcon = (ImageView) convertView.findViewById(R.id.icon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.ivIcon.setImageResource(imageId[position]);
        holder.tvTitle.setText(itemName[position]);
        return convertView;

    }

    static class ViewHolder {
        private TextView tvTitle;
        private ImageView ivIcon;
    }
}
