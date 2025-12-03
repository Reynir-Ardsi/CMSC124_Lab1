sealed class Expr {
    data class Assign(val name: Token, val value: Expr) : Expr()
    data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr()
    data class Grouping(val expression: Expr) : Expr()
    data class Literal(val value: Any?) : Expr()
    data class Unary(val operator: Token, val right: Expr) : Expr()
    data class Variable(val name: Token) : Expr()
    
    // New: Casting a spell (Function Call)
    data class Cast(val callee: Expr, val paren: Token, val arguments: List<Expr>) : Expr()
}