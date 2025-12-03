data class Token(
    val type: TokenType,
    val lexeme: String, // The actual entity name or command string (e.g., "AntiMage")
    val literal: Any?,  // The realized stats: Gold amount (Int), Damage (Double), or Chat message (String)
    val line: Int       // The Match Tick (Line number) used for pinging errors to teammates
) {
    override fun toString(): String {
        val lit = if (literal != null) ", literal=$literal" else ""
        return "Token(type=$type, lexeme='$lexeme'$lit, line=$line)"
    }
}