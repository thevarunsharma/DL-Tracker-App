package com.example.dltracker.ui.models;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dltracker.R;

import java.util.ArrayList;

public class TrainingListAdapter extends ArrayAdapter<TrainingItem> {

    private ArrayList<TrainingItem> items;
    private String modelKey;
    private TrainingsActivity activity;

    public TrainingListAdapter(@NonNull Context context, ArrayList<TrainingItem> items, String modelKey) {
        super(context, R.layout.trainings_list_item, items);
        this.activity = (TrainingsActivity) context;
        this.items = items;
        this.modelKey = modelKey;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        LayoutInflater myInflator = LayoutInflater.from(getContext());
        View customView = myInflator.inflate(R.layout.trainings_list_item, parent, false);
        final TrainingItem training = getItem(position);

        final TextView trainingId = (TextView) customView.findViewById(R.id.trainingId);
        TextView trainingInfo = (TextView) customView.findViewById(R.id.trainingInfo);
        ImageView trainStatus = (ImageView) customView.findViewById(R.id.trainStatus);
        Button viewEpoch = (Button) customView.findViewById(R.id.viewEpochButton);
        ImageButton deleteButton = (ImageButton) customView.findViewById(R.id.deleteTrainButton);

        trainingId.setText(training.trainingId);
        trainingInfo.setText(training.asString());

        switch (training.status) {
            case 0:
                trainStatus.setBackgroundResource(R.drawable.ic_done);
                break;
            case 1:
                trainStatus.setBackgroundResource(R.drawable.ic_running);
                break;
            default:
                trainStatus.setBackgroundResource(R.drawable.ic_error);
        }

        deleteButton.setOnClickListener(new ImageButton.OnClickListener(){

            @Override
            public void onClick(View v) {
                AlertDialog.Builder deleteAlert = new AlertDialog.Builder(getContext());
                deleteAlert.setMessage("Are you sure you want to delete this training?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (training.status == 1){
                                    Toast.makeText(getContext(),
                                            "Can't delete training in Progress!",
                                            Toast.LENGTH_SHORT).show();
                                    dialog.cancel();
                                    return;
                                }

                                items.remove(position);
                                TrainingListAdapter.this.notifyDataSetChanged();

                                activity.deleteTraining(training.trainingId, training.status);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //  Action for 'NO' Button
                                dialog.cancel();
                            }
                        });
                //Creating dialog box
                AlertDialog alert = deleteAlert.create();
                //Setting the title manually
                alert.setTitle("Confirm Delete");
                alert.show();
            }
        });

        viewEpoch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), EpochActivity.class);
                intent.putExtra("trainingId", training.trainingId);
                intent.putExtra("modelKey", modelKey);
                intent.putExtra("username", activity.username);
                intent.putExtra("status", training.status);
                intent.putExtra("steps", training.steps);
                getContext().startActivity(intent);
            }
        });

        return customView;
    }
}
