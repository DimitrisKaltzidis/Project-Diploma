package com.jim.robotos_v2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.jim.robotos_v2.Utilities.CustomListAdapter;

public class MainMenu extends AppCompatActivity {

    ListView list;
    String[] itemname = {
            "Field Navigation",
            "Road Navigation",
            "Color Detection",
            "Face Recognition",
            "Obstacle Avoidance",
            "Manual Override",
            "Voice Override",
            "Scenario Mode",
            "Settings"
    };

    Integer[] imgcolorid = {
            R.drawable.path,
            R.drawable.road,
            R.drawable.color,
            R.drawable.facial,
            R.drawable.obstacle,
            R.drawable.controler,
            R.drawable.micro,
            R.drawable.serial,
            R.drawable.settings,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        // getSupportActionBar().hide();

        final CustomListAdapter adapter = new CustomListAdapter(this, itemname, imgcolorid);
        list = (ListView) findViewById(R.id.listView);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent openIntent = null;
                switch (position) {
                    case 0:
                        openIntent = new Intent(MainMenu.this, FieldNavigation.class);
                        break;
                    case 1:
                        openIntent = new Intent(MainMenu.this, RoadNavigation.class);
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    case 4:
                        break;
                    case 5:
                        openIntent = new Intent(MainMenu.this, ManualOverride.class);
                        break;
                    case 6:
                        break;
                    case 7:
                        break;
                    case 8:
                        break;
                    default:
                        break;
                }
                startActivity(openIntent);


            }
        });

    }
}
