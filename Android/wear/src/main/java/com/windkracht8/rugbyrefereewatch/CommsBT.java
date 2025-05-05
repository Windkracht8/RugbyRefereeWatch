/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//Thread: All of CommsBT runs on a background thread
class CommsBT{
    private final UUID RRW_UUID = UUID.fromString("8b16601b-5c76-4151-a930-2752849f4552");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket bluetoothServerSocket;
    private BluetoothSocket bluetoothSocket;
    private CommsBTConnect commsBTConnect;
    private CommsBTConnected commsBTConnected;
    private final Main main;

    private boolean disconnect = false;
    private final JSONArray responseQueue = new JSONArray();

    CommsBT(Main main){this.main = main;}

    private void gotRequest(String request){
        Log.d(Main.LOG_TAG, "CommsBTConnected.gotRequest: " + request);
        try{
            JSONObject requestMessage = new JSONObject(request);
            String requestType = requestMessage.getString("requestType");
            switch(requestType){
                case "sync":
                    onReceiveSync(requestMessage.getString("requestData"));
                    break;
                case "prepare":
                    onReceivePrepare(requestMessage.getString("requestData"));
                    break;
                default:
                    Log.e(Main.LOG_TAG, "CommsBTConnected.gotRequest Unknown requestType: " + requestType);
            }

        }catch(Exception e){
            Log.e(Main.LOG_TAG, "CommsBTConnected.gotRequest Exception: " + e.getMessage());
        }
    }
    private void onReceiveSync(String requestData){
        try{
            JSONObject settings = Main.getSettings();
            if(settings == null) return;
            JSONObject responseData = new JSONObject();
            responseData.put("matches", FileStore.deletedMatches(main, requestData));
            responseData.put("settings", settings);
            sendSyncResponse(responseData);
            FileStore.syncCustomMatchTypes(main, requestData);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "CommsBT.onReceiveSync Exception: " + e.getMessage());
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
            Log.e(Main.LOG_TAG, "CommsBT.onReceivePrepare Exception: " + e.getMessage());
            sendResponse("prepare", main.getString(R.string.fail_unexpected));
        }
    }
    private void sendResponse(String requestType, String responseData){
        try{
            JSONObject response = new JSONObject();
            response.put("requestType", requestType);
            response.put("responseData", responseData);
            responseQueue.put(response);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "CommsBT.sendResponse String Exception: " + e.getMessage());
        }
    }
    private void sendSyncResponse(JSONObject responseData){
        try{
            JSONObject response = new JSONObject();
            response.put("requestType", "sync");
            response.put("responseData", responseData);
            responseQueue.put(response);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "CommsBT.sendResponse JSONObject Exception: " + e.getMessage());
        }
    }

    private final BroadcastReceiver btStateReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
                int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if(btState == BluetoothAdapter.STATE_TURNING_OFF){
                    Log.d(Main.LOG_TAG, "CommsBT.btStateReceiver: stop");
                    stopBT();
                }else if(btState == BluetoothAdapter.STATE_ON &&
                        (Main.timer_status == Main.TimerStatus.CONF || Main.timer_status == Main.TimerStatus.FINISHED)
                ){
                    Log.d(Main.LOG_TAG, "CommsBT.btStateReceiver: start");
                    startBT();
                }
            }
        }
    };
    void startBT(){
        disconnect = false;
        if(bluetoothAdapter == null){
            BluetoothManager bluetoothManager = (BluetoothManager) main.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
            if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
                return;
            }
            main.registerReceiver(btStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        }
        if(commsBTConnect == null){
            commsBTConnect = new CommsBTConnect();
            commsBTConnect.start();
        }
    }
    void stopBT(){
        Log.d(Main.LOG_TAG, "CommsBT.stopBT");
        disconnect = true;
        try{
            main.unregisterReceiver(btStateReceiver);
        }catch(Exception e){
            Log.i(Main.LOG_TAG, "CommsBT.stopBT unregisterReceiver: " + e.getMessage());
        }
        try{
            if(bluetoothServerSocket != null) bluetoothServerSocket.close();
        }catch(Exception e){
            Log.i(Main.LOG_TAG, "CommsBT.stopBT bluetoothServerSocket: " + e.getMessage());
        }
        try{
            if(bluetoothSocket != null) bluetoothSocket.close();
        }catch(Exception e){
            Log.i(Main.LOG_TAG, "CommsBT.stopBT bluetoothSocket: " + e.getMessage());
        }
        bluetoothServerSocket = null;
        bluetoothSocket = null;
        bluetoothAdapter = null;
        commsBTConnect = null;
        commsBTConnected = null;
    }

    private class CommsBTConnect extends Thread{
        @SuppressLint("MissingPermission")//Permissions are handled in initBT
        private CommsBTConnect(){
            try{
                Log.d(Main.LOG_TAG, "CommsBTConnect");
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("RugbyRefereeWatch", RRW_UUID);
            }catch(Exception e){
                if(disconnect) return;
                Log.e(Main.LOG_TAG, "CommsBTConnect Exception: " + e.getMessage());
            }
        }
        public void run(){
            try{
                bluetoothSocket = bluetoothServerSocket.accept();
                if(disconnect) return;
                Log.d(Main.LOG_TAG, "CommsBTConnect.run accepted");
                commsBTConnected = new CommsBTConnected();
                commsBTConnected.start();
            }catch(Exception e){
                if(disconnect) return;
                Log.e(Main.LOG_TAG, "CommsBTConnect.run Exception: " + e.getMessage());
            }
        }
    }

    private class CommsBTConnected extends Thread{
        private InputStream inputStream;
        private OutputStream outputStream;

        CommsBTConnected(){
            try{
                Log.d(Main.LOG_TAG, "CommsBTConnected.getInputStream");
                inputStream = bluetoothSocket.getInputStream();
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnected getInputStream Exception: " + e.getMessage());
            }
            try{
                Log.d(Main.LOG_TAG, "CommsBTConnected.getOutputStream");
                outputStream = bluetoothSocket.getOutputStream();
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnected getOutputStream Exception: " + e.getMessage());
            }
        }
        public void run(){
            Log.d(Main.LOG_TAG, "CommsBTConnected.run");
            process(Executors.newSingleThreadScheduledExecutor());
        }
        private void process(ScheduledExecutorService executor){
            if(disconnect){
                return;
            }
            try{
                outputStream.write("".getBytes());
            }catch(Exception e){
                stopBT();
                startBT();
                return;
            }
            if(responseQueue.length() > 0 && !sendNextResponse()){
                stopBT();
                startBT();
                return;
            }
            read();
            executor.schedule(()->process(executor), 100, TimeUnit.MILLISECONDS);
        }
        private boolean sendNextResponse(){
            try{
                JSONObject response = (JSONObject) responseQueue.get(0);
                responseQueue.remove(0);
                Log.d(Main.LOG_TAG, "CommsBTConnected.sendNextResponse: " + response.toString());
                outputStream.write(response.toString().getBytes());
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnected.sendNextResponse Exception: " + e.getMessage());
                return false;
            }
            return true;
        }
        private void read(){
            try{
                if(inputStream.available() < 5) return;
                long last_read_time = System.currentTimeMillis();
                String request = "";
                while(System.currentTimeMillis() - last_read_time < 3000){
                    if(inputStream.available() == 0){
                        sleep100();
                        continue;
                    }
                    byte[] buffer = new byte[inputStream.available()];
                    int numBytes = inputStream.read(buffer);
                    if(numBytes < 0){
                        Log.e(Main.LOG_TAG, "CommsBTConnected.read read error, request: " + request);
                        return;
                    }
                    String temp = new String(buffer);
                    request += temp;
                    if(isValidJSON(request)){
                        gotRequest(request);
                        return;
                    }
                    last_read_time = System.currentTimeMillis();
                }
                Log.e(Main.LOG_TAG, "CommsBTConnected.read no valid message and no new data after 3 sec: " + request);
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnected.read: Input stream read exception: " + e.getMessage());
            }
        }
        private void sleep100(){
            try{
                Thread.sleep(100);
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnected.sleep100 exception: " + e.getMessage());
            }
        }
    }
    private static boolean isValidJSON(String json){
        try{
            new JSONObject(json);
        }catch(JSONException e){
            return false;
        }
        return true;
    }
}