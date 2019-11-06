(function(Prism){var javaDocLike=Prism.languages.javadoclike={parameter:{pattern:/(^\s*(?:\/{3}|\*|\/\*\*)\s*@(?:param|arg|arguments)\s+)\w+/m,lookbehind:!0/* ignoreName */ /* skipSlots */},keyword:{// keywords are the first word in a line preceded be an `@` or surrounded by curly braces.
// @word, {@word}
pattern:/(^\s*(?:\/{3}|\*|\/\*\*)\s*|\{)@[a-z][a-zA-Z-]+\b/m,lookbehind:!0},punctuation:/[{}]/};/**
	 * Adds doc comment support to the given language and calls a given callback on each doc comment pattern.
	 *
	 * @param {string} lang the language add doc comment support to.
	 * @param {(pattern: {inside: {rest: undefined}}) => void} callback the function called with each doc comment pattern as argument.
	 */function docCommentSupport(lang,callback){var tokenName="doc-comment",grammar=Prism.languages[lang];if(!grammar){return}var token=grammar[tokenName];if(!token){// add doc comment: /** */
var definition={};definition[tokenName]={pattern:/(^|[^\\])\/\*\*[^/][\s\S]*?(?:\*\/|$)/,alias:"comment"};grammar=Prism.languages.insertBefore(lang,"comment",definition);token=grammar[tokenName]}if(babelHelpers.instanceof(token,RegExp)){// convert regex to object
token=grammar[tokenName]={pattern:token}}if(Array.isArray(token)){for(var i=0,l=token.length;i<l;i++){if(babelHelpers.instanceof(token[i],RegExp)){token[i]={pattern:token[i]}}callback(token[i])}}else{callback(token)}}/**
	 * Adds doc-comment support to the given languages for the given documentation language.
	 *
	 * @param {string[]|string} languages
	 * @param {Object} docLanguage
	 */function addSupport(languages,docLanguage){if("string"===typeof languages){languages=[languages]}languages.forEach(function(lang){docCommentSupport(lang,function(pattern){if(!pattern.inside){pattern.inside={}}pattern.inside.rest=docLanguage})})}Object.defineProperty(javaDocLike,"addSupport",{value:addSupport});javaDocLike.addSupport(["java","javascript","php"],javaDocLike)})(Prism);