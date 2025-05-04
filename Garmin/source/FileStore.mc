import Toybox.Application.Storage;
import Toybox.Lang;
import Toybox.System;

class FileStore{
	static var match_ids as Array<Long> = [];
	static var customMatchTypes as Array<String> = [];
	static function read(){
		match_ids = Storage.getValue("match_ids") as Array<Long>;
		if(match_ids == null){match_ids = [] as Array<Long>;}
		System.println("FileStore.read match_ids: " + match_ids);
		customMatchTypes = Storage.getValue("customMatchTypes") as Array<String>;
		if(customMatchTypes == null){customMatchTypes = [] as Array<String>;}
		System.println("FileStore.read customMatchTypes: " + customMatchTypes);
	}
	static function readMatch(match_id as Long) as MatchData{
		var match = new MatchData();
		match.fromDictionary(Storage.getValue(match_id));
		return match;
	}
	(:typecheck(false))//Check fails on match_ids
	static function storeMatch(match as MatchData){
		System.println("FileStore.storeMatch " + match.match_id);
		if(match_ids.size() >= 10){delMatch(match_ids[0]);}
		Storage.setValue(match.match_id, match.toDictionary());
		match_ids.add(match.match_id);
		Storage.setValue("match_ids", match_ids);
	}
	static function delMatch(match_id as Long){
		System.println("FileStore.delMatch " + match_id);
		Storage.deleteValue(match_id);
		match_ids.remove(match_id);
		Storage.setValue("match_ids", match_ids);
	}
	static function readCustomMatchType(name as String) as Dictionary{
		return Storage.getValue(name);
	}
	static function storeCustomMatchType(customMatchType){
		System.println("FileStore.storeCustomMatchType " + customMatchType.name);
		Storage.setValue(customMatchType.name, customMatchType.toDictionary());
		if(customMatchTypes.indexOf(customMatchType.name) == -1){
			customMatchTypes.add(customMatchType.name);
		}
		Storage.setValue("customMatchTypes", customMatchTypes);
	}
	static function delCustomMatchType(customMatchType_name){
		System.println("FileStore.delCustomMatchType " + customMatchType_name);
		Storage.deleteValue(customMatchType_name);
		customMatchTypes.remove(customMatchType_name);
		Storage.setValue("customMatchTypes", customMatchTypes);
	}
}
