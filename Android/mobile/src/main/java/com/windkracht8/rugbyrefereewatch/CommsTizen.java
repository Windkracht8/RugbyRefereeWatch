package com.windkracht8.rugbyrefereewatch;

import java.io.IOException;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.samsung.android.sdk.accessory.*;
import org.json.JSONObject;

public class CommsTizen extends SAAgentV2{
    public String status = "DISCONNECTED";

    private ServiceConnection mConnectionHandler = null;

    public CommsTizen(Context context){
        super("RugbyRefereeWatch", context, ServiceConnection.class);
        SA mAccessory = new SA();
        try{
            mAccessory.initialize(context);
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "CommsTizen.construct Exception: " + e.getMessage());
            updateStatus("FATAL");
            gotError(context.getString(R.string.fail_tizen_init));
            releaseAgent();
        }
    }

    @Override
    protected void onPeerAgentsUpdated(SAPeerAgent[] peerAgents, int result){}

    @Override
    protected void onFindPeerAgentsResponse(SAPeerAgent[] peerAgents, int result){
        if((result == SAAgent.PEER_AGENT_FOUND) && (peerAgents != null)){
            for(SAPeerAgent peerAgent:peerAgents)
                requestServiceConnection(peerAgent);
        }else if(result == SAAgent.FINDPEER_DEVICE_NOT_CONNECTED){
            updateStatus("ERROR");
            gotError(getApplicationContext().getString(R.string.watch_not_found));
        }else if(result == SAAgent.FINDPEER_SERVICE_NOT_FOUND){
            updateStatus("ERROR");
            gotError(getApplicationContext().getString(R.string.app_not_found_on_watch));
        }else{
            updateStatus("ERROR");
            gotError(getApplicationContext().getString(R.string.no_watch_found));
        }
    }

    @Override
    protected void onServiceConnectionRequested(SAPeerAgent peerAgent){
        if(peerAgent != null){
            acceptServiceConnectionRequest(peerAgent);
        }
    }

    @Override
    protected void onServiceConnectionResponse(SAPeerAgent peerAgent, SASocket socket, int result){
        switch(result){
            case SAAgent.CONNECTION_SUCCESS:
                this.mConnectionHandler = (ServiceConnection) socket;
                updateStatus("CONNECTED");
                break;
            case SAAgent.CONNECTION_ALREADY_EXIST:
            case SAAgent.CONNECTION_DUPLICATE_REQUEST:
                break;
            default:
                updateStatus("ERROR");
                gotError(getApplicationContext().getString(R.string.connection_failed)+" "+result);
        }
    }

    @Override
    protected void onError(SAPeerAgent peerAgent, String errorMessage, int errorCode){
        Log.i(MainActivity.RRW_LOG_TAG, "CommsTizen.onError: " + errorCode);
        super.onError(peerAgent, errorMessage, errorCode);
        updateStatus("FATAL");
        gotError(errorMessage);
        closeConnection();
        releaseAgent();
    }

    public class ServiceConnection extends SASocket{
        public ServiceConnection(){
            super(ServiceConnection.class.getName());
        }

        @Override
        public void onError(int channelId, String errorMessage, int errorCode){
            Log.i(MainActivity.RRW_LOG_TAG, "CommsTizen.ServiceConnection.onError: " + errorCode);
            gotError(errorCode + ": "  + errorMessage);
        }

        @Override
        public void onReceive(int channelId, byte[] data){
            try{
                String sData = new String(data);
                Log.i(MainActivity.RRW_LOG_TAG, "CommsTizen.ServiceConnection.onReceive: " + sData);
                JSONObject responseMessage = new JSONObject(sData);
                gotResponse(responseMessage);
            }catch(Exception e){
                Log.e(MainActivity.RRW_LOG_TAG, "CommsTizen.onReceive: " + e.getMessage());
                gotError(getApplicationContext().getString(R.string.fail_response));
            }
        }

        @Override
        protected void onServiceConnectionLost(int reason){
            //521: phone lost connection
            //513: app closed on watch
            updateStatus("CONNECTION_LOST");
            closeConnection();
        }
    }

    public void findPeers(){
        if(status.equals("DISCONNECTED") ||
            status.equals("CONNECTION_LOST") ||
            status.equals("ERROR")
        ){
            updateStatus("FINDING_PEERS");
            findPeerAgents();
        }
    }

    public void sendRequest(final String requestType, final JSONObject requestData){
        if(mConnectionHandler == null){
            gotError(getApplicationContext().getString(R.string.not_connected));
        }
        JSONObject requestMessage = new JSONObject();
        try{
            requestMessage.put("requestType", requestType);
            requestMessage.put("requestData", requestData);
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "CommsTizen.sendRequest json: " + e.getMessage());
            Toast.makeText(getApplicationContext(), R.string.fail_send_message, Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(MainActivity.RRW_LOG_TAG, "CommsTizen.sendRequest: " + requestMessage);
        try{
            mConnectionHandler.send(getServiceChannelId(0), requestMessage.toString().getBytes());
        }catch(IOException e){
            Log.e(MainActivity.RRW_LOG_TAG, "CommsTizen.sendRequest send: " + e.getMessage());
            Toast.makeText(getApplicationContext(), R.string.fail_send_message, Toast.LENGTH_SHORT).show();
        }
    }

    public void closeConnection(){
        if(mConnectionHandler != null){
            mConnectionHandler.close();
            mConnectionHandler = null;
        }
    }

    private void updateStatus(final String status_new){
        this.status = status_new;
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "updateStatus");
        intent.putExtra("source", "tizen");
        intent.putExtra("status_new", status_new);
        getApplicationContext().sendBroadcast(intent);
    }
    private void gotError(final String error){
        Log.e(MainActivity.RRW_LOG_TAG, "CommsTizen.gotError: " + error);
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "gotError");
        intent.putExtra("source", "tizen");
        intent.putExtra("error", error);
        getApplicationContext().sendBroadcast(intent);
    }
    private void gotResponse(final JSONObject responseMessage){
        Log.i(MainActivity.RRW_LOG_TAG, "CommsTizen.gotResponse: " + responseMessage.toString());
        try{
            Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
            intent.putExtra("intent_type", "gotResponse");
            intent.putExtra("source", "tizen");
            intent.putExtra("requestType", responseMessage.getString("requestType"));
            intent.putExtra("responseData", responseMessage.getString("responseData"));
            getApplicationContext().sendBroadcast(intent);
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "CommsTizen.gotResponse Exception: " + e.getMessage());
            gotError(getApplicationContext().getString(R.string.fail_response));
        }
    }
}
