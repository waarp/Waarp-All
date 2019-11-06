Prism.languages.wiki=Prism.languages.extend("markup",{"block-comment":{pattern:/(^|[^\\])\/\*[\s\S]*?\*\//,lookbehind:!0,alias:"comment"},heading:{pattern:/^(=+).+?\1/m,inside:{punctuation:/^=+|=+$/,important:/.+/}},emphasis:{// TODO Multi-line
pattern:/('{2,5}).+?\1/,inside:{"bold italic":{pattern:/(''''').+?(?=\1)/,lookbehind:!0},bold:{pattern:/(''')[^'](?:.*?[^'])?(?=\1)/,lookbehind:!0},italic:{pattern:/('')[^'](?:.*?[^'])?(?=\1)/,lookbehind:!0},punctuation:/^''+|''+$/}},hr:{pattern:/^-{4,}/m,alias:"punctuation"},url:[/ISBN +(?:97[89][ -]?)?(?:\d[ -]?){9}[\dx]\b|(?:RFC|PMID) +\d+/i,/\[\[.+?\]\]|\[.+?\]/],variable:[/__[A-Z]+__/,// FIXME Nested structures should be handled
// {{formatnum:{{#expr:{{{3}}}}}}}
/\{{3}.+?\}{3}/,/\{\{.+?\}\}/],symbol:[/^#redirect/im,/~{3,5}/],// Handle table attrs:
// {|
// ! style="text-align:left;"| Item
// |}
"table-tag":{pattern:/((?:^|[|!])[|!])[^|\r\n]+\|(?!\|)/m,lookbehind:!0,inside:{"table-bar":{pattern:/\|$/,alias:"punctuation"},rest:Prism.languages.markup.tag.inside}},punctuation:/^(?:\{\||\|\}|\|-|[*#:;!|])|\|\||!!/m});Prism.languages.insertBefore("wiki","tag",{// Prevent highlighting inside <nowiki>, <source> and <pre> tags
nowiki:{pattern:/<(nowiki|pre|source)\b[\s\S]*?>[\s\S]*?<\/\1>/i,inside:{tag:{pattern:/<(?:nowiki|pre|source)\b[\s\S]*?>|<\/(?:nowiki|pre|source)>/i,inside:Prism.languages.markup.tag.inside}}}});