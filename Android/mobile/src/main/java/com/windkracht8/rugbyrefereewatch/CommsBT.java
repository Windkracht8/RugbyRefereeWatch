package com.windkracht8.rugbyrefereewatch;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressLint("MissingPermission")//Permissions are handled in Main
class CommsBT{
    private static final String RRW_UUID = "8b16601b-5c76-4151-a930-2752849f4552";
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private CommsBTConnect commsBTConnect;
    private CommsBTConnected commsBTConnected;

    enum Status{DISCONNECTED, CONNECTING, CONNECTED}
    Status status = Status.DISCONNECTED;
    private boolean disconnect = false;
    private boolean startDone = false;
    private Set<String> known_device_addresses = new HashSet<>();
    private final JSONArray requestQueue = new JSONArray();
    private final List<BTInterface> listeners = new ArrayList<>();

    CommsBT(Context context){
        BluetoothManager bm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bm.getAdapter();
        if(bluetoothAdapter == null){
            onBTError(R.string.fail_BT_denied);
            return;
        }

        IntentFilter btIntentFilter = new IntentFilter();
        btIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        BroadcastReceiver btBroadcastReceiver = new BroadcastReceiver(){
            public void onReceive(Context context, Intent intent){
                if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
                    int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    if(btState == BluetoothAdapter.STATE_TURNING_OFF){
                        onBTError(R.string.fail_BT_off);
                        stopBT();
                    }else if(btState == BluetoothAdapter.STATE_ON){
                        startBT();
                    }
                }
            }
        };
        context.registerReceiver(btBroadcastReceiver, btIntentFilter);
    }
    void startBT(){
        Log.d(Main.LOG_TAG, "CommsBT.startBT");
        if(bluetoothAdapter.getState() != BluetoothAdapter.STATE_ON){
            onBTError(R.string.fail_BT_off);
            return;
        }
        known_device_addresses = Main.sharedPreferences.getStringSet("known_device_addresses", known_device_addresses);
        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        Log.d(Main.LOG_TAG, "CommsBT.startBT " + known_device_addresses.size() + " known of " + devices.size() + " total devices");

        if(devices.isEmpty()){
            onBTError(R.string.fail_BT_no_devices);
            return;
        }
        if(known_device_addresses.isEmpty()){
            onBTStartDone();
            return;
        }

        //Clean known devices
        for(String known_device_address : known_device_addresses){
            boolean stillBonded = false;
            for(BluetoothDevice device : devices){
                if(device.getAddress().equals(known_device_address)){
                    stillBonded = true;
                    break;
                }
            }
            if(!stillBonded){
                known_device_addresses.remove(known_device_address);
                Main.sharedPreferences_editor.putStringSet("known_device_addresses", known_device_addresses);
                Main.sharedPreferences_editor.apply();
            }
        }

        //Try to connect to known device
        for(BluetoothDevice device : devices){
            if(known_device_addresses.contains(device.getAddress())){
                connectDevice(device);
                return;
            }
        }
        onBTStartDone();
    }
    void restartBT(){
        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        for(BluetoothDevice device : devices){
            if(known_device_addresses.contains(device.getAddress())){
                connectDevice(device);
                return;
            }
        }
    }
    void stopBT(){
        disconnect = true;
        try{
            if(bluetoothSocket != null) bluetoothSocket.close();
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "CommsBT.stopBT bluetoothSocket: " + e.getMessage());
        }
        bluetoothSocket = null;
        commsBTConnect = null;
        commsBTConnected = null;
    }

    Set<BluetoothDevice> getDevices(){
        if(bluetoothAdapter == null || bluetoothAdapter.getState() != BluetoothAdapter.STATE_ON){
            onBTError(R.string.fail_BT_denied);
            return null;
        }
        return bluetoothAdapter.getBondedDevices();
    }

    void connectDevice(BluetoothDevice device){
        Log.d(Main.LOG_TAG, "CommsBT.connectDevice: " + device.getName());
        if(bluetoothAdapter == null ||
                status != Status.DISCONNECTED ||
                bluetoothAdapter.getState() != BluetoothAdapter.STATE_ON ||
                commsBTConnect != null
        ) return;
        disconnect = false;
        onBTConnecting(device.getName());
        commsBTConnect = new CommsBT.CommsBTConnect(device);
        commsBTConnect.start();
    }

    void sendRequest(String requestType, JSONObject requestData){
        Log.d(Main.LOG_TAG, "CommsBT.sendRequest: " + requestType);
        try{
            JSONObject request = new JSONObject();
            request.put("requestType", requestType);
            request.put("requestData", requestData);
            requestQueue.put(request);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "CommsBT.sendRequest Exception: " + e.getMessage());
        }
    }

    private class CommsBTConnect extends Thread{
        private CommsBTConnect(BluetoothDevice device){
            Log.d(Main.LOG_TAG, "CommsBTConnect " + device.getName());
            try{
                bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(RRW_UUID));
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnect Exception: " + e.getMessage());
                onBTConnectFailed();
            }
        }
        public void run(){
            bluetoothAdapter.cancelDiscovery();
            try{
                bluetoothSocket.connect();
                commsBTConnected = new CommsBTConnected();
                commsBTConnected.start();
            }catch(Exception e){
                Log.d(Main.LOG_TAG, "CommsBTConnect.run failed: " + e.getMessage());
                try{
                    bluetoothSocket.close();
                }catch(Exception e2){
                    Log.d(Main.LOG_TAG, "CommsBTConnect.run close failed: " + e2.getMessage());
                }
                onBTConnectFailed();
            }
        }
    }
    private class CommsBTConnected extends Thread{
        private InputStream inputStream;
        private OutputStream outputStream;

        CommsBTConnected(){
            Log.d(Main.LOG_TAG, "CommsBTConnected");
            try{
                inputStream = bluetoothSocket.getInputStream();
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnected getInputStream Exception: " + e.getMessage());
                onBTDisconnected();
                return;
            }
            try{
                outputStream = bluetoothSocket.getOutputStream();
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnected getOutputStream Exception: " + e.getMessage());
                onBTDisconnected();
                return;
            }
            onBTConnected(bluetoothSocket.getRemoteDevice());
        }
        public void run(){process(Executors.newSingleThreadScheduledExecutor());}
        private void close(){
            Log.d(Main.LOG_TAG, "CommsBTConnected.close");
            onBTDisconnected();
            try{
                for(int i=requestQueue.length(); i>0; i--){
                    requestQueue.remove(i-1);
                }
                if(bluetoothSocket != null) bluetoothSocket.close();
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnected.close exception: " + e.getMessage());
            }
            commsBTConnected = null;
            commsBTConnect = null;
        }
        private void process(ScheduledExecutorService executor){
            if(disconnect){
                close();
                return;
            }
            try{
                outputStream.write("".getBytes());
            }catch(Exception e){
                Log.d(Main.LOG_TAG, "Connection closed");
                close();
                return;
            }
            if(!sendNextRequest()){
                close();
                return;
            }
            read();
            executor.schedule(()->process(executor), 100, TimeUnit.MILLISECONDS);
        }
        private boolean sendNextRequest(){
            try{
                outputStream.write("".getBytes());
                if(requestQueue.length() < 1) return true;
                JSONObject request = (JSONObject) requestQueue.get(0);
                requestQueue.remove(0);
                Log.d(Main.LOG_TAG, "CommsBTConnected.sendNextRequest: " + request.toString());
                outputStream.write(request.toString().getBytes());
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnected.sendNextRequest Exception: " + e.getMessage());
                onBTError(R.string.fail_send_message);
                return false;
            }
            return true;
        }
        private void read(){
            try{
                if(inputStream.available() < 5) return;
                long read_start = (new Date()).getTime();
                String response = "";

                while(inputStream.available() > 0){
                    byte[] buffer = new byte[inputStream.available()];
                    int numBytes = inputStream.read(buffer);
                    if(numBytes < 0){
                        Log.e(Main.LOG_TAG, "CommsBTConnected.read read error: " + response);
                        onBTError(R.string.fail_response);
                        return;
                    }
                    String temp = new String(buffer);
                    response += temp;
                    if(isValidJSON(response)){
                        Log.d(Main.LOG_TAG, "CommsBTConnected.read got message: " + response);
                        onBTResponse(new JSONObject(response));
                        return;
                    }
                    if((new Date()).getTime() - read_start > 3000){
                        Log.e(Main.LOG_TAG, "CommsBTConnected.read no valid message after 3 seconds: " + response);
                        onBTError(R.string.fail_response);
                        return;
                    }
                    sleep100();
                }
                Log.e(Main.LOG_TAG, "CommsBTConnected.read nothing left to read, but no valid message: " + response);
                onBTError(R.string.fail_response);
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnected.read: " + e.getMessage());
                onBTError(R.string.fail_response);
            }
        }
        private void sleep100(){
            try{
                Thread.sleep(100);
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnected.sleep100 exception: " + e.getMessage());
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

    void addListener(BTInterface listener){listeners.add(listener);}
    private void onBTStartDone(){
        Log.d(Main.LOG_TAG, "CommsBT.onBTStartDone");
        startDone = true;
        listeners.remove(null);
        listeners.forEach(BTInterface::onBTStartDone);
    }
    private void onBTConnecting(String deviceName){
        Log.d(Main.LOG_TAG, "CommsBT.onBTConnecting");
        status = Status.CONNECTING;
        listeners.remove(null);
        listeners.forEach((l)->l.onBTConnecting(deviceName));
    }
    private void onBTConnectFailed(){
        Log.d(Main.LOG_TAG, "CommsBT.onBTConnectFailed");
        commsBTConnect = null;
        status = Status.DISCONNECTED;
        if(startDone){
            listeners.remove(null);
            listeners.forEach(BTInterface::onBTConnectFailed);
        }else{
            onBTStartDone();
        }
    }
    private void onBTConnected(BluetoothDevice device){
        Log.d(Main.LOG_TAG, "CommsBT.onBTConnected");
        status = Status.CONNECTED;
        listeners.remove(null);
        listeners.forEach((l)->l.onBTConnected(device.getName()));
        if(!startDone) onBTStartDone();
        if(!known_device_addresses.contains(device.getAddress())){
            known_device_addresses.add(device.getAddress());
            Main.sharedPreferences_editor.putStringSet("known_device_addresses", known_device_addresses);
            Main.sharedPreferences_editor.apply();
        }
    }
    private void onBTDisconnected(){
        Log.d(Main.LOG_TAG, "CommsBT.onBTDisconnected");
        status = Status.DISCONNECTED;
        listeners.remove(null);
        listeners.forEach(BTInterface::onBTDisconnected);
    }
    private void onBTResponse(JSONObject response){
        Log.d(Main.LOG_TAG, "CommsBT.onBTResponse " + response);
        listeners.remove(null);
        listeners.forEach((l)->l.onBTResponse(response));
    }
    private void onBTError(int message){
        Log.d(Main.LOG_TAG, "CommsBT.onBTError");
        listeners.remove(null);
        listeners.forEach((l)->l.onBTError(message));
    }
    public interface BTInterface{
        void onBTStartDone();
        void onBTConnecting(String deviceName);
        void onBTConnectFailed();
        void onBTConnected(String deviceName);
        void onBTDisconnected();
        void onBTResponse(JSONObject response);
        void onBTError(int message);
    }
}