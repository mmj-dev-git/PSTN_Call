package com.confu.mode_verto_kotlin;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WebRTCModel {
    public WebRTCModel() {
    }

    public WebRTCModel(String username, String password, List<IceServer> iceServers) {
        this.username = username;
        this.password = password;
        this.iceServers = iceServers;
    }

    @SerializedName("username")
    @Expose
    public String username;
    @SerializedName("password")
    @Expose
    public String password;
    @SerializedName("ice_servers")
    @Expose
    public List<IceServer> iceServers = null;

    @Override
    public String toString() {
        return "WebRTCModel{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", iceServers=" + iceServers +
                '}';
    }
}