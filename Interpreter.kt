import java.util.Random
import java.util.Scanner as SystemScanner

class RuntimeError(val token: Token?, message: String) : RuntimeException(message)

class Return(val value: Any?) : RuntimeException(null, null, false, false)

interface AncientCallable {
    fun arity(): Int
    fun call(interpreter: Interpreter, arguments: List<Any?>): Any?
}

class AncientSpell(val declaration: Stmt.Invoke, val closure: Environment) : AncientCallable {
    override fun arity() = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val env = Environment(closure)
        for (i in declaration.params.indices) {
            env.define(declaration.params[i].lexeme, arguments[i])
        }
        try {
            interpreter.executeBlock(declaration.body, env)
        } catch (r: Return) {
            return r.value
        }
        return null
    }
}

class Interpreter {
    val globals = Environment()
    private var environment = globals
    private var currentTick = 0

    private val cooldowns = mutableMapOf<String, Int>()

    private val random = Random()
    private var isRepl = false

    init {

        globals.define("check_cooldown", object : AncientCallable {
            override fun arity() = 1
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
                val name = stringify(arguments[0])
                val readyAt = cooldowns.getOrDefault(name, 0)
                val remaining = kotlin.math.max(0, readyAt - currentTick)
                return remaining.toDouble()
            }
        })

        globals.define("refresh_orb", object : AncientCallable {
            override fun arity() = 1
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
                val name = stringify(arguments[0])
                if (cooldowns.containsKey(name)) {
                    cooldowns[name] = 0
                    println("[REFRESHER] $name is now ready!")
                }
                return null
            }
        })

        globals.define("random_proc", object : AncientCallable {
            override fun arity() = 1
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
                val chance = arguments[0] as Double
                val roll = random.nextDouble() * 100
                return roll < chance
            }
        })

        globals.define("spawn_rune", object : AncientCallable {
            override fun arity() = 0
            override fun call(interpreter: Interpreter, arguments: List<Any?>) = (random.nextInt(3) + 1).toDouble()
        })

        globals.define("random_event", object : AncientCallable {
            override fun arity() = 0
            override fun call(interpreter: Interpreter, arguments: List<Any?>) = random.nextDouble()
        })
    }

    fun interpret(stmt: Stmt, isRepl: Boolean = false) {
        this.isRepl = isRepl
        try { execute(stmt) }
        catch (error: RuntimeError) {
            val lineInfo = if (error.token != null) "[Line ${error.token.line}]" else "[System]"
            System.err.println("$lineInfo Game Error: ${error.message}")
        }
    }

    private fun execute(stmt: Stmt?) {
        if (stmt == null) return
        when (stmt) {
            is Stmt.Sequence -> { execute(stmt.first); execute(stmt.next) }
            is Stmt.Program -> execute(stmt.statements)

            is Stmt.Block -> executeBlock(stmt.statements, Environment(environment))

            is Stmt.ChatWheel -> println("[ALL CHAT] ${stringify(evaluate(stmt.expression))}")
            is Stmt.Buy -> environment.define(stmt.name.lexeme, if (stmt.initializer != null) evaluate(stmt.initializer) else null)
            is Stmt.Ward -> if (isTruthy(evaluate(stmt.condition))) execute(stmt.thenBranch) else if (stmt.elseBranch != null) execute(stmt.elseBranch)

            is Stmt.Farm -> {
                while (isTruthy(evaluate(stmt.condition))) {
                    execute(stmt.body)
                    tick()
                }
            }

            is Stmt.Invoke -> {
                val spell = AncientSpell(stmt, environment)
                environment.define(stmt.name.lexeme, spell)
                cooldowns[stmt.name.lexeme] = 0
            }

            is Stmt.TpScroll -> throw Return(if (stmt.value != null) evaluate(stmt.value) else null)

            is Stmt.Expression -> {
                val value = evaluate(stmt.expression)
                if (isRepl) println(stringify(value))
            }
        }
    }

    fun tick() { currentTick++ }

    fun executeBlock(statements: Stmt?, env: Environment) {
        val prev = this.environment
        try {
            this.environment = env
            execute(statements)
        } finally {
            this.environment = prev
        }
    }

    private fun evaluate(expr: Expr): Any? {
        return when (expr) {
            is Expr.Literal -> expr.value
            is Expr.Grouping -> evaluate(expr.expression)
            is Expr.Variable -> environment.get(expr.name)
            is Expr.Assign -> { val v = evaluate(expr.value); environment.assign(expr.name, v); v }
            is Expr.Unary -> {
                val right = evaluate(expr.right)
                when (expr.operator.type) {
                    TokenType.MINUS -> { checkNumberOperand(expr.operator, right); -(right as Double) }
                    TokenType.NOT -> !isTruthy(right)
                    else -> null
                }
            }
            is Expr.Binary -> {
                if (expr.operator.type == TokenType.OR) {
                    val left = evaluate(expr.left)
                    if (isTruthy(left)) return left
                    return evaluate(expr.right)
                }
                if (expr.operator.type == TokenType.AND) {
                    val left = evaluate(expr.left)
                    if (!isTruthy(left)) return left
                    return evaluate(expr.right)
                }
                evaluateBinary(expr)
            }
            is Expr.Cast -> evaluateCast(expr)
        }
    }

    private fun evaluateBinary(expr: Expr.Binary): Any? {
        val l = evaluate(expr.left)
        val r = evaluate(expr.right)
        return when (expr.operator.type) {
            TokenType.MINUS -> { checkNumberOperands(expr.operator, l, r); (l as Double) - (r as Double) }
            TokenType.SLASH -> {
                checkNumberOperands(expr.operator, l, r)
                if ((r as Double) == 0.0) throw RuntimeError(expr.operator, "Division by zero.")
                (l as Double) / (r as Double)
            }
            TokenType.STAR -> { checkNumberOperands(expr.operator, l, r); (l as Double) * (r as Double) }
            TokenType.PLUS -> {
                if (l is Double && r is Double) l + r
                else "$l$r"
            }
            TokenType.GREATER -> { checkNumberOperands(expr.operator, l, r); (l as Double) > (r as Double) }
            TokenType.GREATER_EQUAL -> { checkNumberOperands(expr.operator, l, r); (l as Double) >= (r as Double) }
            TokenType.LESS -> { checkNumberOperands(expr.operator, l, r); (l as Double) < (r as Double) }
            TokenType.LESS_EQUAL -> { checkNumberOperands(expr.operator, l, r); (l as Double) <= (r as Double) }
            TokenType.NOT_EQUAL -> l != r
            TokenType.EQUIVALENCE -> l == r
            else -> null
        }
    }

    private fun evaluateCast(expr: Expr.Cast): Any? {
        val callee = evaluate(expr.callee)

        val args = expr.arguments.map { evaluate(it) }

        if (callee !is AncientCallable) {
            throw RuntimeError(expr.paren, "Can only cast spells or items.")
        }

        if (args.size != callee.arity()) {
            throw RuntimeError(expr.paren, "Expected ${callee.arity()} arguments but got ${args.size}.")
        }

        if (callee is AncientSpell) {
            val name = callee.declaration.name.lexeme
            val manaCost = callee.declaration.manaCost
            val cd = callee.declaration.cooldown

            var currentScriptMana = 9999.0
            try { currentScriptMana = environment.get(Token(TokenType.IDENTIFIER, "p1_mana", null, 0)) as Double } catch (e: Exception) {}

            if (currentScriptMana < manaCost) {
                println("[SILENCED] Not enough mana.")
                return null
            }
            if (currentTick < cooldowns.getOrDefault(name, 0)) {
                println("[COOLDOWN] $name not ready.")
                return null
            }
            cooldowns[name] = currentTick + cd.toInt()
        }

        return callee.call(this, args)
    }

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand !is Double) throw RuntimeError(operator, "Operand must be a number.")
    }
    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left !is Double || right !is Double) throw RuntimeError(operator, "Operands must be numbers.")
    }
    private fun isTruthy(o: Any?) = if (o == null) false else if (o is Boolean) o else if (o is Double) o != 0.0 else true
    private fun stringify(o: Any?) = if (o is Double && o.toString().endsWith(".0")) o.toString().dropLast(2) else o.toString()
}