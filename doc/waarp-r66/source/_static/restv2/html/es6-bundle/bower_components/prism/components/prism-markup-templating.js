(function(Prism){/**
	 * Returns the placeholder for the given language id and index.
	 *
	 * @param {string} language
	 * @param {string|number} index
	 * @returns {string}
	 */function getPlaceholder(language,index){return"___"+language.toUpperCase()+index+"___"}Object.defineProperties(Prism.languages["markup-templating"]={},{buildPlaceholders:{/**
			 * Tokenize all inline templating expressions matching `placeholderPattern`.
			 *
			 * If `replaceFilter` is provided, only matches of `placeholderPattern` for which `replaceFilter` returns
			 * `true` will be replaced.
			 *
			 * @param {object} env The environment of the `before-tokenize` hook.
			 * @param {string} language The language id.
			 * @param {RegExp} placeholderPattern The matches of this pattern will be replaced by placeholders.
			 * @param {(match: string) => boolean} [replaceFilter]
			 */value:function(env,language,placeholderPattern,replaceFilter){if(env.language!==language){return}var tokenStack=env.tokenStack=[];env.code=env.code.replace(placeholderPattern,function(match){if("function"===typeof replaceFilter&&!replaceFilter(match)){return match}var i=tokenStack.length,placeholder;// Check for existing strings
while(-1!==env.code.indexOf(placeholder=getPlaceholder(language,i)))++i;// Create a sparse array
tokenStack[i]=match;return placeholder});// Switch the grammar to markup
env.grammar=Prism.languages.markup}},tokenizePlaceholders:{/**
			 * Replace placeholders with proper tokens after tokenizing.
			 *
			 * @param {object} env The environment of the `after-tokenize` hook.
			 * @param {string} language The language id.
			 */value:function(env,language){if(env.language!==language||!env.tokenStack){return}// Switch the grammar back
env.grammar=Prism.languages[language];var j=0,keys=Object.keys(env.tokenStack);function walkTokens(tokens){for(var i=0;i<tokens.length;i++){// all placeholders are replaced already
if(j>=keys.length){break}var token=tokens[i];if("string"===typeof token||token.content&&"string"===typeof token.content){var k=keys[j],t=env.tokenStack[k],s="string"===typeof token?token:token.content,placeholder=getPlaceholder(language,k),index=s.indexOf(placeholder);if(-1<index){++j;var before=s.substring(0,index),middle=new Prism.Token(language,Prism.tokenize(t,env.grammar),"language-"+language,t),after=s.substring(index+placeholder.length),replacement=[];if(before){replacement.push.apply(replacement,walkTokens([before]))}replacement.push(middle);if(after){replacement.push.apply(replacement,walkTokens([after]))}if("string"===typeof token){tokens.splice.apply(tokens,[i,1].concat(replacement))}else{token.content=replacement}}}else if(token.content/* && typeof token.content !== 'string' */){walkTokens(token.content)}}return tokens}walkTokens(env.tokens)}}})})(Prism);