package com.windkracht8.rugbyrefereewatch;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

public class CommsBT {
    private final UUID RRW_UUID = UUID.fromString("8b16601b-5c76-4151-a930-2752849f4552");
    public String status = "DISCONNECTED";
    public boolean listen = false;
    private final JSONArray requestQueue;
    public BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket bluetoothServerSocket;
    private BluetoothSocket bluetoothSocket;

    public CommsBT(MainActivity ma) {
        Log.i(MainActivity.RRW_LOG_TAG, "CommsBT");
        requestQueue = new JSONArray();
        updateStatus(ma, "DISCONNECTED");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void startListening(Context context) {
        Log.i(MainActivity.RRW_LOG_TAG, "CommsBT.startListening");
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
            Log.i(MainActivity.RRW_LOG_TAG, "CommsBT.startListening checkSelfPermission = no");
            updateStatus(context, "DENIED");
            gotError(context, "Permission denied");
            return;
        }
        CommsBTConnect commsBTConnect = new CommsBTConnect(context);
        commsBTConnect.start();
        updateStatus(context, "FINDING_PEERS");
    }

    public void stopListening() {
        Log.i(MainActivity.RRW_LOG_TAG, "CommsBT.stopListening");
        listen = false;
        try{
            bluetoothServerSocket.close();
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "CommsBT.stopListening bluetoothServerSocket: " + e.getMessage());
        }
        try{
            bluetoothSocket.close();
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "CommsBT.stopListening bluetoothServerSocket: " + e.getMessage());
        }
    }

    public void sendRequest(String requestType, JSONObject requestData) {
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
        try {
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

    private void gotError(Context context, final String error){
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "gotError");
        intent.putExtra("source", "BT");
        intent.putExtra("error", error);
        context.sendBroadcast(intent);
    }
    private void updateStatus(Context context, final String status_new) {
        this.status = status_new;
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "updateStatus");
        intent.putExtra("source", "BT");
        intent.putExtra("status_new", status_new);
        context.sendBroadcast(intent);
    }

    private class CommsBTConnect extends Thread{
        public CommsBTConnect(Context context){
            Log.i(MainActivity.RRW_LOG_TAG, "CommsBTConnect " + RRW_UUID);
            try{
                if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
                    Log.i(MainActivity.RRW_LOG_TAG, "checkSelfPermission = no");
                    updateStatus(context, "DENIED");
                    return;
                }
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("RugbyRefereeWatch", RRW_UUID);
            }catch(Exception e){
                Log.e(MainActivity.RRW_LOG_TAG, "CommsBTConnect Exception: " + e.getMessage());
                updateStatus(context, "ERROR");
                gotError(context, e.getMessage());
            }
        }
	   public void run(){
           Log.i(MainActivity.RRW_LOG_TAG, "CommsBTConnect.run");
           try{
               bluetoothSocket = bluetoothServerSocket.accept();
               Log.i(MainActivity.RRW_LOG_TAG, "CommsBTConnect.run accepted " + bluetoothSocket);
               CommsBTConnected commsBTConnected = new CommsBTConnected();
               commsBTConnected.start();
           }catch(Exception e){
               Log.e(MainActivity.RRW_LOG_TAG, "CommsBTConnect.run Exception: " + e.getMessage());
           }
	   }
	}
	
	private class CommsBTConnected extends Thread{
       private InputStream inputStream;
       private OutputStream outputStream;

        public CommsBTConnected(){
           Log.i(MainActivity.RRW_LOG_TAG, "CommsBTConnected");
           try{
               inputStream = bluetoothSocket.getInputStream();
           }catch(Exception e){
			   Log.e(MainActivity.RRW_LOG_TAG, "CommsBTConnected getInputStream Exception: " + e.getMessage());
           }
           try{
               outputStream = bluetoothSocket.getOutputStream();
           }catch(Exception e){
			   Log.e(MainActivity.RRW_LOG_TAG, "CommsBTConnected getOutputStream Exception: " + e.getMessage());
           }
       }

       public void run(){
            listen = true;
            Log.i(MainActivity.RRW_LOG_TAG, "CommsBTConnected.run");
            while(listen){
                try{
                    CommsBTConnectedSend("hello");
                    //CommsBTConnectedSend(getNextRequest());
                    CommsBTConnectedRead();
                }catch(Exception e){
                    Log.i(MainActivity.RRW_LOG_TAG, "Input stream was disconnected", e);
                    break;
                }
            }
       }

       private void CommsBTConnectedRead(){
           Log.i(MainActivity.RRW_LOG_TAG, "CommsBTConnectedRead");
           byte[] buffer = new byte[1024];
           int numBytes = 1;
           while(listen && numBytes > 0){
               try{
                   numBytes = inputStream.read(buffer);
                   Log.i(MainActivity.RRW_LOG_TAG, "Message with " + numBytes + " bytes received: " + Arrays.toString(buffer));
               }catch(Exception e){
                   Log.i(MainActivity.RRW_LOG_TAG, "Input stream was disconnected", e);
                   break;
               }
           }
       }
       private void CommsBTConnectedSend(String message_out){
           Log.i(MainActivity.RRW_LOG_TAG, "CommsBTConnected.send: " + message_out);
           try{
               outputStream.write(message_out.getBytes());
           }catch(Exception e){
               Log.e(MainActivity.RRW_LOG_TAG, "CommsBTConnected.send Exception: ", e);
           }
       }
   }
}