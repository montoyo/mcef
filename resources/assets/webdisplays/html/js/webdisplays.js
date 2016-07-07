var wdConcat = function() {
	if(arguments.length <= 1)
		return arguments[0];
	
	var ret = arguments[0] + "$";
	for(var i = 1; i < arguments.length; i++) {
		var str = arguments[i].toString();
		var add = "";
		
		for(var j = 0; j < str.length; j++) {
			var c = str.charAt(j);
			
			if(c == "\\")
				add += "\\\\";
			else if(c == ",")
				add += "\\,";
			else
				add += c;
		}
		
		ret += add;
		if(i < arguments.length - 1)
			ret += ",";
	}
	
	return ret;
}

function wdQuery() {
	var query = "WebDisplays:" + wdConcat.apply(null, arguments);
	
	if(window.mcefQuery) {
		window.mcefQuery({
			"request": query,
			"persistent": false,
			"onSuccess": function(msg) {
			},
			
			"onFailure": function(err, msg) {
			}
		});
	} else {
		console.log(query);
	}
}

function wdCenter() {
	$("#wdContent").css("left", (window.innerWidth - 512) / 2);
	$("#wdContent").css("top", (window.innerHeight - 420) / 2);
}
