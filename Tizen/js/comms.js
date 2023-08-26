/* global settings, matches, incomingSettings, file_deletedMatches, getSettings, syncCustomMatchTypes */
/* exported comms_start, comms_stop */
var RRW_UUID = "8b16601b-5c76-4151-a930-2752849f4552";

var comms_adapter = tizen.bluetooth.getDefaultAdapter();
var comms_socket = null;
var comms_comms_ping_timeout;

function comms_ping(){
	if(!settings.bluetooth){
		comms_stop();
		return;
	}
	if(!comms_adapter.powered || comms_socket !== null){
		comms_comms_ping_timeout = setTimeout(comms_ping, 1000);
		return;
	}
	comms_adapter.getKnownDevices(comms_onGotDevices, function(e){
		console.log("comms_connect error", e);
	});
}
function comms_start(){
	comms_ping();
}
function comms_stop(){
	clearTimeout(comms_comms_ping_timeout);
	if(comms_socket !== null){
		comms_socket.close();
		comms_socket = null;
	}
}

function comms_onGotDevices(devices){
	for(var i=0; i<devices.length; i++){
		if(devices[i].isConnected){
			devices[i].connectToServiceByUUID(RRW_UUID, comms_onsocket, comms_onsocketerror);
		}
	}
}
function comms_onsocket(socket){
	comms_socket = socket;
	comms_socket.onmessage = function(){comms_onmessage();};
	comms_socket.onclose = function(){comms_socket = null;};
}
function comms_onsocketerror(e){
	if(e.code === 8){
		console.log("connectToServiceByUUID not found");
		return;
	}
	console.log("connectToServiceByUUID error", e);
}
function comms_onmessage(){
	var data = comms_socket.readData();
	var request = "";
	for(var i=0; i<data.length; i++){
		request += String.fromCharCode(data[i]);
	}

	var request_js = JSON.parse(request);
	var responseData = "";
	switch(request_js.requestType){
		case "sync":
			file_deletedMatches(request_js.requestData);
			var temp = {};
			temp.matches = matches;
			temp.settings = getSettings();
			responseData = JSON.stringify(temp);
			syncCustomMatchTypes(request_js.requestData);
			break;
		case "prepare":
			if(incomingSettings(request_js.requestData)){
				responseData = '"okilly dokilly"';
			}else{
				responseData = '"match ongoing"';
			}
			break;
		default:
			responseData = '"unknown requestType"';
			break;
	}
	var responseMessage = '{"requestType":"' + request_js.requestType + '","responseData":' + responseData + '}';
	comms_sendResponse(responseMessage);
}
function comms_sendResponse(responseMessage){
	var buffer = [];
	for(var i=0; i<responseMessage.length; i++){
		buffer.push(responseMessage.charCodeAt(i));
	}
	
	if(comms_socket === null){return;}
	comms_socket.writeData(buffer);
}
