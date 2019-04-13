package org.androidtown.datacollection;

import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Locale;

public class FinalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final);

        /* Show the final result (fully processed data at each location) */
        final TextView contents = findViewById(R.id.contents);
        Cursor c = CollectionActivity.db.rawQuery(
                "select location, vertical, horizontal, magnitude " +
                        "from " + CollectionActivity.finalTableName, null);
        int recordCount = c.getCount();
        contents.setText("");
        for (int i = 0; i < recordCount; i++) {
            c.moveToNext();
            String _location = c.getString(0);
            double _vertical = c.getDouble(1);
            double _horizontal = c.getDouble(2);
            double _magnitude = c.getDouble(3);

            contents.append("Location ID: " + _location + "\n" +
                    "Vertical: " +
                    String.format(Locale.KOREA, "%.2f",_vertical) + "\n" +
                    "Horizontal: " +
                    String.format(Locale.KOREA, "%.2f",_horizontal) + "\n" +
                    "Magnitude: " +
                    String.format(Locale.KOREA, "%.2f",_magnitude) + "\n\n");
        }
        c.close();
    }
}
