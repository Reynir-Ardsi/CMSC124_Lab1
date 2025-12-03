sealed class Stmt {
    // Root node: holds the first statement of the chain
    data class Program(val statements: Stmt?) : Stmt()
    
    // Block node: holds the first statement of the inner scope chain
    data class Block(val statements: Stmt?) : Stmt()
    
    // THE LINKED LIST NODE: Connects current stmt -> next stmt
    data class Sequence(val first: Stmt, val next: Stmt?) : Stmt()
    
    data class Expression(val expression: Expr) : Stmt()
    data class ChatWheel(val expression: Expr) : Stmt() 
    data class Buy(val name: Token, val initializer: Expr?) : Stmt() 
    
    data class Ward(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt() 
    data class Farm(val condition: Expr, val body: Stmt) : Stmt() 

    // Invoke now holds a 'Stmt?' body, which is the start of a Sequence chain
    data class Invoke(
        val name: Token, 
        val params: List<Token>, 
        val manaCost: Int, 
        val cooldown: Double, 
        val body: Stmt? 
    ) : Stmt()
    
    data class TpScroll(val keyword: Token, val value: Expr?) : Stmt()
}