package com.example.coordinator_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Random;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.*;

/*
TODO: obsługa bluetooth
 */

public class MainActivity extends AppCompatActivity {

    ArrayList<Victim> victims = new ArrayList<>();
    CustomAdapter customAdapter;
    String[] triageSystems;
    String serviceID = "com.example.coordinator_app";
    ConnectionLifecycleCallback communicationCallbacks;
    PayloadCallback payloadReciever = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String s, Payload payload) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(payload.asBytes());
                ObjectInputStream is = new ObjectInputStream(bis);
                Victim v = (Victim) is.readObject();
                victims.add(v);
                updateVictimsData();
                customAdapter.notifyDataSetChanged();

            } catch (Exception e){
                Toast.makeText(getApplicationContext(), "Błąd odbioru danych: "+e.getMessage(), Toast.LENGTH_SHORT ).show();
            }
        }

        @Override
        public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ListView victimList = findViewById(R.id.victim_list);
        customAdapter = new CustomAdapter(getApplicationContext(), victims);
        victimList.setAdapter(customAdapter);

        //ikony klas w menu głównym
        ImageView imgV;
        imgV = findViewById(R.id.total_black).findViewById(R.id.label); imgV.setImageResource(R.color.colorTriageBlack);
        imgV = findViewById(R.id.total_red).findViewById(R.id.label); imgV.setImageResource(R.color.colorTriageRed);
        imgV = findViewById(R.id.total_yellow).findViewById(R.id.label); imgV.setImageResource(R.color.colorTriageYellow);
        imgV = findViewById(R.id.total_green).findViewById(R.id.label); imgV.setImageResource(R.color.colorTriageGreen);

        //wypełnienie menu wyboru systemu do triage
        Spinner dropdown = findViewById(R.id.classification_system_choice);
        triageSystems = new String[]{"START/JumpSTART", "TRTS", "ISS", "BTTR"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, triageSystems);
        dropdown.setAdapter(adapter); dropdown.getSelectedItem().toString();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Losowa generacja pacjenta", Toast.LENGTH_SHORT ).show();
                Random r = new Random();
                long imei = r.nextLong()%1000000000000000L;
                boolean b = r.nextBoolean();
                int rate = r.nextInt(40) + 10;
                float capRefillTime = r.nextFloat()*3f + 0.5f;
                boolean w = r.nextBoolean();
                Victim.AVPU c = Victim.AVPU.values()[r.nextInt(Victim.AVPU.values().length)];
                Victim randomVictim = new Victim(imei, b, rate, capRefillTime, w, c);
                victims.add(randomVictim);
                updateVictimsData();
                customAdapter.notifyDataSetChanged();
            }
        });

        victimList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), VictimDetailsActivity.class);
                intent.putExtra("victim", (Parcelable) victims.get(position)); //sending victim data to new activity
                startActivity(intent);
            }
        });

        Button btn = findViewById(R.id.classification_system_confirm);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAdvertising();
                /*
                Spinner spnr = findViewById(R.id.classification_system_choice);
                int id = (int)spnr.getSelectedItemId();
                switch(id){
                    case 0:
                        TextView t = findViewById(R.id.classification_system_val); t.setText("START");
                        break;
                    default:
                        Toast.makeText(getApplicationContext(), "Wybrany system nie jest aktualnie zaimplementowany", Toast.LENGTH_SHORT).show();
                }*/
            }
        });

        communicationCallbacks = new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String s, ConnectionInfo connectionInfo) {
                Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(s, payloadReciever);
            }

            @Override
            public void onConnectionResult(String s, ConnectionResolution connectionResolution) {
                switch (connectionResolution.getStatus().getStatusCode()) {
                    case ConnectionsStatusCodes.STATUS_OK:
                        // We're connected! Can now start sending and receiving data.
                        break;
                    case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                        // The connection was rejected by one or both sides.
                        break;
                    case ConnectionsStatusCodes.STATUS_ERROR:
                        // The connection broke before it was able to be accepted.
                        break;
                    default:
                        // Unknown status code
                }
            }

            @Override
            public void onDisconnected(String s) {

            }
        };

        startAdvertising();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        ViewFlipper vf = findViewById(R.id.layout_manager);
        //noinspection SimplifiableIfStatement
        switch(id){
            case R.id.action_bt:
                vf.setDisplayedChild(1);
                return true;
            case R.id.action_victims:
                vf.setDisplayedChild(2);
                return true;
            case R.id.action_rescuers:
                vf.setDisplayedChild(3);
                return true;
            default:
                vf.setDisplayedChild(0);
        }


        return super.onOptionsItemSelected(item);
    }

    private void startAdvertising() {
        Toast.makeText(getApplicationContext(), "Startujemy", Toast.LENGTH_SHORT ).show();
        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
        ConnectionsClient cc = Nearby.getConnectionsClient(getApplicationContext());
        cc.stopAdvertising();
        cc.startAdvertising(
                        "Kierujacy Akcja Medyczna", serviceID, communicationCallbacks, advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            Toast.makeText(getApplicationContext(), "Wystartowaliśmy", Toast.LENGTH_SHORT ).show();

                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // We were unable to start advertising.
                            Toast.makeText(getApplicationContext(), "Nie Wystartowaliśmy", Toast.LENGTH_SHORT ).show();
                            Log.e("Nearby Start", e.getMessage());
                        });
    }



    public void updateVictimsData(){
        int b=0, r=0, y=0, g=0;
        for(Victim v : victims){
            switch(v.getColor()){
                case BLACK: b++; break;
                case RED: r++; break;
                case YELLOW: y++; break;
                case GREEN: g++; break;
            }
        }
        TextView t = findViewById(R.id.total_victims_val); t.setText(victims.size()+"");

        t = findViewById(R.id.total_black).findViewById(R.id.val); t.setText(b+"");
        t = findViewById(R.id.total_red).findViewById(R.id.val); t.setText(r+"");
        t = findViewById(R.id.total_yellow).findViewById(R.id.val); t.setText(y+"");
        t = findViewById(R.id.total_green).findViewById(R.id.val); t.setText(g+"");

    }
}
