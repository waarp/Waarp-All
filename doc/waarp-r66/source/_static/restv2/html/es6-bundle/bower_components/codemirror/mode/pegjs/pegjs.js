// CodeMirror, copyright (c) by Marijn Haverbeke and others
// Distributed under an MIT license: https://codemirror.net/LICENSE
(function(mod){if("object"==typeof exports&&"object"==typeof module)// CommonJS
mod(require("../../lib/codemirror"),require("../javascript/javascript"));else if("function"==typeof define&&define.amd)// AMD
define(["../../lib/codemirror","../javascript/javascript"],mod);else// Plain browser env
mod(CodeMirror)})(function(CodeMirror){"use strict";CodeMirror.defineMode("pegjs",function(config){var jsMode=CodeMirror.getMode(config,"javascript");function identifier(stream){return stream.match(/^[a-zA-Z_][a-zA-Z0-9_]*/)}return{startState:function(){return{inString:/* eat */ /* ignoreName */ /* ignoreName */!1/* skipSlots */ /* skipSlots */,stringType:null,inComment:!1,inCharacterClass:!1,braced:0,lhs:!0/* skipSlots */,localState:null}},token:function(stream,state){if(stream)//check for state changes
if(!state.inString&&!state.inComment&&("\""==stream.peek()||"'"==stream.peek())){state.stringType=stream.peek();stream.next();// Skip quote
state.inString=!0;// Update state
}if(!state.inString&&!state.inComment&&stream.match(/^\/\*/)){state.inComment=!0}//return state
if(state.inString){while(state.inString&&!stream.eol()){if(stream.peek()===state.stringType){stream.next();// Skip quote
state.inString=!1;// Clear flag
}else if("\\"===stream.peek()){stream.next();stream.next()}else{stream.match(/^.[^\\\"\']*/)}}return state.lhs?"property string":"string";// Token style
}else if(state.inComment){while(state.inComment&&!stream.eol()){if(stream.match(/\*\//)){state.inComment=!1;// Clear flag
}else{stream.match(/^.[^\*]*/)}}return"comment"}else if(state.inCharacterClass){while(state.inCharacterClass&&!stream.eol()){if(!(stream.match(/^[^\]\\]+/)||stream.match(/^\\./))){state.inCharacterClass=!1}}}else if("["===stream.peek()){stream.next();state.inCharacterClass=!0;return"bracket"}else if(stream.match(/^\/\//)){stream.skipToEnd();return"comment"}else if(state.braced||"{"===stream.peek()){if(null===state.localState){state.localState=CodeMirror.startState(jsMode)}var token=jsMode.token(stream,state.localState),text=stream.current();if(!token){for(var i=0;i<text.length;i++){if("{"===text[i]){state.braced++}else if("}"===text[i]){state.braced--}};}return token}else if(identifier(stream)){if(":"===stream.peek()){return"variable"}return"variable-2"}else if(-1!=["[","]","(",")"].indexOf(stream.peek())){stream.next();return"bracket"}else if(!stream.eatSpace()){stream.next()}return null}}},"javascript")});