// CodeMirror, copyright (c) by Marijn Haverbeke and others
// Distributed under an MIT license: https://codemirror.net/LICENSE
/*
  This MUMPS Language script was constructed using vbscript.js as a template.
*/(function(mod){if("object"==("undefined"===typeof exports?"undefined":babelHelpers.typeof(exports))&&"object"==("undefined"===typeof module?"undefined":babelHelpers.typeof(module)))// CommonJS
mod(require("../../lib/codemirror"));else if("function"==typeof define&&define.amd)// AMD
define(["../../lib/codemirror"],mod);else// Plain browser env
mod(CodeMirror)})(function(CodeMirror){"use strict";CodeMirror.defineMode("mumps",function(){function wordRegexp(words){return new RegExp("^(("+words.join(")|(")+"))\\b","i")}var singleOperators=/^[\+\-\*\/&#!_?\\<>=\'\[\]]/,doubleOperators=/^(('=)|(<=)|(>=)|('>)|('<)|([[)|(]])|(^$))/,singleDelimiters=/^[\.,:]/,brackets=/[()]/,identifiers=/^[%A-Za-z][A-Za-z0-9]*/,commandKeywords=["break","close","do","else","for","goto","halt","hang","if","job","kill","lock","merge","new","open","quit","read","set","tcommit","trollback","tstart","use","view","write","xecute","b","c","d","e","f","g","h","i","j","k","l","m","n","o","q","r","s","tc","tro","ts","u","v","w","x"],intrinsicFuncsWords=["\\$ascii","\\$char","\\$data","\\$ecode","\\$estack","\\$etrap","\\$extract","\\$find","\\$fnumber","\\$get","\\$horolog","\\$io","\\$increment","\\$job","\\$justify","\\$length","\\$name","\\$next","\\$order","\\$piece","\\$qlength","\\$qsubscript","\\$query","\\$quit","\\$random","\\$reverse","\\$select","\\$stack","\\$test","\\$text","\\$translate","\\$view","\\$x","\\$y","\\$a","\\$c","\\$d","\\$e","\\$ec","\\$es","\\$et","\\$f","\\$fn","\\$g","\\$h","\\$i","\\$j","\\$l","\\$n","\\$na","\\$o","\\$p","\\$q","\\$ql","\\$qs","\\$r","\\$re","\\$s","\\$st","\\$t","\\$tr","\\$v","\\$z"],intrinsicFuncs=wordRegexp(intrinsicFuncsWords),command=wordRegexp(commandKeywords);function tokenBase(stream,state){if(stream.sol()){state.label=!0/* ignoreName */ /* skipSlots */;state.commandMode=0}// The <space> character has meaning in MUMPS. Ignoring consecutive
// spaces would interfere with interpreting whether the next non-space
// character belongs to the command or argument context.
// Examine each character and update a mode variable whose interpretation is:
//   >0 => command    0 => argument    <0 => command post-conditional
var ch=stream.peek();if(" "==ch||"\t"==ch){// Pre-process <space>
state.label=/* eat */ /* ignoreName */!1/* skipSlots */ /* skipSlots */;if(0==state.commandMode)state.commandMode=1;else if(0>state.commandMode||2==state.commandMode)state.commandMode=0}else if("."!=ch&&0<state.commandMode){if(":"==ch)state.commandMode=-1;// SIS - Command post-conditional
else state.commandMode=2}// Do not color parameter list as line tag
if("("===ch||"\t"===ch)state.label=!1;// MUMPS comment starts with ";"
if(";"===ch){stream.skipToEnd();return"comment"}// Number Literals // SIS/RLM - MUMPS permits canonic number followed by concatenate operator
if(stream.match(/^[-+]?\d+(\.\d+)?([eE][-+]?\d+)?/))return"number";// Handle Strings
if("\""==ch){if(stream.skipTo("\"")){stream.next();return"string"}else{stream.skipToEnd();return"error"}}// Handle operators and Delimiters
if(stream.match(doubleOperators)||stream.match(singleOperators))return"operator";// Prevents leading "." in DO block from falling through to error
if(stream.match(singleDelimiters))return null;if(brackets.test(ch)){stream.next();return"bracket"}if(0<state.commandMode&&stream.match(command))return"variable-2";if(stream.match(intrinsicFuncs))return"builtin";if(stream.match(identifiers))return"variable";// Detect dollar-sign when not a documented intrinsic function
// "^" may introduce a GVN or SSVN - Color same as function
if("$"===ch||"^"===ch){stream.next();return"builtin"}// MUMPS Indirection
if("@"===ch){stream.next();return"string-2"}if(/[\w%]/.test(ch)){stream.eatWhile(/[\w%]/);return"variable"}// Handle non-detected items
stream.next();return"error"}return{startState:function startState(){return{label:!1,commandMode:0}},token:function token(stream,state){var style=tokenBase(stream,state);if(state.label)return"tag";return style}}});CodeMirror.defineMIME("text/x-mumps","mumps")});