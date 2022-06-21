package com.confu.mode_verto_kotlin;

import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;

import org.webrtc.IceCandidate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public class UtilityMethods {

    public static final String TAG = "UtilityMethods";
    private static int jsonRPCId = 0;

    public static boolean ipInCGN(String ip) {
        String[] octets = ip.split("\\.");
        return Integer.parseInt(octets[0]) == 100 && Integer.parseInt(octets[1]) > 63 && Integer.parseInt(octets[1]) < 128;
    }

    public static boolean parseIceCandidate(IceCandidate iceCandidate) {
        boolean isValidIceCandidate = false;

        String ip = iceCandidate.sdp.trim().split("\\s+")[4];
        Log.d(TAG, "parseIceCandidate: IceCandidateIP" + ip);
        try {
            InetAddress ia = InetAddress.getByName(ip);
            if (ia.isSiteLocalAddress() || ia.isLoopbackAddress() || ipInCGN(ip)) {
                Log.d(TAG, "parseIceCandidate: Invalid IceCandidate.");
                isValidIceCandidate = false;
            }
            isValidIceCandidate = true;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return isValidIceCandidate;
    }

    public static void parseIceCandidate(IceCandidate iceCandidate, WebRtc webRtc) {
        String ip = iceCandidate.sdp.trim().split("\\s+")[4];
        Log.d(TAG, "parseIceCandidate: IceCandidateIP" + ip);
        try {
            InetAddress ia = InetAddress.getByName(ip);
            if (ia.isSiteLocalAddress() || ia.isLoopbackAddress() || ipInCGN(ip)) {
                Log.d(TAG, "parseIceCandidate: Invalid IceCandidate.");
                return;
            }
            webRtc.handleCallInvitationAndResponse();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static void callJSONRPC2(String method, Map<String, Object> params, WebSocket webSocket) {
        int i = jsonRPCId + 1;
        jsonRPCId = i;
        JSONRPC2Request jsonrpc2Request = new JSONRPC2Request(method, params, Integer.valueOf(i));
        Log.d(TAG, "callJSONRPC2: \n" + jsonrpc2Request.toString() + "\n--------------------------------------------\n");

        webSocket.sendText(jsonrpc2Request.toString());
        webSocket.flush();
    }

}
