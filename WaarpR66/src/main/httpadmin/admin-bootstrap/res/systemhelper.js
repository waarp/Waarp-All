/**
 * Rule Helper
 */
// Source Input field name
var dialog_inputName = '';
// Current tasks array
var arrayTasks = new Array();
// Available Task's types
var availableTasks = new Array("LOG","SNMP","MOVE","MOVERENAME","COPY","COPYRENAME","LINKRENAME","RENAME","DELETE","VALIDFILEPATH","CHKFILE","TRANSCODE","TAR","ZIP","UNZEROED","CHMOD","EXEC","EXECMOVE","EXECOUTPUT","EXECJAVA","TRANSFER","FTP","RESCHEDULE","RESTART");
var titles= new Array("Task Type","Path or argument","Delay","Comment (optional)","Controls");
var thead=$("<thead/>"); var tr=$('<tr/>');$.each(titles, function(col, value){tr.append($("<th/>").text(value));}); thead.append(tr);
// Load dialog
function dialog_load() {
	var table=$("#tblGrid");
	table.empty();
	table.append(thead);
	var tbody=$("<tbody/>");$.each(arrayTasks, function(rowIndex, r) {
		var row = $("<tr/>");
		$.each(r, function(colIndex, c) {
			switch (colIndex) {
			case 0:
				var tselect=$("<select/>").addClass("form-control input-sm").attr("id", "type"+rowIndex).attr("onChange", "dialog_typeSelect("+rowIndex+" )");
				$.each(availableTasks, function(col, value){
					if (value==c){tselect.append($("<option/>").prop('selected', true).text(value));}
					else{tselect.append($("<option/>").text(value));}});
				row.append($("<td/>").append(tselect));
				break;
			case 1:
				row.append($("<td/>").append($("<input/>").addClass("form-control input-sm").attr("id", "path"+rowIndex).attr("type","text").attr("size","20").attr("placeholder","Path or argument").attr("onChange", "dialog_typeInput("+rowIndex+",1)").val(c)));
				break;
			case 2:
				row.append($("<td/>").append($("<input/>").addClass("form-control input-sm").attr("id", "delay"+rowIndex).attr("type","number").attr("size","6").attr("onChange", "dialog_typeInput("+rowIndex+",2)").val(c)));
				break;
			case 3:
				row.append($("<td/>").append($("<input/>").addClass("form-control input-sm").attr("id", "comment"+rowIndex).attr("type","text").attr("size","20").attr("placeholder","Comment").attr("onChange", "dialog_typeInput("+rowIndex+",3)").val(c)));
				break;
			}
		});
		if (r.length == 3) {
			row.append($("<td/>").append($("<input/>").addClass("form-control input-sm").attr("id", "comment"+rowIndex).attr("type","text").attr("size","20").attr("placeholder","Comment").attr("onChange", "dialog_typeInput("+rowIndex+",3)")));
		}
		row.append($("<td/>").append($("<button/>").addClass("btn btn-primary btn-sm").attr("id", "add"+rowIndex).attr("onClick","dialog_add("+rowIndex+")").text("Add before"))
				.append($("<button/>").addClass("btn btn-danger btn-sm").attr("id", "del"+rowIndex).attr("onclick","dialog_delete("+rowIndex+")").text("Remove")));
		tbody.append(row);
	});table.append(tbody);
}
// Select value from SELECT
function dialog_typeSelect(rank) {arrayTasks[rank][0]=$("#type"+rank).val();}
// Select value from INPUT
function dialog_typeInput(rank, field) {
	var fieldname = '';if (field == 1) { fieldname = "#path"+rank; } else if (field == 2) { fieldname = "#delay"+rank; } else { fieldname = "#comment"+rank; }
	arrayTasks[rank][field]=$(fieldname).val();
}
/*******************************************************************************
 * inputName : field name that contains the value of rule tasks
 ******************************************************************************/
function dialog_open(inputName) {
	dialog_inputName = inputName;
	if (inputName.charAt(0) == 'R') { code = " Recv "; } else { code = " Send "; }
	if (inputName.charAt(1) == 'E') { code = code + " Error"; } else if (inputName.charAt(2) == 'O') { code = code + " Post"; } else { code = code + " Pre"; }
	$("#titlemodal").text(code+" Tasks Editor");
	var val = $("#"+inputName).val();
	var parser = new marknote.Parser();var xml = parser.parse(val);var root = xml.getRootElement();var tasks = root.getChildElements();arrayTasks = new Array(tasks.length);
	for ( var i = 0; i < tasks.length; i++) {
		var task = tasks[i];var type = task.getChildElement("type");var path = task.getChildElement("path");var delay = task.getChildElement("delay");var comment = task.getChildElement("comment");if (comment) { arrayTasks[i] = new Array(type.getText(), path.getText(), delay.getText(), comment.getText()); } else { arrayTasks[i] = new Array(type.getText(), path.getText(), delay.getText(), ""); }
	}
	dialog_load();
	$('#myModal').modal('show');
}
// Add a new Task
function dialog_add(rank) {
	var tempElt = new Array("LOG",'','0','');if (rank < 0) {arrayTasks.push(tempElt);} else if (rank == 0) { arrayTasks.unshift(tempElt); } else if (rank < arrayTasks.length) { var before = arrayTasks.slice(0, rank);before.push(tempElt);arrayTasks = before.concat(arrayTasks.slice(rank, arrayTasks.length)); } else { arrayTasks.push(tempElt); }
	dialog_load();
}
// Delete a Task
function dialog_delete(rank) {
	var max = arrayTasks.length-1;if (rank == 0) { arrayTasks.shift(); } else if (rank < max) { var before = arrayTasks.slice(0, rank);arrayTasks = before.concat(arrayTasks.slice(rank+1, arrayTasks.length)); } else if (rank == max) { arrayTasks.pop(); }
	dialog_load();
}
// Update tasks
function dialog_selectOk() {
	var root = new marknote.Element("tasks");
	for ( var j = 0; j < arrayTasks.length; j++) {
		var task = new marknote.Element("task");
		var val = arrayTasks[j][0];
		if (val && val.length > 0) {
			var sub = new marknote.Element("type");sub.setText(val);task.addContent(sub);
			sub = new marknote.Element("path");sub.setText(arrayTasks[j][1]);task.addContent(sub);
			val = arrayTasks[j][2]; if (!val) {val = 0;}
			sub = new marknote.Element("delay");sub.setText(val);task.addContent(sub);
			val = arrayTasks[j][3];
			if (val && val.length > 0) { sub = new marknote.Element("comment");sub.setText(val);task.addContent(sub); }
			root.addContent(task);
		}
	}
	var result = root.toString("\f");var regexp = new RegExp("^\n\r|^\r\n|^\n|^\r|\f", "g");result = result.replace(regexp, "");
	$("#"+dialog_inputName).val(result);
	$('#myModal').modal("hide");
}

/**
 * Allowed Server Helper
 */
// Source Input field name
var sh_dialog_inputName = '';
// Current Business array
var sh_array = new Array();
var sh_titles= new Array("Host Id");
var sh_thead=$("<thead/>"); var bh_tr=$('<tr/>');$.each(sh_titles, function(col, value){bh_tr.append($("<th/>").text(value));}); sh_thead.append(bh_tr);
// Load dialog
function sh_dialog_load() {
	var table=$("#sh_tblGrid");
	table.empty();
	table.append(sh_thead);
	var tbody=$("<tbody/>");
	var row = $("<tr/>");
	var tselect=$("<select/>").addClass("form-control input-sm").attr("id", "sid0").attr("onChange", "sh_dialog_typeMultiSelect()").attr("multiple", "multiple");
	$.each(hostids, function(col, value){
		var found = false
		$.each(sh_array, function(rowIndex, r) {
		if (value==r){tselect.append($("<option/>").prop('selected', true).text(value)); found = true;}});
		if (! found) {tselect.append($("<option/>").text(value));};
	});
	row.append($("<td/>").append(tselect)); tselect.multiselect({maxHeight: 200});
	tbody.append(row);
	table.append(tbody);
}
// Select multiple value from SELECT
function sh_dialog_typeMultiSelect() {sh_array=$("#sid0").val();}
/*******************************************************************************
 * inputName : field name that contains the value of allowed host ids
 ******************************************************************************/
function sh_dialog_open(inputName) {
	sh_dialog_inputName = inputName;
	$("#sh_titlemodal").text("Allowed Servers Editor");
	var val = $("#"+inputName).val();
	var parser = new marknote.Parser();var xml = parser.parse(val);var root = xml.getRootElement();var allowed = root.getChildElements();sh_array = new Array(allowed.length);
	for ( var i = 0; i < allowed.length; i++) { sh_array[i] = allowed[i].getContents();}
	sh_dialog_load();
	$('#sh_myModal').modal('show');
}
// Update Allowed Server
function sh_dialog_selectOk() {
	var root = new marknote.Element("hostids");
	for ( var j = 0; j < sh_array.length; j++) {
		var hostid = new marknote.Element("hostid");
		var obj = sh_array[j];
		hostid.setText(obj); root.addContent(hostid);
	}
	var result = root.toString("\f");var regexp = new RegExp("^\n\r|^\r\n|^\n|^\r|\f", "g");result = result.replace(regexp, "");
	$("#"+sh_dialog_inputName).val(result);
	$('#sh_myModal').modal("hide");
}

/**
 * Business Helper
 */
// Source Input field name
var bh_dialog_inputName = '';
// Current Business array
var bh_array = new Array();
var bh_titles= new Array("Host Id");
var bh_thead=$("<thead/>"); var bh_tr=$('<tr/>');$.each(bh_titles, function(col, value){bh_tr.append($("<th/>").text(value));}); bh_thead.append(bh_tr);
// Load dialog
function bh_dialog_load() {
	var table=$("#bh_tblGrid");
	table.empty();
	table.append(bh_thead);
	var tbody=$("<tbody/>");
	var row = $("<tr/>");
	var tselect=$("<select/>").addClass("form-control input-sm").attr("id", "hid0").attr("onChange", "bh_dialog_typeMultiSelect()").attr("multiple", "multiple");
	$.each(hostids, function(col, value){
		var found = false
		$.each(bh_array, function(rowIndex, r) {
		if (value==r){tselect.append($("<option/>").prop('selected', true).text(value)); found = true;}});
		if (! found) {tselect.append($("<option/>").text(value));};
	});
	row.append($("<td/>").append(tselect)); tselect.multiselect({maxHeight: 200});
	tbody.append(row);
	table.append(tbody);
}
// Select multiple value from SELECT
function bh_dialog_typeMultiSelect() {bh_array=$("#hid0").val();}
/*******************************************************************************
 * inputName : field name that contains the value of business host ids
 ******************************************************************************/
function bh_dialog_open(inputName) {
	bh_dialog_inputName = inputName;
	$("#bh_titlemodal").text("Business Editor");
	var val = $("#"+inputName).val();
	var parser = new marknote.Parser();var xml = parser.parse(val);var root = xml.getRootElement();var business = root.getChildElements();bh_array = new Array(business.length);
	for ( var i = 0; i < business.length; i++) { bh_array[i] = business[i].getContents();}
	bh_dialog_load();
	$('#bh_myModal').modal('show');
}
// Update Business
function bh_dialog_selectOk() {
	var root = new marknote.Element("business");
	for ( var j = 0; j < bh_array.length; j++) {
		var businessid = new marknote.Element("businessid");
		var obj = bh_array[j];
		businessid.setText(obj); root.addContent(businessid);
	}
	var result = root.toString("\f");var regexp = new RegExp("^\n\r|^\r\n|^\n|^\r|\f", "g");result = result.replace(regexp, "");
	$("#"+bh_dialog_inputName).val(result);
	$('#bh_myModal').modal("hide");
}

/**
 * Roles Helper
 */
// Source Input field name
var rh_dialog_inputName = '';
// Current Roles array
var rh_array = new Array();
var rh_titles= new Array("Host Id", "Roles", "Controls");
var rh_thead=$("<thead/>"); var rh_tr=$('<tr/>');$.each(rh_titles, function(col, value){rh_tr.append($("<th/>").text(value));}); rh_thead.append(rh_tr);
// Load dialog
function rh_dialog_load() {
	var table=$("#rh_tblGrid");
	table.empty();
	table.append(rh_thead);
	var tbody=$("<tbody/>");$.each(rh_array, function(rowIndex, r) {
		var row = $("<tr/>");
		$.each(r, function(colIndex, c) {
			switch (colIndex) {
			case 0:
				var tselect=$("<select/>").addClass("form-control input-sm").attr("id", "rid"+rowIndex).attr("onChange", "rh_dialog_typeSelect("+rowIndex+" )");
				$.each(hostids, function(col, value){
					if (value==c){tselect.append($("<option/>").prop('selected', true).text(value));}
					else{tselect.append($("<option/>").text(value));}});
				row.append($("<td/>").append(tselect));
				break;
			case 1:
				var tselect=$("<select/>").addClass("form-control input-sm").attr("id", "roles"+rowIndex).attr("onChange", "rh_dialog_typeMultiSelect("+rowIndex+" )").attr("multiple", "multiple");
				$.each(roles, function(col, value){tselect.append($("<option/>").text(value));});
				tselect.val(c);
				row.append($("<td/>").append(tselect)); tselect.multiselect({maxHeight: 200});
				break;
			}
		});
		row.append($("<td/>")
			.append($("<button/>").addClass("btn btn-danger btn-sm").attr("id", "del"+rowIndex).attr("onclick","rh_dialog_delete("+rowIndex+")").text("Remove")));
		tbody.append(row);
	});table.append(tbody);
}
// Select value from SELECT
function rh_dialog_typeSelect(rank) {rh_array[rank][0]=$("#rid"+rank).val();}
// Select multiple value from SELECT
function rh_dialog_typeMultiSelect(rank) {rh_array[rank][1]=$("#roles"+rank).val();}
/*******************************************************************************
 * inputName : field name that contains the value of business host ids
 ******************************************************************************/
function rh_dialog_open(inputName) {
	rh_dialog_inputName = inputName;
	$("#rh_titlemodal").text("Roles Editor");
	var val = $("#"+inputName).val();
	var parser = new marknote.Parser();var xml = parser.parse(val);var root = xml.getRootElement();var rolesElt = root.getChildElements();rh_array = new Array(rolesElt.length);
	for ( var i = 0; i < rolesElt.length; i++) { 
		var task = rolesElt[i];var roleid = task.getChildElement("roleid");var roleset = task.getChildElement("roleset");
		var roleArray = roleset.getText().split(" ");
		rh_array[i] = new Array(roleid.getText(), roleArray);
	}
	rh_dialog_load();
	$('#rh_myModal').modal('show');
}
// Add a new Roles Ids
function rh_dialog_add(rank) {
	var tempElt = new Array(hostids[0], []); rh_array.push(tempElt);
	rh_dialog_load();
}
// Delete a Roles
function rh_dialog_delete(rank) {
	var max = rh_array.length-1; if (rank == 0) { rh_array.shift(); } else if (rank < max) { var before = rh_array.slice(0, rank); rh_array = before.concat(rh_array.slice(rank+1, rh_array.length)); } else if (rank == max) { rh_array.pop(); }
	rh_dialog_load();
}
// Update Roles
function rh_dialog_selectOk() {
	var root = new marknote.Element("roles");
	for ( var j = 0; j < rh_array.length; j++) {
		var role = new marknote.Element("role");
		var val = rh_array[j][0];
		if (val && val.length > 0) {
			var sub = new marknote.Element("roleid");sub.setText(val);role.addContent(sub);
			val = rh_array[j][1];
			if (val && val.length > 0) {
				var roleset = val[0];
				for (var i = 1; i < val.length ; i++) { roleset += " "+val[i]; }
				sub = new marknote.Element("roleset");sub.setText(roleset);role.addContent(sub);
			} else { sub = new marknote.Element("roleset");role.addContent(sub); }
		}
		root.addContent(role);
	}
	var result = root.toString("\f");var regexp = new RegExp("^\n\r|^\r\n|^\n|^\r|\f", "g");result = result.replace(regexp, "");
	$("#"+rh_dialog_inputName).val(result);
	$('#rh_myModal').modal("hide");
}

/**
 * Alias Helper
 */
// Source Input field name
var ah_dialog_inputName = '';
// Current Alias array
var ah_array = new Array();
var ah_titles= new Array("Host Id", "Aliases", "New Alias", "Add Alias", "Controls");
var ah_thead=$("<thead/>"); var ah_tr=$('<tr/>');$.each(ah_titles, function(col, value){ah_tr.append($("<th/>").text(value));}); ah_thead.append(ah_tr);
// Load dialog
function ah_dialog_load() {
	var table=$("#ah_tblGrid");
	table.empty();
	table.append(ah_thead);
	var tbody=$("<tbody/>");
	$.each(ah_array, function(rowIndex, r) {
		var row = $("<tr/>");
		$.each(r, function(colIndex, c) {
			switch (colIndex) {
			case 0:
				var tselect=$("<select/>").addClass("form-control input-sm").attr("id", "aid"+rowIndex).attr("onChange", "ah_dialog_typeSelect("+rowIndex+" )");
				$.each(hostids, function(col, value){
					if (value==c){tselect.append($("<option/>").prop('selected', true).text(value));}
					else{tselect.append($("<option/>").text(value));}});
				row.append($("<td/>").append(tselect));
				break;
			case 1:
				var tselect=$("<select/>").addClass("form-control input-sm").attr("id", "alias"+rowIndex).attr("multiple", "multiple").attr("onChange", "ah_dialog_typeMultiSelect("+rowIndex+")");
				$.each(c, function(col, value){tselect.append($("<option/>").text(value));});
				tselect.val(c);
				row.append($("<td/>").append(tselect)); tselect.multiselect({maxHeight: 200});
				var tinput=$("<input/>").addClass("form-control input-sm").attr("id", "newitem"+rowIndex).attr("type","text").attr("size","20").attr("placeholder","new aliases");
				row.append($("<td/>").append(tinput));
				var tbutton = $("<button/>").addClass("btn btn-danger btn-sm").attr("onclick","ah_dialog_typeInput("+rowIndex+")").text("New Alias");
				row.append($("<td/>").append(tbutton));
				break;
			}
		});
		row.append($("<td/>")
			.append($("<button/>").addClass("btn btn-danger btn-sm").attr("id", "del"+rowIndex).attr("onclick","ah_dialog_delete("+rowIndex+")").text("Remove")));
		tbody.append(row);
	});table.append(tbody);
}
// Select value from SELECT
function ah_dialog_typeSelect(rank) {ah_array[rank][0]=$("#aid"+rank).val();}
// Select multiple value from SELECT
function ah_dialog_typeMultiSelect(rank) {
	var tselect = $("#alias"+rank);
	ah_array[rank][1]=tselect.val();
}
// Select value from INPUT
function ah_dialog_typeInput(rank) {
	var val = $("#newitem"+rank).val();
	if (val && val.length > 0) {
		var tselect = $("#alias"+rank);
		var values = tselect.val();
		if (!values) {values = [];}
		if (values.indexOf(val) == -1) {
			values.push(val);
			ah_array[rank][1] = values;
			//add new Select option
			tselect.append($("<option/>").text(val));
			tselect.val(values);
			tselect.multiselect('rebuild');
		}
		$("#newitem"+rank).text("");
	}
}
/*******************************************************************************
 * inputName : field name that contains the value of aliases host ids
 ******************************************************************************/
function ah_dialog_open(inputName) {
	ah_dialog_inputName = inputName;
	$("#ah_titlemodal").text("Alias Editor");
	var val = $("#"+inputName).val();
	var parser = new marknote.Parser();var xml = parser.parse(val);var root = xml.getRootElement();var aliasElt = root.getChildElements();ah_array = new Array(aliasElt.length);
	for ( var i = 0; i < aliasElt.length; i++) { 
		var task = aliasElt[i];var realid = task.getChildElement("realid");var aliasid = task.getChildElement("aliasid");
		var aliasArray = aliasid.getText().split(" ");
		ah_array[i] = new Array(realid.getText(), aliasArray);
	}
	ah_dialog_load();
	$('#ah_myModal').modal('show');
}
// Add a new Alias Ids
function ah_dialog_add(rank) {
	var tempElt = new Array(hostids[0], []); ah_array.push(tempElt);
	ah_dialog_load();
}
// Delete a Alias
function ah_dialog_delete(rank) {
	var max = ah_array.length-1; if (rank == 0) { ah_array.shift(); } else if (rank < max) { var before = ah_array.slice(0, rank); ah_array = before.concat(ah_array.slice(rank+1, ah_array.length)); } else if (rank == max) { ah_array.pop(); }
	ah_dialog_load();
}
// Update Alias
function ah_dialog_selectOk() {
	var root = new marknote.Element("aliases");
	for ( var j = 0; j < ah_array.length; j++) {
		var alias = new marknote.Element("alias");
		var val = ah_array[j][0];
		if (val && val.length > 0) {
			var sub = new marknote.Element("realid");sub.setText(val);alias.addContent(sub);
			val = ah_array[j][1];
			if (val && val.length > 0) {
				var aliasid = val[0];
				for (var i = 1; i < val.length ; i++) { aliasid += " "+val[i]; }
				sub = new marknote.Element("aliasid");sub.setText(aliasid);alias.addContent(sub);
			} else { sub = new marknote.Element("aliasid");alias.addContent(sub); }
		}
		root.addContent(alias);
	}
	var result = root.toString("\f");var regexp = new RegExp("^\n\r|^\r\n|^\n|^\r|\f", "g");result = result.replace(regexp, "");
	$("#"+ah_dialog_inputName).val(result);
	$('#ah_myModal').modal("hide");
}
