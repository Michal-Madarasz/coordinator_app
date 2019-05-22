package com.example.coordinator_app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.*;
import com.triage.model.Rescuer;
import com.triage.model.Victim;

import org.javatuples.Triplet;

public class MainActivity extends AppCompatActivity {

    ArrayList<Triplet<String, Rescuer, Victim>> victims = new ArrayList<>();
    CustomAdapter customAdapter;
    String[] triageSystems;
    final String SERVICE_ID = "triage.simulator-rescuer";
    String log_filename;
    ConnectionsClient connectionsClient;
    private boolean advertising = false;

    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
            };
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    ConnectionLifecycleCallback communicationCallbacks = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String s, ConnectionInfo connectionInfo) {
            connectionsClient.acceptConnection(s, payloadReceiver);
            Toast.makeText(getApplicationContext(), "Wykryto "+s, Toast.LENGTH_SHORT).show();
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
            //Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
        }
    };
    PayloadCallback payloadReceiver = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String s, Payload payload) {
            try {//próba interpretacji jako połączenie od ratownika
                ByteArrayInputStream bis = new ByteArrayInputStream(payload.asBytes());//czytanie przychodzących danych jako bajty
                ObjectInputStream is = new ObjectInputStream(bis);
                Rescuer rescuer = (Rescuer) is.readObject();//konwersja bajtów na obiekt rescuer

                StringBuilder sb = new StringBuilder();
                sb.append("Nawiązano połączenie z ratownikiem: ");
                sb.append(rescuer.toString());
                printToLogWithTimestamp(sb.toString());

                TextView t = findViewById(R.id.classification_system_val);
                String classSystem = t.getText().toString();
                Payload p = Payload.fromBytes(classSystem.getBytes());
                connectionsClient.sendPayload(s, p);//wysłanie informacji o systemie klasyfikacji
                return;
            } catch (Exception exception){ } //nastąpił błąd konwersji

            try {//próba interpretacji jako połączenie od czujnika
                ByteArrayInputStream bis = new ByteArrayInputStream(payload.asBytes());
                ObjectInputStream is = new ObjectInputStream(bis);
                Triplet<String, Rescuer, Victim> data = (Triplet<String, Rescuer, Victim>) is.readObject();
                data.getValue2().calculateColor();
                for(Triplet<String, Rescuer, Victim> row : victims){
                    if(row.getValue0().equals(data.getValue0())){
                        Triplet<String, Rescuer, Victim> newRow = row.setAt2(data.getValue2());
                        victims.remove(row);
                        victims.add(newRow);
                        updateVictimsData();
                        customAdapter.notifyDataSetChanged();

                        StringBuilder sb = new StringBuilder();
                        sb.append("Aktualizacja danych od czujnika: IMEI=");
                        sb.append(newRow.getValue0());
                        sb.append("; Zaktualizowane dane: ");
                        sb.append(newRow.getValue2().toString());
                        printToLogWithTimestamp(sb.toString());

                        return;
                    }
                }

                StringBuilder sb = new StringBuilder();
                sb.append("Nowy poszkodowany: IMEI czujnika=");
                sb.append(data.getValue0());
                sb.append("; Ratownik: ");
                sb.append(data.getValue1().toString());
                sb.append("; Dane poszkodowanego: ");
                sb.append(data.getValue2().toString());
                printToLogWithTimestamp(sb.toString());

                victims.add(data);
                updateVictimsData();
                customAdapter.notifyDataSetChanged();
            } catch (Exception e){
                Toast.makeText(getApplicationContext(), "Błąd odbioru informacji o poszkodowanym", Toast.LENGTH_SHORT ).show();
                Log.e("TAG", e.getMessage());
            }
        }

        @Override
        public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Victim v = new Victim(1L, true, 48, 1.5f, false, Victim.AVPU.VERBAL);
        //victims.add(v);
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
        triageSystems = new String[]{"START/JumpSTART", "System2", "System3", "System4"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, triageSystems);
        dropdown.setAdapter(adapter); dropdown.getSelectedItem().toString();

        Date d = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_kkmmss");
        log_filename = dateFormat.format(d)+".txt";

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Losowa generacja pacjenta", Toast.LENGTH_SHORT ).show();
                Random r = new Random();
                boolean b = r.nextBoolean();
                int rate = r.nextInt(40) + 10;
                float capRefillTime = r.nextFloat()*3f + 0.5f;
                boolean w = r.nextBoolean();
                Victim.AVPU c = Victim.AVPU.values()[r.nextInt(Victim.AVPU.values().length)];
                Victim randomVictim = new Victim(b, rate, capRefillTime, w, c);

                Rescuer rescuer = new Rescuer("0", "test");
                Triplet<String, Rescuer, Victim> newRow = new Triplet<String, Rescuer, Victim>("", rescuer, randomVictim);
                victims.add(newRow);
                updateVictimsData();
                customAdapter.notifyDataSetChanged();
            }
        });

        victimList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), VictimDetailsActivity.class);
                intent.putExtra("victim", (Parcelable) victims.get(position).getValue2()); //sending victim data to new activity
                startActivity(intent);
            }
        });

        Button btn = findViewById(R.id.classification_system_confirm);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Spinner spnr = findViewById(R.id.classification_system_choice);
                int id = (int)spnr.getSelectedItemId();
                switch(id){
                    case 0:
                        TextView t = findViewById(R.id.classification_system_val); t.setText("START");
                        break;
                    default:
                        Toast.makeText(getApplicationContext(), "Wybrany system nie jest aktualnie zaimplementowany", Toast.LENGTH_SHORT).show();
                }
            }
        });

        TextView t = findViewById(R.id.reset_transmitter);
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(advertising){
                    Nearby.getConnectionsClient(getApplicationContext()).stopAdvertising();
                }
                startAdvertising();
                updateSettings();
            }
        });

        t = findViewById(R.id.clear_endpoints);
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Nearby.getConnectionsClient(getApplicationContext()).stopAllEndpoints();
            }
        });

    }


    public void updateSettings(){
        TextView t = findViewById(R.id.transmitter_status_label);
        t.setText( advertising ? "czeka na połączenia" : "bezczynny");
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
        switch(id){
            case R.id.action_victims:
                vf.setDisplayedChild(1);
                return true;
            case R.id.action_settings:
                updateSettings();
                vf.setDisplayedChild(2);
                return true;
            default:
                vf.setDisplayedChild(0);
        }


        return super.onOptionsItemSelected(item);
    }

    private void startAdvertising() {
        //Toast.makeText(getApplicationContext(), "Startujemy", Toast.LENGTH_SHORT).show();
        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
        connectionsClient = Nearby.getConnectionsClient(getApplicationContext());
        connectionsClient.startAdvertising(
                "Kierujacy Akcja Medyczna", SERVICE_ID, communicationCallbacks, advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            Toast.makeText(getApplicationContext(), "Startujemy nadawanie", Toast.LENGTH_SHORT).show();
                            advertising = true;
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            Toast.makeText(getApplicationContext(), "Nie wystartowano nadawania", Toast.LENGTH_SHORT).show();
                            Log.e("TAG", e.getMessage());
                        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!hasPermissions(this, getRequiredPermissions())) {
            if (!hasPermissions(this, getRequiredPermissions())) {
                if (Build.VERSION.SDK_INT < 23) {
                    ActivityCompat.requestPermissions(
                            this, getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
                } else {
                    requestPermissions(getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
                }
            }
        }
        startAdvertising();
    }

    protected String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }

    public void updateVictimsData(){
        int b=0, r=0, y=0, g=0;
        for(Triplet<String, Rescuer, Victim> row : victims){
            Victim v = row.getValue2();
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

    private void printToLogWithTimestamp(String message){
        Date d = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
        String timestamp = dateFormat.format(d);
        message = message.replace("\r\n", " ").replace("\n", " ");
        String line = timestamp+": "+message+"\n";
        try {
            File folder = getApplicationContext(). getExternalFilesDir(null);
            File log = new File(folder, log_filename);

            FileOutputStream outputStream;
            outputStream = new FileOutputStream(log, true); //openFileOutput(log_filename, Context.MODE_APPEND);
            outputStream.write(line.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
