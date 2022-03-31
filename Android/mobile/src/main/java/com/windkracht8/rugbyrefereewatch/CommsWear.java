package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONObject;

import java.util.Date;
import java.util.List;

public class CommsWear extends WearableListenerService implements DataClient.OnDataChangedListener{
    private final DataClient dataClient;
    public String status;
    private Handler handler_main;
    private boolean isCheckIfConnected = false;

    public CommsWear(Context context){
        dataClient = Wearable.getDataClient(context);
        dataClient.addListener(this);
        status = "Searching";
        checkIfConnected(context);

        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "updateStatus");
        intent.putExtra("source", "wear");
        intent.putExtra("status_new", status);
        context.sendBroadcast(intent);
    }
    public void checkIfConnected(Context context){
        if(isCheckIfConnected) return;
        isCheckIfConnected = true;
        Task<List<Node>> nodeListTask = Wearable.getNodeClient(context).getConnectedNodes();
        nodeListTask.addOnCompleteListener(task ->{
            if(task.isSuccessful()){
                for(Node node : task.getResult()){
                    if(node.isNearby()){
                        if(!status.equals("CONNECTED")) {
                            status = "CONNECTED";
                            Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
                            intent.putExtra("intent_type", "updateStatus");
                            intent.putExtra("source", "wear");
                            intent.putExtra("status_new", status);
                            context.sendBroadcast(intent);
                        }
                        if(handler_main == null) handler_main = new Handler(Looper.getMainLooper());
                        isCheckIfConnected = false;
                        handler_main.postDelayed(() -> checkIfConnected(context), 10000);
                        return;
                    }
                }
            }
            if(!status.equals("OFFLINE")) {
                status = "OFFLINE";
                Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
                intent.putExtra("intent_type", "updateStatus");
                intent.putExtra("source", "wear");
                intent.putExtra("status_new", status);
                context.sendBroadcast(intent);
            }
            if(handler_main == null) handler_main = new Handler(Looper.getMainLooper());
            isCheckIfConnected = false;
            handler_main.postDelayed(() -> checkIfConnected(context), 10000);
        });
    }
    public void stop(){
        dataClient.removeListener(this);
        if(handler_main != null) handler_main.removeCallbacksAndMessages(null);
    }
    @Override
    public void onDataChanged(DataEventBuffer dataEvents){
        for(DataEvent event : dataEvents){
            DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
            String requestType = dataMap.getString("requestType");
            if(requestType == null){
                Log.e("communication", "No requestType");
                return;
            }
            if(dataMap.getString("responseData") == null){return;}
            String responseData = dataMap.getString("responseData");
            Log.i("CommsWear", "Received " + requestType + ": " + responseData);
            gotResponse(requestType, responseData);
        }
    }

    public static void sendRequest(Context context, final String requestType, final JSONObject requestData){
        Log.i("CommsWear", "sendRequest: " + requestType);
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/rrw");
        putDataMapReq.getDataMap().putLong("timestamp", (new Date()).getTime());
        putDataMapReq.getDataMap().putString("requestType", requestType);
        if(requestData != null){
            putDataMapReq.getDataMap().putString("requestData", requestData.toString());
        }
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        Task<DataItem> putDataTask = Wearable.getDataClient(context).putDataItem(putDataReq);
        putDataTask.addOnFailureListener(exception -> gotError(context, exception.getMessage()));
    }

    private static void gotError(Context context, final String error){
        Log.e("CommsWear", "gotError: " + error);
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "gotError");
        intent.putExtra("source", "wear");
        intent.putExtra("error", error);
        context.sendBroadcast(intent);
    }
    private void gotResponse(final String requestType, final String responseData){
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "gotResponse");
        intent.putExtra("source", "wear");
        intent.putExtra("requestType", requestType);
        intent.putExtra("responseData", responseData);
        dataClient.getApplicationContext().sendBroadcast(intent);
    }
}
