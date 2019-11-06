// CodeMirror, copyright (c) by Marijn Haverbeke and others
// Distributed under an MIT license: https://codemirror.net/LICENSE
(function(mod){if("object"==("undefined"===typeof exports?"undefined":babelHelpers.typeof(exports))&&"object"==("undefined"===typeof module?"undefined":babelHelpers.typeof(module)))// CommonJS
mod(require("../../lib/codemirror"));else if("function"==typeof define&&define.amd)// AMD
define(["../../lib/codemirror"],mod);else// Plain browser env
mod(CodeMirror)})(function(CodeMirror){"use strict";CodeMirror.defineMode("properties",function(){return{token:function token(stream,state){var sol=stream.sol()||state.afterSection,eol=stream.eol();state.afterSection=/* eat */ /* ignoreName */ /* ignoreName */!1/* skipSlots */ /* skipSlots */;if(sol){if(state.nextMultiline){state.inMultiline=!0/* skipSlots */;state.nextMultiline=!1}else{state.position="def"}}if(eol&&!state.nextMultiline){state.inMultiline=!1;state.position="def"}if(sol){while(stream.eatSpace()){}}var ch=stream.next();if(sol&&("#"===ch||"!"===ch||";"===ch)){state.position="comment";stream.skipToEnd();return"comment"}else if(sol&&"["===ch){state.afterSection=!0;stream.skipTo("]");stream.eat("]");return"header"}else if("="===ch||":"===ch){state.position="quote";return null}else if("\\"===ch&&"quote"===state.position){if(stream.eol()){// end of line?
// Multiline value
state.nextMultiline=!0}}return state.position},startState:function startState(){return{position:"def",// Current position, "def", "quote" or "comment"
nextMultiline:!1,// Is the next line multiline value
inMultiline:!1,// Is the current line a multiline value
afterSection:!1// Did we just open a section
}}}});CodeMirror.defineMIME("text/x-properties","properties");CodeMirror.defineMIME("text/x-ini","properties")});