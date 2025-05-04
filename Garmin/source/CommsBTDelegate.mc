/*
using Toybox.BluetoothLowEnergy as Ble;
import Toybox.Lang;

class CommsBTDelegate extends Ble.BleDelegate{
	static const RRW_UUID = Ble.stringToUuid("00001828-0000-1000-8000-00805F9B34FB");
	static const RRW_UUID_CHAR = Ble.stringToUuid("00001828-0000-1000-8000-00805F9B34FC");
	var characteristic;
	function initialize(){BleDelegate.initialize();}
	function onConnectedStateChanged(device, state){
		System.println("onConnectedStateChanged device: " + device + " state: " + state);
		if(state == Ble.CONNECTION_STATE_CONNECTED && device != null){
			var service = device.getService(RRW_UUID);
			System.println("service: " + service);
			if(service != null){
				characteristic = service.getCharacteristic(RRW_UUID);
				System.println("characteristic: " + characteristic);
				if(characteristic != null){
					//var descriptor = characteristic.getDescriptor(RRW_UUID);
					//System.println("descriptor: " + descriptor);
					characteristic.requestRead();
				}
			}
		}else{//disconnected
		}
	}
	function onProfileRegister(uuid, status){
		System.println("onProfileRegister uuid: " + uuid + " status: " + status);
	}
	function onCharacteristicRead(characteristic, status, value){
		System.println("onCharacteristicRead characteristic: " + characteristic + " status: " + status + " value: " + value);
		//TODO wait for next chunk, combine into one message
	}
	function onCharacteristicWrite(characteristic, status){
		System.println("onCharacteristicWrite characteristic: " + characteristic + " status: " + status);
		//TODO continue to write next 20 byte chunk
	}
	function onDescriptorRead(descriptor, status, value){
		System.println("onDescriptorRead descriptor: " + descriptor + " status: " + status + " value: " + value);
	}
	function onDescriptorWrite(descriptor, status){
		System.println("onDescriptorWrite descriptor: " + descriptor + " status: " + status);
	}

	function sendResponse(response){
		System.println("CommsBTDelegate.sendResponse: " + response);
		//TODO send in chunks of 20 bytes
		var response_bytes = toByteArray(response);
		characteristic.requestWrite(response_bytes, {:writeType => Ble.WRITE_TYPE_DEFAULT});
	}
	function toByteArray(input) as Lang.ByteArray{
		var byteArray = []b;
		byteArray.addAll(input.toCharArray());
		return byteArray;
	}

	function gotRequest(request){
		System.println("CommsBTDelegate.gotRequest: " + request + "\n");
		var requestType = Utils.getJsonString("requestType", request, "");
		if(requestType == ""){
			System.println("CommsBTDelegate.gotRequest no requestType");
			sendResponse("{\"requestType\":\"unknown\",\"responseData\":\"no requestType\"}");
			return;
		}
		if(request.find("requestData") == null){
			System.println("CommsBTDelegate.gotRequest no requestData");
			sendResponse("{\"requestType\":\"" + requestType + "\",\"responseData\":\"no requestData\"}");
			return;
		}
		switch(requestType){
			case "sync":
				onReceiveSync(request);
				break;
			case "prepare":
				onReceivePrepare(request);
				break;
			default:
				System.println("CommsBTDelegate.gotRequest Unknown requestType: " + requestType);
				sendResponse( "{\"requestType\":\"" + requestType + "\",\"responseData\":\"unknown requestType\"}");
		}
	}
	function onReceiveSync(requestData){
		//System.println("CommsBTDelegate.onReceiveSync: " + requestData);
		var deleted_matches = Utils.getJsonArray("deleted_matches", requestData);
		while(deleted_matches.length() > 0){
			var end = deleted_matches.find(",");
			FileStore.delMatch(deleted_matches.substring(0, end == null ? deleted_matches.length() : end).toLong());
			if(end == null){
				break;
			}else{
				deleted_matches = deleted_matches.substring(end+1, deleted_matches.length());
			}
		}
		var custom_match_types = Utils.getJsonArray("custom_match_types", requestData);
		var custom_match_types_array = [];
		while(custom_match_types.length() > 0){
			var start = custom_match_types.find("{");
			var end = custom_match_types.find("}");
			custom_match_types_array.add(new CustomMatchType(custom_match_types.substring(start+1, end == null ? custom_match_types.length() : end)));
			if(end == null){
				break;
			}else{
				custom_match_types = custom_match_types.substring(end+1, custom_match_types.length());
			}
		}
		for(var iw=FileStore.customMatchTypes.size()-1; iw>=0; iw--){
			delCustomMatchTypeIf(FileStore.customMatchTypes[iw], custom_match_types_array);
		}
		for(var i=0; i<custom_match_types_array.size(); i++){
			FileStore.storeCustomMatchType(custom_match_types_array[i]);
		}
		sendResponse(buildSyncResponse());
	}
	function delCustomMatchTypeIf(name, custom_match_types_array as Array<CustomMatchType>){
		for(var ip=0; ip<custom_match_types_array.size(); ip++){
			if(name.hashCode() == custom_match_types_array[ip].name.hashCode()){
				return;
			}
		}
		FileStore.delCustomMatchType(name);
	}
	(:typecheck(false))//Check fails on FileStore.match_ids
	function buildSyncResponse() as Lang.String{
		var syncResponse = "{\"requestType\":\"sync\",\"responseData\":{\"matches\":[";
		for(var i=0; i<FileStore.match_ids.size(); i++){
			if(i>0){syncResponse += ",";}
			syncResponse += FileStore.readMatch(FileStore.match_ids[i]).toJson();
		}
		syncResponse += "],\"settings\":{";
		syncResponse += "\"home_name\":\"" + MainView.main.match.home.team + "\",";
		syncResponse += "\"home_color\":\"" + MainView.main.match.home.color + "\",";
		syncResponse += "\"away_name\":\"" + MainView.main.match.away.team + "\",";
		syncResponse += "\"away_color\":\"" + MainView.main.match.away.color + "\",";
		syncResponse += "\"match_type\":\"" + MainView.main.match.match_type + "\",";
		syncResponse += "\"period_time\":\"" + MainView.main.match.period_time + "\",";
		syncResponse += "\"period_count\":\"" + MainView.main.match.period_count + "\",";
		syncResponse += "\"period_time\":\"" + MainView.main.match.period_time + "\",";
		syncResponse += "\"sinbin\":\"" + MainView.main.match.sinbin + "\",";
		syncResponse += "\"points_try\":\"" + MainView.main.match.points_try + "\",";
		syncResponse += "\"points_con\":\"" + MainView.main.match.points_con + "\",";
		syncResponse += "\"points_goal\":\"" + MainView.main.match.points_goal + "\",";
		syncResponse += "\"clock_pk\":\"" + MainView.main.match.clock_pk + "\",";
		syncResponse += "\"clock_con\":\"" + MainView.main.match.clock_con + "\",";
		syncResponse += "\"clock_restart\":\"" + MainView.main.match.clock_restart + "\",";
		syncResponse += "\"screen_on\":\"" + MainView.main.screen_on + "\",";
		syncResponse += "\"timer_type\":\"" + MainView.main.timer_type + "\",";
		syncResponse += "\"record_player\":\"" + MainView.main.record_player + "\",";
		syncResponse += "\"record_pens\":\"" + MainView.main.record_pens + "\",";
		syncResponse += "\"delay_end\":\"" + MainView.main.delay_end + "\"";
		syncResponse += "}}}";
		return syncResponse;
	}

	function onReceivePrepare(requestData){
		if(MainView.main.timer_status != STATUS_CONF){
			sendResponse("{\"requestType\":\"prepare\",\"responseData\":\"match ongoing\"}");
			return;
		}
		MainView.main.match.home.team = Utils.getJsonString("home_name", requestData, "home");
		MainView.main.match.home.color = Utils.getJsonString("home_color", requestData, "green");
		MainView.main.match.away.team = Utils.getJsonString("away_name", requestData, "away");
		MainView.main.match.away.color = Utils.getJsonString("away_color", requestData, "blue");
		MainView.main.match.match_type = Utils.getJsonString("match_type", requestData, "custom");
		MainView.main.match.period_time = Utils.getJsonNumber("period_time", requestData);
		MainView.main.timer_period_time = MainView.main.match.period_time*60;
		MainView.main.match.period_count = Utils.getJsonNumber("period_count", requestData);
		MainView.main.match.sinbin = Utils.getJsonNumber("sinbin", requestData);
		MainView.main.match.points_try = Utils.getJsonNumber("points_try", requestData);
		MainView.main.match.points_con = Utils.getJsonNumber("points_con", requestData);
		MainView.main.match.points_goal = Utils.getJsonNumber("points_goal", requestData);
		MainView.main.match.clock_pk = Utils.getJsonNumber("clock_pk", requestData);
		MainView.main.match.clock_con = Utils.getJsonNumber("clock_con", requestData);
		MainView.main.match.clock_restart = Utils.getJsonNumber("clock_restart", requestData);
		sendResponse("{\"requestType\":\"prepare\",\"responseData\":\"okilly dokilly\"}");
		MainView.main.updateAfterConfig();
	}
}
*/