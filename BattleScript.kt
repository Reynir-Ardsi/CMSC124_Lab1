import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File
import java.util.Scanner as SystemScanner

data class DraftSkill(
    val name: String,
    val type: Int,
    val dmg: Int,
    val cost: Int,
    val cd: Int
)

data class DraftItem(
    val name: String,
    val isDamage: Boolean,
    val value: Int,
    val cd: Int
)

class BattleScript {
    private val interpreter = Interpreter()
    private var hasError = false

    fun runFile(path: String) {
        val file = File(path)
        if (!file.exists()) {
            println("Error: File '$path' not found. Please check the path and try again.")
            return
        }
        val source = file.readText()
        run(source, isRepl = false)

        if (hasError) System.exit(65)
    }

    fun runPrompt() {
        val input = InputStreamReader(System.`in`)
        val reader = BufferedReader(input)

        println("=== BATTLESCRIPT CONSOLE (v7.33) ===")
        println("Connected to: Radiant Fountain")
        println("Type 'exit' or 'gg' to disconnect.")
        println("Type 'draft' to launch the 1v1 Simulator Wizard.\n")

        while (true) {
            print("Radiant> ")
            val line = reader.readLine() ?: break
            val trim = line.trim()

            if (trim == "exit" || trim == "gg") {
                println("Disconnecting...")
                break
            }

            if (trim == "draft") {
                runInteractiveMode()
                continue
            }

            run(line, isRepl = true)

            hasError = false
        }
    }

    private fun runInteractiveMode() {
        val input = SystemScanner(System.`in`)
        println("\nBATTLESCRIPT: DRAFT SIMULATOR")

        print("HERO NAME: ")
        val p1 = input.next()

        println("\nATTRIBUTES")
        print("STRENGTH (HP): "); val str = input.nextInt()
        print("AGILITY (Armor): "); val agi = input.nextInt()
        print("INTELLIGENCE (Mana): "); val int = input.nextInt()

        val skills = mutableListOf<DraftSkill>()
        println("\nABILITY DRAFT (4 Skills)")
        for (i in 1..4) {
            println("Skill $i:")
            print("  Name: "); val name = input.next()
            print("  Type (1=Physical, 2=Magic, 3=Pure): "); val type = input.nextInt()
            print("  Damage: "); val dmg = input.nextInt()
            print("  Mana Cost: "); val cost = input.nextInt()
            print("  Cooldown (ticks): "); val cd = input.nextInt()
            skills.add(DraftSkill(name, type, dmg, cost, cd))
        }

        val items = mutableListOf<DraftItem>()
        println("\nSHOPPING PHASE (3 Active Items)")
        for (i in 1..3) {
            println("Item $i:")
            print("  Name: "); val name = input.next()
            print("  Target (1=Enemy Damage, 2=Self Heal): "); val target = input.nextInt()
            print("  Value: "); val value = input.nextInt()
            print("  Cooldown: "); val cd = input.nextInt()
            items.add(DraftItem(name, target == 1, value, cd))
        }

        println("\nCompiling Combat Script...")

        val sb = StringBuilder()

        sb.append("""
            buy game_over = 0;
            buy active_rune = 0;
            buy p1_str = $str; buy p1_agi = $agi; buy p1_int = $int;
            buy p1_hp = p1_str * 20; buy p1_max_hp = p1_hp;
            buy p1_mana = p1_int * 12; buy p1_armor = p1_agi * 0.1;
            buy p1_deaths = 0; buy p1_tower = 2000; buy p1_gold = 1000;

            buy p2_hp = 1200; buy p2_armor = 5; buy p2_mag_res = 0.25;
            buy p2_tower = 2000; buy p2_deaths = 0;
        """.trimIndent())

        skills.forEach { skill ->
            sb.append("\ninvoke ${skill.name}() [mana: ${skill.cost}, cd: ${skill.cd}] {\n")
            sb.append("    buy raw = ${skill.dmg};\n")
            when (skill.type) {
                1 -> sb.append("    buy final = raw * (1 - (p2_armor * 0.05));\n")
                2 -> sb.append("    buy final = raw * (1 - p2_mag_res);\n")
                3 -> sb.append("    buy final = raw;\n")
            }
            sb.append("    chat_wheel \"${skill.name}! Deals \" + final + \" dmg.\";\n")
            sb.append("    p2_hp = p2_hp - final;\n")
            sb.append("    p1_mana = p1_mana - ${skill.cost};\n}\n")
        }

        items.forEach { item ->
            sb.append("\ninvoke ${item.name}() [mana: 0, cd: ${item.cd}] {\n")
            if (item.isDamage) {
                sb.append("    p2_hp = p2_hp - ${item.value};\n    chat_wheel \"Used ${item.name}! Zapped enemy.\";\n")
            } else {
                sb.append("    p1_hp = p1_hp + ${item.value};\n    chat_wheel \"Used ${item.name}! Healed.\";\n")
            }
            sb.append("}\n")
        }

        sb.append("""
            invoke BotNuke() [mana:0, cd:5] {
               buy dmg = 150 * (1 - (p1_int * 0.001));
                chat_wheel "Bot used Nuke! You took " + dmg;
                p1_hp = p1_hp - dmg;
            }

            chat_wheel ">>> MATCH START: $p1 vs Bot";
            buy tick = 0;
            farm (game_over == 0) {
                ward (random_event() > 0.9) { active_rune = spawn_rune(); chat_wheel ">>> RUNE SPAWNED"; }
        """.trimIndent())

        skills.forEach { sb.append("\n                ${it.name}();") }
        items.forEach { sb.append("\n                ${it.name}();") }

        sb.append("""
                BotNuke();
                ward (tick == 5) { chat_wheel "HP: " + p1_hp + " | Enemy: " + p2_hp; tick = 0; }
                ward (p1_hp <= 0) { chat_wheel "YOU DIED"; p1_deaths = p1_deaths + 1; p1_hp = p1_max_hp; }
                ward (p2_hp <= 0) { chat_wheel "ENEMY DIED"; p2_deaths = p2_deaths + 1; p2_hp = 1200; }
                ward (p1_deaths >= 2) { game_over = 1; chat_wheel "DIRE VICTORY"; }
                ward (p2_deaths >= 2) { game_over = 1; chat_wheel "RADIANT VICTORY"; }
                tick = tick + 1;
            }
        """.trimIndent())

        println("Generated. Executing...\n")
        run(sb.toString(), isRepl = false)
    }

    private fun run(source: String, isRepl: Boolean) {
        try {
            val scanner = Scanner(source)
            val tokens = scanner.scanTokens()

            val parser = Parser(tokens)
            val program = parser.parse()

            interpreter.interpret(program, isRepl)

        } catch (e: Exception) {
            println("Error: ${e.message}")
            hasError = true
        }
    }
}