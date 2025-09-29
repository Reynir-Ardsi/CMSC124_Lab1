// Scanner.kt
// A simple lexical scanner for a tiny dynamically-typed language (ktlox-like).
// Features:
// - Single-char tokens: ( ) { } , . - + ; * / etc.
// - Multi-char operators: == != <= >=
// - Strings with escapes: "..." supporting \n \t \\ \"
// - Numbers: integers and decimals
// - Identifiers and keywords (var, fun, if, else, true, false, nil, return, while, for)
// - Comments: // single-line and /* block comment */
// - Whitespace & line tracking
// - Error reporting
// - REPL that prints tokens

import java.io.BufferedReader
import java.io.InputStreamReader

// ----------------------------- Token Types -----------------------------
enum class TokenType {
    // Single-character tokens.
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    // One or two character tokens.
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // Literals.
    IDENTIFIER, STRING, NUMBER,

    // Keywords.
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

    // End of file.
    EOF
}

// ----------------------------- Token -----------------------------
data class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any?,
    val line: Int
) {
    override fun toString(): String {
        val lit = if (literal != null) ", literal=$literal" else ""
        return "Token(type=$type, lexeme='$lexeme'$lit, line=$line)"
    }
}

// ----------------------------- Scanner -----------------------------
class Scanner(private val source: String) {
    private val tokens = mutableListOf<Token>()
    private var start = 0      // start index of current lexeme
    private var current = 0    // current index in source
    private var line = 1       // current line

    private val keywords = mapOf(
        "and" to TokenType.AND,
        "class" to TokenType.CLASS,
        "else" to TokenType.ELSE,
        "false" to TokenType.FALSE,
        "for" to TokenType.FOR,
        "fun" to TokenType.FUN,
        "if" to TokenType.IF,
        "nil" to TokenType.NIL,
        "or" to TokenType.OR,
        "print" to TokenType.PRINT,
        "return" to TokenType.RETURN,
        "super" to TokenType.SUPER,
        "this" to TokenType.THIS,
        "true" to TokenType.TRUE,
        "var" to TokenType.VAR,
        "while" to TokenType.WHILE
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

            // Slash may start comment or be division operator
            '/' -> {
                if (match('/')) {
                    // Single-line comment: consume until end of line
                    while (peek() != '\n' && !isAtEnd()) advance()
                } else if (match('*')) {
                    // Block comment: /* ... */ (non-nested)
                    blockComment()
                } else {
                    addToken(TokenType.SLASH)
                }
            }

            // One or two char tokens
            '!' -> addToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
            '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)

            // Whitespace
            ' ', '\r', '\t' -> { /* ignore */ }
            '\n' -> line++

            // Strings
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

    // ---------- helpers ----------
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

    // ---------- literals ----------
    private fun string() {
        val sb = StringBuilder()
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++ // allow multi-line strings
            if (peek() == '\\') { // escape sequence
                advance() // consume '\'
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
                        // Unrecognized escape: keep the char (tolerant)
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

        advance() // closing "
        // lexeme includes the quotes; literal is parsed content
        addToken(TokenType.STRING, sb.toString())
    }

    private fun number() {
        // integer part
        while (isDigit(peek())) advance()

        // fractional part
        if (peek() == '.' && isDigit(peekNext())) {
            // consume '.'
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

    // Block comment: consume until '*/' or EOF. Non-nested.
    private fun blockComment() {
        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                // consume '*/'
                advance()
                advance()
                return
            }
            if (peek() == '\n') line++
            advance()
        }
        // unterminated block comment: report error but continue scanning
        error("Unterminated block comment (/*) starting at line $line")
    }

    // ---------- char classification ----------
    private fun isDigit(c: Char) = c in '0'..'9'
    private fun isAlpha(c: Char) = c == '_' || c in 'a'..'z' || c in 'A'..'Z'
    private fun isAlphaNumeric(c: Char) = isAlpha(c) || isDigit(c)

    // ---------- error handling ----------
    private fun error(message: String) {
        System.err.println("[Line $line] Error: $message")
    }
}

// ----------------------------- REPL / Main -----------------------------
fun main() {
    val reader = BufferedReader(InputStreamReader(System.`in`))
    println("ktlox scanner REPL — type a line to scan, or 'exit' to quit")
    while (true) {
        print("> ")
        val line = reader.readLine() ?: break
        if (line.trim().lowercase() == "exit") break
        // For multi-line convenience: allow the user to enter multiple lines ended by blank line
        // but for simplicity here we scan single-line input.
        val scanner = Scanner(line)
        val tokens = scanner.scanTokens()
        tokens.forEach { println(it) }
    }

    // Example quick run (uncomment to auto-run examples when executing directly):
    /*
    val example = """
        var someString = "I am scanning\nand this is a second line"
        var someNumber = 3.1415 + 6.9420
        // this is a comment
        var parenthesizedEquation = (1 + 3) - (2 * 5)
        /* block comment */
    """.trimIndent()
    println("---- Example scan ----")
    Scanner(example).scanTokens().forEach { println(it) }
    */
}
