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
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Comms{
    final UUID RRW_UUID = UUID.fromString("8b16601b-5c76-4151-a930-2752849f4552");
    final BluetoothAdapter bluetoothAdapter;
    BluetoothServerSocket bluetoothServerSocket;
    BluetoothSocket bluetoothSocket;
    final Main main;
    final Handler handler;

    boolean listen = false;
    final JSONArray responseQueue = new JSONArray();

    public Comms(Main main){
        this.main = main;
        handler = new Handler(Looper.getMainLooper());
        BluetoothManager bluetoothManager = (BluetoothManager) main.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null){
            CommsLog.addToLog("Bluetooth disabled");
            return;
        }
        main.registerReceiver(btStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    private final BroadcastReceiver btStateReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
                int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if(btState == BluetoothAdapter.STATE_TURNING_OFF){
                    stopListening();
                }else if(btState == BluetoothAdapter.STATE_ON &&
                        (Main.timer_status.equals("conf") || Main.timer_status.equals("finished"))
                ){
                    startListening();
                }
            }
        }
    };

    public void startListening(){
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
            CommsLog.addToLog("Bluetooth disabled");
            return;
        }
        CommsLog.addToLog("Start listening");
        CommsConnect commsConnect = new CommsConnect();
        commsConnect.start();
    }

    public void stopListening(){
        listen = false;
        Log.d(Main.RRW_LOG_TAG, "Comms.stopListening");
        try{
            main.unregisterReceiver(btStateReceiver);
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

    private class CommsConnect extends Thread{
        @SuppressLint("MissingPermission") //Permissions are handled in initBT, no further need to complain
        public CommsConnect(){
            if(bluetoothServerSocket != null) return;
            try{
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("MusicPlayer", RRW_UUID);
            }catch(Exception e){
                CommsLog.addToLog("Connect failed: " + e.getMessage());
                Log.e(Main.RRW_LOG_TAG, "CommsConnect Exception: " + e.getMessage());
            }
        }

        public void run(){
            try{
                bluetoothSocket = bluetoothServerSocket.accept();
                CommsConnected commsConnected = new CommsConnected();
                commsConnected.start();
            }catch(Exception e){
                CommsLog.addToLog("Connect to socket failed: " + e.getMessage());
            }
        }
    }

    private class CommsConnected extends Thread{
        private InputStream inputStream;
        private OutputStream outputStream;

        public CommsConnected(){
            try{
                CommsLog.addToLog("Connected, get input stream");
                inputStream = bluetoothSocket.getInputStream();
            }catch(Exception e){
                CommsLog.addToLog("Connected, get input stream failed: " + e.getMessage());
                Log.e(Main.RRW_LOG_TAG, "CommsConnected getInputStream Exception: " + e.getMessage());
            }
            try{
                CommsLog.addToLog("Connected, get output stream");
                outputStream = bluetoothSocket.getOutputStream();
            }catch(Exception e){
                CommsLog.addToLog("Connected, get output stream failed: " + e.getMessage());
                Log.e(Main.RRW_LOG_TAG, "CommsConnected getOutputStream Exception: " + e.getMessage());
            }
        }

        public void run(){
            Log.d(Main.RRW_LOG_TAG, "CommsConnected.run");
            listen = true;
            process();
        }

        private void process(){
            if(!listen){
                close();
                return;
            }
            if(!sendNextResponse()){
                close();
                startListening();
                return;
            }
            read();
            handler.postDelayed(this::process, 100);
        }

        private void close(){
            try{
                CommsLog.addToLog("Close");
                bluetoothSocket.close();
            }catch(Exception e){
                CommsLog.addToLog("Close failed: " + e.getMessage());
                Log.e(Main.RRW_LOG_TAG, "CommsConnected.close exception: " + e.getMessage());
            }
        }

        private boolean sendNextResponse(){
            try{
                outputStream.write("".getBytes());
                if(responseQueue.length() < 1) return true;
                JSONObject response = (JSONObject) responseQueue.get(0);
                responseQueue.remove(0);
                Log.d(Main.RRW_LOG_TAG, "CommsConnected.sendNextResponse: " + response.toString());
                CommsLog.addToLog("Send response " + response.getString("requestType"));
                outputStream.write(response.toString().getBytes());
            }catch(Exception e){
                CommsLog.addToLog("Send response failed: " + e.getMessage());
                if(e.getMessage() != null && e.getMessage().contains("Broken pipe")){
                    return false;
                }
                Log.e(Main.RRW_LOG_TAG, "CommsConnected.sendNextResponse Exception: " + e.getMessage());
            }
            return true;
        }

        private void read(){
            try{
                int available = inputStream.available();
                if(available == 0) return;
                byte[] buffer = new byte[available];
                int numBytes = inputStream.read(buffer);
                if(numBytes > 3){
                    String request = new String(buffer);
                    JSONObject requestMessage = new JSONObject(request);
                    CommsLog.addToLog("Read " + requestMessage.getString("requestType"));
                    gotRequest(requestMessage);
                }
            }catch(Exception e){
                CommsLog.addToLog("Read failed: " + e.getMessage());
                Log.e(Main.RRW_LOG_TAG, "CommsConnected.read: Input stream read exception: " + e.getMessage());
            }
        }

        private void gotRequest(final JSONObject requestMessage){
            Log.d(Main.RRW_LOG_TAG, "CommsConnected.gotRequest: " + requestMessage.toString());
            try{
                String requestType = requestMessage.getString("requestType");
                switch(requestType){
                    case "sync":
                        onReceiveSync(requestMessage.getString("requestData"));
                        break;
                    case "prepare":
                        onReceivePrepare(requestMessage.getString("requestData"));
                        break;
                    default:
                        Log.e(Main.RRW_LOG_TAG, "CommsConnected.gotRequest Unknown requestType: " + requestType);
                }

            }catch(Exception e){
                CommsLog.addToLog("gotRequest failed: " + e.getMessage());
                Log.e(Main.RRW_LOG_TAG, "CommsConnected.gotRequest Exception: " + e.getMessage());
            }
        }
    }

    private void onReceiveSync(String requestData){
        try{
            JSONObject settings = Main.getSettings();
            if(settings == null) return;
            JSONObject responseData = new JSONObject();
            responseData.put("matches", FileStore.deletedMatches(main, requestData));
            responseData.put("settings", settings);
            sendResponse("sync", responseData);
            Conf.syncCustomMatchTypes(main.handler_message, requestData);
        }catch(Exception e){
            CommsLog.addToLog("onReceiveSync failed: " + e.getMessage());
            Log.e(Main.RRW_LOG_TAG, "Comms.onReceiveSync Exception: " + e.getMessage());
            sendResponse("sync", "unexpected error");
        }
    }
    private void onReceivePrepare(String requestData){
        try{
            JSONObject requestData_json = new JSONObject(requestData);
            if(Main.incomingSettings(main.handler_message, requestData_json)){
                sendResponse("prepare", "okilly dokilly");
                FileStore.storeSettings(main);
            }else{
                sendResponse("prepare", "match ongoing");
            }
            main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_PREPARE_RECEIVED));
        }catch(Exception e){
            CommsLog.addToLog("onReceivePrepare failed: " + e.getMessage());
            Log.e(Main.RRW_LOG_TAG, "Comms.onReceivePrepare Exception: " + e.getMessage());
            sendResponse("prepare", "unexpected error");
        }
    }
    public void sendResponse(final String requestType, final String responseData){
        try{
            JSONObject response = new JSONObject();
            response.put("requestType", requestType);
            response.put("responseData", responseData);
            responseQueue.put(response);
        }catch(Exception e){
            CommsLog.addToLog("sendResponse failed: " + e.getMessage());
            Log.e(Main.RRW_LOG_TAG, "Comms.sendResponse String Exception: " + e.getMessage());
        }
    }
    public void sendResponse(final String requestType, final JSONObject responseData){
        try{
            JSONObject response = new JSONObject();
            response.put("requestType", requestType);
            response.put("responseData", responseData);
            responseQueue.put(response);
        }catch(Exception e){
            CommsLog.addToLog("sendResponse failed: " + e.getMessage());
            Log.e(Main.RRW_LOG_TAG, "Comms.sendResponse JSONObject Exception: " + e.getMessage());
        }
    }
}