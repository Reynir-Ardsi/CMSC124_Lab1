import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File
import java.util.Scanner as SystemScanner
import kotlin.concurrent.thread
import kotlin.math.max

data class DraftSkill(val name: String, val type: Int, val dmg: Int, val cost: Int, val cd: Int)
data class DraftItem(val name: String, val isDamage: Boolean, val value: Int, val cd: Int)

class BattleScript {
    private val interpreter = Interpreter()
    private var hasError = false

    fun runFile(path: String) {
        val file = File(path)
        if (!file.exists()) {
            println("Error: File '$path' not found.")
            return
        }
        val source = file.readText()

        // Mock Arena Environment for file execution
        val mockArenaEnv = Environment()
        mockArenaEnv.define("match_active", 1.0)
        mockArenaEnv.define("top_tower_rad", 2000.0)
        mockArenaEnv.define("mid_tower_rad", 2000.0)
        mockArenaEnv.define("bot_tower_rad", 2000.0)
        mockArenaEnv.define("radiant_ancient_hp", 5000.0)
        mockArenaEnv.define("top_tower_dire", 2000.0)
        mockArenaEnv.define("mid_tower_dire", 2000.0)
        mockArenaEnv.define("bot_tower_dire", 2000.0)
        mockArenaEnv.define("dire_ancient_hp", 5000.0)

        val fileInterpreter = Interpreter(mockArenaEnv)
        run(source, isRepl = false, sharedEnv = mockArenaEnv, specificInterpreter = fileInterpreter)
        
        if (hasError) System.exit(65)
    }

    fun runPrompt() {
        val input = InputStreamReader(System.`in`)
        val reader = BufferedReader(input)

        println("=== BATTLESCRIPT CONSOLE (v7.33) ===")
        println("Type 'arena' for 5v5 Multithreaded Simulation.")
        println("Type 'draft' for 1v1 Simulator.\n")

        while (true) {
            print("Radiant> ")
            val line = reader.readLine() ?: break
            val trim = line.trim()

            if (trim == "exit" || trim == "gg") break
            if (trim == "draft") { runInteractiveMode(); continue }
            if (trim == "arena") { runArenaMode(); continue }

            run(line, isRepl = true, sharedEnv = null)
            hasError = false
        }
    }

    // === NEW 5v5 THREADED ARENA WITH FULL DRAFTING ===
    private fun runArenaMode() {
        val input = SystemScanner(System.`in`)
        println("\n=== 5v5 ARENA MATCHMAKING ===")
        println("Select Side: 1. Radiant (Map Bottom)  2. Dire (Map Top)")
        val sideChoice = input.nextInt()
        val playerIsRadiant = sideChoice == 1
        
        // Data structure to hold hero info
        data class HeroDraft(val name: String, val hp: Double, val dmg: Double)
        
        val radiantHeroes = mutableListOf<HeroDraft>()
        val direHeroes = mutableListOf<HeroDraft>()
        
        // 1. DRAFTING PHASE
        println("\n>>> ENTERING CAPTAINS MODE DRAFT <<<")
        println("You will draft all 10 heroes for this match.")

        // Draft Radiant
        println("\n--- DRAFTING RADIANT TEAM ---")
        for (i in 1..5) {
            val isPlayer = (playerIsRadiant && i == 1) // Player is Radiant Hero 1
            val prefix = if (isPlayer) "[PLAYER] " else "[BOT] "
            
            print("\n$prefix Radiant Hero $i Name: ")
            val name = input.next()
            
            print("  Base HP (e.g. 1000): "); val hp = input.nextDouble()
            print("  Base DMG (e.g. 50): "); val dmg = input.nextDouble()
            
            radiantHeroes.add(HeroDraft(name, hp, dmg))
        }

        // Draft Dire
        println("\n--- DRAFTING DIRE TEAM ---")
        for (i in 1..5) {
            val isPlayer = (!playerIsRadiant && i == 1) // Player is Dire Hero 1
            val prefix = if (isPlayer) "[PLAYER] " else "[BOT] "

            print("\n$prefix Dire Hero $i Name: ")
            val name = input.next()
            
            print("  Base HP (e.g. 1000): "); val hp = input.nextDouble()
            print("  Base DMG (e.g. 50): "); val dmg = input.nextDouble()
            
            direHeroes.add(HeroDraft(name, hp, dmg))
        }

        // Shared "Match" Environment (Map State)
        val matchState = Environment()
        matchState.define("radiant_ancient_hp", 5000.0)
        matchState.define("dire_ancient_hp", 5000.0)
        
        // Towers
        matchState.define("top_tower_rad", 2000.0); matchState.define("mid_tower_rad", 2000.0); matchState.define("bot_tower_rad", 2000.0)
        matchState.define("top_tower_dire", 2000.0); matchState.define("mid_tower_dire", 2000.0); matchState.define("bot_tower_dire", 2000.0)
        
        // Game State Flags
        matchState.define("match_active", 1.0)

        // Initialize Natives
        Interpreter(matchState) 

        val threads = mutableListOf<Thread>()
        val lanes = listOf("TOP", "TOP", "MID", "BOT", "BOT")
        
        println("\n>>> LOADING MAP... SPAWNING CREEPS...")

        // Spawn Radiant Heroes
        for (i in 0..4) {
            val hero = radiantHeroes[i]
            val stats = mapOf("hp" to hero.hp, "dmg" to hero.dmg)
            threads.add(createHeroThread(hero.name, true, lanes[i], stats, matchState))
        }

        // Spawn Dire Heroes
        for (i in 0..4) {
            val hero = direHeroes[i]
            val stats = mapOf("hp" to hero.hp, "dmg" to hero.dmg)
            threads.add(createHeroThread(hero.name, false, lanes[i], stats, matchState))
        }

        println(">>> MATCH STARTED! 10 HEROES ENTERING THE ARENA...")
        threads.forEach { it.start() }
        
        // Match Monitor Loop
        while ((matchState.get("match_active") as Double) > 0.0) {
            try { Thread.sleep(2000) } catch(e:Exception){}
            
            // Scoreboard Logic
            val rHpRaw = matchState.get("radiant_ancient_hp") as Double
            val dHpRaw = matchState.get("dire_ancient_hp") as Double
            
            // Clean up numbers: Clamp to 0 and convert to Int
            val rHp = max(0.0, rHpRaw).toInt()
            val dHp = max(0.0, dHpRaw).toInt()
            
            println("\n*** SCOREBOARD: Rad Ancient: $rHp | Dire Ancient: $dHp ***")
            
            if (rHpRaw <= 0 || dHpRaw <= 0) {
                matchState.assign(Token(TokenType.IDENTIFIER, "match_active", null, 0), 0.0)
            }
        }
        
        // Final Score Check
        val finalRad = matchState.get("radiant_ancient_hp") as Double
        val finalDire = matchState.get("dire_ancient_hp") as Double

        if (finalRad <= 0 && finalDire <= 0) {
            println("\n>>> GAME OVER: IT'S A DRAW! <<<")
        } else if (finalRad <= 0) {
            println("\n>>> GAME OVER: DIRE VICTORY! <<<")
        } else {
            println("\n>>> GAME OVER: RADIANT VICTORY! <<<")
        }
    }

    private fun createHeroThread(name: String, isRadiant: Boolean, lane: String, stats: Map<String, Double>, globalEnv: Environment): Thread {
        return thread(start = false, name = name) {
            val myTeam = if (isRadiant) "RADIANT" else "DIRE"
            // Determine targets based on lane and team
            val myTowerName = "${lane.lowercase()}_tower_" + (if (isRadiant) "rad" else "dire")
            val enemyTowerName = "${lane.lowercase()}_tower_" + (if (isRadiant) "dire" else "rad")
            val enemyAncient = if (isRadiant) "dire_ancient_hp" else "radiant_ancient_hp"

            // Construct the script dynamically
            val script = """
                buy my_hp = ${stats["hp"]};
                buy my_max_hp = ${stats["hp"]};
                buy my_dmg = ${stats["dmg"]};
                buy my_gold = 600.0;
                buy dead_timer = 0.0;
                
                chat_wheel "$name ($myTeam) spawning in $lane Lane!";
                
                farm (match_active == 1.0) {
                    
                    // 1. Death / Respawn Logic
                    ward (dead_timer > 0.0) {
                        dead_timer = dead_timer - 1.0;
                        ward (dead_timer <= 0.0) {
                            my_hp = my_max_hp;
                            chat_wheel "$name ($myTeam) has respawned!";
                        }
                    } gank {
                        
                        // 2. Alive Logic
                        buy target = 0.0;
                        buy is_ancient = 0.0;
                        
                        // Check Enemy Tower
                        ward ($enemyTowerName > 0.0) {
                            target = $enemyTowerName;
                        } gank {
                            // Tower down, push Ancient
                            target = $enemyAncient;
                            is_ancient = 1.0;
                        }
                        
                        // Combat
                        ward (target > 0.0) {
                            buy roll = random_event();
                            
                            // Farm Gold (30%)
                            ward (roll < 0.3) {
                                my_gold = my_gold + 45.0;
                            }
                            
                            // Push Objective (10%)
                            ward (roll > 0.9) {
                                ward (is_ancient == 1.0) {
                                    $enemyAncient = $enemyAncient - my_dmg;
                                    chat_wheel "$name attacking Ancient!";
                                } gank {
                                    $enemyTowerName = $enemyTowerName - my_dmg;
                                    ward ($enemyTowerName <= 0.0) {
                                        chat_wheel ">>> $name DESTROYED $lane TOWER! <<<";
                                        my_gold = my_gold + 500.0;
                                    }
                                }
                            }
                            
                            // Die (5%)
                            ward (roll > 0.95) {
                                my_hp = 0.0;
                                chat_wheel "X_X $name was killed!";
                                dead_timer = 5.0;
                            }
                        }
                        
                        // Shopping
                        ward (my_gold > 2000.0) {
                            my_dmg = my_dmg + 30.0;
                            my_gold = my_gold - 2000.0;
                            chat_wheel "$name bought item! Dmg: " + my_dmg;
                        }
                    }
                }
            """.trimIndent()

            val instance = Interpreter(globalEnv)
            run(script, false, globalEnv, instance)
        }
    }

    private fun runInteractiveMode() {
        val input = SystemScanner(System.`in`)
        println("\nBATTLESCRIPT: 1v1 DRAFT SIMULATOR")
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
            buy p1_str = $str.0; buy p1_agi = $agi.0; buy p1_int = $int.0;
            buy p1_hp = p1_str * 20.0; buy p1_max_hp = p1_hp;
            buy p1_mana = p1_int * 12.0; buy p1_armor = p1_agi * 0.1;
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

    private fun run(source: String, isRepl: Boolean, sharedEnv: Environment? = null, specificInterpreter: Interpreter? = null) {
        try {
            val scanner = Scanner(source)
            val tokens = scanner.scanTokens()
            val parser = Parser(tokens)
            val program = parser.parse()

            val interp = specificInterpreter ?: interpreter
            interp.interpret(program, isRepl)

        } catch (e: Exception) {
            synchronized(System.out) { println("Error: ${e.message}") }
            hasError = true
        }
    }
}