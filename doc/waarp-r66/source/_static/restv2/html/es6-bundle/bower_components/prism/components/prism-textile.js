(function(Prism){// We don't allow for pipes inside parentheses
// to not break table pattern |(. foo |). bar |
var modifierRegex=/(?:\([^|)]+\)|\[[^\]]+\]|\{[^}]+\})+/.source,modifierTokens={css:{pattern:/\{[^}]+\}/,inside:{rest:Prism.languages.css}},"class-id":{pattern:/(\()[^)]+(?=\))/,lookbehind:!0,alias:"attr-value"},lang:{pattern:/(\[)[^\]]+(?=\])/,lookbehind:!0,alias:"attr-value"},// Anything else is punctuation (the first pattern is for row/col spans inside tables)
punctuation:/[\\\/]\d+|\S/},textile=Prism.languages.textile=Prism.languages.extend("markup",{phrase:{pattern:/(^|\r|\n)\S[\s\S]*?(?=$|\r?\n\r?\n|\r\r)/,lookbehind:!0,inside:{// h1. Header 1
"block-tag":{pattern:RegExp("^[a-z]\\w*(?:"+modifierRegex+"|[<>=()])*\\."),inside:{modifier:{pattern:RegExp("(^[a-z]\\w*)(?:"+modifierRegex+"|[<>=()])+(?=\\.)"),lookbehind:!0,inside:modifierTokens},tag:/^[a-z]\w*/,punctuation:/\.$/}},// # List item
// * List item
list:{pattern:RegExp("^[*#]+(?:"+modifierRegex+")?\\s+.+","m"),inside:{modifier:{pattern:RegExp("(^[*#]+)"+modifierRegex),lookbehind:!0,inside:modifierTokens},punctuation:/^[*#]+/}},// | cell | cell | cell |
table:{// Modifiers can be applied to the row: {color:red}.|1|2|3|
// or the cell: |{color:red}.1|2|3|
pattern:RegExp("^(?:(?:"+modifierRegex+"|[<>=()^~])+\\.\\s*)?(?:\\|(?:(?:"+modifierRegex+"|[<>=()^~_]|[\\\\/]\\d+)+\\.)?[^|]*)+\\|","m"),inside:{modifier:{// Modifiers for rows after the first one are
// preceded by a pipe and a line feed
pattern:RegExp("(^|\\|(?:\\r?\\n|\\r)?)(?:"+modifierRegex+"|[<>=()^~_]|[\\\\/]\\d+)+(?=\\.)"),lookbehind:!0,inside:modifierTokens},punctuation:/\||^\./}},inline:{pattern:RegExp("(\\*\\*|__|\\?\\?|[*_%@+\\-^~])(?:"+modifierRegex+")?.+?\\1"),inside:{// Note: superscripts and subscripts are not handled specifically
// *bold*, **bold**
bold:{pattern:RegExp("(^(\\*\\*?)(?:"+modifierRegex+")?).+?(?=\\2)"),lookbehind:!0},// _italic_, __italic__
italic:{pattern:RegExp("(^(__?)(?:"+modifierRegex+")?).+?(?=\\2)"),lookbehind:!0},// ??cite??
cite:{pattern:RegExp("(^\\?\\?(?:"+modifierRegex+")?).+?(?=\\?\\?)"),lookbehind:!0,alias:"string"},// @code@
code:{pattern:RegExp("(^@(?:"+modifierRegex+")?).+?(?=@)"),lookbehind:!0,alias:"keyword"},// +inserted+
inserted:{pattern:RegExp("(^\\+(?:"+modifierRegex+")?).+?(?=\\+)"),lookbehind:!0},// -deleted-
deleted:{pattern:RegExp("(^-(?:"+modifierRegex+")?).+?(?=-)"),lookbehind:!0},// %span%
span:{pattern:RegExp("(^%(?:"+modifierRegex+")?).+?(?=%)"),lookbehind:!0},modifier:{pattern:RegExp("(^\\*\\*|__|\\?\\?|[*_%@+\\-^~])"+modifierRegex),lookbehind:!0,inside:modifierTokens},punctuation:/[*_%?@+\-^~]+/}},// [alias]http://example.com
"link-ref":{pattern:/^\[[^\]]+\]\S+$/m,inside:{string:{pattern:/(\[)[^\]]+(?=\])/,lookbehind:!0},url:{pattern:/(\])\S+$/,lookbehind:!0},punctuation:/[\[\]]/}},// "text":http://example.com
// "text":link-ref
link:{pattern:RegExp("\"(?:"+modifierRegex+")?[^\"]+\":.+?(?=[^\\w/]?(?:\\s|$))"),inside:{text:{pattern:RegExp("(^\"(?:"+modifierRegex+")?)[^\"]+(?=\")"),lookbehind:!0},modifier:{pattern:RegExp("(^\")"+modifierRegex),lookbehind:!0,inside:modifierTokens},url:{pattern:/(:).+/,lookbehind:!0},punctuation:/[":]/}},// !image.jpg!
// !image.jpg(Title)!:http://example.com
image:{pattern:RegExp("!(?:"+modifierRegex+"|[<>=()])*[^!\\s()]+(?:\\([^)]+\\))?!(?::.+?(?=[^\\w/]?(?:\\s|$)))?"),inside:{source:{pattern:RegExp("(^!(?:"+modifierRegex+"|[<>=()])*)[^!\\s()]+(?:\\([^)]+\\))?(?=!)"),lookbehind:!0,alias:"url"},modifier:{pattern:RegExp("(^!)(?:"+modifierRegex+"|[<>=()])+"),lookbehind:!0,inside:modifierTokens},url:{pattern:/(:).+/,lookbehind:!0},punctuation:/[!:]/}},// Footnote[1]
footnote:{pattern:/\b\[\d+\]/,alias:"comment",inside:{punctuation:/\[|\]/}},// CSS(Cascading Style Sheet)
acronym:{pattern:/\b[A-Z\d]+\([^)]+\)/,inside:{comment:{pattern:/(\()[^)]+(?=\))/,lookbehind:!0},punctuation:/[()]/}},// Prism(C)
mark:{pattern:/\b\((?:TM|R|C)\)/,alias:"comment",inside:{punctuation:/[()]/}}}}}),phraseInside=textile.phrase.inside,nestedPatterns={inline:phraseInside.inline,link:phraseInside.link,image:phraseInside.image,footnote:phraseInside.footnote,acronym:phraseInside.acronym,mark:phraseInside.mark};// Only allow alpha-numeric HTML tags, not XML tags
textile.tag.pattern=/<\/?(?!\d)[a-z0-9]+(?:\s+[^\s>\/=]+(?:=(?:("|')(?:\\[\s\S]|(?!\1)[^\\])*\1|[^\s'">=]+))?)*\s*\/?>/i;// Allow some nesting
var phraseInlineInside=phraseInside.inline.inside;phraseInlineInside.bold.inside=nestedPatterns;phraseInlineInside.italic.inside=nestedPatterns;phraseInlineInside.inserted.inside=nestedPatterns;phraseInlineInside.deleted.inside=nestedPatterns;phraseInlineInside.span.inside=nestedPatterns;// Allow some styles inside table cells
var phraseTableInside=phraseInside.table.inside;phraseTableInside.inline=nestedPatterns.inline;phraseTableInside.link=nestedPatterns.link;phraseTableInside.image=nestedPatterns.image;phraseTableInside.footnote=nestedPatterns.footnote;phraseTableInside.acronym=nestedPatterns.acronym;phraseTableInside.mark=nestedPatterns.mark})(Prism);