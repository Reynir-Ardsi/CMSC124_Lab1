# [BattleScript]

## Creator

[Rainier RJ Espinal] & [Matthew Simpas]

## Language Overview

BattleScript is an interpreted, domain-specific language (DSL) designed for simulating MOBA (Multiplayer Online Battle Arena) game mechanics, specifically inspired by Dota 2. It allows users to script combat scenarios, calculate economy (GPM/XPM), and automate hero logic using terminology familiar to gamers.

The language is built on Kotlin and features a recursive descent parser and a tree-walk interpreter. It supports dynamic typing, custom function definitions (Spells), and game-state management through a distinct syntax that replaces standard programming jargon with game macro-management terms.

## Keywords

buy	            Declares a new variable (stats, items, or flags).	            var / let
invoke	        Declares a function or "Spell" with specific attributes.	    fun / function
ward	        Initiates a conditional check (provides vision).	            if
gank	        The alternative path if a condition fails.	                    else
farm	        Initiates a loop that continues while a condition is true.	    while
cycle	        Initiates an iterative loop with initialization and increment.	for
tp_scroll	    Exits a function and returns a value.	                        return
chat_wheel	    Outputs text to the console/game log.	                        print
radiant	        Boolean literal for True.	                                    true
dire	        Boolean literal for False.	                                    false
mana	        Defines the mana cost attribute of a spell.	                    N/A (Metadata)
cd	            Defines the cooldown attribute of a spell.	                    N/A (Metadata)

## Operators

BattleScript supports standard arithmetic and logical operations:

Arithmetic: 
    " + (Add/Concat)"
    " - (Subtract)"
    " * (Multiply)"
    " / (Divide)"

Assignment: = (Assigns value to variable or index)

Comparison:

" == (Equal to)"

" != (Not equal to)"

" > (Greater than)"

" < (Less than)"

" >= (Greater than or equal)"

" <= (Less than or equal)"

Logical:

" and (Logical AND)"

" or (Logical OR)"

" ! (Logical NOT)"

## Literals

BattleScript supports the following literal types:

Numbers: All numbers are treated as Doubles internally (e.g., 100, 50.5).

Strings: Enclosed in double quotes (e.g., "First Blood!"). String indexing is supported.

Booleans: Represented by the keywords radiant (true) and dire (false). true and false are also parsed as fallback literals.

## Identifiers

Identifiers are used for variable names and function names.

Rules: Must start with a letter or underscore. Can contain alphanumeric characters.

Case-Sensitivity: Identifiers are case-sensitive. hero_hp and Hero_Hp are considered different variables.

## Comments

BattleScript supports single-line comments to annotate game logic.

Syntax: //

Usage: Anything following // until the end of the line is ignored by the parser.

"buy gold = 0; //This is a comment"

## Syntax Style

Termination: Statements must be terminated with a semicolon ;.

Blocks: Code blocks for control flow (ward, farm, invoke) are enclosed in curly braces { }.

Grouping: Parentheses ( ) are used for grouping expressions and defining condition headers.

Whitespace: Whitespace (spaces, tabs, newlines) is ignored, allowing for flexible formatting.

## Sample Code

1. Hero Combat (If/Else Logic):
buy enemy_hp = 600;
buy my_mana = 100;

// Harass the enemy if mana permits
ward (my_mana >= 50) {
    chat_wheel "Casting Spell!";
    enemy_hp = enemy_hp - 150;
    my_mana = my_mana - 50;
} gank {
    chat_wheel "Not enough mana, retreating!";
}

2. Jungle Farming (While Loop):
buy creep_hp = 300;
buy hits = 0;

chat_wheel "Entering the Jungle...";

// Keep attacking while creep is alive
farm (creep_hp > 0) {
    hits = hits + 1;
    creep_hp = creep_hp - 50;
    chat_wheel "Hit " + hits + "! Creep HP: " + creep_hp;
}

chat_wheel "Camp cleared!";

3. Defining Spells (Functions):
buy p1_mana = 500;
buy p2_hp = 800;

// Spells can have metadata for simulation engines
invoke SunStrike() [mana: 100, cd: 5] {
    chat_wheel "Exort! SunStrike cast.";
    p2_hp = p2_hp - 450;
    p1_mana = p1_mana - 100;
}

// Cast the spell
SunStrike();

4. String Manipulation:
buy team = "dire";
chat_wheel team; // output: dire

// Change string character by index
team[0] = "f";
chat_wheel team; // output: fire

## Design Rationale

The primary goal of BattleScript is to bridge the gap between gaming concepts and programming logic.

Thematic Keywords: Instead of abstract terms like var or while, the language uses buy (investing in a value) and farm (repetitive action). This makes the code readable as a "game script" rather than just a computer program. ward provides "vision" on a boolean condition, which fits the MOBA metaphor perfectly.

Implicit Types: To simulate the fluid nature of game stats (where damage can be a float, but gold is an integer), numbers are handled uniformly as Doubles, reducing type-casting friction for the user.

Metadata in Functions: The invoke syntax includes [mana: x, cd: y]. While standard functions only need parameters, game skills specifically need resource management data. Embedding this directly into the function declaration makes it easier to build simulation engines that track cooldowns and mana pools automatically without cluttering the function body.

Chat Wheel: Output is framed as in-game communication, reinforcing the environment the language is designed to simulate.