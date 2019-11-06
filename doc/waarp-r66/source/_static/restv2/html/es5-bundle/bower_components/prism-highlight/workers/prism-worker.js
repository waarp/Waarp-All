'use strict';/* global self, Prism, importScripts */self.document={currentScript:null,getElementsByTagName:function getElementsByTagName(){return[]}};var errored=/* eat */ /* ignoreName */ /* ignoreName */!1/* skipSlots */ /* skipSlots */;try{importScripts("../../prism/prism.js","../../prism/plugins/autolinker/prism-autolinker.min.js","prism-modes.js")}catch(e){errored=!0/* skipSlots */;self.postMessage({payload:"error",message:"Unable to import Prism library."})}function getLanguagePath(lang){return Prism.plugins.mods.path+"prism-"+lang+".min.js"}/**
 * Load a grammar with its dependencies
 * @param {string} lang
 */var loadLanguage=function loadLanguage(lang){var dependencies=Prism.plugins.mods.dependencies[lang];if(dependencies&&dependencies.length){loadLanguages(dependencies)}if(0<=lang.indexOf("!")){lang=lang.replace("!","")}var src=getLanguagePath(lang);importScripts(src)},loadLanguages=function loadLanguages(langs){if("string"===typeof langs){langs=[langs]}langs.forEach(function(lang){loadLanguage(lang)})};/**
 * Sequentially loads an array of grammars.
 * @param {string[]|string} langs
 * @param {function=} success
 * @param {function=} error
 */ // Prism.plugins.mods.langs.forEach(function(lang) {
//   loadLanguage(lang);
// });
function ensureLanguage(lang){if(lang in Prism.languages){return}if(-1!==Prism.plugins.mods.langs.indexOf(lang)){loadLanguage(lang)}}/**
 * Guess proper language parser for given code and mime type (lang).
 *
 * @param {string} code The source being highlighted.
 * @param {string=} mime A mime type.
 * @return {!prism.Lang}
 */function detectLang(code,mime){// console.log('Detecting lang for: ', lang);
if(!mime){ensureLanguage("markup");return Prism.languages.html}if(-1!==mime.indexOf("html")){ensureLanguage("markup");return Prism.languages.html}if(-1!==mime.indexOf("js")||0===mime.indexOf("es")){ensureLanguage("javascript");return Prism.languages.javascript}else if(-1!==mime.indexOf("css")){ensureLanguage("css");return Prism.languages.css}else if("c"===mime){// console.log('Lang detected: clike');
ensureLanguage("clike");return Prism.languages.clike}{// text/html; charset=ISO-8859-2
// application/vnd.dart;charset=utf-8
// text/x-java-source;charset=utf-8
var i=mime.indexOf("/");if(-1!==i){mime=mime.substr(i+1);i=mime.indexOf(";");if(-1!==i){mime=mime.substr(0,i).trim()}}}// remove "vnd." prefix
if(0===mime.indexOf("vnd.")){mime=mime.substr(4)}if(0===mime.toLowerCase().indexOf("x-")){mime=mime.substr(2)}var srcI=mime.toLowerCase().indexOf("-source");if(0<srcI){mime=mime.substr(0,srcI)}if(-1===Prism.plugins.mods.langs.indexOf(mime)){// console.log('No lang found for mime: ', mime);
// console.log('Lang detected: html');
ensureLanguage("markup");return Prism.languages.html}ensureLanguage(mime);if(mime in Prism.languages){return Prism.languages[mime]}return Prism.languages.html}function tokenize(data){var lang=data.language,code=data.code;lang=detectLang(code,lang);Prism.hooks.run("before-highlight",{code:code,grammar:lang});return Prism.tokenize(code,lang)}function makeTokens(obj){if(babelHelpers.instanceof(obj,Array)){return obj.map(makeTokens)}else if("string"===typeof obj){return obj}else{return new Prism.Token(obj.type,makeTokens(obj.content||""),obj.alias)}}function stringify(data){data=makeTokens(data.tokens);return Prism.Token.stringify(Prism.util.encode(data))}self.onmessage=function(e){if(errored){return}var data=e.data,result={payload:data.payload};switch(data.payload){case"tokenize":result.tokens=tokenize(data);break;case"stringify":result.html=stringify(data);break;}self.postMessage(result)};