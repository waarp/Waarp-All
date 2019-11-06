(function(Prism){Prism.languages.xquery=Prism.languages.extend("markup",{"xquery-comment":{pattern:/\(:[\s\S]*?:\)/,greedy:!0,alias:"comment"},string:{pattern:/(["'])(?:\1\1|(?!\1)[\s\S])*\1/,greedy:!0},extension:{pattern:/\(#.+?#\)/,alias:"symbol"},variable:/\$[\w-:]+/,axis:{pattern:/(^|[^-])(?:ancestor(?:-or-self)?|attribute|child|descendant(?:-or-self)?|following(?:-sibling)?|parent|preceding(?:-sibling)?|self)(?=::)/,lookbehind:!0,alias:"operator"},"keyword-operator":{pattern:/(^|[^:-])\b(?:and|castable as|div|eq|except|ge|gt|idiv|instance of|intersect|is|le|lt|mod|ne|or|union)\b(?=$|[^:-])/,lookbehind:!0,alias:"operator"},keyword:{pattern:/(^|[^:-])\b(?:as|ascending|at|base-uri|boundary-space|case|cast as|collation|construction|copy-namespaces|declare|default|descending|else|empty (?:greatest|least)|encoding|every|external|for|function|if|import|in|inherit|lax|let|map|module|namespace|no-inherit|no-preserve|option|order(?: by|ed|ing)?|preserve|return|satisfies|schema|some|stable|strict|strip|then|to|treat as|typeswitch|unordered|validate|variable|version|where|xquery)\b(?=$|[^:-])/,lookbehind:!0},function:/[\w-]+(?::[\w-]+)*(?=\s*\()/,"xquery-element":{pattern:/(element\s+)[\w-]+(?::[\w-]+)*/,lookbehind:!0,alias:"tag"},"xquery-attribute":{pattern:/(attribute\s+)[\w-]+(?::[\w-]+)*/,lookbehind:!0,alias:"attr-name"},builtin:{pattern:/(^|[^:-])\b(?:attribute|comment|document|element|processing-instruction|text|xs:(?:anyAtomicType|anyType|anyURI|base64Binary|boolean|byte|date|dateTime|dayTimeDuration|decimal|double|duration|ENTITIES|ENTITY|float|gDay|gMonth|gMonthDay|gYear|gYearMonth|hexBinary|ID|IDREFS?|int|integer|language|long|Name|NCName|negativeInteger|NMTOKENS?|nonNegativeInteger|nonPositiveInteger|normalizedString|NOTATION|positiveInteger|QName|short|string|time|token|unsigned(?:Byte|Int|Long|Short)|untyped(?:Atomic)?|yearMonthDuration))\b(?=$|[^:-])/,lookbehind:!0},number:/\b\d+(?:\.\d+)?(?:E[+-]?\d+)?/,operator:[/[+*=?|@]|\.\.?|:=|!=|<[=<]?|>[=>]?/,{pattern:/(\s)-(?=\s)/,lookbehind:!0}],punctuation:/[[\](){},;:/]/});Prism.languages.xquery.tag.pattern=/<\/?(?!\d)[^\s>\/=$<%]+(?:\s+[^\s>\/=]+(?:=(?:("|')(?:\\[\s\S]|{(?!{)(?:{(?:{[^}]*}|[^}])*}|[^}])+}|(?!\1)[^\\])*\1|[^\s'">=]+))?)*\s*\/?>/i;Prism.languages.xquery.tag.inside["attr-value"].pattern=/=(?:("|')(?:\\[\s\S]|{(?!{)(?:{(?:{[^}]*}|[^}])*}|[^}])+}|(?!\1)[^\\])*\1|[^\s'">=]+)/i;Prism.languages.xquery.tag.inside["attr-value"].inside.punctuation=/^="|"$/;Prism.languages.xquery.tag.inside["attr-value"].inside.expression={// Allow for two levels of nesting
pattern:/{(?!{)(?:{(?:{[^}]*}|[^}])*}|[^}])+}/,inside:{rest:Prism.languages.xquery},alias:"language-xquery"};// The following will handle plain text inside tags
var stringifyToken=function(token){if("string"===typeof token){return token}if("string"===typeof token.content){return token.content}return token.content.map(stringifyToken).join("")},walkTokens=function(tokens){for(var openedTags=[],i=0;i<tokens.length;i++){var token=tokens[i],notTagNorBrace=!1;if("string"!==typeof token){if("tag"===token.type&&token.content[0]&&"tag"===token.content[0].type){// We found a tag, now find its kind
if("</"===token.content[0].content[0].content){// Closing tag
if(0<openedTags.length&&openedTags[openedTags.length-1].tagName===stringifyToken(token.content[0].content[1])){// Pop matching opening tag
openedTags.pop()}}else{if("/>"===token.content[token.content.length-1].content){// Autoclosed tag, ignore
}else{// Opening tag
openedTags.push({tagName:stringifyToken(token.content[0].content[1]),openedBraces:0})}}}else if(0<openedTags.length&&"punctuation"===token.type&&"{"===token.content&&(// Ignore `{{`
!tokens[i+1]||"punctuation"!==tokens[i+1].type||"{"!==tokens[i+1].content)&&(!tokens[i-1]||"plain-text"!==tokens[i-1].type||"{"!==tokens[i-1].content)){// Here we might have entered an XQuery expression inside a tag
openedTags[openedTags.length-1].openedBraces++}else if(0<openedTags.length&&0<openedTags[openedTags.length-1].openedBraces&&"punctuation"===token.type&&"}"===token.content){// Here we might have left an XQuery expression inside a tag
openedTags[openedTags.length-1].openedBraces--}else if("comment"!==token.type){notTagNorBrace=!0}}if(notTagNorBrace||"string"===typeof token){if(0<openedTags.length&&0===openedTags[openedTags.length-1].openedBraces){// Here we are inside a tag, and not inside an XQuery expression.
// That's plain text: drop any tokens matched.
var plainText=stringifyToken(token);// And merge text with adjacent text
if(i<tokens.length-1&&("string"===typeof tokens[i+1]||"plain-text"===tokens[i+1].type)){plainText+=stringifyToken(tokens[i+1]);tokens.splice(i+1,1)}if(0<i&&("string"===typeof tokens[i-1]||"plain-text"===tokens[i-1].type)){plainText=stringifyToken(tokens[i-1])+plainText;tokens.splice(i-1,1);i--}if(/^\s+$/.test(plainText)){tokens[i]=plainText}else{tokens[i]=new Prism.Token("plain-text",plainText,null,plainText)}}}if(token.content&&"string"!==typeof token.content){walkTokens(token.content)}}};Prism.hooks.add("after-tokenize",function(env){if("xquery"!==env.language){return}walkTokens(env.tokens)})})(Prism);