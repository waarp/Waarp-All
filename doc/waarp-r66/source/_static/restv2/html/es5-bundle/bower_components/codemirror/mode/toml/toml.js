// CodeMirror, copyright (c) by Marijn Haverbeke and others
// Distributed under an MIT license: https://codemirror.net/LICENSE
(function(mod){if("object"==("undefined"===typeof exports?"undefined":babelHelpers.typeof(exports))&&"object"==("undefined"===typeof module?"undefined":babelHelpers.typeof(module)))// CommonJS
mod(require("../../lib/codemirror"));else if("function"==typeof define&&define.amd)// AMD
define(["../../lib/codemirror"],mod);else// Plain browser env
mod(CodeMirror)})(function(CodeMirror){"use strict";CodeMirror.defineMode("toml",function(){return{startState:function startState(){return{inString:/* eat */ /* ignoreName */ /* ignoreName */!1/* skipSlots */ /* skipSlots */,stringType:"",lhs:!0/* skipSlots */,inArray:0}},token:function token(stream,state){//check for state changes
if(!state.inString&&("\""==stream.peek()||"'"==stream.peek())){state.stringType=stream.peek();stream.next();// Skip quote
state.inString=!0;// Update state
}if(stream.sol()&&0===state.inArray){state.lhs=!0}//return state
if(state.inString){while(state.inString&&!stream.eol()){if(stream.peek()===state.stringType){stream.next();// Skip quote
state.inString=!1;// Clear flag
}else if("\\"===stream.peek()){stream.next();stream.next()}else{stream.match(/^.[^\\\"\']*/)}}return state.lhs?"property string":"string";// Token style
}else if(state.inArray&&"]"===stream.peek()){stream.next();state.inArray--;return"bracket"}else if(state.lhs&&"["===stream.peek()&&stream.skipTo("]")){stream.next();//skip closing ]
// array of objects has an extra open & close []
if("]"===stream.peek())stream.next();return"atom"}else if("#"===stream.peek()){stream.skipToEnd();return"comment"}else if(stream.eatSpace()){return null}else if(state.lhs&&stream.eatWhile(function(c){return"="!=c&&" "!=c})){return"property"}else if(state.lhs&&"="===stream.peek()){stream.next();state.lhs=!1;return null}else if(!state.lhs&&stream.match(/^\d\d\d\d[\d\-\:\.T]*Z/)){return"atom";//date
}else if(!state.lhs&&(stream.match("true")||stream.match("false"))){return"atom"}else if(!state.lhs&&"["===stream.peek()){state.inArray++;stream.next();return"bracket"}else if(!state.lhs&&stream.match(/^\-?\d+(?:\.\d+)?/)){return"number"}else if(!stream.eatSpace()){stream.next()}return null}}});CodeMirror.defineMIME("text/x-toml","toml")});