package de.inovex.aosptraining.automotive;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends ComponentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        TextView textViewModel = findViewById(R.id.textViewModel);
        TextView textViewGear = findViewById(R.id.textViewGear);

        MainViewModel viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        viewModel.getVehicleModel().observe(this, model ->
                textViewModel.setText(getString(R.string.model_text, model)));

        viewModel.getCurrentGear().observe(this, gear ->
                textViewGear.setText(getString(R.string.gear_text, gear)));
    }
}
