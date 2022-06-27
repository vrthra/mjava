package jcomp;
/**
 * Write a description of class ConstNode here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class ConstNode extends ExprNode {

    private int constValue;

    /**
     * Constructor for objects of class ConstNode
     */
    public ConstNode(int val)
    {
        // initialise instance variables
       constValue = val;
    }

    public void compile (Codestream cs) {
        cs.emitLOADINT(constValue);
    }
    
    public int getValue() {return constValue;}
}
