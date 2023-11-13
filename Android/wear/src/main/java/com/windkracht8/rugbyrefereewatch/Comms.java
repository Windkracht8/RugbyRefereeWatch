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
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

@SuppressLint("MissingPermission") //Permissions are handled in connect, no further need to complain
public class Comms{
    final String RRW_UUID = "8b16601b-5c76-4151-a930-2752849f4552";
    final BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bluetoothSocket;
    final JSONArray responseQueue;
    final Handler handler;
    final Context context;
    final Handler handler_message;

    boolean connect = false;
    public final ArrayList<String> connect_failed_addresses = new ArrayList<>();
    public final ArrayList<String> queried_addresses = new ArrayList<>();

    public Comms(Context context, Handler handler_message){
        this.context = context;
        this.handler_message = handler_message;
        responseQueue = new JSONArray();
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        handler = new Handler(Looper.getMainLooper());
        context.registerReceiver(btStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
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
                    connect();
                }
            }
        }
    };

    public void connect(){
        connect = true;
        search();
    }

    void search(){
        if(!connect) return;
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        for(BluetoothDevice bondedDevice : bondedDevices){
            if(connect_failed_addresses.contains(bondedDevice.getAddress())) continue;
            for(ParcelUuid uuid : bondedDevice.getUuids()){
                if(uuid.toString().equals(RRW_UUID)){
                    CommsConnect commsConnect = new CommsConnect(bondedDevice);
                    commsConnect.start();
                    return;
                }
            }
        }

        for(BluetoothDevice bondedDevice : bondedDevices){
            if(queried_addresses.contains(bondedDevice.getAddress())) continue;
            bondedDevice.fetchUuidsWithSdp();
            queried_addresses.add(bondedDevice.getAddress());
        }

        connect_failed_addresses.clear();
        handler.postDelayed(this::search, 3000);
    }

    void stop(){
        connect = false;
        handler.removeCallbacksAndMessages(null);
        try{
            context.unregisterReceiver(btStateReceiver);
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "Comms.stop unregisterReceiver: " + e.getMessage());
        }
    }

    private class CommsConnect extends Thread{
        private final BluetoothDevice device;

        public CommsConnect(BluetoothDevice device){
            this.device = device;
            try{
                bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(RRW_UUID));
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsConnect Exception: " + e.getMessage());
            }
        }

        public void run(){
            bluetoothAdapter.cancelDiscovery();
            try{
                bluetoothSocket.connect();
            }catch(IOException e){
                connect_failed_addresses.add(device.getAddress());
                search();
                return;
            }
            CommsConnected commsConnected = new CommsConnected();
            commsConnected.start();
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
            process();
        }

        private void process(){
            if(!connect){
                close();
                return;
            }
            if(!sendNextResponse()){
                close();
                search();
                return;
            }
            read();
            sleep100();
            process();
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

        private boolean sendNextResponse(){
            try{
                outputStream.write("".getBytes());
                if(responseQueue.length() < 1) return true;
                JSONObject response = (JSONObject) responseQueue.get(0);
                responseQueue.remove(0);
                Log.d(Main.RRW_LOG_TAG, "CommsConnected.sendNextResponse: " + response.toString());
                outputStream.write(response.toString().getBytes());
            }catch(Exception e){
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
                    gotRequest(requestMessage);
                }
            }catch(Exception e){
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
                Log.e(Main.RRW_LOG_TAG, "CommsConnected.gotRequest Exception: " + e);
                Log.e(Main.RRW_LOG_TAG, "CommsConnected.gotRequest Exception: " + e.getMessage());
            }
        }
    }

    private void onReceiveSync(String requestData){
        try{
            JSONObject responseData = new JSONObject();
            responseData.put("matches", FileStore.deletedMatches(context, handler_message, requestData));
            responseData.put("settings", Main.getSettings(handler_message));
            sendResponse("sync", responseData);
            Conf.syncCustomMatchTypes(handler_message, requestData);
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "Comms.onReceiveSync Exception: " + e.getMessage());
            sendResponse("sync", "unexpected error");
        }
    }
    private void onReceivePrepare(String requestData){
        try{
            JSONObject requestData_json = new JSONObject(requestData);
            if(Main.incomingSettings(handler_message, requestData_json)){
                sendResponse("prepare", "okilly dokilly");
                FileStore.storeSettings(context, handler_message);
            }else{
                sendResponse("prepare", "match ongoing");
            }
            handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_PREPARE_RECEIVED));
        }catch(Exception e){
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
            Log.e(Main.RRW_LOG_TAG, "Comms.sendResponse String Exception: " + e);
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
            Log.e(Main.RRW_LOG_TAG, "Comms.sendResponse JSONObject Exception: " + e);
            Log.e(Main.RRW_LOG_TAG, "Comms.sendResponse JSONObject Exception: " + e.getMessage());
        }
    }
}