package jcomp;
import java.util.HashMap;

/**
 * Preliminary start to this.
 *    Makes backup of Table on entry to new scope, restores original on exit; this is inefficient
 *    Supports functions but not constants
 *    Does not support the detection of duplicate declarations
 * 
 * @author Peter Dordal 
 * @version 0.01
 */
public class SymbolTable1<D>
{
    // instance variables - replace the example below with your own
    
    private int globalpos = 0;	// next global-variable address
    private int localpos  = 0;	// next local-variable address

    private static Tokenizer t;
    private static int scopecount = 0;	// depth of scope levels
    // the symbol table itself, and its (inefficient) backup
    private HashMap<String,IdentInfo> Table, TableBackup;

    
    /**
     * Constructor for objects of class SymbolTable
     */
    public SymbolTable1(Tokenizer tk)
    {
        t = tk;
        IdentInfo.staticInit(t);
        Table = new HashMap<String, IdentInfo>();
    }

    /**
     * allocate() creates an entry in the CURRENT scope for the given identifier.
     * It is an error if there is already an existing entry for that identifier
     * in that scope; this can be indicated by having allocate() return null
     * (paradoxical for a not-found case!) Entries in previous scopes do not matter. 
     * allocate() returns a reference to an object of type D; mutators on the D object
     * (eg setAddress()) act on the stored entry, updating it "in place".
     */
    
    //public IdentInfo allocate(String ident) {
    //     return null;
    //}
    public IdentInfo allocVar(String varname, boolean isGlobal) {
	    System.out.printf("allocating variable %s; isGlobal=%s%n", varname, isGlobal);
	    int address;
        if (isGlobal) address = globalpos++;
        else address = localpos++;
	    IdentInfo ii = new IdentInfo(IdentInfo.IDType.VARIABLE);
	    // fill in ii and STORE (varname,ii) in Table.
	    ii.setAddr(address);
	    ii.setIsGlobal(isGlobal);
	    Table.put(varname, ii);
	    return ii;
    }

    public IdentInfo allocConst(String constname, int constValue) {
	    IdentInfo ii = new IdentInfo(IdentInfo.IDType.CONSTANT);
	    boolean isGlobal = (scopecount == 0);
	    // fill in the value in ii, and STORE (constname,ii) SOMEWHERE
	    return ii;
    }

    public IdentInfo allocProc(String fname, int offset) {
        //if (fname == "f1" || fname == "f2" || fname == "f3" || fname == "f4") {
		//    FHack(fname, offset);
	    //}
	    IdentInfo ii = new IdentInfo(IdentInfo.IDType.FUNCTION);
	    ii.setEntryPoint(offset);
	    // STORE (fname,ii) in Table
	    Table.put(fname, ii);
	    return ii;
    }

    
    /**
     * lookup() finds the IdentInfo for the given identifier, in the "most recent"
     * scope. It is a compiler error if the identifier is not found,
     * although it is sufficient for lookup() to return null in this case
     * and have, say, compileFactor() issue the actual error.
     * lookup() should *never* create new records.
     */
    
    public IdentInfo lookup(String ident) {
        return Table.get(ident);
    }
    /**
     * locSize: returns the number of LOCAL variables in the table
     */
    public int stackFrameSize() {
        // put your code here
        return 0;
    }
    
    /**
     * creates a new scope, "freezing" somehow the state of the existing table
     */
    public void newScope() {
        scopecount++;
        System.out.printf("newScope() called; scopecount=%d%n", scopecount);
        if (scopecount==1) {
            localpos = 0;
            TableBackup = new HashMap<String,IdentInfo>(Table);
        }
    }
    
    /**
     * endScope() has to restore the table to its previous status.
     * Either here or at the end of compileCompound(), where this is called,
     * we also need cs.emit(Machine.ALLOC, stackFrameSize()),
     * where the size is the size *after* the end.
     */
    public void endScope() {
    	scopecount--;
        System.out.printf("endScope() called; scopecount=%d%n", scopecount);
    	if (scopecount == 0) {
    		// be sure there are no local vars around
    		Table = TableBackup;
    	}
    }
    
    public static boolean innerScopes() {return true;}

}
