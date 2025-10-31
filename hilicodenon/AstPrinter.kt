package hilicodenon

class AstPrinter {

    fun print(expr: Expr) {
        val output = when (expr) {
            is Expr.Binary -> parenthesize(expr.operator.lexeme, expr.left, expr.right)
            is Expr.Grouping -> parenthesize("group", expr.expression)
            is Expr.Literal -> expr.value?.toString() ?: "nil"
            is Expr.Unary -> parenthesize(expr.operator.lexeme, expr.right)
        }

        println(output)
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val builder = StringBuilder()
        builder.append("(").append(name)
        for (expr in exprs) {
            builder.append(" ").append(when (expr) {
                is Expr.Binary -> parenthesize(expr.operator.lexeme, expr.left, expr.right)
                is Expr.Grouping -> parenthesize("group", expr.expression)
                is Expr.Literal -> expr.value?.toString() ?: "nil"
                is Expr.Unary -> parenthesize(expr.operator.lexeme, expr.right)
            })
        }
        builder.append(")")
        return builder.toString()
    }
}
