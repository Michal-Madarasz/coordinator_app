package com.triage.model;

import java.io.Serializable;

public class Rescuer implements Serializable {
    private static final long serialVersionUID = 18636221345311L;

    private String id;
    private String name;

    public Rescuer() {
    }

    public Rescuer(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("ID=");
        sb.append(this.getName());
        sb.append(", IMEI=");
        sb.append(this.getId());

        return sb.toString();
    }
}
