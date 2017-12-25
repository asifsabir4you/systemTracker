package com.thereza.systemtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * Created by asifsabir on 12/25/17.
 */

public class PasswordSplash extends AppCompatActivity {
    EditText etPass;
    Button btnEnter, btnChange;
    LinearLayout passLayout;
    public static String PREF = "passPref";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_splash);
        etPass = (EditText) findViewById(R.id.et_password);
        btnEnter = (Button) findViewById(R.id.btn_enter);
        btnChange = (Button) findViewById(R.id.btn_chnage);
        passLayout = (LinearLayout) findViewById(R.id.layout_pass);

        SharedPreferences prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        final int savedPass = prefs.getInt("password", 12345);
        if (savedPass == 12345) {
            Snackbar snackbar = Snackbar.make(passLayout, "password is:" + savedPass, Snackbar.LENGTH_LONG)
                    .setAction("Action", null);
            View sbView = snackbar.getView();
            sbView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
            snackbar.show();
        }


        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int retPass = Integer.parseInt(etPass.getText().toString().trim());

                if (retPass == savedPass) {
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                } else {
                    Snackbar snackbar = Snackbar.make(passLayout, "Password Mismatch", Snackbar.LENGTH_LONG)
                            .setAction("Action", null);
                    View sbView = snackbar.getView();
                    sbView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRed));
                    snackbar.show();
                }

            }
        });
        btnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), PasswordChange.class));
                finish();
            }
        });
    }
}
    /*
         Setting values in Preference:

// MY_PREFS_NAME - a static String variable like:
//public static final String MY_PREFS_NAME = "MyPrefsFile";
         SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
         editor.putString("name", "Elena");
         editor.putInt("idName", 12);
         editor.apply();

         Retrieve data from preference:

         SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
         String restoredText = prefs.getString("text", null);
         if (restoredText != null) {
         String name = prefs.getString("name", "No name defined");//"No name defined" is the default value.
         int idName = prefs.getInt("idName", 0); //0 is the default value.
         }


    */