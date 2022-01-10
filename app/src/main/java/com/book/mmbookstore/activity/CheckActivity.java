package com.book.mmbookstore.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.book.mmbookstore.database.pref.SharedPref;
import com.book.mmbookstore.databinding.ActivityCheckBinding;


public class CheckActivity extends AppCompatActivity {

    private ActivityCheckBinding binding;
    SharedPref sharedPref;

    private Button btnCheck;
    private EditText inputNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        sharedPref = new SharedPref(this);
        initUi();
    }

    private void initUi() {
        btnCheck = binding.btnCheck;
        inputNumber = binding.inputNumber;

        btnCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleCheck();
            }
        });
    }

    private void handleCheck() {
        if (inputNumber.getText().toString().equals("၁၂၃၄၅")) {
            sharedPref.setFirstTimeLogin(false);
            launchMainScreen();
        } else {
            inputNumber.setError("Please try again.");
        }
    }

    private void launchMainScreen() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
    }
}