package com.windkracht8.rugbyrefereewatch;

import java.io.IOException;
import android.content.Context;
import android.util.Log;
import com.samsung.android.sdk.accessory.*;
import org.json.JSONObject;

public class communication extends SAAgentV2 {
    public enum Status {
        DISCONNECTED,
        ERROR,
        FATAL,
        CONNECTION_LOST,
        FINDING_PEERS,
        CONNECTED,
        GETTING_MATCHES,
        GETTING_MATCH,
        PREPARE
    }
    public Status status = Status.DISCONNECTED;

    private ServiceConnection mConnectionHandler = null;

    public communication(Context context) {
        super("RugbyRefereeWatch", context, ServiceConnection.class);
        SA mAccessory = new SA();
        try {
            mAccessory.initialize(context);
        } catch (Exception e) {
            Log.e("communication", "construct: " + e);
            updateStatus(Status.FATAL);
            gotError(e.getMessage());
            releaseAgent();
        }
    }

    @Override
    protected void onPeerAgentsUpdated(SAPeerAgent[] peerAgents, int result) {
    }

    @Override
    protected void onFindPeerAgentsResponse(SAPeerAgent[] peerAgents, int result) {
        Log.i("communication", "onFindPeerAgentsResponse");
        if ((result == SAAgent.PEER_AGENT_FOUND) && (peerAgents != null)) {
            for(SAPeerAgent peerAgent:peerAgents)
                requestServiceConnection(peerAgent);
        } else if (result == SAAgent.FINDPEER_DEVICE_NOT_CONNECTED) {
            updateStatus(Status.ERROR);
            gotError("Watch not found");
        } else if (result == SAAgent.FINDPEER_SERVICE_NOT_FOUND) {
            updateStatus(Status.ERROR);
            gotError("App not found on the watch");
        } else {
            updateStatus(Status.ERROR);
            gotError("No peers found");
        }
    }

    @Override
    protected void onServiceConnectionRequested(SAPeerAgent peerAgent) {
        Log.i("communication", "onServiceConnectionRequested");
        if (peerAgent != null) {
            acceptServiceConnectionRequest(peerAgent);
        }
    }

    @Override
    protected void onServiceConnectionResponse(SAPeerAgent peerAgent, SASocket socket, int result){
        Log.i("communication", "onServiceConnectionResponse: " + result);
        switch(result){
            case SAAgent.CONNECTION_SUCCESS:
                this.mConnectionHandler = (ServiceConnection) socket;
                updateStatus(Status.CONNECTED);
                break;
            case SAAgent.CONNECTION_ALREADY_EXIST:
            case SAAgent.CONNECTION_DUPLICATE_REQUEST:
                break;
            default:
                updateStatus(Status.ERROR);
                gotError("Connection failed: " + result);
        }
    }

    @Override
    protected void onError(SAPeerAgent peerAgent, String errorMessage, int errorCode) {
        Log.i("communication", "onError: " + errorCode);
        super.onError(peerAgent, errorMessage, errorCode);
        gotError(errorMessage);
    }

    public class ServiceConnection extends SASocket {
        public ServiceConnection() {
            super(ServiceConnection.class.getName());
            Log.i("communication", "ServiceConnection.ServiceConnection");
        }

        @Override
        public void onError(int channelId, String errorMessage, int errorCode) {
            Log.i("communication", "ServiceConnection.onError: " + errorCode);
            gotError(errorCode + ": "  + errorMessage);
        }

        @Override
        public void onReceive(int channelId, byte[] data) {
            try {
                String sData = new String(data);
                Log.i("communication", "ServiceConnection.onReceive: " + sData);
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
            updateStatus(Status.CONNECTION_LOST);
            closeConnection();
        }
    }

    public void findPeers() {
        Log.i("communication", "findPeers");
        updateStatus(Status.FINDING_PEERS);
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
        Log.i("communication", "sendRequest: " + requestMessage.toString());
        try {
            mConnectionHandler.send(getServiceChannelId(0), requestMessage.toString().getBytes());
        } catch (IOException e) {
            gotError("Issue with sending request: " + e.getMessage());
        }
    }

    public void closeConnection() {
        Log.i("communication", "closeConnection");
        if (mConnectionHandler != null) {
            mConnectionHandler.close();
            mConnectionHandler = null;
        }
    }

    private void updateStatus(final Status newstatus) {
        Log.i("communication", "updateStatus: " + newstatus);
        this.status = newstatus;
        MainActivity.updateStatus_runOnUiThread(newstatus);
    }
    private void gotError(final String error) {
        Log.e("communication", "gotError: " + error);
        MainActivity.gotError_runOnUiThread(error);
    }
    private void gotResponse(final JSONObject responseMessage){
        MainActivity.gotResponse_runOnUiThread(responseMessage);
    }
}
