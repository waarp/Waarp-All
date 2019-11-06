Prism.languages.javastacktrace={// java.sql.SQLException: Violation of unique constraint MY_ENTITY_UK_1: duplicate value(s) for column(s) MY_COLUMN in statement [...]
// Caused by: java.sql.SQLException: Violation of unique constraint MY_ENTITY_UK_1: duplicate value(s) for column(s) MY_COLUMN in statement [...]
// Caused by: com.example.myproject.MyProjectServletException
// Caused by: MidLevelException: LowLevelException
// Suppressed: Resource$CloseFailException: Resource ID = 0
summary:{pattern:/^[\t ]*(?:(?:Caused by:|Suppressed:|Exception in thread "[^"]*")[\t ]+)?[\w$.]+(?:\:.*)?$/m,inside:{keyword:{pattern:/^(\s*)(?:(?:Caused by|Suppressed)(?=:)|Exception in thread)/m,lookbehind:!0/* ignoreName */ /* skipSlots */},// the current thread if the summary starts with 'Exception in thread'
string:{pattern:/^(\s*)"[^"]*"/,lookbehind:!0},exceptions:{pattern:/^(:?\s*)[\w$.]+(?=:|$)/,lookbehind:!0,inside:{"class-name":/[\w$]+(?=$|:)/,namespace:/[a-z]\w*/,punctuation:/[.:]/}},message:{pattern:/(:\s*)\S.*/,lookbehind:!0,alias:"string"},punctuation:/[:]/}},// at org.mortbay.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1166)
// at org.hsqldb.jdbc.Util.throwError(Unknown Source) here could be some notes
// at Util.<init>(Unknown Source)
"stack-frame":{pattern:/^[\t ]*at [\w$.]+(?:<init>)?\([^()]*\)/m,inside:{keyword:{pattern:/^(\s*)at/,lookbehind:!0},source:[// (Main.java:15)
// (Main.scala:15)
{pattern:/(\()\w+.\w+:\d+(?=\))/,lookbehind:!0,inside:{file:/^\w+\.\w+/,punctuation:/:/,"line-number":{pattern:/\d+/,alias:"number"}}},// (Unknown Source)
// (Native Method)
// (...something...)
{pattern:/(\()[^()]*(?=\))/,lookbehind:!0,inside:{keyword:/^(?:Unknown Source|Native Method)$/}}],"class-name":/[\w$]+(?=\.(?:<init>|[\w$]+)\()/,function:/(?:<init>|[\w$]+)(?=\()/,namespace:/[a-z]\w*/,punctuation:/[.()]/}},// ... 32 more
// ... 32 common frames omitted
more:{pattern:/^[\t ]*\.{3} \d+ [a-z]+(?: [a-z]+)*/m,inside:{punctuation:/\.{3}/,number:/\d+/,keyword:/\b[a-z]+(?: [a-z]+)*\b/}}};