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
        if(handler_main == null) handler_main = new Handler(Looper.getMainLooper());
        status = "SEARCHING";
        checkIfConnected(context, 10000);

        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "updateStatus");
        intent.putExtra("source", "wear");
        intent.putExtra("status_new", status);
        context.sendBroadcast(intent);
    }
    public void search(Context context){
        handler_main.removeCallbacksAndMessages(null);
        checkIfConnected(context, 1000);
        handler_main.postDelayed(() -> searchTimeout(context), 45000);
    }
    private void searchTimeout(Context context){
        handler_main.removeCallbacksAndMessages(null);
        status = "OFFLINE";
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "updateStatus");
        intent.putExtra("source", "wear");
        intent.putExtra("status_new", status);
        context.sendBroadcast(intent);
        handler_main.postDelayed(() -> checkIfConnected(context, 10000), 10000);
    }
    public void checkIfConnected(Context context, int timeout){
        if(isCheckIfConnected) return;
        isCheckIfConnected = true;
        Task<List<Node>> nodeListTask = Wearable.getNodeClient(context).getConnectedNodes();
        nodeListTask.addOnCompleteListener(task ->{
            isCheckIfConnected = false;
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
                        handler_main.postDelayed(() -> checkIfConnected(context, 10000), 10000);
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
            handler_main.postDelayed(() -> checkIfConnected(context, timeout), timeout);
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
                Log.e(MainActivity.RRW_LOG_TAG, "CommsWear.onDataChanged No requestType");
                return;
            }
            if(dataMap.getString("responseData") == null){return;}
            String responseData = dataMap.getString("responseData");
            Log.i(MainActivity.RRW_LOG_TAG, "CommsWear.onDataChanged: " + requestType + ": " + responseData);
            gotResponse(requestType, responseData);
        }
    }

    public static void sendRequest(Context context, final String requestType, final JSONObject requestData){
        Log.i(MainActivity.RRW_LOG_TAG, "CommsWear.sendRequest requestType: " + requestType);
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/rrw");
        putDataMapReq.getDataMap().putLong("timestamp", (new Date()).getTime());
        putDataMapReq.getDataMap().putString("requestType", requestType);
        if(requestData != null){
            Log.i(MainActivity.RRW_LOG_TAG, "CommsWear.sendRequest requestData: " + requestData);
            putDataMapReq.getDataMap().putString("requestData", requestData.toString());
        }
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        Task<DataItem> putDataTask = Wearable.getDataClient(context).putDataItem(putDataReq);
        putDataTask.addOnFailureListener(exception -> gotError(context, exception.getMessage(), R.string.fail_send_message));
    }

    private static void gotError(Context context, final String exception, final int UIMessage){
        Log.e(MainActivity.RRW_LOG_TAG, "CommsWear.gotError: " + exception);
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "gotError");
        intent.putExtra("source", "wear");
        intent.putExtra("error", context.getString(UIMessage));
        context.sendBroadcast(intent);
    }
    private void gotResponse(final String requestType, final String responseData){
        Log.i(MainActivity.RRW_LOG_TAG, "CommsWear.gotResponse: " + requestType + " " + responseData);
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "gotResponse");
        intent.putExtra("source", "wear");
        intent.putExtra("requestType", requestType);
        intent.putExtra("responseData", responseData);
        dataClient.getApplicationContext().sendBroadcast(intent);
    }
}
