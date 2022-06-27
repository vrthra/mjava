package jcomp;
/**
 * Preliminary start to this.
 * 
 * @author Peter Dordal 
 * @version 0.01
 */
public class SymbolTableGL<D>
{
    // instance variables - replace the example below with your own
    
    /**
     * Constructor for objects of class SymbolTable
     */
    public SymbolTableGL()
    {
        // initialise instance variables
    }

    /**
     * allocate() creates an entry in the CURRENT scope for the given identifier.
     * It is an error if there is already an existing entry for that identifier
     * in that scope; this can be indicated by having allocate() return null
     * (paradoxical for a not-found case!) Entries in previous scopes do not matter. 
     * allocate() returns a reference to an object of type D; mutators on the D object
     * (eg setAddress()) act on the stored entry, updating it "in place".
     */
    
    public IdentInfo allocate(String ident) {
        return null;
    }
    
    /**
     * lookup() finds the IdentInfo for the given identifier, in the "most recent"
     * scope. It is a compiler error if the identifier is not found,
     * although it is sufficient for lookup() to return null in this case
     * and have, say, compileFactor() issue the actual error.
     * lookup() should *never* create new records.
     */
    
    public IdentInfo lookup(String ident) {
        return null;
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
    }
    
    /**
     * endScope() has to restore the table to its previous status.
     * Either here or at the end of compileCompound(), where this is called,
     * we also need cs.emit(Machine.ALLOC, stackFrameSize()),
     * where the size is the size *after* the end.
     */
    public void endScope() {
    }
}
