package jcomp;
/**
 * information on program identifiers
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class IdentInfo
{
    static enum IDType {VARIABLE, CONSTANT, FUNCTION};
    
    private IDType theType;
    private int address;        // for VARIABLEs
    private boolean isGlobal;   // for VARIABLEs
    private int value;          //for CONSTANTs
    private int entryPoint;     // for PROCEDUREs
    private static Tokenizer theTokenizer;      // for error messages
    
    /**
     * Constructor for objects of class IDinfo
     */
    public IdentInfo(IDType idt) {
        theType = idt;
    }

    public IDType getType() {return theType;}
    public void   setType(IDType type) {theType = type;}
    
    public static void staticInit(Tokenizer t) {theTokenizer = t;}
    
    /*# VARIABLE stuff */
    public int getAddr()
    {
        if (theType == IDType.VARIABLE) return address;
        else {
            error("IdentInfo called for VARIABLE attributes but is " + theType);
            return 0;
        }
    }
    
    public void setAddr(int addr)
    {
        if (theType == IDType.VARIABLE) address = addr;
        else {
            error("IdentInfo called for VARIABLE attributes but is " + theType);
        }
    }
    
    public boolean getIsGlobal() {
        if (theType == IDType.VARIABLE) return isGlobal;
        else {
            error("IdentInfo called for VARIABLE attributes but is " + theType);
            return false;
        }
    }
    
    public void setIsGlobal(boolean isg) {
        if (theType == IDType.VARIABLE) isGlobal = isg;
        else {
            error("IdentInfo called for VARIABLE attributes but is " + theType);
       }
    }
    
    /*# CONSTANT stuff */
    public int getValue()
    {
        if (theType == IDType.CONSTANT) return value;
        else {
            error("IdentInfo called for CONSTANT attributes but is " + theType);
            return 0;
        }
    }
    
    public void setValue(int val)
    {
        if (theType == IDType.CONSTANT) value = val;
        else {
            error("IdentInfo called for CONSTANT attributes but is " + theType);
        }
    }
 
    /*# FUNCTION stuff */
    
    public int getEntryPoint()
    {
        if (theType == IDType.FUNCTION) return entryPoint;
        else {
            error("IdentInfo called for PROCEDURE attributes but is " + theType);
            return 0;
        }
    }
    
    public void setEntryPoint(int ep)
    {
        if (theType == IDType.FUNCTION) entryPoint = ep;
        else {
            error("IdentInfo called for PROCEDURE attributes but is " + theType);
        }
    }
 
    private void error(String s) {
        System.err.println(s);
    }
}
