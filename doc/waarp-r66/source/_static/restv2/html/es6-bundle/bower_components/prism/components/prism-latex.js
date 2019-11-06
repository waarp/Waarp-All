(function(Prism){var funcPattern=/\\(?:[^a-z()[\]]|[a-z*]+)/i,insideEqu={"equation-command":{pattern:funcPattern,alias:"regex"}};Prism.languages.latex={comment:/%.*/m,// the verbatim environment prints whitespace to the document
cdata:{pattern:/(\\begin\{((?:verbatim|lstlisting)\*?)\})[\s\S]*?(?=\\end\{\2\})/,lookbehind:!0},/*
		 * equations can be between $ $ or \( \) or \[ \]
		 * (all are multiline)
		 */equation:[{pattern:/\$(?:\\[\s\S]|[^\\$])*\$|\\\([\s\S]*?\\\)|\\\[[\s\S]*?\\\]/,inside:insideEqu,alias:"string"},{pattern:/(\\begin\{((?:equation|math|eqnarray|align|multline|gather)\*?)\})[\s\S]*?(?=\\end\{\2\})/,lookbehind:!0,inside:insideEqu,alias:"string"}],/*
		 * arguments which are keywords or references are highlighted
		 * as keywords
		 */keyword:{pattern:/(\\(?:begin|end|ref|cite|label|usepackage|documentclass)(?:\[[^\]]+\])?\{)[^}]+(?=\})/,lookbehind:!0},url:{pattern:/(\\url\{)[^}]+(?=\})/,lookbehind:!0},/*
		 * section or chapter headlines are highlighted as bold so that
		 * they stand out more
		 */headline:{pattern:/(\\(?:part|chapter|section|subsection|frametitle|subsubsection|paragraph|subparagraph|subsubparagraph|subsubsubparagraph)\*?(?:\[[^\]]+\])?\{)[^}]+(?=\}(?:\[[^\]]+\])?)/,lookbehind:!0,alias:"class-name"},function:{pattern:funcPattern,alias:"selector"},punctuation:/[[\]{}&]/}})(Prism);