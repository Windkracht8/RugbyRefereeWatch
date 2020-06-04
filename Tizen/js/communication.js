var SAAgent;

var communicationListener = {
	onrequest: function(peerAgent){
		if(peerAgent.appName === "RugbyReferee"){
			SAAgent.acceptServiceConnectionRequest(peerAgent);
		}else{
			SAAgent.rejectServiceConnectionRequest(peerAgent);
		}
	},
	onconnect: function(socket){
		var dataOnReceive = function dataOnReceive(channelId, requestMessage){
			console.log("dataOnReceive", requestMessage);
			var requestMessage_js = JSON.parse(requestMessage);
			var responseMessage = "";
			switch(requestMessage_js.requestType){
				case "getMatch":
					responseMessage = '{"requestType":"getMatch","responseData":' + JSON.stringify(match) + '}';
					break;
				case "setSettings":
					if(incomingSettings(requestMessage_js.requestData)){
						responseMessage = '{"requestType":"setSettings","responseData":"okilly dokilly"}';
					}else{
						responseMessage = '{"requestType":"setSettings","responseData":"no no no"}';
					}
					break;
				default:
					showMessage("Did not understand message '" + requestMessage_js.requestType + "'");
					return;
			}
			console.log("sendData", responseMessage);
			socket.sendData(channelId, responseMessage);
		};
		socket.setDataReceiveListener(dataOnReceive);
	},
	onerror: function(errorCode){
		showMessage("Communication error: " + errorCode);
	}
}

function requestOnSuccess(agents){
	for(var i = 0; i < agents.length; i += 1) {
		if (agents[i].role === "PROVIDER") {
			SAAgent = agents[i];
			break;
		}
	}
	SAAgent.setServiceConnectionListener(communicationListener);
}

function requestOnError(e){
	showMessage("Communication error" + e.name + "<br />" + e.message);
}

webapis.sa.requestSAAgent(requestOnSuccess, requestOnError);
