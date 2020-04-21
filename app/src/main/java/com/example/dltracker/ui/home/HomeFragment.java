package com.example.dltracker.ui.home;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.example.dltracker.R;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        ConstraintLayout introLayout = (ConstraintLayout) root.findViewById(R.id.intro);
        ConstraintLayout instructions = (ConstraintLayout) root.findViewById(R.id.instructions);
        introLayout.getBackground().setTint(Color.parseColor("#4186C3"));
        instructions.getBackground().setTint(Color.parseColor("#CF041C"));
        return root;
    }
}
