package jcomp;
/**
 * For holding variables
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class VarNode extends ExprNode
{
    // instance variables - replace the example below with your own
    private int location;       // stack offset or global addr
    private boolean isGlobal;
    
    /**
     * Constructor for objects of class VarNode
     */
    public VarNode(boolean isglobal, int offset)
    {
        location = offset;
        isGlobal = isglobal;
    }

    public void compile (Codestream cs) {
        if (isGlobal) {
            cs.emit(Machine.LOAD, location);
        } else {
            cs.emit(Machine.LOADF, location);
        }
    }
    
    private int getAddress() {return location;}
    private boolean isGlobal() {return this.isGlobal;}
}
