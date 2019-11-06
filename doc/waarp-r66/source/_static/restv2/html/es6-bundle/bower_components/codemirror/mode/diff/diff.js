// CodeMirror, copyright (c) by Marijn Haverbeke and others
// Distributed under an MIT license: https://codemirror.net/LICENSE
(function(mod){if("object"==typeof exports&&"object"==typeof module)// CommonJS
mod(require("../../lib/codemirror"));else if("function"==typeof define&&define.amd)// AMD
define(["../../lib/codemirror"],mod);else// Plain browser env
mod(CodeMirror)})(function(CodeMirror){"use strict";CodeMirror.defineMode("diff",function(){var TOKEN_NAMES={"+":"positive","-":"negative","@":"meta"};return{token:function(stream){var tw_pos=stream.string.search(/[\t ]+?$/);if(!stream.sol()||0===tw_pos){stream.skipToEnd();return("error "+(TOKEN_NAMES[stream.string.charAt(0)]||"")).replace(/ $/,"")}var token_name=TOKEN_NAMES[stream.peek()]||stream.skipToEnd();if(-1===tw_pos){stream.skipToEnd()}else{stream.pos=tw_pos}return token_name}}});CodeMirror.defineMIME("text/x-diff","diff")});