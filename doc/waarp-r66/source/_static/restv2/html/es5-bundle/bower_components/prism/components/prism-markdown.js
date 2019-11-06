Prism.languages.markdown=Prism.languages.extend("markup",{});Prism.languages.insertBefore("markdown","prolog",{blockquote:{// > ...
pattern:/^>(?:[\t ]*>)*/m,alias:"punctuation"},code:[{// Prefixed by 4 spaces or 1 tab
pattern:/^(?: {4}|\t).+/m,alias:"keyword"},{// `code`
// ``code``
pattern:/``.+?``|`[^`\n]+`/,alias:"keyword"},{// ```optional language
// code block
// ```
pattern:/^```[\s\S]*?^```$/m,greedy:!0/* ignoreName */ /* skipSlots */,inside:{"code-block":{pattern:/^(```.*(?:\r?\n|\r))[\s\S]+?(?=(?:\r?\n|\r)^```$)/m,lookbehind:!0},"code-language":{pattern:/^(```).+/,lookbehind:!0},punctuation:/```/}}],title:[{// title 1
// =======
// title 2
// -------
pattern:/\S.*(?:\r?\n|\r)(?:==+|--+)/,alias:"important",inside:{punctuation:/==+$|--+$/}},{// # title 1
// ###### title 6
pattern:/(^\s*)#+.+/m,lookbehind:!0,alias:"important",inside:{punctuation:/^#+|#+$/}}],hr:{// ***
// ---
// * * *
// -----------
pattern:/(^\s*)([*-])(?:[\t ]*\2){2,}(?=\s*$)/m,lookbehind:!0,alias:"punctuation"},list:{// * item
// + item
// - item
// 1. item
pattern:/(^\s*)(?:[*+-]|\d+\.)(?=[\t ].)/m,lookbehind:!0,alias:"punctuation"},"url-reference":{// [id]: http://example.com "Optional title"
// [id]: http://example.com 'Optional title'
// [id]: http://example.com (Optional title)
// [id]: <http://example.com> "Optional title"
pattern:/!?\[[^\]]+\]:[\t ]+(?:\S+|<(?:\\.|[^>\\])+>)(?:[\t ]+(?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\((?:\\.|[^)\\])*\)))?/,inside:{variable:{pattern:/^(!?\[)[^\]]+/,lookbehind:!0},string:/(?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\((?:\\.|[^)\\])*\))$/,punctuation:/^[\[\]!:]|[<>]/},alias:"url"},bold:{// **strong**
// __strong__
// Allow only one line break
pattern:/(^|[^\\])(\*\*|__)(?:(?:\r?\n|\r)(?!\r?\n|\r)|.)+?\2/,lookbehind:!0,greedy:!0,inside:{punctuation:/^\*\*|^__|\*\*$|__$/}},italic:{// *em*
// _em_
// Allow only one line break
pattern:/(^|[^\\])([*_])(?:(?:\r?\n|\r)(?!\r?\n|\r)|.)+?\2/,lookbehind:!0,greedy:!0,inside:{punctuation:/^[*_]|[*_]$/}},strike:{// ~~strike through~~
// ~strike~
// Allow only one line break
pattern:/(^|[^\\])(~~?)(?:(?:\r?\n|\r)(?!\r?\n|\r)|.)+?\2/,lookbehind:!0,greedy:!0,inside:{punctuation:/^~~?|~~?$/}},url:{// [example](http://example.com "Optional title")
// [example] [id]
pattern:/!?\[[^\]]+\](?:\([^\s)]+(?:[\t ]+"(?:\\.|[^"\\])*")?\)| ?\[[^\]\n]*\])/,inside:{variable:{pattern:/(!?\[)[^\]]+(?=\]$)/,lookbehind:!0},string:{pattern:/"(?:\\.|[^"\\])*"(?=\)$)/}}}});["bold","italic","strike"].forEach(function(token){["url","bold","italic","strike"].forEach(function(inside){if(token!==inside){Prism.languages.markdown[token].inside[inside]=Prism.languages.markdown[inside]}})});Prism.hooks.add("after-tokenize",function(env){if("markdown"!==env.language&&"md"!==env.language){return}function walkTokens(tokens){if(!tokens||"string"===typeof tokens){return}for(var i=0,l=tokens.length,token;i<l;i++){token=tokens[i];if("code"!==token.type){walkTokens(token.content);continue}var codeLang=token.content[1],codeBlock=token.content[3];if(codeLang&&codeBlock&&"code-language"===codeLang.type&&"code-block"===codeBlock.type&&"string"===typeof codeLang.content){// this might be a language that Prism does not support
var alias="language-"+codeLang.content.trim().split(/\s+/)[0].toLowerCase();// add alias
if(!codeBlock.alias){codeBlock.alias=[alias]}else if("string"===typeof codeBlock.alias){codeBlock.alias=[codeBlock.alias,alias]}else{codeBlock.alias.push(alias)}}}}walkTokens(env.tokens)});Prism.hooks.add("wrap",function(env){if("code-block"!==env.type){return}for(var codeLang="",i=0,l=env.classes.length;i<l;i++){var cls=env.classes[i],match=/language-(.+)/.exec(cls);if(match){codeLang=match[1];break}}var grammar=Prism.languages[codeLang];if(!grammar){return}// reverse Prism.util.encode
var code=env.content.replace(/&lt;/g,"<").replace(/&amp;/g,"&");env.content=Prism.highlight(code,grammar,codeLang)});Prism.languages.md=Prism.languages.markdown;