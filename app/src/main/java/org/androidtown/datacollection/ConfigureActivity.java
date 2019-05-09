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
    EditText id1, id2, id3, id4, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4;
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
        id4 = findViewById(R.id.id4);
        x1 = findViewById(R.id.x1);
        y1 = findViewById(R.id.y1);
        z1 = findViewById(R.id.z1);
        x2 = findViewById(R.id.x2);
        y2 = findViewById(R.id.y2);
        z2 = findViewById(R.id.z2);
        x3 = findViewById(R.id.x3);
        y3 = findViewById(R.id.y3);
        z3 = findViewById(R.id.z3);
        x4 = findViewById(R.id.x4);
        y4 = findViewById(R.id.y4);
        z4 = findViewById(R.id.z4);
    }

    public void onClick (View view) {
        short _id1, _id2, _id3, _id4;
        float _x1, _y1, _z1, _x2, _y2, _z2, _x3, _y3, _z3, _x4, _y4, _z4;
        Intent intent = new Intent(getApplicationContext(), CollectionActivity.class);
        try {
            _id1 = (short) Integer.parseInt(id1.getText().toString().trim());
            _id2 = (short) Integer.parseInt(id2.getText().toString().trim());
            _id3 = (short) Integer.parseInt(id3.getText().toString().trim());
            _id4 = (short) Integer.parseInt(id4.getText().toString().trim());
            _x1 = Float.parseFloat(x1.getText().toString().trim());
            _y1 = Float.parseFloat(y1.getText().toString().trim());
            _z1 = Float.parseFloat(z1.getText().toString().trim());
            _x2 = Float.parseFloat(x2.getText().toString().trim());
            _y2 = Float.parseFloat(y2.getText().toString().trim());
            _z2 = Float.parseFloat(z2.getText().toString().trim());
            _x3 = Float.parseFloat(x3.getText().toString().trim());
            _y3 = Float.parseFloat(y3.getText().toString().trim());
            _z3 = Float.parseFloat(z3.getText().toString().trim());
            _x4 = Float.parseFloat(x4.getText().toString().trim());
            _y4 = Float.parseFloat(y4.getText().toString().trim());
            _z4 = Float.parseFloat(z4.getText().toString().trim());
            intent.putExtra("reset", resetFlag);
            intent.putExtra("id1", _id1);
            intent.putExtra("id2", _id2);
            intent.putExtra("id3", _id3);
            intent.putExtra("id4", _id4);
            intent.putExtra("x1", _x1);
            intent.putExtra("y1", _y1);
            intent.putExtra("z1", _z1);
            intent.putExtra("x2", _x2);
            intent.putExtra("y2", _y2);
            intent.putExtra("z2", _z2);
            intent.putExtra("x3", _x3);
            intent.putExtra("y3", _y3);
            intent.putExtra("z3", _z3);
            intent.putExtra("x4", _x4);
            intent.putExtra("y4", _y4);
            intent.putExtra("z4", _z4);
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
