package com.example.dltracker.ui.models;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.dltracker.R;

import java.util.ArrayList;

public class ModelListAdapter extends ArrayAdapter<ModelListItem> implements Filterable {

    private ArrayList<ModelListItem> items, originalItems;
    private ModelsFragment fragment;

    public ModelListAdapter(@NonNull Context context, ModelsFragment fragment, ArrayList<ModelListItem> items) {
        super(context, R.layout.model_list_item, items);
        this.fragment = fragment;
        this.items = items;
        this.originalItems = (ArrayList<ModelListItem>) items.clone();
    }

    public void setOriginalItems(ArrayList<ModelListItem> listItems) {
        originalItems = listItems;
    }

    private class AdapterFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            ArrayList<ModelListItem> filteredData = new ArrayList<ModelListItem>();
            for (int i=0; i<originalItems.size(); i++)
                if (originalItems.get(i).modelName.toLowerCase().contains(constraint))
                    filteredData.add(originalItems.get(i));

            filterResults.count = filteredData.size();
            filterResults.values = filteredData;
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            clear();
            addAll((ArrayList<ModelListItem>) results.values);
            notifyDataSetChanged();;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // make view in list
        LayoutInflater myInflator = LayoutInflater.from(getContext());
        View customView = myInflator.inflate(R.layout.model_list_item, parent, false);

        final ModelListItem modelItem = getItem(position);
        TextView model_name = (TextView) customView.findViewById(R.id.list_model_name);
        ImageButton copy_button = (ImageButton) customView.findViewById(R.id.model_id_copy_button);
        ImageButton delete_button = (ImageButton) customView.findViewById(R.id.deleteModelButton);

        model_name.setText(modelItem.modelName);
        customView.getBackground().setTint(Color.parseColor(modelItem.color));

        copy_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("model key", modelItem.modelKey);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Model Key: "+modelItem.modelKey+" copied to clipboard",
                        Toast.LENGTH_LONG).show();
            }
        });

        Button viewTrainingButton = (Button) customView.findViewById(R.id.view_training_button);
        viewTrainingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), TrainingsActivity.class);
                intent.putExtra("modelName", modelItem.modelName);
                intent.putExtra("modelKey", modelItem.modelKey);
                getContext().startActivity(intent);
            }
        });

        delete_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder deleteAlert = new AlertDialog.Builder(getContext());
                deleteAlert.setMessage("Are you sure you want to delete this Model?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                items.remove(position);
                                ModelListAdapter.this.notifyDataSetChanged();

                                fragment.deleteModel(modelItem.modelKey);
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

        return  customView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new AdapterFilter();
    }
}
