/* global match, matches, showMessage, incomingSettings, file_deletedMatches, getSettings, syncCustomMatchTypes, timer */
var SAAgent;

var communicationListener = {
	onrequest: function(peerAgent){
		if(peerAgent.appName === "RugbyRefereeWatch"){
			SAAgent.acceptServiceConnectionRequest(peerAgent);
		}else{
			SAAgent.rejectServiceConnectionRequest(peerAgent);
		}
	},
	onconnect: function(socket){
		var dataOnReceive = function dataOnReceive(channelId, requestMessage){
			console.log("dataOnReceive", requestMessage);
			var requestMessage_js = JSON.parse(requestMessage);
			var responseData = "";
			switch(requestMessage_js.requestType){
				case "sync":
					file_deletedMatches(requestMessage_js.requestData);
					var temp = {};
					temp["matches"] = matches;
					temp["settings"] = getSettings();
					responseData = JSON.stringify(temp);
					syncCustomMatchTypes(requestMessage_js.requestData);
					break;
				case "getMatches":
					file_deletedMatches(requestMessage_js.requestData);
					responseData = JSON.stringify(matches);
					syncCustomMatchTypes(requestMessage_js.requestData);
					break;
				case "getMatch":
					var responseData_js = match;
					responseData_js.timer = timer;
					responseData = JSON.stringify(responseData_js);
					break;
				case "prepare":
					if(incomingSettings(requestMessage_js.requestData)){
						responseData = '"okilly dokilly"';
					}else{
						responseData = '"match ongoing"';
					}
					break;
				default:
					responseData = '"unknown requestType"';
					break;
			}
			var responseMessage = '{"requestType":"' + requestMessage_js.requestType +
				'","responseData":' + responseData + '}';
			console.log("sendData", responseMessage);
			socket.sendData(channelId, responseMessage);
		};
		socket.setDataReceiveListener(dataOnReceive);
	},
	onerror: function(errorCode){
		showMessage("Communication error: " + errorCode);
	}
};

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

if(typeof webapis !== "undefined"){
	webapis.sa.requestSAAgent(requestOnSuccess, requestOnError);
}
