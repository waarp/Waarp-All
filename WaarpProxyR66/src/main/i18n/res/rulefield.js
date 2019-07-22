/**
 * Rule Helper
 */
// Source Input field name
var dialog_inputName = '';
// Current tasks array
var arrayTasks = new Array();
// Available Task's types
var availableTasks = new Array("LOG", "MOVE", "MOVERENAME", "COPY", "COPYRENAME", "EXEC", "EXECMOVE", "EXECOUTPUT", "EXECJAVA", "TRANSFER", "VALIDFILEPATH", "DELETE", "LINKRENAME", "RESCHEDULE", "TAR", "ZIP", "TRANSCODE", "FTP", "RENAME", "RESTART", "UNZEROED");
// Load dialog
function dialog_load() {
	// get container
	var dialog_box = document.getElementById('dialog_box');var pt = window.center({width:800,height:300});dialog_box.style.top = pt.y + "px";dialog_box.style.left = pt.x + "px";var dialog_content = document.getElementById('dialog_content');dialog_content.innerHTML = '';var tab=document.createElement('table');var row=document.createElement('tr');var header=document.createElement('th');header.textContent = "Task Type";row.appendChild(header);header=document.createElement('th');header.textContent = "Path argument";row.appendChild(header);header=document.createElement('th');header.textContent = "Delay";row.appendChild(header);header=document.createElement('th');header.textContent = "Comment (optional)";row.appendChild(header);tab.appendChild(row);
	for ( var j = 0; j < arrayTasks.length; j++) {
		var rownext=document.createElement('tr');var field=document.createElement('td');
		/*
		<SELECT name="type" id="typeX">LOG, MOVE, MOVERENAME, COPY, COPYRENAME, EXEC, EXECMOVE, EXECOUTPUT, EXECJAVA, TRANSFER, VALIDFILEPATH, DELETE, LINKRENAME, RESCHEDULE, TAR, ZIP, TRANSCODE, FTP, RENAME, RESTARTn UNZEROED<OPTION VALUE="LOG" SELECTED>LOG</OPTION>
		<OPTION VALUE="MOVE">MOVE</OPTION>
		...
		</SELECT>
		*/
		var typeTask = document.createElement("select");typeTask.id = "type" + j;typeTask.setAttribute('name', 'type');var found = 0;
		for (var k = 0; k < availableTasks.length ; k++) {
			var option = document.createElement("option");option.setAttribute('value', availableTasks[k]);var text = document.createTextNode(availableTasks[k]);option.appendChild(text);
			if (availableTasks[k] == arrayTasks[j][0]) { found = k;option.setAttribute("selected","selected"); }
			typeTask.appendChild(option);
		}
		typeTask.options[found].selected = true;typeTask.setAttribute("onChange", "dialog_typeSelect(" + j + " )" );field.appendChild(typeTask);rownext.appendChild(field);field=document.createElement('td');
		// <input name="path" id="pathX" value="sub" type="text" size="30">
		var path = document.createElement("input");path.id = "path" + j;path.setAttribute('type', 'text');path.setAttribute('name', 'path');path.setAttribute('size', '30');path.setAttribute('value', arrayTasks[j][1]);path.setAttribute("onChange", "dialog_typeInput(" + j + "," + 1 + " )" );field.appendChild(path);rownext.appendChild(field);field=document.createElement('td');
		// <input name="delay" id="delayX" value="sub" type="text" size="5">
		var delay = document.createElement("input");delay.id = "delay" + j;delay.setAttribute('type', 'text');delay.setAttribute('name', 'delay');delay.setAttribute('size', '5');delay.setAttribute('value', arrayTasks[j][2]);delay.setAttribute("onChange", "dialog_typeInput(" + j + "," + 2 + " )" );field.appendChild(delay);rownext.appendChild(field);field=document.createElement('td');
		// <input name="comment" id="commentX" value="sub" type="text" size="20">
		var comment = document.createElement("input");comment.id = "comment" + j;comment.setAttribute('type', 'text');comment.setAttribute('name', 'comment');comment.setAttribute('size', '20');
		if (arrayTasks[j].length > 3) { comment.setAttribute('value', arrayTasks[j][3]); } else { comment.setAttribute('value', ''); }
		comment.setAttribute("onChange", "dialog_typeInput(" + j + "," + 3 + " )" );field.appendChild(comment);rownext.appendChild(field);field=document.createElement('td');
		// <input value="ADD BEFORE" name="ADD" type="submit">
		var oTask = document.createElement("input");oTask.id = "add_" + j;oTask.setAttribute('type', 'submit');oTask.setAttribute('name', 'ADD');oTask.setAttribute('value', 'ADD BEFORE');oTask.setAttribute("onClick", "dialog_add(" + j + " )" );field.appendChild(oTask);rownext.appendChild(field);field=document.createElement('td');
		// <input value="REMOVE" name="REMOVE" type="submit">
		oTask = document.createElement("input");oTask.id = "del_" + j;oTask.setAttribute('type', 'submit');oTask.setAttribute('name', 'REMOVE');oTask.setAttribute('value', 'REMOVE');oTask.setAttribute("onClick", "dialog_delete(" + j + " )" );field.appendChild(oTask);rownext.appendChild(field);tab.appendChild(rownext);
	}
	dialog_content.appendChild(tab);
	// <input value="ADD AFTER" name="ADD" type="submit">
	var oTask = document.createElement("input");oTask.id = "add_" + (arrayTasks.length);oTask.setAttribute('type', 'submit');oTask.setAttribute('name', 'ADD');oTask.setAttribute('value', 'ADD AFTER');oTask.setAttribute("onClick", "dialog_add(" + arrayTasks.length + " )" );dialog_content.appendChild(oTask);
	// <input value="SET" name="SET" type="submit">
	var oValid = document.createElement("input");oValid.id = "set";oValid.setAttribute('type', 'submit');oValid.setAttribute('name', 'SET');oValid.setAttribute('value', 'SET');oValid.onclick = function() {dialog_selectOk();};dialog_content.appendChild(oValid);
	// <input value="CANCEL" name="CANCEL" type="submit">
	oValid = document.createElement("input");oValid.id = "cancel";oValid.setAttribute('type', 'submit');oValid.setAttribute('name', 'CANCEL');oValid.setAttribute('value', 'CANCEL');oValid.onclick = function() {dialog_cancel();};dialog_content.appendChild(oValid);var br = document.createElement("br");dialog_content.appendChild(br);var tt = document.createElement("tt");
	var text = document.createTextNode("Keywords: #TRUEFULLPATH# #TRUEFILENAME# #ORIGINALFULLPATH# #ORIGINALFILENAME# #FILESIZE# #RULE# #DATE# #HOUR# #REMOTEHOST# #REMOTEHOSTADDR# #LOCALHOST# #LOCALHOSTADDR# #TRANSFERID# #REQUESTERHOST# #REQUESTEDHOST# #FULLTRANSFERID# #RANKTRANSFER# #BLOCKSIZE# #INPATH# #OUTPATH# #WORKPATH# #ARCHPATH# #HOMEPATH# #ERRORMSG# #ERRORCODE# #ERRORSTRCODE# #NOWAIT# #LOCALEXEC#");
	tt.appendChild(text);dialog_content.appendChild(tt);
}
// Select value from SELECT
function dialog_typeSelect(rank) {
	var select = document.getElementById('type'+rank);var chosenoption = select.options[select.selectedIndex];arrayTasks[rank][0] = chosenoption.value;
}
// Select value from INPUT
function dialog_typeInput(rank, field) {
	var fieldname = '';if (field == 1) { fieldname = "path"+rank; } else if (field == 2) { fieldname = "delay"+rank; } else { fieldname = "comment"+rank; }
	var input = document.getElementById(fieldname);arrayTasks[rank][field] = input.value;
}
/*******************************************************************************
 * inputName : field name that contains the value of rule tasks
 ******************************************************************************/
function dialog_open(inputName, title) {
	// back zone unvalidated
	var dialog_background = document.getElementById('dialog_background');dialog_background.style.display = "block";
	// Keeo original object
	var obInput = document.getElementById(inputName);dialog_input = obInput;
	// dialog show
	var dialog_box = document.getElementById('dialog_box');dialog_box.style.display = "block";var dialog_title = document.getElementById('dialog_title');var code = '';
	if (dialog_input.id.charAt(0) == 'r') { code = " Recv "; } else { code = " Send "; }
	if (dialog_input.id.charAt(1) == 'e') { code = code + " Error Tasks"; } else if (dialog_input.id.charAt(2) == 'o') { code = code + " Post Tasks"; } else { code = code + " Pre Tasks"; }
	dialog_title.innerHTML = title + code;dialog_box.style.top = (document.body.scrollTop * 1) + 100;
	// parser xml tasks
	value1 = obInput.value;var parser = new marknote.Parser();var xml = parser.parse(value1);var root = xml.getRootElement();var tasks = root.getChildElements();arrayTasks = new Array(tasks.length);
	for ( var i = 0; i < tasks.length; i++) {
		var task = tasks[i];var type = task.getChildElement("type");var path = task.getChildElement("path");var delay = task.getChildElement("delay");var comment = task.getChildElement("comment");if (comment) { arrayTasks[i] = new Array(type.getText(), path.getText(), delay.getText(), comment.getText()); } else { arrayTasks[i] = new Array(type.getText(), path.getText(), delay.getText(), ""); }
	}
	dialog_load();
	// unallow bar box behind
	document.body.style.overflow = 'hidden';
}
// Add a new Task
function dialog_add(rank) {
	var tempElt = new Array("LOG",'',0,'');if (rank == 0) { arrayTasks.unshift(tempElt); } else if (rank < arrayTasks.length) { var before = arrayTasks.slice(0, rank);before.push(tempElt);arrayTasks = before.concat(arrayTasks.slice(rank, arrayTasks.length)); } else { arrayTasks.push(tempElt); }
	dialog_load();
}
// Delete a Task
function dialog_delete(rank) {
	var max = arrayTasks.length-1;if (rank == 0) { arrayTasks.shift(); } else if (rank < max) { var before = arrayTasks.slice(0, rank);arrayTasks = before.concat(arrayTasks.slice(rank+1, arrayTasks.length)); } else if (rank == max) { arrayTasks.pop(); }
	dialog_load();
}
// Get value from SELECT
function getSelectValue(selectElmt) {
	return selectElmt.options[selectElmt.selectedIndex].value;
}
// Ignore changes
function dialog_cancel() {
	dialog_close();
}
function dialog_close() {
	// Mask dialog
	dialog_win = document.getElementById('dialog_background');dialog_win.style.display = "none";var dialog_box = document.getElementById('dialog_box');dialog_box.style.display = "none";
	// scrollbar activated
	document.body.style.overflow = 'scroll';
}
// Update tasks
function dialog_selectOk() {
	var root = new marknote.Element("tasks");
	for ( var j = 0; j < arrayTasks.length; j++) {
		var task = new marknote.Element("task");var obj = document.getElementById("type" + j);
		if (obj) { var val = getSelectValue(obj);
			if (val && val.length > 0) {
				var sub = new marknote.Element("type");sub.setText(val);task.addContent(sub);obj = document.getElementById("path" + j);
				if (obj && obj.value && obj.value.length > 0) { sub = new marknote.Element("path");sub.setText(obj.value);task.addContent(sub); } else { sub = new marknote.Element("path");task.addContent(sub); }
				obj = document.getElementById("delay" + j);
				if (obj && obj.value && obj.value.length > 0) { sub = new marknote.Element("delay");sub.setText(obj.value);task.addContent(sub); } else { sub = new marknote.Element("delay");sub.setText(0);task.addContent(sub); }
				obj = document.getElementById("comment" + j);
				if (obj && obj.value && obj.value.length > 0) { sub = new marknote.Element("comment");sub.setText(obj.value);task.addContent(sub); }
				root.addContent(task);
			}
		}
	}
	var result = root.toString("\f");var regexp = new RegExp("^\n\r|^\r\n|^\n|^\r|\f", "g");result = result.replace(regexp, "");dialog_input.value = result;dialog_close();
}
// code from: http://www.geekdaily.net/2007/07/04/javascript-cross-browser-window-size-and-centering/
window.size = function() {
   var w = 0;var h = 0;
   //IE
   if(!window.innerWidth) {
      //strict mode
      if(!(document.documentElement.clientWidth == 0)) { w = document.documentElement.clientWidth;h = document.documentElement.clientHeight; } //quirks mode
      else { w = document.body.clientWidth;h = document.body.clientHeight; }
   } //w3c
   else { w = window.innerWidth;h = window.innerHeight; }
   return {width:w,height:h};
}
window.center = function() {
   var hWnd = (arguments[0] != null) ? arguments[0] : {width:0,height:0};var _x = 0;var _y = 0;var offsetX = 0;var offsetY = 0;
   //IE
   if(!window.pageYOffset) {
      //strict mode
      if(!(document.documentElement.scrollTop == 0)) { offsetY = document.documentElement.scrollTop;offsetX = document.documentElement.scrollLeft; } //quirks mode
      else { offsetY = document.body.scrollTop;offsetX = document.body.scrollLeft; }
   } //w3c
   else { offsetX = window.pageXOffset;offsetY = window.pageYOffset; }
   _x = ((this.size().width-hWnd.width)/2)+offsetX;_y = ((this.size().height-hWnd.height)/2)+offsetY;return{x:_x,y:_y};
}
