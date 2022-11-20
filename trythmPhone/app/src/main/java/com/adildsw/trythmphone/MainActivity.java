package com.adildsw.trythmphone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.adildsw.trythmphone.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageOptions;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.testBtn.setOnClickListener(v -> {
            Log.e("TAG", "onCreate: " );
            Toast.makeText(this, "Testing", Toast.LENGTH_SHORT).show();
        });

        Wearable.getMessageClient(this).sendMessage("phone", "test", new byte[0]).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.e("TAG", "Message Sent" );
            }
        });
    }
}