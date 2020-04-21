package com.example.dltracker.ui.settings;

import androidx.constraintlayout.widget.ConstraintLayout;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dltracker.MainActivity;
import com.example.dltracker.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private SettingsViewModel mViewModel;
    TextView profileName;
    ConstraintLayout editName, editPassword;
    String firstName, lastName, username;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_fragment, container, false);
        SharedPreferences sharedPrefs = getActivity().getSharedPreferences("userinfo", Context.MODE_PRIVATE);
        firstName = sharedPrefs.getString("firstName", "");
        lastName = sharedPrefs.getString("lastName", "");
        username = sharedPrefs.getString("username", "");

        profileName = (TextView) view.findViewById(R.id.profileNameShow);
        profileName.setText(firstName + " " + lastName);
        editName = (ConstraintLayout) view.findViewById(R.id.editName);
        editPassword = (ConstraintLayout) view.findViewById(R.id.editPassword);

        editName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editNameDialog();
            }
        });

        editPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editPasswordDialog();
            }
        });

        return view;
    }

    public void editNameDialog(){
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext());
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        // Set up the input
        TextView firstNameTitle = new TextView(getContext());
        firstNameTitle.setText("First Name");
        final EditText firstNameInput = new EditText(getContext());
        firstNameInput.setText(firstName);
        TextView lastNameTitle = new TextView(getContext());
        lastNameTitle.setText("Last Name");
        final EditText lastNameInput = new EditText(getContext());
        lastNameInput.setText(lastName);
        // specify type of input
        firstNameInput.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        lastNameInput.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        // add to layout
        layout.addView(firstNameTitle);
        layout.addView(firstNameInput);
        layout.addView(lastNameTitle);
        layout.addView(lastNameInput);

        alertBuilder.setView(layout);

        // Set up the buttons
        alertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                firstName = firstNameInput.getText().toString();
                lastName = lastNameInput.getText().toString();
                Map<String, Object> hm = new HashMap<String, Object>();
                hm.put("firstName", firstName);
                hm.put("lastName", lastName);
                db.document("users/"+username).update(hm).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        SharedPreferences sharedPrefs = getActivity().getSharedPreferences("userinfo",
                                Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPrefs.edit();
                        editor.putString("firstName", firstName);
                        editor.putString("lastName", lastName);
                        editor.apply();
                        profileName.setText(firstName+" "+lastName);
                        MainActivity context = (MainActivity) getContext();
                        context.resetNavBar(firstName+" "+lastName);
                        Toast.makeText(getContext(), "Profile Name Updated", Toast.LENGTH_SHORT).show();
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
        alert.setTitle("Edit Profile Name");
        alert.show();
    }

    public void editPasswordDialog() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext());
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        // Set up the input
        TextView currPasswordTitle = new TextView(getContext());
        currPasswordTitle.setText("Current Password");
        final EditText currPasswordInput = new EditText(getContext());
        TextView newPasswordTitle = new TextView(getContext());
        newPasswordTitle.setText("New Password");
        final EditText newPasswordInput = new EditText(getContext());
        TextView confirmPasswordTitle = new TextView(getContext());
        confirmPasswordTitle.setText("Confirm New Password");
        final EditText confirmPasswordInput = new EditText(getContext());
        // specify type of input
        currPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // add to layout
        layout.addView(currPasswordTitle);
        layout.addView(currPasswordInput);
        layout.addView(newPasswordTitle);
        layout.addView(newPasswordInput);
        layout.addView(confirmPasswordTitle);
        layout.addView(confirmPasswordInput);

        alertBuilder.setView(layout);

        // Set up the buttons
        alertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String currPassword = currPasswordInput.getText().toString();
                String newPassword = newPasswordInput.getText().toString();
                String confirmPassword = confirmPasswordInput.getText().toString();

                // first check current password
                db.document("users/"+username).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        String password = documentSnapshot.getString("password");
                        if (!currPassword.equals(password)){
                            Toast.makeText(getContext(), "Incorrect Current Password", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (!newPassword.equals(confirmPassword)){
                            Toast.makeText(getContext(), "New Password fields didn't match", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (newPassword.length() < 6){
                            Toast.makeText(getContext(), "Weak New Password", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Map<String, Object> hm = new HashMap<String, Object>();
                        hm.put("password", newPassword);
                        db.document("users/"+username).update(hm).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(getContext(), "Password Changed", Toast.LENGTH_SHORT).show();
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
        alert.setTitle("Change Password");
        alert.show();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(SettingsViewModel.class);
        // TODO: Use the ViewModel
    }

}
