package hilicodenon

import java.io.BufferedReader
import java.io.InputStreamReader

class HiliCodeNonApp {
    private val reader = BufferedReader(InputStreamReader(System.`in`))

    fun run() {
        println("HiliCodenon Scanner + Parser + AST Printer")
        println("Type a line to parse, or 'exit' to quit.")

        while (true) {
            print("> ")
            val line = reader.readLine() ?: break
            if (line.trim().lowercase() == "exit") break

            try {
                val scanner = Scanner(line)
                val tokens = scanner.scanTokens()

                val parser = Parser(tokens)
                val expression = parser.parse()

                if (expression != null) {
                    AstPrinter().print(expression)
                } else {
                }

            } catch (e: Parser.ParseError) {
                System.err.println(e.message)
            } catch (e: Exception) {
                System.err.println("[Unexpected Error] ${e.message}")
            }
        }
    }
}

fun main() {
    val app = HiliCodeNonApp()
    app.run()
}

try {
} catch (e: ParseError) {
    System.err.println(e.message)
}