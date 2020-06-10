package com.windkracht8.rugbyrefereewatch;

import java.io.IOException;

import android.content.Intent;
import android.os.Handler;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.samsung.android.sdk.accessory.*;

import org.json.JSONObject;

public class communication extends SAAgent {
    public enum Status {
        DISCONNECTED,
        ERROR,
        CONNECTION_LOST,
        FINDING_PEERS,
        CONNECTED,
        GETTING_MATCH,
        PREPARE
    }
    public Status status = Status.DISCONNECTED;

    private ServiceConnection mConnectionHandler = null;
    Handler mHandler = new Handler();
    private final IBinder mBinder = new LocalBinder();
    class LocalBinder extends Binder {
        communication getService() {
            Log.i("communication", "LocalBinder.getService");
            return communication.this;
        }
    }

    public communication() {
        super("RugbyRefereeWatch", ServiceConnection.class);
    }

    @Override
    public void onCreate() {
        Log.i("communication", "onCreate");
        super.onCreate();
        SA mAccessory = new SA();
        try {
            mAccessory.initialize(this);
        } catch (Exception e) {
            updateStatus(Status.ERROR);
            gotError("Cannot load: " + e.getMessage());
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("communication", "onBind");
        return mBinder;
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

    @Override
    protected void onPeerAgentsUpdated(SAPeerAgent[] peerAgents, int result) {
        Log.i("communication", "onPeerAgentsUpdated: " + result);
        //TODO: do we need this, it is never called
        final SAPeerAgent[] peers = peerAgents;
        final int status = result;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (peers != null) {
                    if (status == SAAgent.PEER_AGENT_AVAILABLE) {
                        Log.i("communication", "onPeerAgentsUpdated.post PEER_AGENT_AVAILABLE");
                        //Toast.makeText(getApplicationContext(), "PEER_AGENT_AVAILABLE", Toast.LENGTH_LONG).show();
                    } else {
                        Log.i("communication", "onPeerAgentsUpdated.post PEER_AGENT_UNAVAILABLE");
                        //Toast.makeText(getApplicationContext(), "PEER_AGENT_UNAVAILABLE", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
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
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                MainActivity.updateStatus();
            }
        });
    }
    private void gotError(final String error) {
        Log.e("communication", "gotError: " + error);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                MainActivity.gotError(error);
            }
        });
    }
    private void gotResponse(final JSONObject responseMessage){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                MainActivity.gotResponse(responseMessage);
            }
        });
    }
}
