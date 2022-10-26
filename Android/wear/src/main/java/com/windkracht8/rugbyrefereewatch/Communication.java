package com.windkracht8.rugbyrefereewatch;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONObject;

import java.util.Date;

public class Communication extends WearableListenerService{
    public static long lastSyncRequest = 0;
    public Communication(){}

    @Override
    public void onDataChanged(DataEventBuffer dataEvents){
        for(DataEvent event : dataEvents){
            DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
            String requestType = dataMap.getString("requestType");
            if(requestType == null){
                Log.e(MainActivity.RRW_LOG_TAG, "Communication.onDataChanged No requestType");
                return;
            }
            if(dataMap.getString("responseData") != null) return;

            Log.i(MainActivity.RRW_LOG_TAG, "Communication.onDataChanged requestType: " + requestType);
            switch(requestType){
                case "sync":
                    onReceiveSync(requestType, dataMap);
                    break;
                case "getMatches":
                    onReceiveGetMatches(requestType, dataMap);
                    break;
                case "getMatch":
                    onReceiveGetMatch(requestType);
                    break;
                case "prepare":
                    onReceivePrepare(requestType, dataMap);
                    break;
                default:
                    Log.e(MainActivity.RRW_LOG_TAG, "Communication.onDataChanged Did not understand message");
                    sendRequest(requestType, "responseData", "Did not understand message");
            }
        }
    }

    private void onReceiveSync(String requestType, DataMap dataMap){
        String requestData = dataMap.getString("requestData");
        if(requestData == null){
            Log.e(MainActivity.RRW_LOG_TAG, "Communication.onReceiveSync No requestData for request sync");
            return;
        }
        long timestamp = dataMap.getLong("timestamp");
        if(timestamp < lastSyncRequest) return;
        lastSyncRequest = timestamp;
        try{
            JSONObject responseData_json = new JSONObject();
            responseData_json.put("matches", FileStore.file_deletedMatches(getApplicationContext(), requestData));
            responseData_json.put("settings", MainActivity.getSettings(getBaseContext()));
            sendRequest(requestType, "responseData", responseData_json.toString());
            Conf.syncCustomMatchTypes(getBaseContext(), requestData);
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "Communication.onReceiveSync Exception: " + e.getMessage());
            sendRequest(requestType, "responseData", "unexpected error");
        }
    }
    private void onReceiveGetMatches(String requestType, DataMap dataMap){
        String requestData = dataMap.getString("requestData");
        if(requestData == null){
            Log.e(MainActivity.RRW_LOG_TAG, "Communication.onReceiveGetMatches No requestData for request getMatches");
            return;
        }
        String responseData = FileStore.file_deletedMatches(getApplicationContext(), requestData).toString();
        sendRequest(requestType, "responseData", responseData);
        Conf.syncCustomMatchTypes(getBaseContext(), requestData);
    }
    private void onReceiveGetMatch(String requestType){
        if(MainActivity.match == null) return;
        try{
            JSONObject responseData = MainActivity.match.toJson(getBaseContext());
            responseData.put("timer", MainActivity.getTimer());
            sendRequest(requestType, "responseData", responseData.toString());
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "Communication.onReceiveGetMatch Exception: " + e.getMessage());
            sendRequest(requestType, "responseData", "unexpected error");
        }
    }
    private void onReceivePrepare(String requestType, DataMap dataMap){
        String requestData = dataMap.getString("requestData");
        if(requestData == null){
            Log.e(MainActivity.RRW_LOG_TAG, "Communication.onReceivePrepare No requestData for request prepare");
            return;
        }
        try{
            JSONObject requestData_json = new JSONObject(requestData);
            if(MainActivity.incomingSettings(getBaseContext(), requestData_json)){
                sendRequest(requestType, "responseData", "okilly dokilly");
                FileStore.file_storeSettings(getApplicationContext());
            }else{
                sendRequest(requestType, "responseData", "match ongoing");
            }
            Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
            intent.putExtra("intent_type", "onReceivePrepare");
            this.sendBroadcast(intent);
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "Communication.onReceivePrepare Exception: " + e.getMessage());
            sendRequest(requestType, "responseData", "unexpected error");
        }
    }

    public void sendRequest(final String requestType, final String requestDataType, final String requestData){
        Log.i(MainActivity.RRW_LOG_TAG, "Communication.sendRequest: " + requestType);
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/rrw");
        putDataMapReq.getDataMap().putLong("timestamp", (new Date()).getTime());
        putDataMapReq.getDataMap().putString("requestType", requestType);
        putDataMapReq.getDataMap().putString(requestDataType, requestData);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        DataClient dataClient = Wearable.getDataClient(getApplicationContext());
        Task<DataItem> putDataTask = dataClient.putDataItem(putDataReq);
        putDataTask.addOnFailureListener(exception -> Log.e(MainActivity.RRW_LOG_TAG, "Communication.sendRequest Exception: " + exception.getMessage()));
    }
}
