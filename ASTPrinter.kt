class AstPrinter {
    fun print(expr: Expr): String {
        return when (expr) {
            is Expr.Binary -> parenthesize(expr.operator.lexeme, expr.left, expr.right)
            is Expr.Grouping -> parenthesize("group", expr.expression)
            is Expr.Literal -> if (expr.value == null) "nil" else expr.value.toString()
            is Expr.Unary -> parenthesize(expr.operator.lexeme, expr.right)
            is Expr.Variable -> expr.name.lexeme
            is Expr.Assign -> parenthesize("= ${expr.name.lexeme}", expr.value)
            is Expr.Cast -> parenthesize("call", expr.callee, *expr.arguments.toTypedArray())
        }
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val builder = StringBuilder()
        builder.append("(").append(name)
        for (expr in exprs) {
            builder.append(" ")
            builder.append(print(expr))
        }
        builder.append(")")
        return builder.toString()
    }
}