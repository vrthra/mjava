package jcomp;
/**
 * ExprNode for a unary operator
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class UnOpNode extends ExprNode
{
    // instance variables - replace the example below with your own
    private Tokenizer.Tokens operand;
    private ExprNode subexpr;
    /**
     * Constructor for objects of class BinOpNode
     */
    public UnOpNode(Tokenizer.Tokens operand, ExprNode subexpr)
    {
        this.operand = operand;
        this.subexpr = subexpr;
    }

    public void compile(Codestream cs) {
        subexpr.compile(cs);
        if (operand == Tokenizer.Tokens.MINUS) {
            cs.emit(Machine.NEG);
        } else {
            System.err.println("inappropriate opcode " + operand);
            System.exit(1);
        }
    }
        
}
