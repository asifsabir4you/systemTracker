package com.thereza.systemtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * Created by asifsabir on 12/25/17.
 */

public class PasswordChange extends AppCompatActivity {
    EditText etOld, etNew;
    Button btnChange;
    LinearLayout passCngLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_change);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Change Password");
        etOld = (EditText) findViewById(R.id.et_old_password);
        etNew = (EditText) findViewById(R.id.et_new_password);
        btnChange = (Button) findViewById(R.id.btn_pass_change);
        SharedPreferences prefs = getSharedPreferences("passPref", MODE_PRIVATE);
        final int savedPass = prefs.getInt("password", 12345);
        passCngLayout = (LinearLayout) findViewById(R.id.layout_pass_change);

        btnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Integer.parseInt(etOld.getText().toString().trim()) == savedPass) {
                    if (etNew != null && !etNew.getText().toString().trim().equals("")) {
                        int newPass = Integer.parseInt(etNew.getText().toString().trim());

                        SharedPreferences.Editor editor = getSharedPreferences(PasswordSplash.PREF, MODE_PRIVATE).edit();
                        editor.putInt("password", newPass);
                        editor.apply();
                        //successful change
                        Snackbar snackbar = Snackbar.make(passCngLayout, "Successfully Changed!", Snackbar.LENGTH_LONG)
                                .setAction("Action", null);
                        View sbView = snackbar.getView();
                        sbView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
                        snackbar.show();

                    } else {
                        //empty new password
                        Snackbar snackbar = Snackbar.make(passCngLayout, "Empty new password", Snackbar.LENGTH_LONG)
                                .setAction("Action", null);
                        View sbView = snackbar.getView();
                        sbView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRed));
                        snackbar.show();
                    }
                } else {
                    //wrong old pass word
                    Snackbar snackbar = Snackbar.make(passCngLayout, "Wrong Old Password", Snackbar.LENGTH_LONG)
                            .setAction("Action", null);
                    View sbView = snackbar.getView();
                    sbView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRed));
                    snackbar.show();
                }
            }
        });


    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                Intent intent = new Intent(this, PasswordSplash.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivityIfNeeded(intent, 0);
                finish();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getApplicationContext(), PasswordSplash.class));
        finish();
    }
}
