// CodeMirror, copyright (c) by Marijn Haverbeke and others
// Distributed under an MIT license: https://codemirror.net/LICENSE
(function(mod){if("object"==typeof exports&&"object"==typeof module)// CommonJS
mod(require("../../lib/codemirror"));else if("function"==typeof define&&define.amd)// AMD
define(["../../lib/codemirror"],mod);else// Plain browser env
mod(CodeMirror)})(function(CodeMirror){"use strict";CodeMirror.defineMode("puppet",function(){// Stores the words from the define method
var words={},variable_regex=/({)?([a-z][a-z0-9_]*)?((::[a-z][a-z0-9_]*)*::)?[a-zA-Z0-9_]+(})?/;// Taken, mostly, from the Puppet official variable standards regex
// Takes a string of words separated by spaces and adds them as
// keys with the value of the first argument 'style'
function define(style,string){for(var split=string.split(" "),i=0;i<split.length;i++){words[split[i]]=style}}// Takes commonly known puppet types/words and classifies them to a style
define("keyword","class define site node include import inherits");define("keyword","case if else in and elsif default or");define("atom","false true running present absent file directory undef");define("builtin","action augeas burst chain computer cron destination dport exec "+"file filebucket group host icmp iniface interface jump k5login limit log_level "+"log_prefix macauthorization mailalias maillist mcx mount nagios_command "+"nagios_contact nagios_contactgroup nagios_host nagios_hostdependency "+"nagios_hostescalation nagios_hostextinfo nagios_hostgroup nagios_service "+"nagios_servicedependency nagios_serviceescalation nagios_serviceextinfo "+"nagios_servicegroup nagios_timeperiod name notify outiface package proto reject "+"resources router schedule scheduled_task selboolean selmodule service source "+"sport ssh_authorized_key sshkey stage state table tidy todest toports tosource "+"user vlan yumrepo zfs zone zpool");// After finding a start of a string ('|") this function attempts to find the end;
// If a variable is encountered along the way, we display it differently when it
// is encapsulated in a double-quoted string.
function tokenString(stream,state){var current,prev,found_var=/* eat */ /* ignoreName */ /* ignoreName */!1/* skipSlots */ /* skipSlots */;while(!stream.eol()&&(current=stream.next())!=state.pending){if("$"===current&&"\\"!=prev&&"\""==state.pending){found_var=!0/* skipSlots */;break}prev=current}if(found_var){stream.backUp(1)}if(current==state.pending){state.continueString=!1}else{state.continueString=!0}return"string"}// Main function
function tokenize(stream,state){// Matches one whole word
var word=stream.match(/[\w]+/,!1),attribute=stream.match(/(\s+)?\w+\s+=>.*/,!1),resource=stream.match(/(\s+)?[\w:_]+(\s+)?{/,!1),special_resource=stream.match(/(\s+)?[@]{1,2}[\w:_]+(\s+)?{/,!1),ch=stream.next();// Matches attributes (i.e. ensure => present ; 'ensure' would be matched)
// Have we found a variable?
if("$"===ch){if(stream.match(variable_regex)){// If so, and its in a string, assign it a different color
return state.continueString?"variable-2":"variable"}// Otherwise return an invalid variable
return"error"}// Should we still be looking for the end of a string?
if(state.continueString){// If so, go through the loop again
stream.backUp(1);return tokenString(stream,state)}// Are we in a definition (class, node, define)?
if(state.inDefinition){// If so, return def (i.e. for 'class myclass {' ; 'myclass' would be matched)
if(stream.match(/(\s+)?[\w:_]+(\s+)?/)){return"def"}// Match the rest it the next time around
stream.match(/\s+{/);state.inDefinition=!1}// Are we in an 'include' statement?
if(state.inInclude){// Match and return the included class
stream.match(/(\s+)?\S+(\s+)?/);state.inInclude=!1;return"def"}// Do we just have a function on our hands?
// In 'ensure_resource("myclass")', 'ensure_resource' is matched
if(stream.match(/(\s+)?\w+\(/)){stream.backUp(1);return"def"}// Have we matched the prior attribute regex?
if(attribute){stream.match(/(\s+)?\w+/);return"tag"}// Do we have Puppet specific words?
if(word&&words.hasOwnProperty(word)){// Negates the initial next()
stream.backUp(1);// rs move the stream
stream.match(/[\w]+/);// We want to process these words differently
// do to the importance they have in Puppet
if(stream.match(/\s+\S+\s+{/,!1)){state.inDefinition=!0}if("include"==word){state.inInclude=!0}// Returns their value as state in the prior define methods
return words[word]}// Is there a match on a reference?
if(/(^|\s+)[A-Z][\w:_]+/.test(word)){// Negate the next()
stream.backUp(1);// Match the full reference
stream.match(/(^|\s+)[A-Z][\w:_]+/);return"def"}// Have we matched the prior resource regex?
if(resource){stream.match(/(\s+)?[\w:_]+/);return"def"}// Have we matched the prior special_resource regex?
if(special_resource){stream.match(/(\s+)?[@]{1,2}/);return"special"}// Match all the comments. All of them.
if("#"==ch){stream.skipToEnd();return"comment"}// Have we found a string?
if("'"==ch||"\""==ch){// Store the type (single or double)
state.pending=ch;// Perform the looping function to find the end
return tokenString(stream,state)}// Match all the brackets
if("{"==ch||"}"==ch){return"bracket"}// Match characters that we are going to assume
// are trying to be regex
if("/"==ch){stream.match(/.*?\//);return"variable-3"}// Match all the numbers
if(ch.match(/[0-9]/)){stream.eatWhile(/[0-9]+/);return"number"}// Match the '=' and '=>' operators
if("="==ch){if(">"==stream.peek()){stream.next()}return"operator"}// Keep advancing through all the rest
stream.eatWhile(/[\w-]/);// Return a blank line for everything else
return null}// Start it all
return{startState:function(){var state={inDefinition:!1,inInclude:!1,continueString:!1,pending:!1};return state},token:function(stream,state){// Strip the spaces, but regex will account for them eitherway
if(stream.eatSpace())return null;// Go through the main process
return tokenize(stream,state)}}});CodeMirror.defineMIME("text/x-puppet","puppet")});