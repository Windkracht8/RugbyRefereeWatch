package com.windkracht8.rugbyrefereewatch;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Comms{
    final UUID RRW_UUID = UUID.fromString("8b16601b-5c76-4151-a930-2752849f4552");
    String status = "DISCONNECTED";
    boolean listen = false;
    final JSONArray requestQueue;
    final BluetoothAdapter bluetoothAdapter;
    BluetoothServerSocket bluetoothServerSocket;
    BluetoothSocket bluetoothSocket;
    final Context context;
    final Handler handler_message;

    public Comms(Context context, Handler handler_message){
        this.context = context;
        this.handler_message = handler_message;
        requestQueue = new JSONArray();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        context.registerReceiver(btStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    private final BroadcastReceiver btStateReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
                int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if(btState == BluetoothAdapter.STATE_TURNING_OFF){
                    stopListening();
                    gotError(context.getString(R.string.fail_BT_disabled));
                }else if(btState == BluetoothAdapter.STATE_ON){
                    startListening();
                }
            }
        }
    };

    public void startListening(){
        if(!bluetoothAdapter.isEnabled()){
            gotError(context.getString(R.string.fail_BT_disabled));
            return;
        }
        CommsBTConnect commsBTConnect = new CommsBTConnect();
        commsBTConnect.start();
        updateStatus("LISTENING");
    }

    public void stopListening(){
        listen = false;
        try{
            context.unregisterReceiver(btStateReceiver);
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

    public void sendRequest(String requestType, JSONObject requestData){
        Log.d(Main.RRW_LOG_TAG, "CommsBT.sendRequest: " + requestType);
        try{
            JSONObject request = new JSONObject();
            request.put("requestType", requestType);
            request.put("requestData", requestData);
            requestQueue.put(request);
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "CommsBT.sendRequest Exception: " + e);
            Log.e(Main.RRW_LOG_TAG, "CommsBT.sendRequest Exception: " + e.getMessage());
        }
    }

    private void gotError(final String error){
        handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_GOT_ERROR, error));
    }

    private void updateStatus(final String status_new){
        Log.i(Main.RRW_LOG_TAG, "CommsBT.updateStatus: " + status_new);
        this.status = status_new;
        handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_UPDATE_STATUS, status_new));
    }

    private class CommsBTConnect extends Thread{
        @SuppressLint("MissingPermission")//already checked in startListening
        public CommsBTConnect(){
            try{
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("RugbyRefereeWatch", RRW_UUID);
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnect Exception: " + e.getMessage());
                gotError(e.getMessage());
            }
        }

        public void run(){
            try{
                bluetoothSocket = bluetoothServerSocket.accept();
                CommsBTConnected commsBTConnected = new CommsBTConnected();
                commsBTConnected.start();
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnect.run Exception: " + e.getMessage());
            }
        }
    }

    private class CommsBTConnected extends Thread{
        private InputStream inputStream;
        private OutputStream outputStream;

        public CommsBTConnected(){
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
            sleep100();
            process();
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
                Log.i(Main.RRW_LOG_TAG, "CommsBTConnected.sendNextRequest: " + request.toString());
                outputStream.write(request.toString().getBytes());
            }catch(Exception e){
                if(e.getMessage() != null && e.getMessage().contains("Broken pipe")){
                    return false;
                }
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected.sendNextRequest Exception: " + e.getMessage());
                Toast.makeText(context, R.string.fail_send_message, Toast.LENGTH_SHORT).show();
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
                    String response = new String(buffer);
                    JSONObject responseMessage = new JSONObject(response);
                    gotResponse(responseMessage);
                }
            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected.read: Input stream read exception: " + e.getMessage());
                gotError(context.getString(R.string.fail_response));
            }
        }

        private void gotResponse(final JSONObject responseMessage){
            Log.i(Main.RRW_LOG_TAG, "CommsBTConnected.gotResponse: " + responseMessage.toString());
            try{
                handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_GOT_RESPONSE, responseMessage));

            }catch(Exception e){
                Log.e(Main.RRW_LOG_TAG, "CommsBTConnected.gotResponse Exception: " + e.getMessage());
                gotError(context.getString(R.string.fail_response));
            }
        }
    }
}