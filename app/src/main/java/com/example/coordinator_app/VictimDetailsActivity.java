package com.example.coordinator_app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.TextView;

import com.triage.model.Victim;

import org.w3c.dom.Text;

public class VictimDetailsActivity extends AppCompatActivity {

    Victim victim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_victim_details);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        victim = intent.getParcelableExtra("victim");
        setTitle("Info o poszkodowanym");

        TextView t = findViewById(R.id.breath_val);
        if(victim.isBreathing())
            t.setText("tak");
        else
            t.setText("nie");

        t = findViewById(R.id.refill_val);
        t.setText(victim.getRespiratoryRate()+"odd./min");

        t = findViewById(R.id.pulse_val);
        t.setText(victim.getCapillaryRefillTime()+"s");

        t = findViewById(R.id.walking_val);
        if(victim.isWalking())
            t.setText("tak");
        else
            t.setText("nie");

        t = findViewById(R.id.conscious_val);
        switch(victim.getConsciousness()){
            case AWAKE: t.setText("przytomny"); break;
            case VERBAL: t.setText("reag. na głos"); break;
            case PAIN: t.setText("reag. na ból"); break;
            case UNRESPONSIVE: t.setText("nieprzytomny"); break;
        }

        ImageView img = findViewById(R.id.color_val);
        switch(victim.getColor()){
            case BLACK: img.setImageResource(R.color.colorTriageBlack); break;
            case RED: img.setImageResource(R.color.colorTriageRed); break;
            case YELLOW: img.setImageResource(R.color.colorTriageYellow); break;
            case GREEN: img.setImageResource(R.color.colorTriageGreen); break;
        }
    }
}
