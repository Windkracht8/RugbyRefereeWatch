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

//Thread: All of FileStore runs on a background thread
public class CommsBT{
    private final UUID RRW_UUID = UUID.fromString("8b16601b-5c76-4151-a930-2752849f4552");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket bluetoothServerSocket;
    private BluetoothSocket bluetoothSocket;
    private CommsBTConnect commsBTConnect;
    private CommsBTConnected commsBTConnected;
    private final Main main;
    private final Handler handler;

    private boolean closeConnection = false;
    private final JSONArray responseQueue = new JSONArray();

    public CommsBT(Main main){
        this.main = main;
        handler = new Handler(Looper.getMainLooper());
        main.registerReceiver(btStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    private final BroadcastReceiver btStateReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
                int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if(btState == BluetoothAdapter.STATE_TURNING_OFF){
                    Log.d(Main.RRW_LOG_TAG, "btStateReceiver: stop");
                    stopComms();
                }else if(btState == BluetoothAdapter.STATE_ON &&
                        (Main.timer_status.equals("conf") || Main.timer_status.equals("finished"))
                ){
                    Log.d(Main.RRW_LOG_TAG, "btStateReceiver: start");
                    startComms();
                }
            }
        }
    };

    public void startComms(){
        closeConnection = false;
        BluetoothManager bluetoothManager = (BluetoothManager) main.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
            main.commsBTLog.addToLog(main.getString(R.string.bt_disabled));
            return;
        }
        main.commsBTLog.addToLog(main.getString(R.string.start_listening));
        commsBTConnect = new CommsBTConnect();
        commsBTConnect.start();
    }

    public void stopComms(){
        Log.d(Main.RRW_LOG_TAG, "CommsBT.stopComms");
        main.commsBTLog.addToLog(main.getString(R.string.stop_listening));
        closeConnection = true;
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
        commsBTConnect = null;
        commsBTConnected = null;
    }

    private class CommsBTConnect extends Thread{
        @SuppressLint("MissingPermission") //Permissions are handled in initBT
        public CommsBTConnect(){
            try{
                Log.d(Main.RRW_LOG_TAG, "CommsBTConnect");
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("RugbyRefereeWatch", RRW_UUID);
            }catch(Exception e){
                if(closeConnection) return;
                main.commsBTLog.addToLog(main.getString(R.string.listening_failed) + e.getMessage());
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnect Exception: " + e.getMessage());
            }
            Log.d(Main.RRW_LOG_TAG, "CommsBTConnect done");
        }

        public void run(){
            try{
                Log.d(Main.RRW_LOG_TAG, "CommsBTConnect.run");
                bluetoothSocket = bluetoothServerSocket.accept();
                if(closeConnection) return;
                Log.d(Main.RRW_LOG_TAG, "CommsBTConnect.run accepted");
                commsBTConnected = new CommsBTConnected();
                commsBTConnected.start();
            }catch(Exception e){
                Log.d(Main.RRW_LOG_TAG, "CommsBTConnect.run Exception closeConnection:" + closeConnection);
                if(closeConnection) return;
                main.commsBTLog.addToLog(main.getString(R.string.connect_failed) + e.getMessage());
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnect.run Exception: " + e.getMessage());
            }
            Log.d(Main.RRW_LOG_TAG, "CommsBTConnect.run done");
        }
    }

    private class CommsBTConnected extends Thread{
        private InputStream inputStream;
        private OutputStream outputStream;

        public CommsBTConnected(){
            try{
                Log.d(Main.RRW_LOG_TAG, "CommsBTConnected.getInputStream");
                inputStream = bluetoothSocket.getInputStream();
            }catch(Exception e){
                main.commsBTLog.addToLog(main.getString(R.string.input_stream_failed) + e.getMessage());
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected getInputStream Exception: " + e.getMessage());
            }
            try{
                Log.d(Main.RRW_LOG_TAG, "CommsBTConnected.getOutputStream");
                outputStream = bluetoothSocket.getOutputStream();
            }catch(Exception e){
                main.commsBTLog.addToLog(main.getString(R.string.output_stream_failed) + e.getMessage());
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected getOutputStream Exception: " + e.getMessage());
            }
        }

        public void run(){
            Log.d(Main.RRW_LOG_TAG, "CommsBTConnected.run");
            process();
        }

        private void process(){
            if(closeConnection){
                return;
            }
            try{
                outputStream.write("".getBytes());
            }catch(Exception e){
                main.commsBTLog.addToLog(main.getString(R.string.connection_closed));
                stopComms();
                startComms();
                return;
            }
            if(responseQueue.length() > 0 && !sendNextResponse()){
                stopComms();
                startComms();
                return;
            }
            read();
            handler.postDelayed(this::process, 100);
        }

        private boolean sendNextResponse(){
            try{
                JSONObject response = (JSONObject) responseQueue.get(0);
                responseQueue.remove(0);
                Log.d(Main.RRW_LOG_TAG, "CommsBTConnected.sendNextResponse: " + response.toString());
                main.commsBTLog.addToLog(main.getString(R.string.send_response) + response.getString("requestType"));
                outputStream.write(response.toString().getBytes());
            }catch(Exception e){
                if(e.getMessage() != null && e.getMessage().contains("Broken pipe")){
                    main.commsBTLog.addToLog(main.getString(R.string.connection_closed));
                    return false;
                }
                main.commsBTLog.addToLog(main.getString(R.string.sendNextResponse_failed) + e.getMessage());
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
                    main.commsBTLog.addToLog(main.getString(R.string.received_request) + requestMessage.getString("requestType"));
                    gotRequest(requestMessage);
                }
            }catch(Exception e){
                main.commsBTLog.addToLog(main.getString(R.string.read_failed) + e.getMessage());
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
                main.commsBTLog.addToLog(main.getString(R.string.gotRequest_failed) + e.getMessage());
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
            Conf.syncCustomMatchTypes(main, requestData);
        }catch(Exception e){
            main.commsBTLog.addToLog(main.getString(R.string.onReceiveSync_failed) + e.getMessage());
            Log.e(Main.RRW_LOG_TAG, "CommsBT.onReceiveSync Exception: " + e.getMessage());
            sendResponse("sync", main.getString(R.string.fail_unexpected));
        }
    }
    private void onReceivePrepare(String requestData){
        try{
            JSONObject requestData_json = new JSONObject(requestData);
            if(main.incomingSettings(requestData_json)){
                sendResponse("prepare", "okilly dokilly");
                FileStore.storeSettings(main);
            }else{
                sendResponse("prepare", "match ongoing");
            }
            main.runOnUiThread(main::updateAfterConfig);
        }catch(Exception e){
            main.commsBTLog.addToLog(main.getString(R.string.onReceivePrepare_failed) + e.getMessage());
            Log.e(Main.RRW_LOG_TAG, "CommsBT.onReceivePrepare Exception: " + e.getMessage());
            sendResponse("prepare", main.getString(R.string.fail_unexpected));
        }
    }
    public void sendResponse(final String requestType, final String responseData){
        try{
            JSONObject response = new JSONObject();
            response.put("requestType", requestType);
            response.put("responseData", responseData);
            responseQueue.put(response);
        }catch(Exception e){
            main.commsBTLog.addToLog(main.getString(R.string.sendResponse_failed) + e.getMessage());
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
            main.commsBTLog.addToLog(main.getString(R.string.sendResponse_failed) + e.getMessage());
            Log.e(Main.RRW_LOG_TAG, "CommsBT.sendResponse JSONObject Exception: " + e.getMessage());
        }
    }
}