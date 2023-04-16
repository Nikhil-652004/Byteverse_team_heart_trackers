package com.example.heartwatch;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText phoneNumberEditText;
    private EditText emailEditText;

    public static final String KEY_EMERGENCY_CONTACT_NAME = "emergency_contact_name";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        nameEditText = findViewById(R.id.name_edit_text);
        phoneNumberEditText = findViewById(R.id.phone_number_edit_text);
        emailEditText = findViewById(R.id.email_edit_text);

        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveContactInformation();
            }
        });
    }

    private void saveContactInformation() {
        String name = nameEditText.getText().toString();
        String phoneNumber = phoneNumberEditText.getText().toString();
        String email = emailEditText.getText().toString();

        // Save the contact information to shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("emergency_contact_name", name);
        editor.putString("emergency_contact_phone_number", phoneNumber);
        editor.putString("emergency_contact_email", email);
        editor.apply();

        // Display a toast message to confirm that the information was saved
        Toast.makeText(this, "Emergency contact information saved", Toast.LENGTH_SHORT).show();
    }
}
