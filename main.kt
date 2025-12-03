fun main(args: Array<String>) {
    val engine = BattleScript()

    if (args.isNotEmpty()) {
        println("BATCH EXECUTION STARTED\n")

        for (filename in args) {
            println(">>> LOADING SCRIPT: $filename")
            engine.runFile(filename)
            println(">>> END OF SCRIPT: $filename\n")
        }

        println("BATCH EXECUTION FINISHED")
    } else {
        engine.runPrompt()
    }
}