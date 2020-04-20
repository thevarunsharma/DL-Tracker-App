package com.example.dltracker.ui.models;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dltracker.R;

import java.util.ArrayList;

import static java.lang.Math.min;

public class EpochsListAdapter extends ArrayAdapter<EpochObject> {

    EpochActivity context;

    public EpochsListAdapter(@NonNull Context context, ArrayList<EpochObject> items) {
        super(context, R.layout.epochs_list_item, items);
        this.context = (EpochActivity) context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater myInflator = LayoutInflater.from(getContext());
        View customView = myInflator.inflate(R.layout.epochs_list_item, parent, false);
        EpochObject epochItem = getItem(position);

        TextView epochNum = (TextView) customView.findViewById(R.id.epochNum);
        TextView epochInfo = (TextView) customView.findViewById(R.id.epochInfo);
        TextView percProgress = (TextView) customView.findViewById(R.id.percProgress);
        ImageView epochStatus = (ImageView) customView.findViewById(R.id.epochStatus);
        ProgressBar epochProgress = (ProgressBar) customView.findViewById(R.id.epochProgress);

        percProgress.setText(String.format("%d/%d", epochItem.progress, context.steps));
        epochNum.setText(String.format("epoch : %d", epochItem.epoch_num));
        epochInfo.setText(epochItem.getInfo());

        if (epochItem.done) {
            epochStatus.setBackgroundResource(R.drawable.ic_done);
        } else {
            epochStatus.setBackgroundResource(R.drawable.ic_running);
        }
        epochProgress.setMax(context.steps);
        epochProgress.setProgress(epochItem.progress);

        return customView;
    }
}
