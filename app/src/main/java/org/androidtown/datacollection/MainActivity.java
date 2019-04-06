package org.androidtown.datacollection;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_COLLECTION = 101;
    public static final String KEY = "reset";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onButtonClicked (View v) {
        Intent intent = new Intent(getApplicationContext(), CollectionActivity.class);

        final RadioGroup radioGroup = findViewById(R.id.radioGroup);
        int id = radioGroup.getCheckedRadioButtonId();
        ResetOrKeep r = null;
        boolean checked;

        if (id == R.id.radio1){ // Reset DB
            r = new ResetOrKeep(1);
            checked = true;
        } else if (id == R.id.radio2){  // Keep the existing DB
            r = new ResetOrKeep(0);
            checked = true;
        } else {    // No radio button is checked
            checked = false;
            Toast.makeText(getApplicationContext(),
                    "Please check one of the options.", Toast.LENGTH_SHORT).show();
        }

        if (checked) {
            intent.putExtra(KEY, r);
            startActivityForResult(intent, REQUEST_CODE_COLLECTION);
        }
    }

}
