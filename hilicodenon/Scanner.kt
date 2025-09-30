package hilicodenon

class Scanner(private val source: String) {
    private val tokens = mutableListOf<Token>()
    private var start = 0
    private var current = 0
    private var line = 1

    private val keywords = mapOf(
        "kag" to TokenType.AND,
        "klase" to TokenType.CLASS,
        "ang" to TokenType.DEF,
        "teh" to TokenType.ELSE,
        "sa" to TokenType.CASE,
        "untat" to TokenType.BREAK,
        "sige" to TokenType.CONTINUE,
        "sala" to TokenType.FALSE,
        "para" to TokenType.FOR,
        "kundi" to TokenType.ELIF,
        "kung" to TokenType.IF,
        "ukon" to TokenType.OR,
        "ipakita" to TokenType.PRINT,
        "ibalik" to TokenType.RETURN,
        "tisting" to TokenType.TRY,
        "this" to TokenType.CASE,
        "tuod" to TokenType.TRUE,
        "samtang" to TokenType.WHILE
    )

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }
        tokens.add(Token(TokenType.EOF, "", null, line))
        return tokens
    }

    private fun scanToken() {
        val c = advance()
        when (c) {
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            '-' -> addToken(TokenType.MINUS)
            '+' -> addToken(TokenType.PLUS)
            ';' -> addToken(TokenType.SEMICOLON)
            '*' -> addToken(TokenType.STAR)
            ':' -> addToken(TokenType.COLON)

            '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance()
                } else if (match('*')) {

                    blockComment()
                } else {
                    addToken(TokenType.SLASH)
                }
            }

            '!' -> addToken(if (match('=')) TokenType.NOT_EQUAL else TokenType.NOT)
            '=' -> addToken(if (match('=')) TokenType.EQUIVALENCE else TokenType.EQUAL)
            '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)

            ' ', '\r', '\t' -> { /* ignore */ }
            '\n' -> line++
            
            '"' -> string()

            else -> {
                when {
                    isDigit(c) -> number()
                    isAlpha(c) -> identifier()
                    else -> error("Unexpected character '$c' at line $line")
                }
            }
        }
    }

    private fun isAtEnd(): Boolean = current >= source.length
    private fun advance(): Char = source[current++]
    private fun peek(): Char = if (isAtEnd()) '\u0000' else source[current]
    private fun peekNext(): Char = if (current + 1 >= source.length) '\u0000' else source[current + 1]
    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[current] != expected) return false
        current++
        return true
    }

    private fun addToken(type: TokenType, literal: Any? = null) {
        val text = source.substring(start, current)
        tokens.add(Token(type, text, literal, line))
    }

    private fun string() {
        val sb = StringBuilder()
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++
            if (peek() == '\\') {
                advance()
                val esc = if (isAtEnd()) {
                    error("Unfinished escape sequence in string at line $line")
                    return
                } else advance()
                when (esc) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    '\\' -> sb.append('\\')
                    '"' -> sb.append('"')
                    else -> {
                        sb.append(esc)
                    }
                }
            } else {
                sb.append(advance())
            }
        }

        if (isAtEnd()) {
            error("Unterminated string at line $line")
            return
        }

        advance()
        addToken(TokenType.STRING, sb.toString())
    }

    private fun number() {
        while (isDigit(peek())) advance()


        if (peek() == '.' && isDigit(peekNext())) {
            advance()
            while (isDigit(peek())) advance()
        }

        val numText = source.substring(start, current)
        val value = try {
            numText.toDouble()
        } catch (e: NumberFormatException) {
            error("Malformed number '$numText' at line $line")
            null
        }

        addToken(TokenType.NUMBER, value)
    }

    private fun identifier() {
        while (isAlphaNumeric(peek())) advance()
        val text = source.substring(start, current)
        val type = keywords[text] ?: TokenType.IDENTIFIER
        addToken(type)
    }

    private fun blockComment() {
        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                advance()
                advance()
                return
            }
            if (peek() == '\n') line++
            advance()
        }
        error("Unterminated block comment (/*) starting at line $line")
    }

    private fun isDigit(c: Char) = c in '0'..'9'
    private fun isAlpha(c: Char) = c == '_' || c in 'a'..'z' || c in 'A'..'Z'
    private fun isAlphaNumeric(c: Char) = isAlpha(c) || isDigit(c)

    private fun error(message: String) {
        System.err.println("[Line $line] Error: $message")
    }
}
