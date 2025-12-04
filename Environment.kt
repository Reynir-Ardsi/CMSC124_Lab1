import java.util.concurrent.ConcurrentHashMap

class Environment(val enclosing: Environment? = null) {
    private val values = ConcurrentHashMap<String, Any?>()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) {
            return values[name.lexeme]
        }

        if (enclosing != null) return enclosing.get(name)

        throw RuntimeError(name, "Missing: Unit '${name.lexeme}' is not in this lane (Undefined Variable).")
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }

        if (enclosing != null) {
            enclosing.assign(name, value)
            return
        }

        throw RuntimeError(name, "Invalid Target: '${name.lexeme}' not found.")
    }

    fun get(name: String): Any? {
        if (values.containsKey(name)) return values[name]
        if (enclosing != null) return enclosing.get(name)
        return null
    }
}