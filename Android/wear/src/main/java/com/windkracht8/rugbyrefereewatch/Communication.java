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

public class Communication extends WearableListenerService {
    public Communication(){
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
            String requestType = dataMap.getString("requestType");
            if(requestType == null){
                Log.e("communication", "No requestType");
                return;
            }
            if(dataMap.getString("responseData") != null){return;}

            String requestData = dataMap.getString("requestData");
            Log.i("communication", requestType + ": " + requestData);
            String responseData;
            switch(requestType){
                case "getMatches":
                    if(requestData == null){
                        Log.e("communication", "No requestData for request getMatches");
                        return;
                    }
                    responseData = Filestore.file_deletedMatches(getApplicationContext(), requestData);
                    break;
                case "getMatch":
                    responseData = MainActivity.match.tojson().toString();
                    break;
                case "prepare":
                    if(requestData == null){
                        Log.e("communication", "No requestData for request prepare");
                        return;
                    }
                    try{
                        JSONObject requestData_json = new JSONObject(requestData);
                        if(MainActivity.incomingSettings(requestData_json)){
                            responseData = "okilly dokilly";
                            Filestore.file_storeSettings(getApplicationContext());
                        }else{
                            responseData = "match ongoing";
                        }
                        this.sendBroadcast(new Intent("com.windkracht8.rugbyrefereewatch.settingsupdate"));
                    } catch (Exception e) {
                        Log.e("communication", "prepare: " + e.getMessage());
                        responseData = "unexpected error";
                    }
                    break;
                default:
                    Log.e("communication", "Did not understand message");
                    responseData = "Did not understand message";
            }
            sendRequest(requestType, "responseData", responseData);
        }
    }

    public void sendRequest(final String requestType, final String requestDataType, final String requestData) {
        Log.i("communication", "sendRequest: " + requestType);
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/rrw");
        putDataMapReq.getDataMap().putLong("timestamp", (new Date()).getTime());
        putDataMapReq.getDataMap().putString("requestType", requestType);
        putDataMapReq.getDataMap().putString(requestDataType, requestData);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        DataClient dataClient = Wearable.getDataClient(getApplicationContext());
        Task<DataItem> putDataTask = dataClient.putDataItem(putDataReq);
        putDataTask.addOnFailureListener(exception -> Log.e("communication", "sendRequest failed: " + exception.getMessage()));
    }
}
