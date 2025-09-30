package hilicodenon

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