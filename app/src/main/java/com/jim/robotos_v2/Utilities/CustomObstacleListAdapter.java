package com.jim.robotos_v2.Utilities;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jim.robotos_v2.R;

/**
 * Created by Jim on 16/3/2016.
 */
public class CustomObstacleListAdapter extends ArrayAdapter<String> {


    private final Activity context;
    private final String[] itemName;
    private final Integer[] imageId;

    public CustomObstacleListAdapter(Activity context, String[] itemName, Integer[] imageId) {
        super(context, R.layout.row, itemName);

        this.context = context;
        this.itemName = itemName;
        this.imageId = imageId;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            convertView = inflater.inflate(R.layout.mytextview, parent, false);
            holder = new ViewHolder();
            holder.tvTitle = (TextView) convertView.findViewById(R.id.title);
            holder.ivIcon = (ImageView) convertView.findViewById(R.id.icon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.ivIcon.setBackgroundColor(imageId[position]);
        holder.tvTitle.setText(itemName[position]);
        return convertView;

    }

    static class ViewHolder {
        private TextView tvTitle;
        private ImageView ivIcon;
    }
}
