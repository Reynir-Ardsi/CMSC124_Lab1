package hilicodenon

import java.io.BufferedReader
import java.io.InputStreamReader

class HiliCodeNonApp {
    private val reader = BufferedReader(InputStreamReader(System.`in`))

    fun run() {
        println("HiliCodenon Scanner, type a line to scan, or 'exit' to quit")
        while (true) {
            print("> ")
            val line = reader.readLine() ?: break
            if (line.trim().lowercase() == "exit") break
            val scanner = Scanner(line)
            val tokens = scanner.scanTokens()
            tokens.forEach { println(it) }
        }
    }
}

fun main() {
    HiliCodeNonApp().run()
}
