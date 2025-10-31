Programming Language Name:

HiliCodeNon

Creator:

Rainier RJ Espinal & Matthew Simpas

Language Overview:

HiliCodeNon is a programming language inspired by Python but designed with simplicity and localization in mind. Its main goal is to bring programming concepts closer to native Hiligaynon speakers by using familiar words while retaining a Python-like structure. It is beginner-friendly, easy to read, and encourages logical thinking through familiar syntax.

Keywords:

"kag" = AND,
"klase" = CLASS,
"ang" = DEF,
"teh" = ELSE,
"sa" = CASE,
"untat" = BREAK,
"sige" = CONTINUE,
"sala" = FALSE,
"para" = FOR,
"kundi" = ELIF,
"kung" = IF,
"ukon" = OR,
"ipakita" = PRINT,
"ibalik" = RETURN,
"tisting" = TRY,
"this" = CASE,
"tuod" = TRUE,
"samtang" = WHILE

Operators:

Arithmetic Operators:
“+” = addition
“-” = subtraction
“*” = multiplication
“/” = division

Comparison Operators:
“==” = equivalence
“!=” = not equal
“>” = greater than
“<” = less than
“>=” = greater than or equal
“<=” = less than or equal

Logical Operators:
“and” = logical AND
“or” = logical OR
“not” = logical NOT

Assignment Operators:
“=” = assign value

Literals:

Numbers: integers and floats (e.g., 123, 3.14)
Strings: written double quotes ("text")
Booleans: tuod (True), sala (False)

Identifiers:

Must begin with a letter or underscore (_)
Can contain letters, digits, and underscores after the first character
Case-sensitive

Comments:

Single-line comments = // comment
Multi-line comments = /* comment block */
Nested comments = are not supported

Syntax Style:

Whitespace (indentation) is significant, like Python
Blocks are grouped by indentation

Sample Code:

[ang switch()
sa 1:
kung(ikaw >18)]
	ipakita(“minor”)
	untat
sa 2:
kung(18 <= ikaw <= 19)
ipakita(“teen”)
untat
sa 3:
	kung(ikaw > 19)
			ipakita(“oldie”)

Design Rationale

The language uses Hiligaynon keywords to make programming feel more approachable for local speakers, while keeping the structure close to Python for readability and ease of learning. The design choices focus on simplicity, cultural connection, and reducing confusion by mapping each keyword to a familiar Hiligaynon word.
