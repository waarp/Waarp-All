(function(Prism){Prism.languages.http={"request-line":{pattern:/^(?:POST|GET|PUT|DELETE|OPTIONS|PATCH|TRACE|CONNECT)\s(?:https?:\/\/|\/)\S+\sHTTP\/[0-9.]+/m,inside:{// HTTP Verb
property:/^(?:POST|GET|PUT|DELETE|OPTIONS|PATCH|TRACE|CONNECT)\b/,// Path or query argument
"attr-name":/:\w+/}},"response-status":{pattern:/^HTTP\/1.[01] \d+.*/m,inside:{// Status, e.g. 200 OK
property:{pattern:/(^HTTP\/1.[01] )\d+.*/i,lookbehind:!0/* ignoreName */ /* skipSlots */}}},// HTTP header name
"header-name":{pattern:/^[\w-]+:(?=.)/m,alias:"keyword"}};// Create a mapping of Content-Type headers to language definitions
var langs=Prism.languages,httpLanguages={"application/javascript":langs.javascript,"application/json":langs.json||langs.javascript,"application/xml":langs.xml,"text/xml":langs.xml,"text/html":langs.html,"text/css":langs.css},suffixTypes={"application/json":!0,"application/xml":!0};/**
	 * Returns a pattern for the given content type which matches it and any type which has it as a suffix.
	 *
	 * @param {string} contentType
	 * @returns {string}
	 */function getSuffixPattern(contentType){var suffix=contentType.replace(/^[a-z]+\//,""),suffixPattern="\\w+/(?:[\\w.-]+\\+)+"+suffix+"(?![+\\w.-])";return"(?:"+contentType+"|"+suffixPattern+")"}// Insert each content type parser that has its associated language
// currently loaded.
var options;for(var contentType in httpLanguages){if(httpLanguages[contentType]){options=options||{};var pattern=suffixTypes[contentType]?getSuffixPattern(contentType):contentType;options[contentType]={pattern:RegExp("(content-type:\\s*"+pattern+"[\\s\\S]*?)(?:\\r?\\n|\\r){2}[\\s\\S]*","i"),lookbehind:!0,inside:{rest:httpLanguages[contentType]}}}}if(options){Prism.languages.insertBefore("http","header-name",options)}})(Prism);