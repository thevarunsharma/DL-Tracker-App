package com.example.dltracker.ui.models;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class EpochObject {

    public int epoch_num;
    public int progress;
    public boolean done;
    public HashMap<String, String> info;

    public EpochObject(int epoch_num, int progress, boolean done, HashMap<String, String> info) {
        this.epoch_num = epoch_num;
        this.progress = progress;
        this.done = done;
        this.info = info;
    }

    public void updateProgress(int progress) {
        this.progress = progress;
    }

    public void setDoneEpoch() {
        done = true;
    }

    public String getInfo() {
        String result = "";
        for (Map.Entry e : info.entrySet())
            result += String.format("%s : %s", e.getKey(), e.getValue()) + "\n";
        return result;
    }
}
