package com.example.dltracker.ui.models;

import android.content.Intent;

import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TrainingItem {
    public String trainingId;
    public HashMap<String, Object> data;
    public int status;      //0->done   1->running    -1->error
    public int steps;

    public TrainingItem(String trainingId, HashMap<String, Object> data, int status, int steps) {
        this.trainingId = trainingId;
        this.data = data;
        this.status = status;
        this.steps = steps;
    }

    public String asString(){
        String s = "";
        for (Map.Entry e: data.entrySet()){
            s += String.format("%s : %s", e.getKey(), e.getValue()) + "\n";
        }
        return s;
    }
}
