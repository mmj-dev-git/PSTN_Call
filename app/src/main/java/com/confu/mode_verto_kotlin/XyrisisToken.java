package com.confu.mode_verto_kotlin;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class XyrisisToken extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "ZyrisToken";
    private StringBuilder result;
    private String command;
    private HttpURLConnection conn;
    private WebRTCModel example = new WebRTCModel();
    private VertoWSTransport.NetworkCallback networkCallback;

    public XyrisisToken(Context context, VertoWSTransport.NetworkCallback networkCallback) {
        command = "https://xirsys.confu.info/getIceServers";
        this.networkCallback = networkCallback;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        result = new StringBuilder();
        URL url = null;
        try {
            url = new URL(command);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
            JSONObject jObject = new JSONObject(result.toString());

            JSONObject iceServersObject = jObject.getJSONObject("v").getJSONObject("iceServers");
            String username = iceServersObject.getString("username");
            String credential = iceServersObject.getString("credential");
            JSONArray iceServers = iceServersObject.getJSONArray("urls");
            List<IceServer> iceServerList = new ArrayList<>();
            Log.d(TAG, "doInBackground: " + iceServerList.size());
            for (int i = 0; i < iceServers.length(); i++) {
                String urlString = iceServers.get(i).toString();
//                if (!urlString.contains("transport=tcp")) {
                String un = "";
                String pass = "";
                if (urlString.contains("stun")) {
                    un = "";
                    pass = "";
                } else {
                    un = username;
                    pass = credential;
                }
//                if (!un.isEmpty() && !pass.isEmpty()) {
                IceServer iceServer = new IceServer(urlString, un, pass);
                iceServerList.add(iceServer);
//                }
//                }
            }
            example = new WebRTCModel(username, credential, iceServerList);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "THAT DIDN'T work: " + e.toString());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        networkCallback.JsonData(example);
        networkCallback.initializeOutboundCall(example);
    }
}