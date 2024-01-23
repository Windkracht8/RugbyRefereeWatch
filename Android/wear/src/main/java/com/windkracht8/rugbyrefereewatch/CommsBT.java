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

public class CommsBT{
    final UUID RRW_UUID = UUID.fromString("8b16601b-5c76-4151-a930-2752849f4552");
    final BluetoothAdapter bluetoothAdapter;
    BluetoothServerSocket bluetoothServerSocket;
    BluetoothSocket bluetoothSocket;
    final Main main;
    final Handler handler;

    boolean listen = false;
    final JSONArray responseQueue = new JSONArray();

    public CommsBT(Main main){
        this.main = main;
        handler = new Handler(Looper.getMainLooper());
        BluetoothManager bluetoothManager = (BluetoothManager) main.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null){
            CommsBTLog.addToLog("Bluetooth disabled");
            return;
        }
        main.registerReceiver(btStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    private final BroadcastReceiver btStateReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
                int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if(btState == BluetoothAdapter.STATE_TURNING_OFF){
                    stop();
                }else if(btState == BluetoothAdapter.STATE_ON &&
                        (Main.timer_status.equals("conf") || Main.timer_status.equals("finished"))
                ){
                    start();
                }
            }
        }
    };

    public void start(){
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
            CommsBTLog.addToLog("Bluetooth disabled");
            return;
        }
        CommsBTLog.addToLog("Start listening");
        CommsBTConnect commsBTConnect = new CommsBTConnect();
        commsBTConnect.start();
    }

    public void stop(){
        listen = false;
        Log.d(Main.RRW_LOG_TAG, "CommsBT.stopListening");
        try{
            main.unregisterReceiver(btStateReceiver);
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "CommsBT.stopListening unregisterReceiver: " + e.getMessage());
        }
        try{
            if(bluetoothSocket != null) bluetoothSocket.close();
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "CommsBT.stopListening bluetoothSocket: " + e.getMessage());
        }
        try{
            if(bluetoothServerSocket != null) bluetoothServerSocket.close();
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "CommsBT.stopListening bluetoothServerSocket: " + e.getMessage());
        }
    }

    private class CommsBTConnect extends Thread{
        @SuppressLint("MissingPermission") //Permissions are handled in initBT, no further need to complain
        public CommsBTConnect(){
            if(bluetoothServerSocket != null) return;
            try{
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("MusicPlayer", RRW_UUID);
            }catch(Exception e){
                CommsBTLog.addToLog("Connect failed: " + e.getMessage());
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnect Exception: " + e.getMessage());
            }
        }

        public void run(){
            try{
                bluetoothSocket = bluetoothServerSocket.accept();
                CommsBTConnected commsBTConnected = new CommsBTConnected();
                commsBTConnected.start();
            }catch(Exception e){
                CommsBTLog.addToLog("Connect to socket failed: " + e.getMessage());
            }
        }
    }

    private class CommsBTConnected extends Thread{
        private InputStream inputStream;
        private OutputStream outputStream;

        public CommsBTConnected(){
            try{
                CommsBTLog.addToLog("Connected, get input stream");
                inputStream = bluetoothSocket.getInputStream();
            }catch(Exception e){
                CommsBTLog.addToLog("Connected, get input stream failed: " + e.getMessage());
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected getInputStream Exception: " + e.getMessage());
            }
            try{
                CommsBTLog.addToLog("Connected, get output stream");
                outputStream = bluetoothSocket.getOutputStream();
            }catch(Exception e){
                CommsBTLog.addToLog("Connected, get output stream failed: " + e.getMessage());
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected getOutputStream Exception: " + e.getMessage());
            }
        }

        public void run(){
            Log.d(Main.RRW_LOG_TAG, "CommsBTConnected.run");
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
                CommsBT.this.start();
                return;
            }
            read();
            handler.postDelayed(this::process, 100);
        }

        private void close(){
            try{
                CommsBTLog.addToLog("Close");
                bluetoothSocket.close();
            }catch(Exception e){
                CommsBTLog.addToLog("Close failed: " + e.getMessage());
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected.close exception: " + e.getMessage());
            }
        }

        private boolean sendNextResponse(){
            try{
                outputStream.write("".getBytes());
                if(responseQueue.length() < 1) return true;
                JSONObject response = (JSONObject) responseQueue.get(0);
                responseQueue.remove(0);
                Log.d(Main.RRW_LOG_TAG, "CommsBTConnected.sendNextResponse: " + response.toString());
                CommsBTLog.addToLog("Send response " + response.getString("requestType"));
                outputStream.write(response.toString().getBytes());
            }catch(Exception e){
                CommsBTLog.addToLog("Send response failed: " + e.getMessage());
                if(e.getMessage() != null && e.getMessage().contains("Broken pipe")){
                    return false;
                }
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected.sendNextResponse Exception: " + e.getMessage());
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
                    CommsBTLog.addToLog("Read " + requestMessage.getString("requestType"));
                    gotRequest(requestMessage);
                }
            }catch(Exception e){
                CommsBTLog.addToLog("Read failed: " + e.getMessage());
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected.read: Input stream read exception: " + e.getMessage());
            }
        }

        private void gotRequest(final JSONObject requestMessage){
            Log.d(Main.RRW_LOG_TAG, "CommsBTConnected.gotRequest: " + requestMessage.toString());
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
                        Log.e(Main.RRW_LOG_TAG, "CommsBTConnected.gotRequest Unknown requestType: " + requestType);
                }

            }catch(Exception e){
                CommsBTLog.addToLog("gotRequest failed: " + e.getMessage());
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected.gotRequest Exception: " + e.getMessage());
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
            CommsBTLog.addToLog("onReceiveSync failed: " + e.getMessage());
            Log.e(Main.RRW_LOG_TAG, "CommsBT.onReceiveSync Exception: " + e.getMessage());
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
            CommsBTLog.addToLog("onReceivePrepare failed: " + e.getMessage());
            Log.e(Main.RRW_LOG_TAG, "CommsBT.onReceivePrepare Exception: " + e.getMessage());
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
            CommsBTLog.addToLog("sendResponse failed: " + e.getMessage());
            Log.e(Main.RRW_LOG_TAG, "CommsBT.sendResponse String Exception: " + e.getMessage());
        }
    }
    public void sendResponse(final String requestType, final JSONObject responseData){
        try{
            JSONObject response = new JSONObject();
            response.put("requestType", requestType);
            response.put("responseData", responseData);
            responseQueue.put(response);
        }catch(Exception e){
            CommsBTLog.addToLog("sendResponse failed: " + e.getMessage());
            Log.e(Main.RRW_LOG_TAG, "CommsBT.sendResponse JSONObject Exception: " + e.getMessage());
        }
    }
}