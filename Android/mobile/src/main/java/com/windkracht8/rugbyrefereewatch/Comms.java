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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.ConnectIQ.ConnectIQListener;
import com.garmin.android.connectiq.ConnectIQ.IQApplicationEventListener;
import com.garmin.android.connectiq.ConnectIQ.IQApplicationInfoListener;
import com.garmin.android.connectiq.ConnectIQ.IQDeviceEventListener;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressLint("MissingPermission")//Handled by Permissions.hasXPermission
class Comms implements ConnectIQListener, IQDeviceEventListener, IQApplicationInfoListener, IQApplicationEventListener{
    private final boolean emulatorMode = false;//false = release; true = testing in emulators
    private final UUID RRW_UUID = UUID.fromString("8b16601b-5c76-4151-a930-2752849f4552");
    private final Main main;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private CommsBTConnect commsBTConnect;
    private CommsBTConnected commsBTConnected;
    private Set<String> knownBTAddresses = new HashSet<>();
    final ArrayList<BluetoothDevice> knownBTDevices = new ArrayList<>();
    private static final String IQ_APP_ID = "A5E772CFCE6A4CF082133E2E8C52FFBA";
    private IQApp iQApp;
    private ConnectIQ connectIQ;
    private final Set<Long> knownIQIds = new HashSet<>();
    final ArrayList<IQDevice> knownIQDevices = new ArrayList<>();
    enum IQSdkStatus{UNAVAILABLE, READY, GCM_NOT_INSTALLED, GCM_UPGRADE_NEEDED, ERROR}
    IQSdkStatus iQSdkStatus = IQSdkStatus.UNAVAILABLE;
    private boolean isIQSending = false;
    private IQDevice iQDevice;

    enum Status{DISCONNECTED, CONNECTING, CONNECTED_BT, CONNECTED_IQ}
    Status status = Status.DISCONNECTED;
    private boolean startDone = false;
    private final ArrayList<Request> requestQueue = new ArrayList<>();

    Comms(Main main){
        Log.d(Main.LOG_TAG, "Comms");
        this.main = main;
        addListener(main);
        BluetoothManager bm = (BluetoothManager) main.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bm.getAdapter();
        if(bluetoothAdapter == null) return;

        IntentFilter btIntentFilter = new IntentFilter();
        btIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        BroadcastReceiver btBroadcastReceiver = new BroadcastReceiver(){
            public void onReceive(Context context, Intent intent){
                if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
                    int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    if(btState == BluetoothAdapter.STATE_TURNING_OFF){
                        onCommsError(R.string.fail_BT_off);
                        main.runInBackground(()->stop());
                    }else if(btState == BluetoothAdapter.STATE_ON){
                        main.runInBackground(()->start());
                    }
                }
            }
        };
        main.registerReceiver(btBroadcastReceiver, btIntentFilter);
    }
    void start(){
        startDone = false;
        if(bluetoothAdapter == null){
            onCommsStartDone();
            return;
        }
        if(bluetoothAdapter.getState() != BluetoothAdapter.STATE_ON){
            onCommsStartDone();
            onCommsError(R.string.fail_BT_off);
            return;
        }

        knownBTAddresses = Main.sharedPreferences.getStringSet("knownBTAddresses", knownBTAddresses);
        Log.d(Main.LOG_TAG, "Comms.start " + knownBTAddresses.size() + " known BT addresses");
        Set<BluetoothDevice> bondedBTDevices = bluetoothAdapter.getBondedDevices();
        //Find and clean known BT devices
        for(String address : knownBTAddresses) checkKnownBTDevice(address, bondedBTDevices);
        //Try to connect to known BT device
        for(BluetoothDevice device : knownBTDevices){
            connectBTDevice(device);
            return;//having multiple watches is rare, user will have to select from DeviceSelect
        }

        //Check if Garmin connect app is installed
        try{
            main.getPackageManager().getPackageInfo("com.garmin.android.apps.connectmobile", 0);
        }catch(PackageManager.NameNotFoundException e){
            iQSdkStatus = IQSdkStatus.GCM_NOT_INSTALLED;
            onCommsStartDone();
            return;
        }
        Set<String> knownIQIds_s = Main.sharedPreferences.getStringSet("knownIQIds", null);
        if(knownIQIds_s != null) knownIQIds_s.forEach(id->knownIQIds.add(Long.valueOf(id)));
        Log.d(Main.LOG_TAG, "Comms.start " + knownIQIds.size() + " known IQ devices");

        if(connectIQ == null){
            connectIQ = ConnectIQ.getInstance(main,
                    emulatorMode ? ConnectIQ.IQConnectType.TETHERED : ConnectIQ.IQConnectType.WIRELESS
            );
            try{
                Looper.prepare();
            }catch(Exception e){
                Log.d(Main.LOG_TAG, "Looper.prepare failed, probably already done");
            }
            connectIQ.initialize(main, false, this);
            //The rest of start will be done in onSdkReady
        }
    }
    //Check if a known device is still bound, if so, add it to known devices, else remove
    private void checkKnownBTDevice(String address, Set<BluetoothDevice> bondedBTDevices){
        for(BluetoothDevice device : bondedBTDevices){
            if(device.getAddress().equals(address)){
                knownBTDevices.add(device);
                return;
            }
        }
        removeKnownBTAddress(address);
    }
    void removeKnownBTAddress(String address){
        if(!knownBTAddresses.contains(address)) return;
        knownBTAddresses.remove(address);
        storeKnownBTAddresses();
    }
    private void addKnownBTDevice(BluetoothDevice device){
        if(knownBTDevices.contains(device)) return;
        knownBTDevices.add(device);
        addKnownBTAddress(device.getAddress());
    }
    private void addKnownBTAddress(String address){
        if(knownBTAddresses.contains(address)) return;
        knownBTAddresses.add(address);
        storeKnownBTAddresses();
    }
    private void storeKnownBTAddresses(){
        if(knownBTAddresses.isEmpty()){
            Main.sharedPreferences_editor.remove("knownBTAddresses");
        }else{
            Main.sharedPreferences_editor.putStringSet("knownBTAddresses", knownBTAddresses);
        }
        Main.sharedPreferences_editor.apply();
    }

    //Check if a known device is still bound, if so, add it to known devices, else remove
    private void checkKnownIQDevice(long id, List<IQDevice> bondedDevices){
        for(IQDevice device : bondedDevices){
            if(device.getDeviceIdentifier () == id){
                knownIQDevices.add(device);
                return;
            }
        }
        removeKnownIQId(id);
    }
    void removeKnownIQId(long id){
        if(!knownIQIds.contains(id)) return;
        knownIQIds.remove(id);
        storeKnownIQIds();
    }
    private void addKnownIQDevice(IQDevice device){
        if(knownIQDevices.contains(device)) return;
        knownIQDevices.add(device);
        addKnownIQDeviceId(device.getDeviceIdentifier());
    }
    private void addKnownIQDeviceId(long id){
        if(knownIQIds.contains(id)) return;
        knownIQIds.add(id);
        storeKnownIQIds();
    }
    private void storeKnownIQIds(){
        if(knownIQIds.isEmpty()){
            Main.sharedPreferences_editor.remove("knownIQIds");
        }else{
            Set<String> knownIQIds_s = new HashSet<>();
            knownIQIds.forEach(id->knownIQIds_s.add(String.valueOf(id)));
            Main.sharedPreferences_editor.putStringSet("knownIQIds", knownIQIds_s);
        }
        Main.sharedPreferences_editor.apply();
    }
    void stop(){
        status = Status.DISCONNECTED;
        requestQueue.clear();
        try{
            if(bluetoothSocket != null) bluetoothSocket.close();
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "Comms.stop bluetoothSocket: " + e.getMessage());
        }

        bluetoothSocket = null;
        commsBTConnect = null;
        commsBTConnected = null;

        isIQSending = false;
        iQDevice = null;
        iQApp = null;

        if(startDone) onCommsDisconnected();
        else onCommsStartDone();
    }
    void onDestroy(Context context){
        if(connectIQ != null){
            try{
                connectIQ.unregisterAllForEvents();
                connectIQ.shutdown(context);
            }catch(Exception e){
                Log.d(Main.LOG_TAG, "Comms.onDestroy exception: " + e.getMessage());
            }
            connectIQ = null;
        }
    }

    void sync(){addRequest(new Request());}
    private void getMatch(long match_id){addRequest(new Request(Request.Type.GET_MATCH, match_id));}
    private void delMatch(long match_id){addRequest(new Request(Request.Type.DEL_MATCH, match_id));}
    void prep(JSONObject requestData){addRequest(new Request(requestData));}
    private void addRequest(Request request){
        requestQueue.add(request);
        if(Looper.getMainLooper().isCurrentThread()) main.runInBackground(this::sendNextIQMessage);
        else sendNextIQMessage();
    }
    private void onResponse(JSONObject response) throws JSONException{
        Log.d(Main.LOG_TAG, "CommsBTConnected.onResponse: " + response);
        String requestType = response.getString("requestType");
        switch(requestType){
            case "sync":
                //version 1 {"requestType":"sync","responseData":{"matches":[],"settings":{}}}
                //version 2 {"requestType":"sync","responseData":{"match_ids":[],"settings":{}}}
                if(!response.has("responseData")){
                    onCommsError(R.string.fail_unexpected);
                    return;
                }
                JSONObject responseData = response.getJSONObject("responseData");
                if(!responseData.has("settings") ||
                        (!responseData.has("matches") && !responseData.has("match_ids"))
                ){
                    onCommsError(R.string.fail_response);
                    return;
                }
                if(responseData.has("match_ids")){//version 2
                    Set<Long> match_ids = new HashSet<>();
                    for(int i = 0; i < responseData.getJSONArray("match_ids").length(); i++){
                        match_ids.add(responseData.getJSONArray("match_ids").getLong(i));
                    }
                    gotMatchIds(match_ids);
                }//version 1 (matches) is handled in Main.onCommsResponse
                onCommsResponse(Request.Type.SYNC, responseData);
                break;
            case "getMatch":
                //{"requestType":"getMatch","responseData":{ match }}
                if(response.has("responseData")){
                    onCommsResponse(Request.Type.GET_MATCH, response.getJSONObject("responseData"));
                }else{
                    onCommsError(R.string.fail_unexpected);
                }
                break;
            case "delMatch":
                //{"requestType":"delMatch","responseData":"okilly dokilly"}
                if(response.has("responseData") &&
                        response.getString("responseData").equals("okilly dokilly")
                ){
                    onCommsResponse(Request.Type.DEL_MATCH, null);
                }else{
                    onCommsError(R.string.fail_unexpected);
                }
                break;
            case "prepare":
                //{"requestType":"prepare","responseData":"okilly dokilly"}
                if(response.has("responseData") &&
                        response.getString("responseData").equals("okilly dokilly")
                ){
                    onCommsResponse(Request.Type.PREP, null);
                }else if(response.has("responseData") &&
                        response.getString("responseData").equals("match ongoing")
                ){
                    onCommsError(R.string.match_ongoing);
                }else{
                    onCommsError(R.string.fail_unexpected);
                }
                break;
            default:
                Log.e(Main.LOG_TAG, "CommsBTConnected.read unexpected requestType: " + requestType);
                onCommsError(R.string.fail_unexpected);
        }
    }
    private void gotMatchIds(Set<Long> match_ids){
        match_ids.forEach(match_id->{
            if(TabHistory.deleted_matches.contains(match_id)){
                delMatch(match_id);
            }else{
                for(int i=0; i<TabHistory.matches.size(); i++){
                    JSONObject match = TabHistory.matches.get(i);
                    try{
                        if(match.getLong("matchid") == match_id) return;
                    }catch(JSONException e){
                        return;
                    }
                }
                getMatch(match_id);
            }
        });
        for(int i=TabHistory.deleted_matches.size()-1; i>=0; i--){
            if(!match_ids.contains(TabHistory.deleted_matches.get(i))){
                TabHistory.deleted_matches.remove(i);
            }
        }
    }

    static class Request{
        public enum Type{SYNC, GET_MATCH, DEL_MATCH, PREP}
        private final Type type;
        private long match_id;
        private JSONObject requestData;
        private Request(){type = Type.SYNC;}
        private Request(Type type, long match_id){//DEL_MATCH, GET_MATCH
            this.type = type;
            this.match_id = match_id;
        }
        private Request(JSONObject requestData){
            type = Type.PREP;
            this.requestData = requestData;
        }
        String getJsonString() throws JSONException{
            JSONObject request_json = new JSONObject();
            switch(type){
                case SYNC:
                    //{"version":2,"requestType":"sync","requestData":{"deleted_matches":[],"custom_match_types":[]}}
                    request_json.put("version", 2);//2 = watch to respond with match_ids, phone sends getMatch/delMatch
                    request_json.put("requestType", "sync");
                    JSONObject requestDataSync = new JSONObject();
                    requestDataSync.put("deleted_matches", new JSONArray(TabHistory.deleted_matches));//DEPRECATED
                    requestDataSync.put("custom_match_types", TabPrepare.customMatchTypes);
                    request_json.put("requestData", requestDataSync);
                    break;
                case GET_MATCH:
                    //{"requestType":"getMatch","requestData":123456789}
                    request_json.put("requestType", "getMatch");
                    request_json.put("requestData", match_id);
                    break;
                case DEL_MATCH:
                    //{"requestType":"delMatch","requestData":123456789}
                    request_json.put("requestType", "delMatch");
                    request_json.put("requestData", match_id);
                    break;
                case PREP:
                    //{"requestType":"prepare","requestData":{}}
                    request_json.put("requestType", "prepare");
                    request_json.put("requestData", requestData);
                    break;
            }
            return request_json.toString();
        }
    }

    private final List<Interface> listeners = new ArrayList<>();
    void addListener(Interface listener){listeners.add(listener);}
    void removeListener(Interface listener){listeners.remove(listener);}
    private void onCommsStartDone(){
        Log.d(Main.LOG_TAG, "Comms.onCommsStartDone");
        startDone = true;
        listeners.remove(null);
        for(int i=0; i<listeners.size(); i++) listeners.get(i).onCommsStartDone();
    }
    private void onCommsConnecting(String deviceName){
        Log.d(Main.LOG_TAG, "Comms.onCommsConnecting");
        listeners.remove(null);
        for(int i=0; i<listeners.size(); i++) listeners.get(i).onCommsConnecting(deviceName);
    }
    private void onCommsConnectFailed(){
        Log.d(Main.LOG_TAG, "Comms.onCommsConnectFailed");
        status = Status.DISCONNECTED;
        commsBTConnect = null;
        commsBTConnected = null;
        if(startDone){
            listeners.remove(null);
            for(int i=0; i<listeners.size(); i++) listeners.get(i).onCommsConnectFailed();
        }else{
            onCommsStartDone();
        }
    }
    private void onCommsConnected(String deviceName){
        Log.d(Main.LOG_TAG, "Comms.onCommsConnected");
        listeners.remove(null);
        for(int i=0; i<listeners.size(); i++) listeners.get(i).onCommsConnected(deviceName);
        if(!startDone) onCommsStartDone();
        sync();
    }
    private void onCommsSending(){
        Log.d(Main.LOG_TAG, "Comms.onCommsSending");
        listeners.remove(null);
        for(int i=0; i<listeners.size(); i++) listeners.get(i).onCommsSending();
    }
    private void onCommsSendingFinished(){
        Log.d(Main.LOG_TAG, "Comms.onCommsSendingFinished");
        listeners.remove(null);
        for(int i=0; i<listeners.size(); i++) listeners.get(i).onCommsSendingFinished();
    }
    private void onCommsDisconnected(){
        Log.d(Main.LOG_TAG, "Comms.onCommsDisconnected");
        status = Status.DISCONNECTED;
        commsBTConnect = null;
        commsBTConnected = null;
        listeners.remove(null);
        for(int i=0; i<listeners.size(); i++) listeners.get(i).onCommsDisconnected();
    }
    private void onCommsResponse(Request.Type requestType, JSONObject responseData){
        Log.d(Main.LOG_TAG, "Comms.onCommsResponse " + responseData);
        listeners.remove(null);
        for(int i=0; i<listeners.size(); i++) listeners.get(i).onCommsResponse(requestType, responseData);
    }
    private void onCommsError(int message){
        Log.d(Main.LOG_TAG, "Comms.onCommsError");
        requestQueue.clear();
        listeners.remove(null);
        for(int i=0; i<listeners.size(); i++) listeners.get(i).onCommsError(message);
    }
    interface Interface{
        void onCommsStartDone();
        void onCommsConnecting(String deviceName);
        void onCommsConnectFailed();
        void onCommsConnected(String deviceName);
        void onCommsDisconnected();
        void onCommsSending();
        void onCommsSendingFinished();
        void onCommsResponse(Request.Type requestType, JSONObject responseData);
        void onCommsError(int message);
    }

    Set<BluetoothDevice> getBondedBTDevices(){
        if(bluetoothAdapter == null){
            onCommsError(R.string.fail_BT_denied);
            return null;
        }
        if(bluetoothAdapter.getState() != BluetoothAdapter.STATE_ON){
            onCommsError(R.string.fail_BT_off);
            return null;
        }
        return bluetoothAdapter.getBondedDevices();
    }
    void connectBTDevice(BluetoothDevice device){
        Log.d(Main.LOG_TAG, "Comms.connectBTDevice: " + device.getName());
        if(bluetoothAdapter == null ||
                status != Status.DISCONNECTED ||
                bluetoothAdapter.getState() != BluetoothAdapter.STATE_ON ||
                commsBTConnect != null
        ) return;
        status = Status.CONNECTING;
        onCommsConnecting(device.getName());
        commsBTConnect = new CommsBTConnect(device);
        commsBTConnect.start();
    }
    private class CommsBTConnect extends Thread{
        private CommsBTConnect(BluetoothDevice device){
            Log.d(Main.LOG_TAG, "CommsBTConnect " + device.getName());
            try{
                bluetoothSocket = device.createRfcommSocketToServiceRecord(RRW_UUID);
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnect Exception: " + e.getMessage());
                onCommsConnectFailed();
            }
        }
        public void run(){
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
                onCommsConnectFailed();
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
                onCommsDisconnected();
                return;
            }
            try{
                outputStream = bluetoothSocket.getOutputStream();
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnected getOutputStream Exception: " + e.getMessage());
                onCommsDisconnected();
                return;
            }
            status = Status.CONNECTED_BT;
            onCommsConnected(bluetoothSocket.getRemoteDevice().getName());
            addKnownBTDevice(bluetoothSocket.getRemoteDevice());
        }
        public void run(){process(Executors.newSingleThreadScheduledExecutor());}
        private void close(){
            Log.d(Main.LOG_TAG, "CommsBTConnected.close");
            onCommsDisconnected();
            try{
                requestQueue.clear();
                if(bluetoothSocket != null) bluetoothSocket.close();
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnected.close exception: " + e.getMessage());
            }
            commsBTConnected = null;
            commsBTConnect = null;
        }
        private void process(ScheduledExecutorService executor){
            if(status == Status.DISCONNECTED){
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
                if(requestQueue.isEmpty()) return true;
                onCommsSending();
                String jsonString = requestQueue.remove(0).getJsonString();
                Log.d(Main.LOG_TAG, "CommsBTConnected.sendNextRequest: " + jsonString);
                outputStream.write(jsonString.getBytes());
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnected.sendNextRequest Exception: " + e.getMessage());
                onCommsError(R.string.fail_send_message);
                return false;
            }
            return true;
        }
        private void read(){
            try{
                if(inputStream.available() < 5) return;
                long last_read_time = System.currentTimeMillis();
                String response = "";
                while(System.currentTimeMillis() - last_read_time < 3000){
                    if(inputStream.available() == 0){
                        sleep100();
                        continue;
                    }
                    byte[] buffer = new byte[inputStream.available()];
                    int numBytes = inputStream.read(buffer);
                    if(numBytes < 0){
                        Log.e(Main.LOG_TAG, "CommsBTConnected.read read error: " + response);
                        onCommsError(R.string.fail_response);
                        return;
                    }
                    String temp = new String(buffer);
                    response += temp;
                    if(isValidJSON(response)){
                        onResponse(new JSONObject(response));
                        onCommsSendingFinished();
                        return;
                    }
                    last_read_time = System.currentTimeMillis();
                }
                Log.e(Main.LOG_TAG, "CommsBTConnected.read no valid message and no new data after 3 sec: " + response);
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnected.read: " + e.getMessage());
            }
            onCommsError(R.string.fail_response);
        }
        private void sleep100(){
            try{
                Thread.sleep(100);
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "CommsBTConnected.sleep100 exception: " + e.getMessage());
            }
        }
        private boolean isValidJSON(String test){
            if(!test.endsWith("}")) return false;
            try{
                new JSONObject(test);
                return true;
            }catch(JSONException e){
                return false;
            }
        }
    }

    List<IQDevice> getBondedIQDevices(){
        if(iQSdkStatus != IQSdkStatus.READY) return new ArrayList<>();
        try{
            List<IQDevice> bondedIQDevices = connectIQ.getKnownDevices();
            if(bondedIQDevices != null) return bondedIQDevices;
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "Comms.getBondedIQDevices exception: " + e.getMessage());
        }
        return new ArrayList<>();
    }
    void connectIQDevice(IQDevice device){
        Log.d(Main.LOG_TAG, "Comms.connectIQDevice: " + device.getFriendlyName() + " status: " + device.getStatus());
        if(bluetoothAdapter == null ||
                status != Status.DISCONNECTED ||
                bluetoothAdapter.getState() != BluetoothAdapter.STATE_ON
        ) return;
        status = Status.CONNECTING;
        onCommsConnecting(device.getFriendlyName());
        iQDevice = device;
        try{
            connectIQ.getApplicationInfo(IQ_APP_ID, iQDevice, this);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "Comms.connectIQDevice exception: " + e.getMessage());
        }
    }
    private void sendNextIQMessage(){
        if(isIQSending || status != Status.CONNECTED_IQ || requestQueue.size() < 1) return;
        isIQSending = true;
        onCommsSending();
        try{
            String jsonString = requestQueue.remove(0).getJsonString();
            Log.d(Main.LOG_TAG, "Comms.sendNextIQMessage: " + jsonString);
            connectIQ.sendMessage(iQDevice, iQApp, jsonString, (d, a, messageStatus)->{
                Log.d(Main.LOG_TAG, "Comms.sendNextIQMessage.onMessageStatus status: " + messageStatus.name() + " " + iQApp.getApplicationId());
                if(messageStatus != ConnectIQ.IQMessageStatus.SUCCESS)
                    onCommsError(R.string.fail_send_message);
            });
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "Comms.sendNextIQMessage exception: " + e);
            Log.e(Main.LOG_TAG, "Comms.sendNextIQMessage exception: " + e.getMessage());
            onCommsError(R.string.fail_unexpected);
        }
    }
    //IQApplicationEventListener
    @Override public void onMessageReceived(IQDevice d, IQApp a, List<Object> messageData, ConnectIQ.IQMessageStatus messageStatus){
        Log.d(Main.LOG_TAG, "Comms.onMessageReceived messageStatus: " + messageStatus + " messageData: " + messageData);
        isIQSending = false;
        onCommsSendingFinished();
        if(messageStatus == ConnectIQ.IQMessageStatus.SUCCESS){
            messageData.forEach(message->{
                try{
                    onResponse(new JSONObject((String)message));
                }catch(JSONException e){
                    onCommsError(R.string.fail_response);
                }
            });
        }else{
            onCommsError(R.string.fail_response);
        }
        sendNextIQMessage();
    }
    //ConnectIQListener
    @Override public void onSdkReady(){
        Log.d(Main.LOG_TAG, "Comms.onSdkReady");
        iQSdkStatus = IQSdkStatus.READY;

        //Resume IQ part of start
        List<IQDevice> bondedIQDevices = getBondedIQDevices();
        //Find and clean known IQ devices
        for(long id : knownIQIds) checkKnownIQDevice(id, bondedIQDevices);

        //Try to connect to known IQ device
        for(IQDevice device : knownIQDevices){
            connectIQDevice(device);
            return;//having multiple watches is rare, user will have to select from DeviceSelect
        }
        onCommsStartDone();
    }
    @Override public void onInitializeError(ConnectIQ.IQSdkErrorStatus iqSdkErrorStatus){
        Log.d(Main.LOG_TAG, "Comms.onInitializeError: " + iqSdkErrorStatus);
        switch(iqSdkErrorStatus){
            case GCM_NOT_INSTALLED:
                iQSdkStatus = IQSdkStatus.GCM_NOT_INSTALLED;
                break;
            case GCM_UPGRADE_NEEDED:
                iQSdkStatus = IQSdkStatus.GCM_UPGRADE_NEEDED;
                break;
            case SERVICE_ERROR:
                iQSdkStatus = IQSdkStatus.ERROR;
                break;
        }
        onCommsStartDone();
    }
    @Override public void onSdkShutDown(){
        Log.d(Main.LOG_TAG, "Comms.onSdkShutDown");
        iQSdkStatus = IQSdkStatus.UNAVAILABLE;
    }
    //IQApplicationInfoListener
    @Override public void onApplicationInfoReceived(IQApp app){
        Log.d(Main.LOG_TAG, "Comms.onApplicationInfoReceived status: " + app.getStatus() + " name: " + app.getDisplayName() + " version: " + app.version());
        if(status == Status.CONNECTED_BT || status == Status.CONNECTED_IQ) return;
        if(emulatorMode || app.getStatus() == IQApp.IQAppStatus.INSTALLED){
            status = Status.CONNECTED_IQ;
            iQApp = emulatorMode ? new IQApp("") : app;//in the emulator the applicationID is empty
            try{
                connectIQ.registerForDeviceEvents(iQDevice, this);
                connectIQ.registerForAppEvents(iQDevice, iQApp, this);
                onCommsConnected(iQDevice.getFriendlyName());
                addKnownIQDevice(iQDevice);
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "Comms.onApplicationInfoReceived exception: " + e);
                Log.e(Main.LOG_TAG, "Comms.onApplicationInfoReceived exception: " + e.getMessage());
                onCommsError(R.string.fail_unexpected);
            }
        }else if(status == Status.CONNECTING) onCommsConnectFailed();
    }
    @Override public void onApplicationNotInstalled(String applicationId){
        Log.d(Main.LOG_TAG, "Comms.onApplicationNotInstalled: " + applicationId);
        main.runOnUiThread(()->Toast.makeText(main, R.string.fail_app_not_installed, Toast.LENGTH_SHORT).show());
        iQDevice = null;
        onCommsConnectFailed();
    }
    //IQDeviceEventListener
    @Override public void onDeviceStatusChanged(IQDevice device, IQDevice.IQDeviceStatus status_new){
        Log.d(Main.LOG_TAG, "Comms.onDeviceStatusChanged device: " + device + " status_new: " + status_new);
        if(status_new == IQDevice.IQDeviceStatus.CONNECTED){
            if(status == Status.CONNECTED_BT || status == Status.CONNECTED_IQ) return;
            iQDevice = device;
            try{
                Log.d(Main.LOG_TAG, "getApplicationInfo");
                connectIQ.getApplicationInfo(IQ_APP_ID, device, this);
                //onApplicationInfoReceived/onApplicationNotInstalled will be called to make sure RRW is installed on the watch
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "Comms.onDeviceStatusChanged getApplicationInfo exception: " + e.getMessage());
                onCommsError(R.string.fail_unexpected);
            }
        }else if(status == Status.CONNECTED_IQ) onCommsDisconnected();
    }
}
