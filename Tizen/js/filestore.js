/* global getCurrentTimestamp, settingsRead, noStoredSettings, customMatchTypesRead */
/* exported file_init, file_storeMatch, file_deletedMatches, file_storeSettings, file_storeCustomMatchTypes */
var useFileHandle = typeof(tizen.filesystem.openFile) === "function";
var dirname = "wgt-private";
var matches_filename = "matches.json";
var matches_path = dirname + "/" + matches_filename;
var file_matches;
var matches = [];

var settings_filename = "settings.json";
var settings_path = dirname + "/" + settings_filename;
var file_settings;

var match_types_filename = "settings.json";
var match_types_path = dirname + "/" + match_types_filename;
var file_match_types;

function file_init(){
	if(useFileHandle){
		file_readSettings();
		file_readCustomMatchTypes();
		file_cleanMatches();
	}else{
		try{
			tizen.filesystem.resolve(dirname,
				function(dir){
				tizen.filesystem.resolve(settings_path,
						function(foundfile){
							file_settings = foundfile;
							file_readSettings();
						},
						function(){
							file_settings = dir.createFile(settings_filename);
							if(typeof(file_settings) === "undefined"){
								console.log("file_init failed to create settings file");
							}
							noStoredSettings();
						},
						"rw"
					);
					tizen.filesystem.resolve(match_types_path,
						function(foundfile){
							file_match_types = foundfile;
							file_readCustomMatchTypes();
						},
						function(){
							file_match_types = dir.createFile(match_types_filename);
							if(typeof(file_match_types) === "undefined"){
								console.log("file_init failed to create match settings file");
							}
						},
						"rw"
					);
					tizen.filesystem.resolve(matches_path,
						function(foundfile){
							file_matches = foundfile;
							file_cleanMatches();
						},
						function(){
							file_matches = dir.createFile(matches_filename);
							file_cleanMatches();
							if(typeof(file_matches) === "undefined"){
								console.log("file_init failed to create matches file");
							}
						},
						"rw"
					);
				},
				function(e){
					console.log("file_init error " + e.message);
				},
				"rw"
			);
		}catch(e){
			console.log("file_init exception " + e.message);
		}
	}
}

//Append a new match
function file_storeMatch(match){
	file_readMatches(function(newmatches){
		newmatches.push(JSON.parse(JSON.stringify(match)));
		file_storeMatches(newmatches);
	});
}

function file_storeMatches(newmatches){
	matches = newmatches;

	if(useFileHandle){
		try {
			file_matches = tizen.filesystem.openFile(matches_path, "w");
			file_matches.writeString(JSON.stringify(newmatches));
			file_matches.close();
		}catch(e){
			console.log("file_storeMatches exception " + e.message);
		}
		return;
	}

	if(typeof(file_matches) === "undefined"){return;}
	try {
		file_matches.openStream(
			"w",
			function(fs){
				fs.write(JSON.stringify(newmatches));
				fs.close();
			},
			function(e){
				console.log("file_storeMatches error " + e.message);
				return;
			}
		);
	}catch(e){
		console.log("file_storeMatches exception " + e.message);
		return;
	}
}

//Return an array of stored matches
function file_readMatches(callback){
	if(useFileHandle){
		var str = "";
		try {
			var tempfile = tizen.filesystem.openFile(matches_path, "r");
			str = tempfile.readString();
			tempfile.close();
		}catch(e){
			console.log("file_readMatches exception " + e.message);
		}
		if(str.length > 10){
			matches = JSON.parse(str);
			callback(JSON.parse(str));
		}else{
			callback([]);
		}
		return;
	}
	
	if(typeof(file_matches) === "undefined"){callback([]);}
	try {
		file_matches.readAsText(
			function(str){
				if(str.length > 10){
					matches = JSON.parse(str);
					callback(JSON.parse(str));
				}else{
					callback([]);
				}
			},
			function(e){
				console.log("file_readMatches error " + e.message);
				callback([]);
			}
		);
	}catch(e){
		console.log("file_readMatches exception " + e.message);
		callback([]);
	}
}

//Go through stored matches and remove old ones 
function file_cleanMatches(){
	file_readMatches(function(newmatches){
		for(var i = newmatches.length-1; i >=0; i--){
			if(newmatches[i].matchid < getCurrentTimestamp() - (1000*60*60*24*14)){
				newmatches.splice(i, 1);
			}
		}
		file_storeMatches(newmatches);
	});
	
}

//The phone sends a list of matches that can be deleted
function file_deletedMatches(requestData){
	if(typeof(requestData) === "undefined" || typeof(requestData.deleted_matches) === "undefined"){return;}
	
	var deleted_matches = requestData.deleted_matches;
	if(deleted_matches.length < 1){return;}
	for(var i = 0; i < deleted_matches.length; i++){
		for(var y = matches.length-1; y >=0; y--){
			if(matches[y].matchid === deleted_matches[i]){
				matches.splice(y, 1);
			}
		}
	}
	file_storeMatches(matches);
}

function file_storeSettings(newsettings){
	if(useFileHandle){
		try {
			file_settings = tizen.filesystem.openFile(settings_path, "w");
			file_settings.writeString(JSON.stringify(newsettings));
			file_settings.close();
		}catch(e){
			console.log("file_storeSettings exception " + e.message);
		}
		return;
	}

	if(typeof(file_settings) === "undefined"){return;}
	try {
		file_settings.openStream(
			"w",
			function(fs){
				fs.write(JSON.stringify(newsettings));
				fs.close();
			},
			function(e){
				console.log("file_storeSettings error " + e.message);
				return;
			}
		);
	}catch(e){
		console.log("file_storeSettings exception " + e.message);
		return;
	}
}

function file_readSettings(){
	if(useFileHandle){
		try {
			file_settings = tizen.filesystem.openFile(settings_path, "r");
			var str = file_settings.readString();
			if(str.length > 10){
				settingsRead(JSON.parse(str));
			}else{
				noStoredSettings();
			}
			file_settings.close();
		}catch(e){
			console.log("file_readSettings exception " + e.message);
			noStoredSettings();
		}
		return;
	}

	if(typeof(file_settings) === "undefined"){
		noStoredSettings();
		return;
	}
	try {
		file_settings.readAsText(
			function(str){
				if(str.length > 10){
					settingsRead(JSON.parse(str));
					return;
				}
			},
			function(e){
				console.log("file_readSettings error " + e.message);
			}
		);
	}catch(e){
		console.log("file_readSettings exception " + e.message);
	}
	noStoredSettings();
}

function file_storeCustomMatchTypes(newcustom_match_types){
	if(useFileHandle){
		try {
			file_match_types = tizen.filesystem.openFile(match_types_path, "w");
			file_match_types.writeString(JSON.stringify(newcustom_match_types));
			file_match_types.close();
		}catch(e){
			console.log("file_storeCustomMatchTypes exception " + e.message);
		}
		return;
	}

	if(typeof(file_match_types) === "undefined"){return;}
	try {
		file_match_types.openStream(
			"w",
			function(fs){
				fs.write(JSON.stringify(match_types_path));
				fs.close();
			},
			function(e){
				console.log("file_storeCustomMatchTypes error " + e.message);
				return;
			}
		);
	}catch(e){
		console.log("file_storeCustomMatchTypes exception " + e.message);
		return;
	}
}
function file_readCustomMatchTypes(){
	if(useFileHandle){
		try {
			file_match_types = tizen.filesystem.openFile(match_types_path, "r");
			var str = file_match_types.readString();
			if(str.length > 10){
				customMatchTypesRead(JSON.parse(str));
			}
			file_match_types.close();
		}catch(e){
			console.log("file_readCustomMatchTypes exception " + e.message);
		}
		return;
	}

	if(typeof(file_match_types) === "undefined"){return;}
	try {
		file_match_types.readAsText(
			function(str){
				if(str.length > 10){
					customMatchTypesRead(JSON.parse(str));
				}
			},
			function(e){
				console.log("file_readCustomMatchTypes error " + e.message);
			}
		);
	}catch(e){
		console.log("file_readCustomMatchTypes exception " + e.message);
	}
}
