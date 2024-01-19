package com.windkracht8.rugbyrefereewatch;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.UUID;

public class Comms{
    final UUID RRW_UUID = UUID.fromString("8b16601b-5c76-4151-a930-2752849f4552");
    String status = "INIT";
    boolean listen = false;
    final JSONArray requestQueue;
    final BluetoothAdapter bluetoothAdapter;
    BluetoothServerSocket bluetoothServerSocket;
    BluetoothSocket bluetoothSocket;
    final Context context;
    final Handler handler_message;
    final Handler handler;

    public Comms(Context context, Handler handler_message){
        this.context = context;
        this.handler_message = handler_message;
        handler = new Handler(Looper.getMainLooper());
        requestQueue = new JSONArray();
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null){
            bt_off();
            return;
        }
        context.registerReceiver(btStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    private final BroadcastReceiver btStateReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
                int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if(btState == BluetoothAdapter.STATE_TURNING_OFF){
                    stopListening();
                    bt_off();
                }else if(btState == BluetoothAdapter.STATE_ON){
                    startListening();
                }
            }
        }
    };

    public void startListening(){
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
            bt_off();
            return;
        }
        CommsConnect commsConnect = new CommsConnect();
        commsConnect.start();
        updateStatus("LISTENING");
    }

    public void stopListening(){
        listen = false;
        try{
            context.unregisterReceiver(btStateReceiver);
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "Comms.stopListening unregisterReceiver: " + e.getMessage());
        }
        try{
            if(bluetoothSocket != null) bluetoothSocket.close();
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "Comms.stopListening bluetoothSocket: " + e.getMessage());
        }
        try{
            if(bluetoothServerSocket != null) bluetoothServerSocket.close();
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "Comms.stopListening bluetoothServerSocket: " + e.getMessage());
        }
    }

    public void sendRequest(String requestType, JSONObject requestData){
        Log.d(Main.RRW_LOG_TAG, "Comms.sendRequest: " + requestType);
        try{
            JSONObject request = new JSONObject();
            request.put("requestType", requestType);
            request.put("requestData", requestData);
            requestQueue.put(request);
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "Comms.sendRequest Exception: " + e);
            Log.e(Main.RRW_LOG_TAG, "Comms.sendRequest Exception: " + e.getMessage());
        }
    }

    private void gotError(String fatal_string){
        updateStatus("FATAL");
        handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_GOT_ERROR, fatal_string));
    }
    private void bt_off(){
        updateStatus("FATAL");
        handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_BT_OFF));
    }
    private void updateStatus(String status){
        this.status = status;
        handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_UPDATE_STATUS, status));
    }

    private class CommsConnect extends Thread{
        @SuppressLint("MissingPermission")//already checked in startListening
        public CommsConnect(){
            if(bluetoothServerSocket != null) return;
            try{
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("RugbyRefereeWatch", RRW_UUID);
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsConnect Exception: " + e.getMessage());
                gotError(e.getMessage());
            }
        }

        public void run(){
            try{
                bluetoothSocket = bluetoothServerSocket.accept();
                CommsConnected commsConnected = new CommsConnected();
                commsConnected.start();
            }catch(Exception e){
                Log.i(Main.RRW_LOG_TAG, "CommsConnect.run Exception: " + e.getMessage());
            }
        }
    }

    private class CommsConnected extends Thread{
        private InputStream inputStream;
        private OutputStream outputStream;

        public CommsConnected(){
            try{
                inputStream = bluetoothSocket.getInputStream();
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsConnected getInputStream Exception: " + e.getMessage());
            }
            try{
                outputStream = bluetoothSocket.getOutputStream();
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsConnected getOutputStream Exception: " + e.getMessage());
            }
        }

        public void run(){
            listen = true;
            updateStatus("CONNECTED");
            process();
        }

        private void process(){
            if(!listen){
                close();
                return;
            }
            if(!sendNextRequest()){
                close();
                startListening();
                return;
            }
            read();
            handler.postDelayed(this::process, 100);
        }

        private void close(){
            try{
                bluetoothSocket.close();
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsConnected.close exception: " + e.getMessage());
            }
        }

        private void sleep100(){
            try{
                Thread.sleep(100);
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsConnected.sleep100 exception: " + e.getMessage());
            }
        }

        private boolean sendNextRequest(){
            try{
                outputStream.write("".getBytes());
                if(requestQueue.length() < 1) return true;
                JSONObject request = (JSONObject) requestQueue.get(0);
                requestQueue.remove(0);
                Log.d(Main.RRW_LOG_TAG, "CommsConnected.sendNextRequest: " + request.toString());
                outputStream.write(request.toString().getBytes());
            }catch(Exception e){
                if(e.getMessage() != null && e.getMessage().contains("Broken pipe")){
                    return false;
                }
                Log.e(Main.RRW_LOG_TAG, "CommsConnected.sendNextRequest Exception: " + e.getMessage());
                handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_send_message));
            }
            return true;
        }

        private void read(){
            try{
                if(inputStream.available() < 5) return;
                long read_start = (new Date()).getTime();
                String response = "";

                while(inputStream.available() > 0){
                    byte[] buffer = new byte[500];
                    int numBytes = inputStream.read(buffer);
                    if(numBytes < 0){
                        Log.e(Main.RRW_LOG_TAG, "CommsConnected.read read error: " + response);
                        handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_response));
                        return;
                    }
                    String temp = new String(buffer);
                    response += temp;
                    if(isValidJSON(response)){
                        gotResponse(response);
                        return;
                    }
                    if((new Date()).getTime() - read_start > 3000){
                        Log.e(Main.RRW_LOG_TAG, "CommsConnected.read started to read, no complete message after 3 seconds: " + response);
                        handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_response));
                        return;
                    }
                    sleep100();
                }
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsConnected.read: " + e.getMessage());
                handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_response));
            }
        }

        private void gotResponse(String response){
            try{
                JSONObject responseMessage = new JSONObject(response);
                Log.d(Main.RRW_LOG_TAG, "CommsConnected.gotResponse: " + responseMessage);
                handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_GOT_RESPONSE, responseMessage));
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsConnected.gotResponse: " + e.getMessage());
                handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_response));
            }
        }

        private boolean isValidJSON(String json){
            try{
                new JSONObject(json);
            }catch(JSONException e){
                return false;
            }
            return true;
        }

    }
}