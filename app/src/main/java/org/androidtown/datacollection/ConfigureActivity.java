package org.androidtown.datacollection;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ConfigureActivity extends AppCompatActivity {
    Button button;
    EditText id1, id2, id3, u, vx, vy;
    boolean resetFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure);

        Intent intent = getIntent();
        processIntent(intent);

        button = findViewById(R.id.button);
        id1 = findViewById(R.id.id1);
        id2 = findViewById(R.id.id2);
        id3 = findViewById(R.id.id3);
        u = findViewById(R.id.u);
        vx = findViewById(R.id.vx);
        vy = findViewById(R.id.vy);
    }

    public void onClick (View view) {
        short _id1, _id2, _id3;
        float _u, _vx, _vy;
        Intent intent = new Intent(getApplicationContext(), CollectionActivity.class);
        try {
            _id1 = (short) Integer.parseInt(id1.getText().toString().trim());
            _id2 = (short) Integer.parseInt(id2.getText().toString().trim());
            _id3 = (short) Integer.parseInt(id3.getText().toString().trim());
            _u = Float.parseFloat(u.getText().toString().trim());
            _vx = Float.parseFloat(vx.getText().toString().trim());
            _vy = Float.parseFloat(vy.getText().toString().trim());
            intent.putExtra("reset", resetFlag);
            intent.putExtra("id1", _id1);
            intent.putExtra("id2", _id2);
            intent.putExtra("id3", _id3);
            intent.putExtra("u", _u);
            intent.putExtra("vx", _vx);
            intent.putExtra("vy", _vy);
            startActivity(intent);
        } catch (NumberFormatException e) {
            Toast.makeText(getApplicationContext(),
                    "Fill in the blanks properly.", Toast.LENGTH_SHORT).show();
        }
    }

    private void processIntent (Intent intent) {
        resetFlag = intent.getExtras().getBoolean("reset");
    }
}
