package com.triage.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

//klasa reprezentująca poszkodowanego rozszerzona o
//interfejs pozwalający na przesyłanie obiektu między aktywnościami
public class Victim implements Parcelable, Serializable {
    private static final long serialVersionUID = 186362213453111235L;

    public enum TriageColor{ BLACK, RED, YELLOW, GREEN }
    public enum AVPU{ AWAKE, VERBAL, PAIN, UNRESPONSIVE }

    private static int totalID = 0;

    private boolean changingState;
    private long id;
    private boolean breathing;
    private float respiratoryRate;
    private float capillaryRefillTime;
    private boolean walking;
    private TriageColor color;
    private AVPU consciousness;

    public Victim(boolean breathing, float respiratoryRate, float capillaryRefillTime, boolean walking, AVPU consciousness) {
        this.id = totalID; totalID++;
        this.breathing = breathing;
        this.respiratoryRate = respiratoryRate;
        this.capillaryRefillTime = capillaryRefillTime;
        this.walking = walking;
        this.consciousness = consciousness;
        calculateColor();
    }

    public Victim() {
        this.id = totalID; totalID++;
    }

    protected Victim(Parcel in) {
        id = in.readLong();
        breathing = (boolean)in.readValue(null);
        respiratoryRate = in.readFloat();
        capillaryRefillTime = in.readFloat();
        walking = (boolean)in.readValue(null);
        color = (TriageColor)in.readValue(null);
        consciousness = (AVPU)in.readValue(null);
    }

    public static final Creator<Victim> CREATOR = new Creator<Victim>() {
        @Override
        public Victim createFromParcel(Parcel in) {
            return new Victim(in);
        }

        @Override
        public Victim[] newArray(int size) {
            return new Victim[size];
        }
    };

    public void calculateColor(){
        if(walking){
            color = TriageColor.GREEN;
            return;
        }

        if(!breathing){
            color = TriageColor.BLACK;
            return;
        }

        if(respiratoryRate>30){
            color = TriageColor.RED;
            return;
        }

        if(capillaryRefillTime>2){
            color = TriageColor.RED;
            return;
        }

        if(consciousness==AVPU.PAIN || consciousness==AVPU.UNRESPONSIVE){
            color = TriageColor.RED;
            return;
        }
        color = TriageColor.YELLOW;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isBreathing() {
        return breathing;
    }

    public void setBreathing(boolean breathing) {
        this.breathing = breathing;
    }

    public float getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(float respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    public boolean isWalking() {
        return walking;
    }

    public void setWalking(boolean walking) {
        this.walking = walking;
    }

    public TriageColor getColor() {
        return color;
    }

    public void setColor(TriageColor color) {
        this.color = color;
    }

    public AVPU getConsciousness() {
        return consciousness;
    }

    public void setConsciousness(AVPU consciousness) {
        this.consciousness = consciousness;
    }

    public float getCapillaryRefillTime() {
        return capillaryRefillTime;
    }

    public void setCapillaryRefillTime(float capillaryRefillTime) {
        this.capillaryRefillTime = capillaryRefillTime;
    }

    //Parceable methods
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeValue(breathing);
        dest.writeFloat(respiratoryRate);
        dest.writeFloat(capillaryRefillTime);
        dest.writeValue(walking);
        dest.writeValue(color);
        dest.writeValue(consciousness);
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Kolor: ");
        switch(color){
            case BLACK:  sb.append("czarny");   break;
            case RED:    sb.append("czerwony"); break;
            case YELLOW: sb.append("żółty");    break;
            case GREEN:  sb.append("zielony");  break;
        }

        sb.append(", Oddycha: ");
        sb.append(breathing ? "tak" : "nie");

        sb.append(", Częst. oddechów: ");
        sb.append(respiratoryRate);

        sb.append("odd/min, Nawrót Kapilarny: ");
        sb.append(capillaryRefillTime);

        sb.append("s, chodzi: ");
        sb.append(walking ? "tak" : "nie");

        sb.append(", AVPU: ");
        switch(consciousness){
            case AWAKE: sb.append("przytomny"); break;
            case VERBAL: sb.append("reag. na głos"); break;
            case PAIN: sb.append("reag. na ból"); break;
            case UNRESPONSIVE: sb.append("nieprzytomny"); break;
        }

        return sb.toString();
    }
}
