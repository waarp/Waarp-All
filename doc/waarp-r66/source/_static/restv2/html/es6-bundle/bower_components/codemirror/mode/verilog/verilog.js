// CodeMirror, copyright (c) by Marijn Haverbeke and others
// Distributed under an MIT license: https://codemirror.net/LICENSE
(function(mod){if("object"==typeof exports&&"object"==typeof module)// CommonJS
mod(require("../../lib/codemirror"));else if("function"==typeof define&&define.amd)// AMD
define(["../../lib/codemirror"],mod);else// Plain browser env
mod(CodeMirror)})(function(CodeMirror){"use strict";CodeMirror.defineMode("verilog",function(config,parserConfig){var indentUnit=config.indentUnit,statementIndentUnit=parserConfig.statementIndentUnit||indentUnit,dontAlignCalls=parserConfig.dontAlignCalls,noIndentKeywords=parserConfig.noIndentKeywords||[],multiLineStrings=parserConfig.multiLineStrings,hooks=parserConfig.hooks||{};function words(str){for(var obj={},words=str.split(" "),i=0;i<words.length;++i)obj[words[i]]=!0/* ignoreName */ /* skipSlots */;return obj}/**
   * Keywords from IEEE 1800-2012
   */var keywords=words("accept_on alias always always_comb always_ff always_latch and assert assign assume automatic before begin bind "+"bins binsof bit break buf bufif0 bufif1 byte case casex casez cell chandle checker class clocking cmos config "+"const constraint context continue cover covergroup coverpoint cross deassign default defparam design disable "+"dist do edge else end endcase endchecker endclass endclocking endconfig endfunction endgenerate endgroup "+"endinterface endmodule endpackage endprimitive endprogram endproperty endspecify endsequence endtable endtask "+"enum event eventually expect export extends extern final first_match for force foreach forever fork forkjoin "+"function generate genvar global highz0 highz1 if iff ifnone ignore_bins illegal_bins implements implies import "+"incdir include initial inout input inside instance int integer interconnect interface intersect join join_any "+"join_none large let liblist library local localparam logic longint macromodule matches medium modport module "+"nand negedge nettype new nexttime nmos nor noshowcancelled not notif0 notif1 null or output package packed "+"parameter pmos posedge primitive priority program property protected pull0 pull1 pulldown pullup "+"pulsestyle_ondetect pulsestyle_onevent pure rand randc randcase randsequence rcmos real realtime ref reg "+"reject_on release repeat restrict return rnmos rpmos rtran rtranif0 rtranif1 s_always s_eventually s_nexttime "+"s_until s_until_with scalared sequence shortint shortreal showcancelled signed small soft solve specify "+"specparam static string strong strong0 strong1 struct super supply0 supply1 sync_accept_on sync_reject_on "+"table tagged task this throughout time timeprecision timeunit tran tranif0 tranif1 tri tri0 tri1 triand trior "+"trireg type typedef union unique unique0 unsigned until until_with untyped use uwire var vectored virtual void "+"wait wait_order wand weak weak0 weak1 while wildcard wire with within wor xnor xor"),isOperatorChar=/[\+\-\*\/!~&|^%=?:]/,isBracketChar=/[\[\]{}()]/,unsignedNumber=/\d[0-9_]*/,decimalLiteral=/\d*\s*'s?d\s*\d[0-9_]*/i,binaryLiteral=/\d*\s*'s?b\s*[xz01][xz01_]*/i,octLiteral=/\d*\s*'s?o\s*[xz0-7][xz0-7_]*/i,hexLiteral=/\d*\s*'s?h\s*[0-9a-fxz?][0-9a-fxz?_]*/i,realLiteral=/(\d[\d_]*(\.\d[\d_]*)?E-?[\d_]+)|(\d[\d_]*\.\d[\d_]*)/i,closingBracketOrWord=/^((\w+)|[)}\]])/,closingBracket=/[)}\]]/,curPunc,curKeyword,blockKeywords=words("case checker class clocking config function generate interface module package "+"primitive program property specify sequence table task"),openClose={};/** Operators from IEEE 1800-2012
     unary_operator ::=
       + | - | ! | ~ | & | ~& | | | ~| | ^ | ~^ | ^~
     binary_operator ::=
       + | - | * | / | % | == | != | === | !== | ==? | !=? | && | || | **
       | < | <= | > | >= | & | | | ^ | ^~ | ~^ | >> | << | >>> | <<<
       | -> | <->
     inc_or_dec_operator ::= ++ | --
     unary_module_path_operator ::=
       ! | ~ | & | ~& | | | ~| | ^ | ~^ | ^~
     binary_module_path_operator ::=
       == | != | && | || | & | | | ^ | ^~ | ~^
  */for(var keyword in blockKeywords){openClose[keyword]="end"+keyword}openClose.begin="end";openClose.casex="endcase";openClose.casez="endcase";openClose["do"]="while";openClose.fork="join;join_any;join_none";openClose.covergroup="endgroup";for(var i in noIndentKeywords){var keyword=noIndentKeywords[i];if(openClose[keyword]){openClose[keyword]=void 0}}// Keywords which open statements that are ended with a semi-colon
var statementKeywords=words("always always_comb always_ff always_latch assert assign assume else export for foreach forever if import initial repeat while");function tokenBase(stream,state){var ch=stream.peek(),style;if(hooks[ch]&&/* eat */ /* ignoreName */!1/* skipSlots */ /* skipSlots */!=(style=hooks[ch](stream,state)))return style;if(hooks.tokenBase&&!1!=(style=hooks.tokenBase(stream,state)))return style;if(/[,;:\.]/.test(ch)){curPunc=stream.next();return null}if(isBracketChar.test(ch)){curPunc=stream.next();return"bracket"}// Macros (tick-defines)
if("`"==ch){stream.next();if(stream.eatWhile(/[\w\$_]/)){return"def"}else{return null}}// System calls
if("$"==ch){stream.next();if(stream.eatWhile(/[\w\$_]/)){return"meta"}else{return null}}// Time literals
if("#"==ch){stream.next();stream.eatWhile(/[\d_.]/);return"def"}// Strings
if("\""==ch){stream.next();state.tokenize=tokenString(ch);return state.tokenize(stream,state)}// Comments
if("/"==ch){stream.next();if(stream.eat("*")){state.tokenize=tokenComment;return tokenComment(stream,state)}if(stream.eat("/")){stream.skipToEnd();return"comment"}stream.backUp(1)}// Numeric literals
if(stream.match(realLiteral)||stream.match(decimalLiteral)||stream.match(binaryLiteral)||stream.match(octLiteral)||stream.match(hexLiteral)||stream.match(unsignedNumber)||stream.match(realLiteral)){return"number"}// Operators
if(stream.eatWhile(isOperatorChar)){return"meta"}// Keywords / plain variables
if(stream.eatWhile(/[\w\$_]/)){var cur=stream.current();if(keywords[cur]){if(openClose[cur]){curPunc="newblock"}if(statementKeywords[cur]){curPunc="newstatement"}curKeyword=cur;return"keyword"}return"variable"}stream.next();return null}function tokenString(quote){return function(stream,state){var escaped=!1,next,end=!1;while(null!=(next=stream.next())){if(next==quote&&!escaped){end=!0;break}escaped=!escaped&&"\\"==next}if(end||!(escaped||multiLineStrings))state.tokenize=tokenBase;return"string"}}function tokenComment(stream,state){var maybeEnd=!1,ch;while(ch=stream.next()){if("/"==ch&&maybeEnd){state.tokenize=tokenBase;break}maybeEnd="*"==ch}return"comment"}function Context(indented,column,type,align,prev){this.indented=indented;this.column=column;this.type=type;this.align=align;this.prev=prev}function pushContext(state,col,type){var indent=state.indented,c=new Context(indent,col,type,null,state.context);return state.context=c}function popContext(state){var t=state.context.type;if(")"==t||"]"==t||"}"==t){state.indented=state.context.indented}return state.context=state.context.prev}function isClosing(text,contextClosing){if(text==contextClosing){return!0}else{// contextClosing may be multiple keywords separated by ;
var closingKeywords=contextClosing.split(";");for(var i in closingKeywords){if(text==closingKeywords[i]){return!0}}return!1}}function buildElectricInputRegEx(){// Reindentation should occur on any bracket char: {}()[]
// or on a match of any of the block closing keywords, at
// the end of a line
var allClosings=[];for(var i in openClose){if(openClose[i]){var closings=openClose[i].split(";");for(var j in closings){allClosings.push(closings[j])}}}var re=new RegExp("[{}()\\[\\]]|("+allClosings.join("|")+")$");return re}// Interface
return{// Regex to force current line to reindent
electricInput:buildElectricInputRegEx(),startState:function(basecolumn){var state={tokenize:null,context:new Context((basecolumn||0)-indentUnit,0,"top",!1),indented:0,startOfLine:!0};if(hooks.startState)hooks.startState(state);return state},token:function(stream,state){var ctx=state.context;if(stream.sol()){if(null==ctx.align)ctx.align=!1;state.indented=stream.indentation();state.startOfLine=!0}if(hooks.token){// Call hook, with an optional return value of a style to override verilog styling.
var style=hooks.token(stream,state);if(style!==void 0){return style}}if(stream.eatSpace())return null;curPunc=null;curKeyword=null;var style=(state.tokenize||tokenBase)(stream,state);if("comment"==style||"meta"==style||"variable"==style)return style;if(null==ctx.align)ctx.align=!0;if(curPunc==ctx.type){popContext(state)}else if(";"==curPunc&&"statement"==ctx.type||ctx.type&&isClosing(curKeyword,ctx.type)){ctx=popContext(state);while(ctx&&"statement"==ctx.type)ctx=popContext(state)}else if("{"==curPunc){pushContext(state,stream.column(),"}")}else if("["==curPunc){pushContext(state,stream.column(),"]")}else if("("==curPunc){pushContext(state,stream.column(),")")}else if(ctx&&"endcase"==ctx.type&&":"==curPunc){pushContext(state,stream.column(),"statement")}else if("newstatement"==curPunc){pushContext(state,stream.column(),"statement")}else if("newblock"==curPunc){if("function"==curKeyword&&ctx&&("statement"==ctx.type||"endgroup"==ctx.type)){// The 'function' keyword can appear in some other contexts where it actually does not
// indicate a function (import/export DPI and covergroup definitions).
// Do nothing in this case
}else if("task"==curKeyword&&ctx&&"statement"==ctx.type){// Same thing for task
}else{var close=openClose[curKeyword];pushContext(state,stream.column(),close)}}state.startOfLine=!1;return style},indent:function(state,textAfter){if(state.tokenize!=tokenBase&&null!=state.tokenize)return CodeMirror.Pass;if(hooks.indent){var fromHook=hooks.indent(state);if(0<=fromHook)return fromHook}var ctx=state.context,firstChar=textAfter&&textAfter.charAt(0);if("statement"==ctx.type&&"}"==firstChar)ctx=ctx.prev;var closing=!1,possibleClosing=textAfter.match(closingBracketOrWord);if(possibleClosing)closing=isClosing(possibleClosing[0],ctx.type);if("statement"==ctx.type)return ctx.indented+("{"==firstChar?0:statementIndentUnit);else if(closingBracket.test(ctx.type)&&ctx.align&&!dontAlignCalls)return ctx.column+(closing?0:1);else if(")"==ctx.type&&!closing)return ctx.indented+statementIndentUnit;else return ctx.indented+(closing?0:indentUnit)},blockCommentStart:"/*",blockCommentEnd:"*/",lineComment:"//"}});CodeMirror.defineMIME("text/x-verilog",{name:"verilog"});CodeMirror.defineMIME("text/x-systemverilog",{name:"verilog"});// TL-Verilog mode.
// See tl-x.org for language spec.
// See the mode in action at makerchip.com.
// Contact: steve.hoover@redwoodeda.com
// TLV Identifier prefixes.
// Note that sign is not treated separately, so "+/-" versions of numeric identifiers
// are included.
var tlvIdentifierStyle={"|":"link",">":"property",// Should condition this off for > TLV 1c.
$:"variable",$$:"variable","?$":"qualifier","?*":"qualifier","-":"hr","/":"property","/-":"property","@":"variable-3","@-":"variable-3","@++":"variable-3","@+=":"variable-3","@+=-":"variable-3","@--":"variable-3","@-=":"variable-3","%+":"tag","%-":"tag","%":"tag",">>":"tag","<<":"tag","<>":"tag","#":"tag",// Need to choose a style for this.
"^":"attribute","^^":"attribute","^!":"attribute","*":"variable-2","**":"variable-2","\\":"keyword",'"':"comment"},tlvScopePrefixChars={"/":"beh-hier",">":"beh-hier","-":"phys-hier","|":"pipe","?":"when","@":"stage","\\":"keyword"},tlvIndentUnit=3,tlvTrackStatements=!1,tlvIdentMatch=/^([~!@#\$%\^&\*-\+=\?\/\\\|'"<>]+)([\d\w_]*)/,tlvFirstLevelIndentMatch=/^[! ]  /,tlvLineIndentationMatch=/^[! ] */,tlvCommentMatch=/^\/[\/\*]/;// Lines starting with these characters define scope (result in indentation).
// Returns a style specific to the scope at the given indentation column.
// Type is one of: "indent", "scope-ident", "before-scope-ident".
function tlvScopeStyle(state,indentation,type){// Begin scope.
var depth=indentation/tlvIndentUnit;// TODO: Pass this in instead.
return"tlv-"+state.tlvIndentationStyle[depth]+"-"+type}// Return true if the next thing in the stream is an identifier with a mnemonic.
function tlvIdentNext(stream){var match;return(match=stream.match(tlvIdentMatch,!1))&&0<match[2].length}CodeMirror.defineMIME("text/x-tlv",{name:"verilog",hooks:{electricInput:!1,// Return undefined for verilog tokenizing, or style for TLV token (null not used).
// Standard CM styles are used for most formatting, but some TL-Verilog-specific highlighting
// can be enabled with the definition of cm-tlv-* styles, including highlighting for:
//   - M4 tokens
//   - TLV scope indentation
//   - Statement delimitation (enabled by tlvTrackStatements)
token:function(stream,state){var style=void 0,match;// Return value of pattern matches.
// Set highlighting mode based on code region (TLV or SV).
if(stream.sol()&&!state.tlvInBlockComment){// Process region.
if("\\"==stream.peek()){style="def";stream.skipToEnd();if(stream.string.match(/\\SV/)){state.tlvCodeActive=!1}else if(stream.string.match(/\\TLV/)){state.tlvCodeActive=!0}}// Correct indentation in the face of a line prefix char.
if(state.tlvCodeActive&&0==stream.pos&&0==state.indented&&(match=stream.match(tlvLineIndentationMatch,!1))){state.indented=match[0].length}// Compute indentation state:
//   o Auto indentation on next line
//   o Indentation scope styles
var indented=state.indented,depth=indented/tlvIndentUnit;if(depth<=state.tlvIndentationStyle.length){// not deeper than current scope
var blankline=stream.string.length==indented,chPos=depth*tlvIndentUnit;if(chPos<stream.string.length){var bodyString=stream.string.slice(chPos),ch=bodyString[0];if(tlvScopePrefixChars[ch]&&(match=bodyString.match(tlvIdentMatch))&&tlvIdentifierStyle[match[1]]){// This line begins scope.
// Next line gets indented one level.
indented+=tlvIndentUnit;// Style the next level of indentation (except non-region keyword identifiers,
//   which are statements themselves)
if(!("\\"==ch&&0<chPos)){state.tlvIndentationStyle[depth]=tlvScopePrefixChars[ch];if(tlvTrackStatements){state.statementComment=!1}depth++}}}// Clear out deeper indentation levels unless line is blank.
if(!blankline){while(state.tlvIndentationStyle.length>depth){state.tlvIndentationStyle.pop()}}}// Set next level of indentation.
state.tlvNextIndent=indented}if(state.tlvCodeActive){// Highlight as TLV.
var beginStatement=!1;if(tlvTrackStatements){// This starts a statement if the position is at the scope level
// and we're not within a statement leading comment.
beginStatement=" "!=stream.peek()&&// not a space
style===void 0&&// not a region identifier
!state.tlvInBlockComment&&// not in block comment
//!stream.match(tlvCommentMatch, false) && // not comment start
stream.column()==state.tlvIndentationStyle.length*tlvIndentUnit;// at scope level
if(beginStatement){if(state.statementComment){// statement already started by comment
beginStatement=!1}state.statementComment=stream.match(tlvCommentMatch,!1);// comment start
}}var match;if(style!==void 0){// Region line.
style+=" "+tlvScopeStyle(state,0,"scope-ident")}else if(stream.pos/tlvIndentUnit<state.tlvIndentationStyle.length&&(match=stream.match(stream.sol()?tlvFirstLevelIndentMatch:/^   /))){// Indentation
style=// make this style distinct from the previous one to prevent
// codemirror from combining spans
"tlv-indent-"+(0==stream.pos%2?"even":"odd")+// and style it
" "+tlvScopeStyle(state,stream.pos-tlvIndentUnit,"indent");// Style the line prefix character.
if("!"==match[0].charAt(0)){style+=" tlv-alert-line-prefix"}// Place a class before a scope identifier.
if(tlvIdentNext(stream)){style+=" "+tlvScopeStyle(state,stream.pos,"before-scope-ident")}}else if(state.tlvInBlockComment){// In a block comment.
if(stream.match(/^.*?\*\//)){// Exit block comment.
state.tlvInBlockComment=!1;if(tlvTrackStatements&&!stream.eol()){// Anything after comment is assumed to be real statement content.
state.statementComment=!1}}else{stream.skipToEnd()}style="comment"}else if((match=stream.match(tlvCommentMatch))&&!state.tlvInBlockComment){// Start comment.
if("//"==match[0]){// Line comment.
stream.skipToEnd()}else{// Block comment.
state.tlvInBlockComment=!0}style="comment"}else if(match=stream.match(tlvIdentMatch)){// looks like an identifier (or identifier prefix)
var prefix=match[1],mnemonic=match[2];if(// is identifier prefix
tlvIdentifierStyle.hasOwnProperty(prefix)&&(// has mnemonic or we're at the end of the line (maybe it hasn't been typed yet)
0<mnemonic.length||stream.eol())){style=tlvIdentifierStyle[prefix];if(stream.column()==state.indented){// Begin scope.
style+=" "+tlvScopeStyle(state,stream.column(),"scope-ident")}}else{// Just swallow one character and try again.
// This enables subsequent identifier match with preceding symbol character, which
//   is legal within a statement.  (Eg, !$reset).  It also enables detection of
//   comment start with preceding symbols.
stream.backUp(stream.current().length-1);style="tlv-default"}}else if(stream.match(/^\t+/)){// Highlight tabs, which are illegal.
style="tlv-tab"}else if(stream.match(/^[\[\]{}\(\);\:]+/)){// [:], (), {}, ;.
style="meta"}else if(match=stream.match(/^[mM]4([\+_])?[\w\d_]*/)){// m4 pre proc
style="+"==match[1]?"tlv-m4-plus":"tlv-m4"}else if(stream.match(/^ +/)){// Skip over spaces.
if(stream.eol()){// Trailing spaces.
style="error"}else{// Non-trailing spaces.
style="tlv-default"}}else if(stream.match(/^[\w\d_]+/)){// alpha-numeric token.
style="number"}else{// Eat the next char w/ no formatting.
stream.next();style="tlv-default"}if(beginStatement){style+=" tlv-statement"}}else{if(stream.match(/^[mM]4([\w\d_]*)/)){// m4 pre proc
style="tlv-m4"}}return style},indent:function(state){return!0==state.tlvCodeActive?state.tlvNextIndent:-1},startState:function(state){state.tlvIndentationStyle=[];// Styles to use for each level of indentation.
state.tlvCodeActive=!0;// True when we're in a TLV region (and at beginning of file).
state.tlvNextIndent=-1;// The number of spaces to autoindent the next line if tlvCodeActive.
state.tlvInBlockComment=!1;// True inside /**/ comment.
if(tlvTrackStatements){state.statementComment=!1;// True inside a statement's header comment.
}}}})});