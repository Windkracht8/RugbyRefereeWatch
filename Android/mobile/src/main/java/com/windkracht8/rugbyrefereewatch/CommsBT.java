package com.windkracht8.rugbyrefereewatch;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

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
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        context.registerReceiver(btStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }
    BroadcastReceiver btStateReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
                int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if(btState == BluetoothAdapter.STATE_TURNING_OFF){
                    stopListening(context);
                    gotError(context, context.getString(R.string.fail_BT_disabled));
                }else if(btState == BluetoothAdapter.STATE_ON){
                    startListening(context);
                }
            }
        }
    };

    public void startListening(Context context){
        if(!bluetoothAdapter.isEnabled()){
            gotError(context, context.getString(R.string.fail_BT_disabled));
            return;
        }
        CommsBTConnect commsBTConnect = new CommsBTConnect(context);
        commsBTConnect.start();
        updateStatus(context, "LISTENING");
    }

    public void stopListening(Context context){
        listen = false;
        try{
            context.unregisterReceiver(btStateReceiver);
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "CommsBT.stopListening unregisterReceiver: " + e.getMessage());
        }
        try{
            if(bluetoothSocket != null) bluetoothSocket.close();
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "CommsBT.stopListening bluetoothSocket: " + e.getMessage());
        }
        try{
            if(bluetoothServerSocket != null) bluetoothServerSocket.close();
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "CommsBT.stopListening bluetoothServerSocket: " + e.getMessage());
        }
    }

    public void sendRequest(String requestType, JSONObject requestData){
        Log.i(MainActivity.RRW_LOG_TAG, "CommsBT.sendRequest: " + requestType);
        try{
            JSONObject request = new JSONObject();
            request.put("requestType", requestType);
            request.put("requestData", requestData);
            requestQueue.put(request);
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "CommsBT.sendRequest Exception: " + e);
            Log.e(MainActivity.RRW_LOG_TAG, "CommsBT.sendRequest Exception: " + e.getMessage());
        }
    }

    private void gotError(Context context, final String error){
		updateStatus(context, "FATAL");
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "gotError");
        intent.putExtra("source", "BT");
        intent.putExtra("error", error);
        context.sendBroadcast(intent);
    }
    private void updateStatus(Context context, final String status_new){
        Log.i(MainActivity.RRW_LOG_TAG, "CommsBT.updateStatus: " + status_new);
        this.status = status_new;
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "updateStatus");
        intent.putExtra("source", "BT");
        intent.putExtra("status_new", status_new);
        context.sendBroadcast(intent);
    }

    private class CommsBTConnect extends Thread{
        private final Context context;
        @SuppressLint("MissingPermission")//already checked in startListening
        public CommsBTConnect(Context context){
            this.context = context;
            try{
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("RugbyRefereeWatch", RRW_UUID);
            }catch(Exception e){
                Log.e(MainActivity.RRW_LOG_TAG, "CommsBTConnect Exception: " + e.getMessage());
                gotError(context, e.getMessage());
            }
        }
        public void run(){
            try{
                bluetoothSocket = bluetoothServerSocket.accept();
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
            updateStatus(context, "CONNECTED");
            process();
        }
        private void process(){
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
            try{
                int available = inputStream.available();
                if(available == 0) return false;
                byte[] buffer = new byte[available];
                int numBytes = inputStream.read(buffer);
                if(numBytes < available){
                    Log.e(MainActivity.RRW_LOG_TAG, "CommsBTConnected.read: read " + numBytes + " bytes of " + available + " bytes available");
                    gotError(context, context.getString(R.string.fail_response));
                }else{
                    String response = new String(buffer);
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