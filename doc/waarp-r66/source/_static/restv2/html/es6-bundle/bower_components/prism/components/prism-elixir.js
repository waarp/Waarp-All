Prism.languages.elixir={comment:{pattern:/#.*/m,lookbehind:!0},// ~r"""foo""" (multi-line), ~r'''foo''' (multi-line), ~r/foo/, ~r|foo|, ~r"foo", ~r'foo', ~r(foo), ~r[foo], ~r{foo}, ~r<foo>
regex:{pattern:/~[rR](?:("""|''')(?:\\[\s\S]|(?!\1)[^\\])+\1|([\/|"'])(?:\\.|(?!\2)[^\\\r\n])+\2|\((?:\\.|[^\\)\r\n])+\)|\[(?:\\.|[^\\\]\r\n])+\]|\{(?:\\.|[^\\}\r\n])+\}|<(?:\\.|[^\\>\r\n])+>)[uismxfr]*/,greedy:!0},string:[{// ~s"""foo""" (multi-line), ~s'''foo''' (multi-line), ~s/foo/, ~s|foo|, ~s"foo", ~s'foo', ~s(foo), ~s[foo], ~s{foo} (with interpolation care), ~s<foo>
pattern:/~[cCsSwW](?:("""|''')(?:\\[\s\S]|(?!\1)[^\\])+\1|([\/|"'])(?:\\.|(?!\2)[^\\\r\n])+\2|\((?:\\.|[^\\)\r\n])+\)|\[(?:\\.|[^\\\]\r\n])+\]|\{(?:\\.|#\{[^}]+\}|[^\\}\r\n])+\}|<(?:\\.|[^\\>\r\n])+>)[csa]?/,greedy:!0,inside:{// See interpolation below
}},{pattern:/("""|''')[\s\S]*?\1/,greedy:!0,inside:{// See interpolation below
}},{// Multi-line strings are allowed
pattern:/("|')(?:\\(?:\r\n|[\s\S])|(?!\1)[^\\\r\n])*\1/,greedy:!0,inside:{// See interpolation below
}}],atom:{// Look-behind prevents bad highlighting of the :: operator
pattern:/(^|[^:]):\w+/,lookbehind:!0,alias:"symbol"},// Look-ahead prevents bad highlighting of the :: operator
"attr-name":/\w+:(?!:)/,capture:{// Look-behind prevents bad highlighting of the && operator
pattern:/(^|[^&])&(?:[^&\s\d()][^\s()]*|(?=\())/,lookbehind:!0,alias:"function"},argument:{// Look-behind prevents bad highlighting of the && operator
pattern:/(^|[^&])&\d+/,lookbehind:!0,alias:"variable"},attribute:{pattern:/@\w+/,alias:"variable"},number:/\b(?:0[box][a-f\d_]+|\d[\d_]*)(?:\.[\d_]+)?(?:e[+-]?[\d_]+)?\b/i,keyword:/\b(?:after|alias|and|case|catch|cond|def(?:callback|exception|impl|module|p|protocol|struct)?|do|else|end|fn|for|if|import|not|or|require|rescue|try|unless|use|when)\b/,boolean:/\b(?:true|false|nil)\b/,operator:[/\bin\b|&&?|\|[|>]?|\\\\|::|\.\.\.?|\+\+?|-[->]?|<[-=>]|>=|!==?|\B!|=(?:==?|[>~])?|[*\/^]/,{// We don't want to match <<
pattern:/([^<])<(?!<)/,lookbehind:!0},{// We don't want to match >>
pattern:/([^>])>(?!>)/,lookbehind:!0}],punctuation:/<<|>>|[.,%\[\]{}()]/};Prism.languages.elixir.string.forEach(function(o){o.inside={interpolation:{pattern:/#\{[^}]+\}/,inside:{delimiter:{pattern:/^#\{|\}$/,alias:"punctuation"},rest:Prism.languages.elixir}}}});