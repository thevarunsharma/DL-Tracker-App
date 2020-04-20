package com.example.dltracker.ui.models;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.example.dltracker.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;

public class TrainingsActivity extends AppCompatActivity {

    String modelKey, modelName;
    ListView trainingsListView;
    TrainingListAdapter trainingListAdapter;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    CollectionReference trainsRef;
    String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainings);

        SharedPreferences sharedPrefs = getSharedPreferences("userinfo", Context.MODE_PRIVATE);
        username = sharedPrefs.getString("username", "");

        ActionBar actionBar = getSupportActionBar();

        // get data from Apples activity
        Bundle modelData = getIntent().getExtras();
        if (modelData == null)
            return;

        modelKey = modelData.getString("modelKey");
        modelName = modelData.getString("modelName");

        actionBar.setTitle(modelName + " Trainings");
        actionBar.setDisplayHomeAsUpEnabled(true);

        trainingsListView = (ListView) findViewById(R.id.trainingsListView);
        trainingListAdapter = new TrainingListAdapter(this, new ArrayList<TrainingItem>(), modelKey);
        trainingsListView.setAdapter(trainingListAdapter);
        trainingsListView.setEmptyView(findViewById(R.id.emptyTrainings));

        trainsRef = db.collection("users/"+username+"/models/"+modelKey+"/trainings");

        trainsRef.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                trainingListAdapter.clear();
                for (QueryDocumentSnapshot documentSnapshot: snapshots){
                    HashMap<String, Object> hm = (HashMap<String, Object>) documentSnapshot.getData();
                    String Id = documentSnapshot.getId();
                    boolean done = (boolean) hm.remove("done");
                    int steps = Integer.parseInt((String) hm.remove("steps"));
                    int status = done?0:1;
                    if (hm.containsKey("error")) {
                        status = -1;
                        hm.remove("error");
                    }
                    trainingListAdapter.add(new TrainingItem(Id, hm, status, steps));
                }
                trainingListAdapter.notifyDataSetChanged();
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

    public void deleteTraining(String trainingId, int status) {
        if (status == 1){
            Toast.makeText(this, "Training in progress. Hence, can't be deleted",
                    Toast.LENGTH_LONG).show();
            return;
        }
        trainsRef.document(trainingId).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(TrainingsActivity.this,"Training deleted",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
