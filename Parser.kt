class Parser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): Stmt.Program = Stmt.Program(parseStatementsRecursive())

    private fun parseStatementsRecursive(): Stmt? {
        if (isAtEnd()) return null
        val decl = declaration() ?: return parseStatementsRecursive()
        return Stmt.Sequence(decl, parseStatementsRecursive())
    }

    private fun declaration(): Stmt? {
        try {
            if (match(TokenType.INVOKE)) return spellDeclaration()
            if (match(TokenType.BUY)) return varDeclaration()
            return statement()
        } catch (e: Exception) { synchronize(); return null }
    }

    private fun spellDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect spell name.")
        consume(TokenType.LEFT_PAREN, "(")
        val params = mutableListOf<Token>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do { params.add(consume(TokenType.IDENTIFIER, "Param name")) } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, ")")

        var mana = 0; var cd = 0.0
        if (match(TokenType.LEFT_BRACKET)) {
            if (!check(TokenType.RIGHT_BRACKET)) {
                do {
                    if (match(TokenType.MANA_COST)) {
                        consume(TokenType.COLON, "Expect ':' after mana.")
                        mana = (consume(TokenType.NUMBER, "Expect mana cost.").literal as Double).toInt()
                    } else if (match(TokenType.COOLDOWN)) {
                        consume(TokenType.COLON, "Expect ':' after cd.")
                        cd = consume(TokenType.NUMBER, "Expect cooldown value.").literal as Double
                    } else {
                        throw RuntimeException("Expect 'mana' or 'cd' in spell attributes.")
                    }
                } while (match(TokenType.COMMA))
            }
            consume(TokenType.RIGHT_BRACKET, "Expect ']' after spell attributes.")
        }

        consume(TokenType.LEFT_BRACE, "{")
        val body = parseBlockStatementsRecursive()
        consume(TokenType.RIGHT_BRACE, "}")
        return Stmt.Invoke(name, params, mana, cd, body)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Var name")
        val init = if (match(TokenType.EQUAL)) expression() else null
        consume(TokenType.SEMICOLON, ";")
        return Stmt.Buy(name, init)
    }

    private fun statement(): Stmt {
        if (match(TokenType.FOR)) return forStatement()
        if (match(TokenType.FARM)) return Stmt.Farm(consumeParenExpr(), statement())
        if (match(TokenType.WARD)) return Stmt.Ward(consumeParenExpr(), statement(), if (match(TokenType.GANK)) statement() else null)
        if (match(TokenType.CHAT_WHEEL)) { val v = expression(); consume(TokenType.SEMICOLON, ";"); return Stmt.ChatWheel(v) }
        if (match(TokenType.CAST)) { val k = previous(); val v = if (!check(TokenType.SEMICOLON)) expression() else null; consume(TokenType.SEMICOLON, ";"); return Stmt.TpScroll(k, v) }
        if (match(TokenType.LEFT_BRACE)) return Stmt.Block(parseBlock())
        val expr = expression(); consume(TokenType.SEMICOLON, ";"); return Stmt.Expression(expr)
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'cycle'.")
        val initializer: Stmt?
        if (match(TokenType.SEMICOLON)) { initializer = null } 
        else if (match(TokenType.BUY)) { initializer = varDeclaration() } 
        else { val expr = expression(); consume(TokenType.SEMICOLON, "Expect ';' after expression."); initializer = Stmt.Expression(expr) }

        var condition: Expr? = null
        if (!check(TokenType.SEMICOLON)) { condition = expression() }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.")

        var increment: Expr? = null
        if (!check(TokenType.RIGHT_PAREN)) { increment = expression() }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.")

        var body = statement()
        if (increment != null) { body = Stmt.Sequence(body, Stmt.Expression(increment)); body = Stmt.Block(body) }
        if (condition == null) condition = Expr.Literal(true)
        body = Stmt.Farm(condition, body)
        if (initializer != null) { body = Stmt.Sequence(initializer, body); body = Stmt.Block(body) }
        return body
    }

    private fun parseBlock(): Stmt? {
        val stmts = parseBlockStatementsRecursive()
        consume(TokenType.RIGHT_BRACE, "}")
        return stmts
    }

    private fun parseBlockStatementsRecursive(): Stmt? {
        if (check(TokenType.RIGHT_BRACE) || isAtEnd()) return null
        val decl = declaration() ?: return parseBlockStatementsRecursive()
        return Stmt.Sequence(decl, parseBlockStatementsRecursive())
    }

    private fun consumeParenExpr(): Expr { consume(TokenType.LEFT_PAREN, "("); val e = expression(); consume(TokenType.RIGHT_PAREN, ")"); return e }

    private fun expression() = assignment()

    private fun assignment(): Expr {
        // Parse the left-hand side (which could be a variable OR a get expression like a[i])
        val expr = or()

        if (match(TokenType.EQUAL)) {
            val value = assignment()

            if (expr is Expr.Variable) {
                return Expr.Assign(expr.name, value)
            } else if (expr is Expr.Get) {
                // NEW: Convert Get expression to Set expression when followed by '='
                return Expr.Set(expr.obj, expr.name, expr.index, value)
            }

            throw RuntimeException("Invalid assignment target.")
        }
        return expr
    }

    private fun or(): Expr { var e = and(); while(match(TokenType.OR)) { val op = previous(); val r = and(); e = Expr.Binary(e, op, r) }; return e }
    private fun and(): Expr { var e = equality(); while(match(TokenType.AND)) { val op = previous(); val r = equality(); e = Expr.Binary(e, op, r) }; return e }
    private fun equality(): Expr { var e = comparison(); while(match(TokenType.EQUIVALENCE, TokenType.NOT_EQUAL)) { val op = previous(); val r = comparison(); e = Expr.Binary(e, op, r) }; return e }
    private fun comparison(): Expr { var e = term(); while(match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) { val op = previous(); val r = term(); e = Expr.Binary(e, op, r) }; return e }
    private fun term(): Expr { var e = factor(); while(match(TokenType.PLUS, TokenType.MINUS)) { val op = previous(); val r = factor(); e = Expr.Binary(e, op, r) }; return e }
    private fun factor(): Expr { var e = unary(); while(match(TokenType.SLASH, TokenType.STAR)) { val op = previous(); val r = unary(); e = Expr.Binary(e, op, r) }; return e }
    private fun unary(): Expr { if (match(TokenType.NOT, TokenType.MINUS)) return Expr.Unary(previous(), unary()); return call() }

    private fun call(): Expr {
        var expr = primary()
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr)
            } else if (match(TokenType.LEFT_BRACKET)) {
                // NEW: Parse array index access: variable[index]
                val index = expression()
                consume(TokenType.RIGHT_BRACKET, "Expect ']' after index.")
                
                if (expr is Expr.Variable) {
                    // Transform Variable into Get expression
                    expr = Expr.Get(expr, expr.name, index)
                } else {
                    throw RuntimeException("Can only index variables.")
                }
            } else {
                break
            }
        }
        return expr
    }
    
    private fun finishCall(callee: Expr): Expr {
        val args = mutableListOf<Expr>(); if (!check(TokenType.RIGHT_PAREN)) do { args.add(expression()) } while (match(TokenType.COMMA))
        consume(TokenType.RIGHT_PAREN, ")"); return Expr.Cast(callee, previous(), args)
    }

    private fun primary(): Expr {
        if (match(TokenType.FALSE)) return Expr.Literal(false)
        if (match(TokenType.TRUE)) return Expr.Literal(true)
        if (match(TokenType.NUMBER, TokenType.STRING)) return Expr.Literal(previous().literal)
        if (match(TokenType.IDENTIFIER)) return Expr.Variable(previous())
        if (match(TokenType.LEFT_PAREN)) return Expr.Grouping(consumeParenExpr())
        throw RuntimeException("Expect expression at line ${tokens[current].line}")
    }

    private fun match(vararg types: TokenType): Boolean { for (t in types) if (check(t)) { advance(); return true }; return false }
    private fun consume(type: TokenType, msg: String): Token { if (check(type)) return advance(); throw RuntimeException(msg) }
    private fun check(type: TokenType) = if (isAtEnd()) false else tokens[current].type == type
    private fun advance() = if (!isAtEnd()) tokens[current++] else tokens[current-1]
    private fun previous() = tokens[current - 1]
    private fun isAtEnd() = tokens[current].type == TokenType.EOF
    private fun synchronize() { advance(); while (!isAtEnd()) { if (previous().type == TokenType.SEMICOLON) return; advance() } }
}