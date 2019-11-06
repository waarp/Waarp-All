// CodeMirror, copyright (c) by Marijn Haverbeke and others
// Distributed under an MIT license: https://codemirror.net/LICENSE
(function(mod){if("object"==typeof exports&&"object"==typeof module)// CommonJS
mod(require("../../lib/codemirror"));else if("function"==typeof define&&define.amd)// AMD
define(["../../lib/codemirror"],mod);else// Plain browser env
mod(CodeMirror)})(function(CodeMirror){"use strict";function wordRegexp(words){return new RegExp("^(("+words.join(")|(")+"))\\b","i")};var keywordArray=["package","message","import","syntax","required","optional","repeated","reserved","default","extensions","packed","bool","bytes","double","enum","float","string","int32","int64","uint32","uint64","sint32","sint64","fixed32","fixed64","sfixed32","sfixed64","option","service","rpc","returns"],keywords=wordRegexp(keywordArray);CodeMirror.registerHelper("hintWords","protobuf",keywordArray);var identifiers=/^[_A-Za-z¡-￿][_A-Za-z0-9¡-￿]*/;function tokenBase(stream){// whitespaces
if(stream.eatSpace())return null;// Handle one line Comments
if(stream.match("//")){stream.skipToEnd();return"comment"}// Handle Number Literals
if(stream.match(/^[0-9\.+-]/,/* eat */ /* ignoreName */ /* ignoreName */!1/* skipSlots */ /* skipSlots */)){if(stream.match(/^[+-]?0x[0-9a-fA-F]+/))return"number";if(stream.match(/^[+-]?\d*\.\d+([EeDd][+-]?\d+)?/))return"number";if(stream.match(/^[+-]?\d+([EeDd][+-]?\d+)?/))return"number"}// Handle Strings
if(stream.match(/^"([^"]|(""))*"/)){return"string"}if(stream.match(/^'([^']|(''))*'/)){return"string"}// Handle words
if(stream.match(keywords)){return"keyword"}if(stream.match(identifiers)){return"variable"};// Handle non-detected items
stream.next();return null};CodeMirror.defineMode("protobuf",function(){return{token:tokenBase}});CodeMirror.defineMIME("text/x-protobuf","protobuf")});