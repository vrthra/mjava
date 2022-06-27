package jcomp;
/**
 * Binary expression node.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class BinOpNode extends ExprNode
{
    // instance variables - replace the example below with your own
    private Tokenizer.Tokens operator;
    private ExprNode left;
    private ExprNode right;

    /**
     * Constructor for objects of class BinOpNode
     */
    public BinOpNode(ExprNode left, Tokenizer.Tokens operator, ExprNode right)
    {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public void compile(Codestream cs) {
        left.compile(cs);
        right.compile(cs);
        // int PLUS = Compiler.Tokens.PLUS;
        //switch((int) operator) {
        //}
        if (operator == Tokenizer.Tokens.PLUS) {
            cs.emit(Machine.ADD);
        } else if (operator == Tokenizer.Tokens.MINUS) {
            cs.emit(Machine.SUB); 
        } else if (operator == Tokenizer.Tokens.TIMES) {
            cs.emit(Machine.MUL);
        } else if (operator == Tokenizer.Tokens.DIV) {
            cs.emit(Machine.DIV);
        } else if (operator == Tokenizer.Tokens.MOD) {
            cs.emit(Machine.MOD);
        } else if (operator == Tokenizer.Tokens.LOGICAND) {
            cs.emit(Machine.LAND);
        } else if (operator == Tokenizer.Tokens.LOGICOR) {
            cs.emit(Machine.LOR);
        } else if (operator == Tokenizer.Tokens.EQUAL) {
            cs.emit(Machine.CEQ);
        } else if (operator == Tokenizer.Tokens.NOTEQUAL) {
            cs.emit(Machine.SUB);
        } else if (operator == Tokenizer.Tokens.LESS) {
            cs.emit(Machine.SWAP);
            cs.emit(Machine.CGT);
        } else if (operator == Tokenizer.Tokens.LESSEQUAL) {
            cs.emit(Machine.SWAP);
            cs.emit(Machine.CGE);
        } else if (operator == Tokenizer.Tokens.GREATER) {
            cs.emit(Machine.CGT);
        } else if (operator == Tokenizer.Tokens.GREATEREQUAL) {
            cs.emit(Machine.CGE);
        } else {
             System.err.println("inappropriate opcode " + operator);
             System.exit(1);
        }
    }
        
}
