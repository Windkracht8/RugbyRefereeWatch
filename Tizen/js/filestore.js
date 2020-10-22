/* global getCurrentTimestamp */
/* exported file_storeMatch, file_deletedMatches */
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
	if(typeof(file) === "undefined"){return;}
	try {
		//TODO: File.openStream() is deprecated since Tizen 5.0. Use FileHandle interface to read/write operations instead.
		file.openStream(
			"w",
			function(fs){
				//TODO: FileStream.write() is deprecated since Tizen 5.0. Use FileHandle.writeString() or FileHandle.writeStringNonBlocking() instead.
				fs.write(JSON.stringify(newmatches));
				//TODO: FileStream.close() is deprecated since Tizen 5.0. Use FileHandle.close() instead.
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
	if(typeof(file) === "undefined"){callback([]);}
	try {
		//TODO: File.readAsText() is deprecated since Tizen 5.0. Use FileHandle.readString() or FileHandle.readStringNonBlocking() instead.
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
	try{
		//TODO: FileSystemManager.resolve() is deprecated since Tizen 5.0. Use FileHandle and FileSystemManager interfaces instead.
		tizen.filesystem.resolve(matches_dirname,
			function(dir){
				console.log("file_getFile found dir");
				//TODO: FileSystemManager.resolve() is deprecated since Tizen 5.0. Use FileHandle and FileSystemManager interfaces instead.
				tizen.filesystem.resolve(matches_path,
					function(foundfile){
						console.log("file_getFile found file");
						file = foundfile;
						file_cleanMatches();
					},
					function(){
						console.log("file_getFile create file");
						//TODO: File.createFile() is deprecated since Tizen 5.0. Use FileSystemManager.createFile() instead.
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
