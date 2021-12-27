package com.windkracht8.rugbyrefereewatch;

import java.io.IOException;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.samsung.android.sdk.accessory.*;
import org.json.JSONObject;

public class communication_tizen extends SAAgentV2 {
    public String status = "DISCONNECTED";

    private ServiceConnection mConnectionHandler = null;

    public communication_tizen(Context context) {
        super("RugbyRefereeWatch", context, ServiceConnection.class);
        SA mAccessory = new SA();
        try {
            mAccessory.initialize(context);
        } catch (Exception e) {
            Log.e("communication_tizen", "construct: " + e);
            updateStatus("FATAL");
            gotError(e.getMessage());
            releaseAgent();
        }
    }

    @Override
    protected void onPeerAgentsUpdated(SAPeerAgent[] peerAgents, int result) {
    }

    @Override
    protected void onFindPeerAgentsResponse(SAPeerAgent[] peerAgents, int result) {
        Log.i("communication_tizen", "onFindPeerAgentsResponse");
        if ((result == SAAgent.PEER_AGENT_FOUND) && (peerAgents != null)) {
            for(SAPeerAgent peerAgent:peerAgents)
                requestServiceConnection(peerAgent);
        } else if (result == SAAgent.FINDPEER_DEVICE_NOT_CONNECTED) {
            updateStatus("ERROR");
            gotError("Watch not found");
        } else if (result == SAAgent.FINDPEER_SERVICE_NOT_FOUND) {
            updateStatus("ERROR");
            gotError("App not found on the watch");
        } else {
            updateStatus("ERROR");
            gotError("No peers found");
        }
    }

    @Override
    protected void onServiceConnectionRequested(SAPeerAgent peerAgent) {
        Log.i("communication_tizen", "onServiceConnectionRequested");
        if (peerAgent != null) {
            acceptServiceConnectionRequest(peerAgent);
        }
    }

    @Override
    protected void onServiceConnectionResponse(SAPeerAgent peerAgent, SASocket socket, int result){
        Log.i("communication_tizen", "onServiceConnectionResponse: " + result);
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
                gotError("Connection failed: " + result);
        }
    }

    @Override
    protected void onError(SAPeerAgent peerAgent, String errorMessage, int errorCode) {
        Log.i("communication_tizen", "onError: " + errorCode);
        super.onError(peerAgent, errorMessage, errorCode);
        gotError(errorMessage);
    }

    public class ServiceConnection extends SASocket {
        public ServiceConnection() {
            super(ServiceConnection.class.getName());
            Log.i("communication_tizen", "ServiceConnection.ServiceConnection");
        }

        @Override
        public void onError(int channelId, String errorMessage, int errorCode) {
            Log.i("communication_tizen", "ServiceConnection.onError: " + errorCode);
            gotError(errorCode + ": "  + errorMessage);
        }

        @Override
        public void onReceive(int channelId, byte[] data) {
            try {
                String sData = new String(data);
                Log.i("communication_tizen", "ServiceConnection.onReceive: " + sData);
                JSONObject responseMessage = new JSONObject(sData);
                gotResponse(responseMessage);
            }catch(Exception e){
                gotError("Response json error: " + e.getMessage());
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

    public void findPeers() {
        Log.i("communication_tizen", "findPeers");
        updateStatus("FINDING_PEERS");
        findPeerAgents();
    }

    public void sendRequest(final String requestType, final JSONObject requestData) {
        if (mConnectionHandler == null) {
            gotError("Not connected");
        }
        JSONObject requestMessage = new JSONObject();
        try {
            requestMessage.put("requestType", requestType);
            requestMessage.put("requestData", requestData);
        }catch(Exception e){
            gotError("Issue with json: " + e.getMessage());
            return;
        }
        Log.i("communication_tizen", "sendRequest: " + requestMessage.toString());
        try {
            mConnectionHandler.send(getServiceChannelId(0), requestMessage.toString().getBytes());
        } catch (IOException e) {
            gotError("Issue with sending request: " + e.getMessage());
        }
    }

    public void closeConnection() {
        Log.i("communication_tizen", "closeConnection");
        if (mConnectionHandler != null) {
            mConnectionHandler.close();
            mConnectionHandler = null;
        }
    }

    private void updateStatus(final String newstatus) {
        Log.i("communication_tizen", "updateStatus: " + newstatus);
        this.status = newstatus;
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intentType", "updateStatus");
        intent.putExtra("source", "tizen");
        intent.putExtra("newstatus", newstatus);
        getApplicationContext().sendBroadcast(intent);
    }
    private void gotError(final String error) {
        Log.e("communication_tizen", "gotError: " + error);
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intentType", "gotError");
        intent.putExtra("source", "tizen");
        intent.putExtra("error", error);
        getApplicationContext().sendBroadcast(intent);
    }
    private void gotResponse(final JSONObject responseMessage){
        try {
            Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
            intent.putExtra("intentType", "gotResponse");
            intent.putExtra("source", "tizen");
            intent.putExtra("requestType", responseMessage.getString("requestType"));
            intent.putExtra("responseData", responseMessage.getString("responseData"));
            getApplicationContext().sendBroadcast(intent);
        }catch(Exception e){
            Log.e("communication_tizen", "gotResponse error: " + e.getMessage());
            gotError("Invalid response");
        }
    }
}