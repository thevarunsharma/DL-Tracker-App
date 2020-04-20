package com.example.dltracker.ui.models;

import androidx.lifecycle.ViewModelProviders;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.dltracker.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ModelsFragment extends Fragment {

    private String username;
    private ModelsViewModel mViewModel;
    ModelListAdapter modelListAdaptor;
    ListView modelsListView;
    SearchView searchModel;
    ProgressBar modelsLoad;
    FloatingActionButton addButton;
    private Filter filter;
    ArrayList<ModelListItem> modelsItems;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    CollectionReference modelsRef;
    static String[] colors = { "#00AA00", "#CC0000", "#FFBB00", "#009999", "#5555FF" , "#555555"};

    public static ModelsFragment newInstance() {
        return new ModelsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_models, container, false);

        SharedPreferences sharedPrefs = getActivity().getSharedPreferences("userinfo", Context.MODE_PRIVATE);
        username = sharedPrefs.getString("username", "");

        modelsItems = new ArrayList<ModelListItem>();

        modelsLoad = (ProgressBar) view.findViewById(R.id.modelsLoad);

        modelListAdaptor = new ModelListAdapter(getContext(), this, modelsItems);
        modelsListView = (ListView) view.findViewById(R.id.modelsListView);
        modelsListView.setEmptyView(view.findViewById(R.id.emptyModels));
        // set adaptor
        modelsListView.setAdapter(modelListAdaptor);
        filter = modelListAdaptor.getFilter();

        addButton = (FloatingActionButton) view.findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ModelsFragment.this.addModel();
            }
        });


        searchModel = (SearchView) view.findViewById(R.id.searchModel);
        searchModel.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter.filter(newText);
                return false;
            }
        });

        modelsRef = db.collection("users/" + username + "/models");

        modelsRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for(QueryDocumentSnapshot documentSnapshot: queryDocumentSnapshots){
                    String modelKey = documentSnapshot.getId();
                    String modelName = documentSnapshot.getString("modelName");
                    String color = documentSnapshot.getString("color");
                    ModelListItem obj = new ModelListItem(modelName, modelKey, color);
                    modelsItems.add(obj);
                }
                modelListAdaptor = new ModelListAdapter(getContext(), ModelsFragment.this, modelsItems);
                modelsListView.setAdapter(modelListAdaptor);
                filter = modelListAdaptor.getFilter();
                modelsLoad.setVisibility(View.INVISIBLE);
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ModelsViewModel.class);
        // TODO: Use the ViewModel
    }

    public void addModel() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext());
        // Set up the input
        final EditText modelNameInput = new EditText(getContext());
        // Specify the type of input expected
        modelNameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        alertBuilder.setView(modelNameInput);

        // Set up the buttons
        alertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String m_Text = modelNameInput.getText().toString();
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("modelName", m_Text);
                data.put("train_count", 0);
                String color = colors[(new Random()).nextInt(6)];
                data.put("color", color);

                modelsRef.add(data).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(final DocumentReference documentReference) {
                        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                String modelKey = documentSnapshot.getId();
                                String modelName = documentSnapshot.getString("modelName");
                                String color = documentSnapshot.getString("color");
                                ModelListItem obj = new ModelListItem(modelName, modelKey, color);
                                Map<String, String> hm = new HashMap<String, String>();
                                hm.put("username", username);
                                db.document("model_keys/"+modelKey).set(hm);
                                modelsItems.add(obj);
                                modelListAdaptor = new ModelListAdapter(getContext(), ModelsFragment.this, modelsItems);
                                modelsListView.setAdapter(modelListAdaptor);
                                filter = modelListAdaptor.getFilter();
                            }
                        });

                    }
                });
                dialog.dismiss();
            }
        });

        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog alert = alertBuilder.create();
        //Setting the title manually
        alert.setTitle("Enter New Model Name");
        alert.show();
    }

    public void deleteModel(String modelKey) {
        modelsRef.document(modelKey).delete();
        db.document("model_keys/"+modelKey).delete();
        Toast.makeText(getContext(),"Model deleted",
                Toast.LENGTH_SHORT).show();
    }

}
