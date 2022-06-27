package jcomp;
/**
 * Abstract base class for the various types of expression nodes
 */
public abstract class ExprNode
{
    // instance variables - replace the example below with your own
     public ExprNode() {
    }

    public abstract void compile (Codestream cs) ;
}
