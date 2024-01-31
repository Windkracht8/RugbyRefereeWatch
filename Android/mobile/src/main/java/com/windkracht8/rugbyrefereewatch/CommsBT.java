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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@SuppressLint("MissingPermission") //Permissions are handled in initBT
public class CommsBT{
    final String RRW_UUID = "8b16601b-5c76-4151-a930-2752849f4552";
    final BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bluetoothSocket;
    final Main main;
    final Handler handler;

    enum Status{INIT, SEARCHING, SEARCH_TIMEOUT, CONNECTED, FATAL}
    public Status status = Status.INIT;
    private boolean closeConnection = false;
    private int searchCount = 0;
    private int failedConnectCount = 0;
    private final JSONArray requestQueue = new JSONArray();
    public Set<String> rrw_device_addresses = new HashSet<>();
    public final ArrayList<String> devices_fetch_pending = new ArrayList<>();

    public CommsBT(Main main){
        this.main = main;
        handler = new Handler(Looper.getMainLooper());
        BluetoothManager bluetoothManager = (BluetoothManager) main.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null){
            gotError(main.getString(R.string.fail_BT_off));
            return;
        }
        IntentFilter btIntentFilter = new IntentFilter();
        btIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        btIntentFilter.addAction(BluetoothDevice.ACTION_UUID);
        main.registerReceiver(btStateReceiver, btIntentFilter);
    }

    private final BroadcastReceiver btStateReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            Log.d(Main.RRW_LOG_TAG, "CommsBT.btStateReceiver: " + intent);
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
                int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if(btState == BluetoothAdapter.STATE_TURNING_OFF){
                    gotError(main.getString(R.string.fail_BT_off));
                    stopComms();
                }else if(btState == BluetoothAdapter.STATE_ON){
                    startComms();
                }
            }else if(BluetoothDevice.ACTION_UUID.equals(intent.getAction())){
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(bluetoothDevice == null || status != Status.SEARCHING) return;
                Log.d(Main.RRW_LOG_TAG, "CommsBT.btStateReceiver ACTION_UUID: " + bluetoothDevice.getName());
                searchCount--;
                isDeviceRRW(bluetoothDevice);
                devices_fetch_pending.remove(bluetoothDevice.getAddress());
            }
        }
    };

    public void startComms(){
        Log.d(Main.RRW_LOG_TAG, "CommsBT.startComms");
        closeConnection = false;
        if(bluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_OFF || bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF){
            gotError(main.getString(R.string.fail_BT_off));
            return;
        }
        rrw_device_addresses = Main.sharedPreferences.getStringSet("rrw_device_addresses", rrw_device_addresses);
        search();
    }
    public void stopComms(){
        Log.d(Main.RRW_LOG_TAG, "CommsBT.stopComms");
        closeConnection = true;
        handler.removeCallbacksAndMessages(null);
        devices_fetch_pending.clear();
        try{
            main.unregisterReceiver(btStateReceiver);
        }catch(Exception e){
            Log.d(Main.RRW_LOG_TAG, "CommsBT.stopComms unregisterReceiver: " + e.getMessage());
        }
    }
    private void search(){
        Log.d(Main.RRW_LOG_TAG, "CommsBT.search closeConnection: " + closeConnection);
        if(closeConnection) return;
        updateStatus(Status.SEARCHING);
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if(bondedDevices == null){
            main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_BT));
            gotError(main.getString(R.string.no_devices));
            return;
        }
        for(String rrw_device_address : rrw_device_addresses){
            boolean stillBonded = false;
            for(BluetoothDevice bondedDevice : bondedDevices){
                if(bondedDevice.getAddress().equals(rrw_device_address)){
                    stillBonded = true;
                    break;
                }
            }
            if(!stillBonded){
                rrw_device_addresses.remove(rrw_device_address);
                Main.sharedPreferences_editor.putStringSet("rrw_device_addresses", rrw_device_addresses);
                Main.sharedPreferences_editor.apply();
            }
        }
        for(BluetoothDevice bondedDevice : bondedDevices){
            isDeviceRRW(bondedDevice);
        }
        failedConnectCount += rrw_device_addresses.size();

        searchCount = 4 * (bondedDevices.size() - rrw_device_addresses.size());
        search_allDevices(bondedDevices);
    }
    private void isDeviceRRW(BluetoothDevice bluetoothDevice){
        if(rrw_device_addresses.contains(bluetoothDevice.getAddress())){
            Log.d(Main.RRW_LOG_TAG, "CommsBT.isDeviceRRW device is in rrw_device_addresses: " + bluetoothDevice.getName());
            try_rrwDevice(bluetoothDevice);
            return;
        }
        ParcelUuid[] uuids = bluetoothDevice.getUuids();
        if(uuids == null) return;
        for(ParcelUuid uuid : uuids){
            if(uuid.toString().equals(RRW_UUID)){
                Log.d(Main.RRW_LOG_TAG, "CommsBT.isDeviceRRW device has RRW_UUID: " + bluetoothDevice.getName());
                rrw_device_addresses.add(bluetoothDevice.getAddress());
                Main.sharedPreferences_editor.putStringSet("rrw_device_addresses", rrw_device_addresses);
                Main.sharedPreferences_editor.apply();
                try_rrwDevice(bluetoothDevice);
                return;
            }
        }
    }
    private void try_rrwDevice(BluetoothDevice rrw_device){
        if(status != Status.SEARCHING) return;
        main.gotStatus(String.format("%s %s", main.getString(R.string.try_connect_to), rrw_device.getName()));
        CommsBTConnect commsBTConnect = new CommsBT.CommsBTConnect(rrw_device);
        commsBTConnect.start();
    }
    private void search_allDevices(Set<BluetoothDevice> bondedDevices){
        Log.d(Main.RRW_LOG_TAG, String.format("CommsBT.search_allDevices status: %s searchCount: %s connectCount: %s", status, searchCount, failedConnectCount));
        if(status != Status.SEARCHING) return;
        if(searchCount <= 0){
            updateStatus(Status.SEARCH_TIMEOUT);
            stopComms();
            return;
        }
        if(failedConnectCount > 0){
            handler.postDelayed(() -> search_allDevices(bondedDevices), 1000);
            return;
        }
        main.gotStatus(main.getString(R.string.look_new_device));
        for(BluetoothDevice bondedDevice : bondedDevices){
            if(devices_fetch_pending.contains(bondedDevice.getAddress()) ||
                rrw_device_addresses.contains(bondedDevice.getAddress())
            ){
                continue;
            }
            Log.d(Main.RRW_LOG_TAG, "CommsBT.search_allDevices fetchUuidsWithSdp for: " + bondedDevice.getName());
            devices_fetch_pending.add(bondedDevice.getAddress());
            bondedDevice.fetchUuidsWithSdp();
        }
        handler.postDelayed(() -> search_allDevices(bondedDevices), 1000);
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
    private void gotError(String message){
        updateStatus(Status.FATAL);
        main.gotError(message);
    }
    private void updateStatus(Status status){
        Log.d(Main.RRW_LOG_TAG, "CommsBT.updateStatus " + this.status + " > " + status);
        if(this.status != status) main.updateStatus(status);
        this.status = status;
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
            }catch(Exception e){
                Log.d(Main.RRW_LOG_TAG, "CommsBTConnect.run failed: " + e.getMessage());
                try{
                    bluetoothSocket.close();
                }catch(Exception e2){
                    Log.d(Main.RRW_LOG_TAG, "CommsBTConnect.run close failed: " + e2.getMessage());
                }
                failedConnectCount--;
                handler.postDelayed(() -> try_rrwDevice(device), 500);
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
            updateStatus(Status.CONNECTED);
        }

        public void run(){
            Log.d(Main.RRW_LOG_TAG, "CommsBTConnected.run");
            process();
        }

        private void process(){
            if(closeConnection){
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
            Log.d(Main.RRW_LOG_TAG, "CommsBTConnected.close");
            main.gotStatus(main.getString(R.string.closing_connection));
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
                main.gotStatus(String.format("%s %s", main.getString(R.string.send_request), request.getString("requestType")));
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
                main.gotResponse(responseMessage);
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