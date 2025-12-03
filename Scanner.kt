class Scanner(private val source: String) {
    private val tokens = mutableListOf<Token>()
    private var start = 0
    private var current = 0
    private var line = 1

    private val keywords = mapOf(
        "ward" to TokenType.WARD,
        "gank" to TokenType.GANK,
        "radiant" to TokenType.TRUE,
        "dire" to TokenType.FALSE,
        "farm" to TokenType.FARM,
        "cycle" to TokenType.FOR,
        "buy" to TokenType.BUY,
        "chat_wheel" to TokenType.CHAT_WHEEL,
        "invoke" to TokenType.INVOKE,
        "tp_scroll" to TokenType.CAST,
        "mana" to TokenType.MANA_COST,
        "cd" to TokenType.COOLDOWN,
        "and" to TokenType.AND,
        "or" to TokenType.OR
    )

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) { start = current; scanToken() }
        tokens.add(Token(TokenType.EOF, "", null, line))
        return tokens
    }

    private fun scanToken() {
        val c = advance()
        when (c) {
            '(' -> addToken(TokenType.LEFT_PAREN); ')' -> addToken(TokenType.RIGHT_PAREN)
            '{' -> addToken(TokenType.LEFT_BRACE); '}' -> addToken(TokenType.RIGHT_BRACE)
            '[' -> addToken(TokenType.LEFT_BRACKET); ']' -> addToken(TokenType.RIGHT_BRACKET)
            ',' -> addToken(TokenType.COMMA); '.' -> addToken(TokenType.DOT)
            '-' -> addToken(TokenType.MINUS); '+' -> addToken(TokenType.PLUS)
            ';' -> addToken(TokenType.SEMICOLON); '*' -> addToken(TokenType.STAR)
            ':' -> addToken(TokenType.COLON)
            '!' -> addToken(if (match('=')) TokenType.NOT_EQUAL else TokenType.NOT)
            '=' -> addToken(if (match('=')) TokenType.EQUIVALENCE else TokenType.EQUAL)
            '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
            '/' -> if (match('/')) while (peek() != '\n' && !isAtEnd()) advance() else addToken(TokenType.SLASH)
            ' ', '\r', '\t' -> {}
            '\n' -> line++
            '"' -> string()
            else -> if (isDigit(c)) number() else if (isAlpha(c)) identifier() else error("Unknown rune: $c")
        }
    }

    private fun advance() = source[current++]
    private fun match(expected: Char) = if (isAtEnd() || source[current] != expected) false else { current++; true }
    private fun peek() = if (isAtEnd()) '\u0000' else source[current]
    private fun peekNext() = if (current + 1 >= source.length) '\u0000' else source[current + 1]
    private fun isAtEnd() = current >= source.length
    private fun addToken(type: TokenType, literal: Any? = null) = tokens.add(Token(type, source.substring(start, current), literal, line))
    private fun isDigit(c: Char) = c in '0'..'9'
    private fun isAlpha(c: Char) = c == '_' || c in 'a'..'z' || c in 'A'..'Z'

    private fun string() {
        val sb = StringBuilder()
        while (peek() != '"' && !isAtEnd()) { if (peek() == '\n') line++; sb.append(advance()) }
        if (!isAtEnd()) advance(); addToken(TokenType.STRING, sb.toString())
    }
    private fun number() {
        while (isDigit(peek())) advance()
        if (peek() == '.' && isDigit(peekNext())) { advance(); while (isDigit(peek())) advance() }
        addToken(TokenType.NUMBER, source.substring(start, current).toDouble())
    }
    private fun identifier() {
        while (isAlpha(peek()) || isDigit(peek())) advance()
        addToken(keywords[source.substring(start, current)] ?: TokenType.IDENTIFIER)
    }
    private fun error(msg: String) = println("[Scanner Line $line] $msg")
}