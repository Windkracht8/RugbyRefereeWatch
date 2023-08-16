/* global match, matches, incomingSettings, file_deletedMatches, getSettings, syncCustomMatchTypes, timer */
var RRW_UUID = "8b16601b-5c76-4151-a930-2752849f4552";

var comms_bt_adapter = tizen.bluetooth.getDefaultAdapter();
var comms_bt_socket = null;

setTimeout(comms_bt_ping, 1000);
function comms_bt_ping(){
	if(timer.status !== "conf" && comms_bt_socket !== null){
		comms_bt_socket.close();
		comms_bt_socket = null;
		setTimeout(comms_bt_ping, 1000);
		return;
	}
	if(!comms_bt_adapter.powered || comms_bt_socket !== null || timer.status !== "conf"){
		setTimeout(comms_bt_ping, 1000);
		return;
	}
	comms_bt_adapter.getKnownDevices(comms_bt_onGotDevices, function(e){
		console.log("comms_bt_connect error", e);
	});
}
function comms_bt_onGotDevices(devices){
	console.log("comms_bt_onGotDevices: ", devices);
	for(var i=0; i<devices.length; i++){
		if(devices[i].isConnected){
			devices[i].connectToServiceByUUID(RRW_UUID, comms_bt_onsocket, comms_bt_onsocketerror);
		}
	}
}
function comms_bt_onsocket(socket){
	console.log('socket connected');
	comms_bt_socket = socket;
	comms_bt_socket.onmessage = function(){comms_bt_onmessage();};
	comms_bt_socket.onclose = function(){comms_bt_socket = null;};
}
function comms_bt_onsocketerror(e){
	if(e.code === 8){
		console.log("connectToServiceByUUID not found");
		return;
	}
	console.log("connectToServiceByUUID error", e);
}
function comms_bt_onmessage(){
	console.log("comms_bt_onmessage");
	var data = comms_bt_socket.readData();
	var request = "";
	for(var i=0; i<data.length; i++){
		request += String.fromCharCode(data[i]);
	}
	console.log("comms_bt_onmessage", request);

	var request_js = JSON.parse(request);
	var responseData = "";
	switch(request_js.requestType){
		case "sync":
			file_deletedMatches(request_js.requestData);
			var temp = {};
			temp["matches"] = matches;
			temp["settings"] = getSettings();
			responseData = JSON.stringify(temp);
			syncCustomMatchTypes(request_js.requestData);
			break;
		case "getMatches":
			file_deletedMatches(request_js.requestData);
			responseData = JSON.stringify(matches);
			syncCustomMatchTypes(request_js.requestData);
			break;
		case "getMatch":
			var responseData_js = match;
			responseData_js.timer = timer;
			responseData = JSON.stringify(responseData_js);
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
	comms_bt_sendResponse(responseMessage);
}
function comms_bt_sendResponse(responseMessage){
	console.log("comms_bt_sendResponse", responseMessage);
	
	var buffer = [];
	for(var i=0; i<responseMessage.length; i++){
		buffer.push(responseMessage.charCodeAt(i));
	}
	
	if(comms_bt_socket === null){return;}
	comms_bt_socket.writeData(buffer);
}
