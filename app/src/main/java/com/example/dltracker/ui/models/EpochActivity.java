package com.example.dltracker.ui.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dltracker.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.min;
import static java.lang.StrictMath.max;

public class EpochActivity extends AppCompatActivity {

    private String modelKey, trainingId;
    private int epoch_num;
    ListView epochsListView;
    ArrayList<EpochObject> epochsList;
    EpochsListAdapter epochsListAdapter;
    ConstraintLayout stopDialog;
    ConnectionService connectionService;
    ProgressBar loader;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    CollectionReference epochsRef;
    DocumentReference trainRef;
    int status, steps;
    boolean isConnected = false;

    private ServiceConnection connection;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epoch);
        ActionBar actionBar = getSupportActionBar();

        // get data from Apples activity
        Bundle modelData = getIntent().getExtras();
        if (modelData == null)
            return;

        modelKey = modelData.getString("modelKey");
        trainingId = modelData.getString("trainingId");
        String username = modelData.getString("username");
        status = modelData.getInt("status");
        steps = modelData.getInt("steps");

        epochsRef = db.collection("users/"+username+
                "/models/"+modelKey+"/trainings/"+trainingId+"/epochs_list");

        trainRef = db.document("users/"+username+"/models/"+modelKey+"/trainings/"+trainingId);

        actionBar.setTitle("Training: "+trainingId);
        actionBar.setDisplayHomeAsUpEnabled(true);

        loader = (ProgressBar) findViewById(R.id.loader);
        loader.setIndeterminate(true);
        loader.setVisibility(View.INVISIBLE);

        epochsListView = (ListView) findViewById(R.id.epochsListView);
        epochsList = new ArrayList<EpochObject>();
        epochsListAdapter = new EpochsListAdapter(EpochActivity.this, epochsList);
        epochsListView.setAdapter(epochsListAdapter);

        stopDialog = (ConstraintLayout) findViewById(R.id.stopDialog);
        if (status != 1)
            stopDialog.setVisibility(View.GONE);

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ConnectionService.LocalBinder binder = (ConnectionService.LocalBinder) service;
                connectionService = binder.getService();
                connectionService.addActivity(EpochActivity.this, modelKey);
                epochsListAdapter.notifyDataSetChanged();
                if (!connectionService.isTrainingRunning(modelKey))
                    stopDialog.setVisibility(View.GONE);

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                connectionService.unbindActivity(modelKey);
            }
        };

        trainRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (!documentSnapshot.exists()){return;}
                boolean done = documentSnapshot.getBoolean("done");
                if (done && isConnected){
                    status = 0;
                    unbindService(connection);
                    isConnected = false;
                }
            }
        });


        epochsRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                EpochActivity.this.epoch_num = queryDocumentSnapshots.size()-1;
                for (QueryDocumentSnapshot documentSnapshot: queryDocumentSnapshots) {
                    int epoch_num = Integer.parseInt(documentSnapshot.getId());
                    Map<String, Object> hm = documentSnapshot.getData();
                    HashMap<String, String> info = new HashMap<String, String>();
                    for (Map.Entry e: hm.entrySet()) {
                        info.put(e.getKey().toString(), e.getValue().toString());
                    }
                    boolean done = (boolean) hm.remove("done");
                    int progress = Integer.parseInt((String) hm.remove("progress"));
                    epochsListAdapter.add(new EpochObject(epoch_num, progress, done, info));
                }
                if (status == 1){
                    EpochObject obj = epochsListAdapter.getItem(EpochActivity.this.epoch_num);
                    if (obj.done) {
                        epochsListAdapter.add(new EpochObject(EpochActivity.this.epoch_num+1, 0,
                                false, new HashMap<String, String>()));
                        EpochActivity.this.epoch_num += 1;
                    }
                    stopDialog.setVisibility(View.VISIBLE);
                    if (!isConnected){
                        Intent intent = new Intent(EpochActivity.this, ConnectionService.class);
                        bindService(intent, connection, Context.BIND_NOT_FOREGROUND);
                        isConnected = true;
                    }
                }
                epochsListAdapter.notifyDataSetChanged();
            }
        });
    }

    public void addNewEpoch(Map<String, String> hm) {
        epoch_num += 1;
        EpochObject epoch = new EpochObject(epoch_num, 0, false, (HashMap<String, String>) hm);
        epochsListAdapter.add(epoch);
        epochsListAdapter.notifyDataSetChanged();
    }

    public void stopEpoch(){
        synchronized (connection){
            connection.notify();
        }
        connectionService.stopConnection(modelKey);
        stopDialog.setVisibility(View.GONE);
        status = 0;
    }

    public void updateProgressBar(Map<String, String> info, boolean done) {
        int epoch = min(Integer.parseInt(info.remove("epoch")), epochsListAdapter.getCount()-1);
        EpochObject obj = epochsListAdapter.getItem(epoch);
        obj.progress = Integer.parseInt(info.remove("progress"));
        obj.done = done;
        obj.info = (HashMap<String, String>) info;
        epochsListAdapter.notifyDataSetChanged();
    }

    public void stopTraining(View v) {
        Log.i("ImageButton", "stop cicked");
        AlertDialog.Builder deleteAlert = new AlertDialog.Builder(this);
        deleteAlert.setMessage("Are you sure you want to stop this epoch?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        EpochActivity.this.stopEpoch();
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
        alert.setTitle("Confirm Stopping");
        alert.show();
    }

    public void trainingEnded() {
        status = 0;
        connectionService.closeConnection(modelKey);
        epochsRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                stopDialog.setVisibility(View.GONE);
                epochsListAdapter.clear();
                for (QueryDocumentSnapshot documentSnapshot: queryDocumentSnapshots) {
                    int epoch_num = Integer.parseInt(documentSnapshot.getId());
                    Map<String, Object> hm = documentSnapshot.getData();
                    HashMap<String, String> info = new HashMap<String, String>();
                    for (Map.Entry e: hm.entrySet()) {
                        info.put(e.getKey().toString(), e.getValue().toString());
                    }
                    boolean done = (boolean) hm.remove("done");
                    int progress = Integer.parseInt((String) hm.remove("progress"));
                    epochsListAdapter.add(new EpochObject(epoch_num, progress, done, info));
                }
                epochsListAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        if (status == 1)
            unbindService(connection);
        super.onDestroy();
    }
}
