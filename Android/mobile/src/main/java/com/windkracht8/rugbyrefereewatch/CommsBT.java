package com.windkracht8.rugbyrefereewatch;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class CommsBT{
    private final UUID RRW_UUID = UUID.fromString("8b16601b-5c76-4151-a930-2752849f4552");
    public String status = "DISCONNECTED";
    public boolean listen = false;
    private final JSONArray requestQueue;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket bluetoothServerSocket;
    private BluetoothSocket bluetoothSocket;

    public CommsBT(Context context){
        requestQueue = new JSONArray();
        updateStatus(context, "DISCONNECTED");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void startListening(Context context){
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
            updateStatus(context, "DENIED");
            gotError(context, "Permission denied");
            return;
        }
        CommsBTConnect commsBTConnect = new CommsBTConnect(context);
        commsBTConnect.start();
        updateStatus(context, "LISTENING");
    }

    public void stopListening(){
        listen = false;
        try{
            bluetoothSocket.close();
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "CommsBT.stopListening bluetoothSocket: " + e.getMessage());
        }
        try{
            bluetoothServerSocket.close();
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "CommsBT.stopListening bluetoothServerSocket: " + e.getMessage());
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

    private void gotError(Context context, final String error){
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "gotError");
        intent.putExtra("source", "BT");
        intent.putExtra("error", error);
        context.sendBroadcast(intent);
    }
    private void updateStatus(Context context, final String status_new){
        this.status = status_new;
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "updateStatus");
        intent.putExtra("source", "BT");
        intent.putExtra("status_new", status_new);
        context.sendBroadcast(intent);
    }

    private class CommsBTConnect extends Thread{
        private final Context context;
        public CommsBTConnect(Context context){
            Log.i(MainActivity.RRW_LOG_TAG, "CommsBTConnect " + RRW_UUID);
            this.context = context;
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
                CommsBTConnected commsBTConnected = new CommsBTConnected(context);
                commsBTConnected.start();
            }catch(Exception e){
                Log.e(MainActivity.RRW_LOG_TAG, "CommsBTConnect.run Exception: " + e.getMessage());
            }
        }
    }
    
    private class CommsBTConnected extends Thread{
        private InputStream inputStream;
        private OutputStream outputStream;
        private final Context context;

        public CommsBTConnected(Context context){
            Log.i(MainActivity.RRW_LOG_TAG, "CommsBTConnected");
            this.context = context;
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
            updateStatus(context, "CONNECTED");

            process();
        }
        private void process(){
            Log.i(MainActivity.RRW_LOG_TAG, "CommsBTConnected.process");
            if(requestQueue.length() > 0){
                sendNextRequest();
                for(int i=0; i<10; i++){
                    if(read()) break;
                    if(!listen) return;
                    sleep100();
                }
            }else{
                for(int i=0; i<10; i++){
                    if(read()) break;
                    if(!listen) return;
                    if(requestQueue.length() > 0) break;
                    sleep100();
                }
            }
            if(listen) process();
        }
        private void sleep100(){
            try{
                Thread.sleep(100);
            }catch(Exception e){
                Log.e(MainActivity.RRW_LOG_TAG, "CommsBTConnected.sleep100 exception: " + e.getMessage());
            }
        }

        private void sendNextRequest(){
            try{
                if(requestQueue.length() < 1) return;
                JSONObject request = (JSONObject) requestQueue.get(0);
                requestQueue.remove(0);
                Log.i(MainActivity.RRW_LOG_TAG, "CommsBTConnected.sendNextRequest: " + request.toString());
                outputStream.write(request.toString().getBytes());
            }catch(Exception e){
                Log.e(MainActivity.RRW_LOG_TAG, "CommsBTConnected.sendNextRequest Exception: " + e.getMessage());
                Toast.makeText(context, R.string.fail_send_message, Toast.LENGTH_SHORT).show();
            }
        }
        private boolean read(){
            Log.i(MainActivity.RRW_LOG_TAG, "CommsBTConnected.read");
            try{
                int available = inputStream.available();
                Log.i(MainActivity.RRW_LOG_TAG, "CommsBTConnected.read: available: " + available);
                if(available == 0) return false;
                byte[] buffer = new byte[available];
                int numBytes = inputStream.read(buffer);
                if(numBytes < available){
                    Log.e(MainActivity.RRW_LOG_TAG, "CommsBTConnected.read: read " + numBytes + " bytes of " + available + " bytes available");
                    gotError(context, context.getString(R.string.fail_response));
                }else{
                    String response = new String(buffer);
                    Log.i(MainActivity.RRW_LOG_TAG, "CommsBTConnected.read: response " + response);
                    JSONObject responseMessage = new JSONObject(response);
                    gotResponse(responseMessage);
                }
                return true;
            }catch(Exception e){
                Log.e(MainActivity.RRW_LOG_TAG, "CommsBTConnected.read: Input stream read exception: " + e.getMessage());
                gotError(context, context.getString(R.string.fail_response));
            }
            return false;
        }
        private void gotResponse(final JSONObject responseMessage){
            Log.i(MainActivity.RRW_LOG_TAG, "CommsBTConnected.gotResponse: " + responseMessage.toString());
            try{
                Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
                intent.putExtra("intent_type", "gotResponse");
                intent.putExtra("source", "BT");
                intent.putExtra("requestType", responseMessage.getString("requestType"));
                intent.putExtra("responseData", responseMessage.getString("responseData"));
                context.sendBroadcast(intent);
            }catch(Exception e){
                Log.e(MainActivity.RRW_LOG_TAG, "CommsBTConnected.gotResponse Exception: " + e.getMessage());
                gotError(context, context.getString(R.string.fail_response));
            }
        }
    }
}