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
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@SuppressLint("MissingPermission") //Permissions are handled in initBT
public class CommsBT{
    final String RRW_UUID = "8b16601b-5c76-4151-a930-2752849f4552";
    final BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bluetoothSocket;
    final Main main;
    final Handler handler;

    public String status = "INIT";
    private boolean connect = false;
    private int searchCount = 0;
    private final JSONArray requestQueue = new JSONArray();
    public final ArrayList<BluetoothDevice> rrw_devices = new ArrayList<>();
    public final ArrayList<String> connect_failed_addresses = new ArrayList<>();
    public final ArrayList<String> queried_addresses = new ArrayList<>();

    public CommsBT(Main main){
        this.main = main;
        handler = new Handler(Looper.getMainLooper());
        BluetoothManager bluetoothManager = (BluetoothManager) main.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null) bt_off();
    }

    private final BroadcastReceiver btStateReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
                int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if(btState == BluetoothAdapter.STATE_TURNING_OFF){
                    bt_off();
                    stopComms();
                }else if(btState == BluetoothAdapter.STATE_ON){
                    startComms();
                }
            }else if(BluetoothDevice.ACTION_UUID.equals(intent.getAction())){
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(bluetoothDevice == null) return;
                Log.d(Main.RRW_LOG_TAG, "ACTION_UUID: " + bluetoothDevice.getName());
                checkDeviceUuids(bluetoothDevice);
                queried_addresses.remove(bluetoothDevice.getAddress());
            }
        }
    };

    public void startComms(){
        Log.d(Main.RRW_LOG_TAG, "CommsBT.startComms");
        connect = true;
        searchCount = 0;
        if(bluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_OFF || bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF){
            gotError(main.getString(R.string.fail_BT_off));
            return;
        }
        IntentFilter btIntentFilter = new IntentFilter();
        btIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        btIntentFilter.addAction(BluetoothDevice.ACTION_UUID);
        main.registerReceiver(btStateReceiver, btIntentFilter);
        search();
    }

    void stopComms(){
        Log.d(Main.RRW_LOG_TAG, "CommsBT.stopComms");
        connect = false;
        handler.removeCallbacksAndMessages(null);
        try{
            main.unregisterReceiver(btStateReceiver);
        }catch(Exception e){
            Log.d(Main.RRW_LOG_TAG, "CommsBT.stopComms unregisterReceiver: " + e.getMessage());
        }
    }

    void search(){
        if(!connect) return;
        searchCount++;
        Log.d(Main.RRW_LOG_TAG, "CommsBT.search " + searchCount);
        if(!status.equals("SEARCHING")) updateStatus("SEARCHING");
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        if(bondedDevices == null){
            main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_BT));
            gotError("No devices");
            return;
        }

        for(BluetoothDevice bondedDevice : bondedDevices){
            checkDeviceUuids(bondedDevice);
        }
        if(connectToRRWDevice()) return;

        main.gotStatus("Query all known devices");
        for(BluetoothDevice bondedDevice : bondedDevices){
            if(queried_addresses.contains(bondedDevice.getAddress())) continue;
            Log.d(Main.RRW_LOG_TAG, "CommsBT.search Query " + bondedDevice.getName());
            bondedDevice.fetchUuidsWithSdp();
            queried_addresses.add(bondedDevice.getAddress());
        }

        if(searchCount > 5){
            stopComms();
            updateStatus("SEARCH_TIMEOUT");
            return;
        }
        connect_failed_addresses.clear();
        handler.postDelayed(this::search, 1000);
    }
    private boolean connectToRRWDevice(){
        for(BluetoothDevice rrw_device : rrw_devices){
            if(connect_failed_addresses.contains(rrw_device.getAddress())) continue;
            main.gotStatus("Trying to connect to " + rrw_device.getName());
            CommsBT.CommsBTConnect commsBTConnect = new CommsBT.CommsBTConnect(rrw_device);
            commsBTConnect.start();
            return true;
        }
        return false;
    }
    private void checkDeviceUuids(BluetoothDevice bluetoothDevice){
        ParcelUuid[] uuids = bluetoothDevice.getUuids();
        if(uuids == null) return;
        for(ParcelUuid uuid : uuids){
            if(uuid.toString().equals(RRW_UUID)){
                rrw_devices.add(bluetoothDevice);
                return;
            }
        }
        rrw_devices.remove(bluetoothDevice);
    }

    public void sendRequest(String requestType, JSONObject requestData){
        Log.d(Main.RRW_LOG_TAG, "CommsBT.sendRequest: " + requestType);
        try{
            JSONObject request = new JSONObject();
            request.put("requestType", requestType);
            request.put("requestData", requestData);
            requestQueue.put(request);
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "CommsBT.sendRequest Exception: " + e.getMessage());
        }
    }

    private void gotError(String fatal_string){
        updateStatus("FATAL");
        main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_GOT_ERROR, fatal_string));
    }
    private void bt_off(){
        updateStatus("FATAL");
        main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_BT_OFF));
    }
    private void updateStatus(String status){
        this.status = status;
        main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_UPDATE_STATUS, status));
    }

    private class CommsBTConnect extends Thread{
        private final BluetoothDevice device;
        public CommsBTConnect(BluetoothDevice device){
            Log.d(Main.RRW_LOG_TAG, "CommsBTConnect " + device.getName());
            this.device = device;
            try{
                bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(RRW_UUID));
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnect Exception: " + e.getMessage());
                main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_BT));
            }
        }

        public void run(){
            Log.d(Main.RRW_LOG_TAG, "CommsBTConnect.run");
            bluetoothAdapter.cancelDiscovery();
            try{
                bluetoothSocket.connect();
            }catch(IOException e){
                Log.d(Main.RRW_LOG_TAG, "CommsBTConnect.run failed");
                connect_failed_addresses.add(device.getAddress());
                search();
                return;
            }
            CommsBTConnected commsBTConnected = new CommsBTConnected();
            commsBTConnected.start();
        }
    }

    private class CommsBTConnected extends Thread{
        private InputStream inputStream;
        private OutputStream outputStream;

        public CommsBTConnected(){
            Log.d(Main.RRW_LOG_TAG, "CommsBTConnected");
            try{
                inputStream = bluetoothSocket.getInputStream();
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected getInputStream Exception: " + e.getMessage());
            }
            try{
                outputStream = bluetoothSocket.getOutputStream();
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected getOutputStream Exception: " + e.getMessage());
            }
            updateStatus("CONNECTED");
        }

        public void run(){
            Log.d(Main.RRW_LOG_TAG, "CommsBTConnected.run");
            process();
        }

        private void process(){
            if(!connect){
                close();
                return;
            }
            if(!sendNextRequest()){
                close();
                search();
                return;
            }
            read();
            handler.postDelayed(this::process, 100);
        }

        private void close(){
            try{
                bluetoothSocket.close();
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected.close exception: " + e.getMessage());
            }
        }

        private void sleep100(){
            try{
                Thread.sleep(100);
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected.sleep100 exception: " + e.getMessage());
            }
        }

        private boolean sendNextRequest(){
            try{
                outputStream.write("".getBytes());
                if(requestQueue.length() < 1) return true;
                JSONObject request = (JSONObject) requestQueue.get(0);
                requestQueue.remove(0);
                Log.d(Main.RRW_LOG_TAG, "CommsBTConnected.sendNextRequest: " + request.toString());
                main.gotStatus("Send request: " + request.getString("requestType"));
                outputStream.write(request.toString().getBytes());
            }catch(Exception e){
                if(e.getMessage() != null && e.getMessage().contains("Broken pipe")){
                    return false;
                }
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected.sendNextRequest Exception: " + e.getMessage());
                main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_send_message));
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
                        Log.e(Main.RRW_LOG_TAG, "CommsBTConnected.read read error: " + response);
                        main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_response));
                        main.gotError("Read error");
                        return;
                    }
                    String temp = new String(buffer);
                    response += temp;
                    if(isValidJSON(response)){
                        gotResponse(response);
                        return;
                    }
                    if((new Date()).getTime() - read_start > 3000){
                        Log.e(Main.RRW_LOG_TAG, "CommsBTConnected.read started to read, no complete message after 3 seconds: " + response);
                        main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_response));
                        main.gotError("Started to read, did not complete");
                        return;
                    }
                    sleep100();
                }
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected.read: " + e.getMessage());
                main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_response));
                main.gotError("Read error: " + e.getMessage());
            }
        }

        private void gotResponse(String response){
            try{
                JSONObject responseMessage = new JSONObject(response);
                Log.d(Main.RRW_LOG_TAG, "CommsBTConnected.gotResponse: " + responseMessage);
                main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_GOT_RESPONSE, responseMessage));
                main.gotStatus("Received response: " + responseMessage.getString("requestType"));
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected.gotResponse: " + e.getMessage());
                main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_response));
                main.gotError("Response error: " + e.getMessage());
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