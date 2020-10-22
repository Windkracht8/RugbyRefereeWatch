/* global getCurrentTimestamp */
/* exported file_storeMatch, file_deletedMatches */
var useFileHandle = typeof(tizen.filesystem.pathExists) === "function";
var matches_dirname = "wgt-private";
var matches_filename = "matches.json";
var matches_path = matches_dirname + "/" + matches_filename;
var file;
var matches = [];

file_getFile();


//Append a new match
function file_storeMatch(match){
	if(typeof(file) === "undefined"){return;}
	file_readMatches(function(newmatches){
		newmatches.push(JSON.parse(JSON.stringify(match)));
		file_storeMatches(newmatches);
	});
}

function file_storeMatches(newmatches){
	matches = newmatches;

	if(useFileHandle){
		try {
			file = tizen.filesystem.openFile(matches_path, "w");
			file.write(JSON.stringify(newmatches));
			file.close();
		}catch(e){
			console.log("file_storeMatches exception " + e.message);
		}
		return;
	}

	if(typeof(file) === "undefined"){return;}
	try {
		file.openStream(
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
		try {
			file = tizen.filesystem.openFile(matches_path, "r");
			var str = file.readString();
			if(str.length > 10){
				callback(JSON.parse(str));
			}else{
				callback([]);
			}
			file.close();
		}catch(e){
			console.log("file_readMatches exception " + e.message);
			callback([]);
		}
		return;
	}
	
	if(typeof(file) === "undefined"){callback([]);}
	
	try {
		file.readAsText(
			function(str){
				if(str.length > 10){
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
	if(typeof(file) === "undefined"){return;}
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

function file_getFile(){
	if(useFileHandle){return;}
	
	try{
		tizen.filesystem.resolve(matches_dirname,
			function(dir){
				tizen.filesystem.resolve(matches_path,
					function(foundfile){
						file = foundfile;
						file_cleanMatches();
					},
					function(){
						file = dir.createFile(matches_filename);
						file_cleanMatches();
						if(typeof(file) === "undefined"){
							console.log("file_getFile failed to create file");
						}
					},
					"rw"
				);
			},
			function(e){
				console.log("file_getFile error " + e.message);
			},
			"rw"
		);
	}catch(e){
		console.log("file_getFile exception " + e.message);
	}
}
