sealed class Expr {
    data class Assign(val name: Token, val value: Expr) : Expr()
    data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr()
    data class Grouping(val expression: Expr) : Expr()
    data class Literal(val value: Any?) : Expr()
    data class Unary(val operator: Token, val right: Expr) : Expr()
    data class Variable(val name: Token) : Expr()
    data class Cast(val callee: Expr, val paren: Token, val arguments: List<Expr>) : Expr()

    data class Get(val obj: Expr, val name: Token, val index: Expr) : Expr()
    data class Set(val obj: Expr, val name: Token, val index: Expr, val value: Expr) : Expr()
}
