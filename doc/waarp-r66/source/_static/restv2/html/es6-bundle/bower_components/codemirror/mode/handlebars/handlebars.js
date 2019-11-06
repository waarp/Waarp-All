// CodeMirror, copyright (c) by Marijn Haverbeke and others
// Distributed under an MIT license: https://codemirror.net/LICENSE
(function(mod){if("object"==typeof exports&&"object"==typeof module)// CommonJS
mod(require("../../lib/codemirror"),require("../../addon/mode/simple"),require("../../addon/mode/multiplex"));else if("function"==typeof define&&define.amd)// AMD
define(["../../lib/codemirror","../../addon/mode/simple","../../addon/mode/multiplex"],mod);else// Plain browser env
mod(CodeMirror)})(function(CodeMirror){"use strict";CodeMirror.defineSimpleMode("handlebars-tags",{start:[{regex:/\{\{!--/,push:"dash_comment",token:"comment"},{regex:/\{\{!/,push:"comment",token:"comment"},{regex:/\{\{/,push:"handlebars",token:"tag"}],handlebars:[{regex:/\}\}/,pop:!0/* ignoreName */ /* skipSlots */,token:"tag"},// Double and single quotes
{regex:/"(?:[^\\"]|\\.)*"?/,token:"string"},{regex:/'(?:[^\\']|\\.)*'?/,token:"string"},// Handlebars keywords
{regex:/>|[#\/]([A-Za-z_]\w*)/,token:"keyword"},{regex:/(?:else|this)\b/,token:"keyword"},// Numeral
{regex:/\d+/i,token:"number"},// Atoms like = and .
{regex:/=|~|@|true|false/,token:"atom"},// Paths
{regex:/(?:\.\.\/)*(?:[A-Za-z_][\w\.]*)+/,token:"variable-2"}],dash_comment:[{regex:/--\}\}/,pop:!0,token:"comment"},// Commented code
{regex:/./,token:"comment"}],comment:[{regex:/\}\}/,pop:!0,token:"comment"},{regex:/./,token:"comment"}],meta:{blockCommentStart:"{{--",blockCommentEnd:"--}}"}});CodeMirror.defineMode("handlebars",function(config,parserConfig){var handlebars=CodeMirror.getMode(config,"handlebars-tags");if(!parserConfig||!parserConfig.base)return handlebars;return CodeMirror.multiplexingMode(CodeMirror.getMode(config,parserConfig.base),{open:"{{",close:"}}",mode:handlebars,parseDelimiters:!0})});CodeMirror.defineMIME("text/x-handlebars-template","handlebars")});