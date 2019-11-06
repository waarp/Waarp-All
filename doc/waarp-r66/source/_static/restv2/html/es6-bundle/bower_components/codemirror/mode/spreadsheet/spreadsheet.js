// CodeMirror, copyright (c) by Marijn Haverbeke and others
// Distributed under an MIT license: https://codemirror.net/LICENSE
(function(mod){if("object"==typeof exports&&"object"==typeof module)// CommonJS
mod(require("../../lib/codemirror"));else if("function"==typeof define&&define.amd)// AMD
define(["../../lib/codemirror"],mod);else// Plain browser env
mod(CodeMirror)})(function(CodeMirror){"use strict";CodeMirror.defineMode("spreadsheet",function(){return{startState:function(){return{stringType:null,stack:[]}},token:function(stream,state){if(!stream)return;//check for state changes
if(0===state.stack.length){//strings
if("\""==stream.peek()||"'"==stream.peek()){state.stringType=stream.peek();stream.next();// Skip quote
state.stack.unshift("string")}}//return state
//stack has
switch(state.stack[0]){case"string":while("string"===state.stack[0]&&!stream.eol()){if(stream.peek()===state.stringType){stream.next();// Skip quote
state.stack.shift();// Clear flag
}else if("\\"===stream.peek()){stream.next();stream.next()}else{stream.match(/^.[^\\\"\']*/)}}return"string";case"characterClass":while("characterClass"===state.stack[0]&&!stream.eol()){if(!(stream.match(/^[^\]\\]+/)||stream.match(/^\\./)))state.stack.shift()}return"operator";}var peek=stream.peek();//no stack
switch(peek){case"[":stream.next();state.stack.unshift("characterClass");return"bracket";case":":stream.next();return"operator";case"\\":if(stream.match(/\\[a-z]+/))return"string-2";else{stream.next();return"atom"}case".":case",":case";":case"*":case"-":case"+":case"^":case"<":case"/":case"=":stream.next();return"atom";case"$":stream.next();return"builtin";}if(stream.match(/\d+/)){if(stream.match(/^\w+/))return"error";return"number"}else if(stream.match(/^[a-zA-Z_]\w*/)){if(stream.match(/(?=[\(.])/,/* eat */ /* ignoreName */ /* ignoreName */!1/* skipSlots */ /* skipSlots */))return"keyword";return"variable-2"}else if(-1!=["[","]","(",")","{","}"].indexOf(peek)){stream.next();return"bracket"}else if(!stream.eatSpace()){stream.next()}return null}}});CodeMirror.defineMIME("text/x-spreadsheet","spreadsheet")});