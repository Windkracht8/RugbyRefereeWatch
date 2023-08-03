package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;

public class CommsInet{
    public String status = "DISCONNECTED";
    public boolean listen = false;
    private ServerSocket serverSocket;
    private Socket socket;
    private int port;
    private final JSONArray hostAddresses;
    private final JSONArray requestQueue;
    public CommsInet(Context context){
        hostAddresses = new JSONArray();
        requestQueue = new JSONArray();
        updateStatus(context, "DISCONNECTED");
    }
    public void startListening(Context context){
        Runnable listeningTask = () -> {
            try{
                if(serverSocket == null) serverSocket = new ServerSocket(0);
                port = serverSocket.getLocalPort();
                for(Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces(); networkInterfaces.hasMoreElements();){
                    NetworkInterface networkInterface = networkInterfaces.nextElement();
                    if(networkInterface.isLoopback()) continue;
                    //String networkName = networkInterface.getDisplayName();
                    //TODO: find a way to prioritize the networks based on isMetered
                    for(Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses(); inetAddresses.hasMoreElements();){
                        InetAddress inetAddress = inetAddresses.nextElement();
                        if(inetAddress.isLinkLocalAddress()) continue;
                        hostAddresses.put(inetAddress.getHostAddress());
                    }
                }
                listen = true;
                status = "LISTENING";
                while(listen){
                    socket = serverSocket.accept();
                    updateStatus(context, "CONNECTED");
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataOutputStream.writeBytes(getNextRequest());
                    String message = bufferedReader.readLine();
                    Log.i(MainActivity.RRW_LOG_TAG, "Received message: " + message);
                }
            }catch(Exception e){
                Log.e(MainActivity.RRW_LOG_TAG, "CommsInet.startListening Exception: " + e);
                Log.e(MainActivity.RRW_LOG_TAG, "CommsInet.startListening Exception: " + e.getMessage());
                updateStatus(context, "ERROR");
            }
        };
        Thread listeningThread = new Thread(listeningTask);
        listeningThread.start();
    }
    public void stopListening(){
        listen = false;
        try{
            socket.close();
            socket = null;
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "CommsInet.stopListening socket Exception: " + e);
            Log.e(MainActivity.RRW_LOG_TAG, "CommsInet.stopListening socket Exception: " + e.getMessage());
        }
        try{
            serverSocket.close();
            serverSocket = null;
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "CommsInet.stopListening serverSocket Exception: " + e);
            Log.e(MainActivity.RRW_LOG_TAG, "CommsInet.stopListening serverSocket Exception: " + e.getMessage());
        }
    }
    public void sendRequest(String requestType, JSONObject requestData){
        Log.i(MainActivity.RRW_LOG_TAG, "CommsInet.sendRequest: " + requestType);
        try{
            JSONObject request = new JSONObject();
            request.put("requestType", requestType);
            request.put("requestData", requestData);
            requestQueue.put(request);
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "CommsInet.sendRequest Exception: " + e);
            Log.e(MainActivity.RRW_LOG_TAG, "CommsInet.sendRequest Exception: " + e.getMessage());
        }
    }
    private String getNextRequest(){
        try{
            if(requestQueue.length() < 1) return "";
            JSONObject request = (JSONObject) requestQueue.get(0);
            String temp = request.toString();
            requestQueue.remove(0);
            return temp;
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "CommsInet.formRequest Exception: " + e);
            Log.e(MainActivity.RRW_LOG_TAG, "CommsInet.formRequest Exception: " + e.getMessage());
        }
        return "";
    }
    private JSONObject getConnectData(){
        try{
            JSONObject connectData = new JSONObject();
            connectData.put("port", port);
            connectData.put("hostAddresses", hostAddresses);
            return connectData;
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "CommsInet.getConnectData Exception: " + e);
            Log.e(MainActivity.RRW_LOG_TAG, "CommsInet.getConnectData Exception: " + e.getMessage());
        }
        return null;
    }
    private void updateStatus(Context context, final String status_new){
        this.status = status_new;
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "updateStatus");
        intent.putExtra("source", "tizen");
        intent.putExtra("status_new", status_new);
        context.sendBroadcast(intent);
    }
}
