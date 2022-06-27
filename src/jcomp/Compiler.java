package jcomp;
import java.util.ArrayList;
/**
 * Compiles a given file.
 * 
 * @author peter dordal 
 * @version 1.001
 * 
 * change 11/18/09: added a parameter isTopLevel to compileCompound(), 
 * to be used in determining if the new scope created by the compound statement
 * is likely to be "big" (isTopLevel==true) or not.
 */

/**
 * formal Extended Backus-Naur syntax for the language minijava.
 * Each statement here is called a "production", with a grammatical symbol on the left
 * and what it can represent on the right.
{foo}   0 or more occurrences of foo
[foo]   0 or 1 occurrences of foo
|   either/or

We compile {foo} by 
    while (theToken is in the set of tokens that can BEGIN a foo) compileFoo();
    
If we have "foo { ',' foo }, then we can compile that with
    compileFoo(); while (theToken==comma) {accept(comma); compileFoo());
However, an alternative idiom is the following:
    while(true) {compileFoo(); if (theToken==comma) accept comma(); else break;}
    
In our simplified parsing techinque, it is essential that we can distinguish which
alternative to pursue by looking only at the first symbol! That way, we can proceed
by examining only theToken, below, without further "lookahead".

The parsing process is supplemented by code generation.

program ::= {function|declaration}

function ::= identifier '(' paramlist ')' compound_stmt

compound_statement ::= '{' statement { statement } '}'

statement ::= ident_stmt | while_stmt | if_stmt | compound_stmt | declaration 
          | print_stmt | ';'

while_stmt ::= 'while' '(' expr ')' stmt
if_stmt    ::= 'if' '(' expr ')' stmt

print_stmt ::= 'print' '(' (expr | string) { ',' (expr|string) } ')'

ident_stmt ::= identifier (assignment | arglist) ';'
assignment ::= '=' expr 
arglist ::= '(' [expr { ',' expr } ] ')'

declaration ::= int identifier [ '[' const ']' ]
        |   'final' 'int'  identifier '=' const

expr ::= simple_expr [ relop simple_expr ]
simple_expr ::= ['+'|'-'] term { addop term }
term ::= factor { mulop factor }
factor ::= identifier | number | '(' expr ')'

const ::= identifier | number

 */
public class Compiler
{
    // instance variables - replace the example below with your own
    Tokenizer t;
    Codestream cs;
    String theToken;
    int mainlabel; //, f1entry, f2entry, f3entry, f4entry;
    boolean DEBUG = true;
    String filename = null;
    boolean isCompiled = false;
    final int INTSIZE = 1;
    boolean EVAL_CONSTANTS = true; // true means we evaluate constant expressions at compile time
    
    private SymbolTableGL stgl;
    private SymbolTable1 st;
    // private NestedSymbolTable st;
    boolean INNERSCOPES = st.innerScopes();
    
    /*# Note that, in the following, IDENTIFIER, STRING, and NUMBER
     * still make use of a String attribute.
     *
    public static enum Tokens {COMMA, SEMICOLON, 
        LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET, 
        ASSIGN, 
        PLUS, MINUS, TIMES, DIV, MOD, LOGICAND, LOGICOR, BITAND, BITOR,
        EQUAL, LESS, LESSEQUAL, GREATER, GREATEREQUAL, NOTEQUAL,
        // reserved words
        DO, WHILE, IF, ELSE, FUNCTION, FINAL, INT, PRINT,
        IDENTIFIER, STRING, NUMBER, EOF, UNKNOWN
    }
    /* */    
    

    /**
     * Constructor for objects of class Compiler
     */
    public Compiler(String fname)
    {
        
        // initialise instance variables
        t = new Tokenizer(fname);
        //t.setDebug(true);         /*# only do this for very small programs! */
        cs = new Codestream();
        theToken = t.token();       // one-symbol lookahead
        //f1entry = f2entry = f3entry = f4entry = -1;         // function hack!
        filename = fname;
        st = new SymbolTable1(t);
	    //st = new NestedSymbolTable(t);
    }
    
    public Compiler() {this("hello1.mjava");}
        
    // this doesn't work for identifiers, strings, and numbers:
    private void accept(String expected) {
        if (theToken == null && expected == null 
            || theToken !=null && theToken.equals(expected)) {
                theToken = t.token();
        } else {
            t.error("expected "+expected+"; got "+ theToken);
        }
    }
    
    public static void main(String[] args) {
        String fname;
        fname = args[0];
        /*fname = "newfact.mjava";
        fname = "hello.mjava";*/
        //Compiler c = new Compiler("mjava/" + fname);   // sets cs
        Compiler c = new Compiler(fname);   // sets cs
        c.compileProgram();
        c.dump();
	    System.exit(9);
        c.run();
    }
    /*
     * utilities for working with the data
     */
    
    private boolean isString(String t) {
        if (t!=null && t.length() > 0 && t.charAt(0) == '"') return true;
        else return false;
    }
    
    /*# this needs to check for reserved words final, int, while, if, print !! */
    private boolean isIdent(String t) {
        if (equals(t, "final") || equals(t, "int") || equals(t, "while")
            || equals(t, "if") || equals(t, "else") || equals(t, "print")) return false;
        if (t!=null && t.length() > 0 && Character.isLetter(t.charAt(0))) return true;
        else return false;
    }
    
    private boolean isNumber(String t) {
        if (t!=null && t.length() > 0 && Character.isDigit(t.charAt(0))) return true;
        else return false;
    }
    
    // theToken can be null (for EOF), which is annoying
    private boolean equals(String s1, String s2) {
        /*if (s1==null) return false;*/
        if (s1==null) { t.error("got " + s1); }
        if (s2==null) { t.error("got " + s2); }
        return s1.equals(s2);
    }
    
    private void dprint(String s) { if (DEBUG) System.out.println(s); }

    public void run() {
        if (! isCompiled) {
            System.out.println("not compiled yet!");
            return;
        }
        System.out.println("running " + filename);
        Machine m = new Machine(cs);
        m.run();
    }
    
    public void dump() {
        if (! isCompiled) {
            System.out.println("not compiled yet!");
            return;
        }
        System.out.println("dumping " + filename);
        cs.dump();
    }   
 
    
    /****************************************************************************
     * 
     *                     MAIN COMPILER SECTION
     *                     
     ****************************************************************************/
    
    /*# these methods each correspond to a specific grammatical production, above */
    // program ::= { function | declaration }
    public void compileProgram() {        // null for EOF!
        System.out.println("compiling " + filename);
        while (theToken != null) {
            if (equals(theToken, "int") || equals(theToken, "final")) {
                compileDeclaration(true);
            } else {
                compileFunction();
            }
        }
        cs.emit(Machine.HALT);
        isCompiled = true;
    }
    
   
    // function ::= identifier '(' paramlist ')' compound_stmt
    // note the "fi" hack
    private void compileFunction() {
        //dprint("FUNCTION");
        String fname = theToken;
        st.allocProc(fname, cs.getPos());
        if (equals(fname, "main")) {
            mainlabel = cs.getPos();
            cs.setEntry(mainlabel);
        } 
        /*
        if (equals(fname, "f1") || equals(fname, "f2") || equals(fname, "f3") || equals(fname, "f4")){
            switch(fname.charAt(1)) {
                case '1': f1entry=cs.getPos();
                case '2': f2entry=cs.getPos();
                case '3': f3entry=cs.getPos();
                case '4': f4entry=cs.getPos();
            }
        }
        */
        if (isIdent(theToken)) theToken = t.token();
        else t.error("expecting identifier, got " + theToken);
        accept("(");
        compileParamList();
        accept(")");
        if (isLHack) cs.emit(Machine.ALLOC, 10);    // space for 10 local vars
        compileCompound(true);
        if (equals(fname, "main")) cs.emit(Machine.HALT);
        else cs.emit(Machine.RET);
    }
    
    private void compileParamList() {
        // in this version, the parameter list must be EMPTY, correctly compiled as follows
    }

    // compound_stmt ::=  '{' stmt { stmt } '}'
    private void compileCompound(boolean isTopLevel) {
        //dprint("COMPOUND");
        accept("{");
        newScope();     // new scope for variable declarations!
        compileStatement();
        while (!equals(theToken, "}")) {
            compileStatement();
        }
        accept("}");
        endScope();
    }
     
    private void compileStatement() {
        if (theToken==null) {
           t.error("got " + theToken);
	}
        if (isIdent(theToken)) compileIdentStmt();
        else if (equals(theToken, "while")) compileWhile();
        else if (equals(theToken, "if")) compileIf();
        else if (equals(theToken, "{")) compileCompound(false);
        else if (equals(theToken, "final") || theToken.equals("int")) compileDeclaration(false);
        else if (equals(theToken, "print")) compilePrint();
        else if (equals(theToken, ";")) {accept (";");}      // empty statement
        else {
            t.error("invalid token : " + theToken);
        }
    }
    
    /* compileWhile needs to deal with labels and JUMPs */
    private void compileWhile() {
        int endLabel, endLabelLoc, topLabel;
        accept("while");
        accept("(");
        topLabel = cs.getPos();     // come here at end
        compileExpr();
        accept(")");
        endLabelLoc = cs.emit(Machine.JZ, 0);  // jump to end if false
        compileStatement();
        cs.emit(Machine.JUMP, topLabel);
        endLabel = cs.getPos();
        cs.pokeLabel(endLabel, endLabelLoc);
    }
    
    /* compileIf also needs to deal with labels and jumps, 
     * which are slightly different if there is an else part
     */
    // condition                      condition
    // if false goto falselabel       if false goto falselabel
    // true_part                      true_part
    // goto endlabel                  falselabel:
    // falselabel: false_part
    // endlabel:
    
    private void compileIf() {
        int falseLabel, falseLabelLoc, endLabel, endLabelLoc;
        accept("if");
        accept("(");
        compileExpr();
        accept(")");
        falseLabelLoc = cs.emit(Machine.JZ, 0);
        
        compileStatement();
        
        if (equals(theToken, "else")) {
            accept("else");
            endLabelLoc = cs.emit(Machine.JUMP, 0);
            falseLabel = cs.getPos();
            compileStatement();
            endLabel = cs.getPos();
            cs.pokeLabel(endLabel, endLabelLoc);
            cs.pokeLabel(falseLabel, falseLabelLoc);
        } else {
            falseLabel = cs.getPos();
            cs.pokeLabel(falseLabel, falseLabelLoc);
        }
    }
    
    // this one actually generates code to print the string.
    private void compilePrint() {
        //dprint("PRINT");
        accept("print");
        accept("(");
        // parsing (expr|string) { ',' (expr|string) }, alternative idiom above
        while (true) {
            if (isString(theToken)) {
                String theString = theToken.substring(1,theToken.length()-1); // strip quotes
                int spos = cs.addString(theString);
                cs.emit(Machine.SPRINT, spos);
                theToken = t.token();
            } else {
                compileExpr();
                cs.emit(Machine.IPRINT);
            }
            if (!equals(theToken, ",")) break;
            accept(",");
        }
        accept(")");
    }
    
    // declaration ::= 'int' identifier ';' | 'final' 'int' identifier '=' number ';'
    // use the parameter to decide if this is a global or local declaration
    private void compileDeclaration(boolean isGlobal) {
        if (equals(theToken, "int")) {
            accept("int");
            String ident = theToken;
            if (!isIdent(theToken)) t.error("expected identifier, got " + theToken);
            IdentInfo ii = st.allocVar(ident, isGlobal);    /*# check if not previously declared, and assign address!*/
            if (!isGlobal) cs.emit(Machine.ALLOC, INTSIZE); // was: stackFrameSize());
            //dprint("declaring int " + ident);
            theToken = t.token();
            accept (";");
        } else if (equals (theToken, "final")) {
            accept("final");
            accept("int");
            String ident = theToken;
            if (!isIdent(theToken)) t.error("expected identifier, got " + theToken);
            theToken = t.token();
            accept("=");
            if (!isNumber(theToken)) t.error("expected number, got " + theToken);
            int numvalue = new Integer(theToken).intValue();
            st.allocConst(ident, numvalue);    /*# check for prev declaration, and set the (const) value below */
            theToken = t.token();
            dprint("declaring constant " + ident + " = " + numvalue);
            accept(";");
        } else {
            t.error("unknown declaration");
        }       
    }
    /**
     * ident_stmt ::= identifier (assignment | arglist) ';'
     * assignment ::= '=' expr 
     * arglist ::= '(' [expr { ',' expr } ] ')'
     */
    private void compileIdentStmt() {
        String ident = theToken;
        if (!isIdent(ident)) t.error("expected identifier, got " + theToken);
        theToken = t.token();
        if (equals(theToken, "=")) {    // ASSIGNMENT
            accept("=");
            // should verify that the identifier is declared as a variable!
            IdentInfo varInfo = st.lookup(ident);
            int location = -1;
            boolean isGlobal = true;
            if (varInfo==null) {
                //rahul t.error("undefined variable: " + ident);
                location = GHack(ident);
                isGlobal = true;
            } else {
                location = varInfo.getAddr();
                isGlobal = varInfo.getIsGlobal();
            }
            if (location == -1) {
                location = LHack(ident);
                isGlobal = false;
            }
            compileExpr();
            if (isGlobal) cs.emit(Machine.STOR, location);
            else cs.emit(Machine.STORF, location);      // FP-relative addr
            accept(";");
        } else if (equals(theToken, "(")) {     // FUNCTION CALL
            IdentInfo funcInfo = st.lookup(ident);
            int loc = 0;
            if (funcInfo == null) {//loc = FHack(ident);
                //t.error("undefined function name: " + ident);
            } else {
            loc = funcInfo.getEntryPoint();
	    }
            dprint("function " + ident + " at location " + loc);
            accept("(");
            /* 
            if (!equals(theToken, ")")) {   // all this deals with nonempty arg lists!!!
                compileExpr();
                while (equals(theToken, ",")) {
                    accept(",");
                    compileExpr();
                }
            }
            /* */
            accept(")");
            accept(";");
            cs.emit(Machine.JSR, loc);
        } else t.error("expected \"=\" or \"(\", got " + theToken);
    }
    
    /*# aside from parsing, compileExpr also generates code 
     * to leave the expression on the runtime stack
     */

    private void compileExpr () {
        if (EVAL_CONSTANTS) {
            compileExpr3().compile(cs);
        } else {
            compileExpr1();
        }
    }

    private void compileExpr1() {
        compileSExpr1();
        if (equals(theToken, "==") || equals(theToken, "<=") || equals(theToken, "<")
            || equals(theToken, ">=") || equals(theToken, ">") || equals(theToken, "!=")) {
                String relop = theToken;
                theToken = t.token();
                compileSExpr1();
                // two exprs now on the stack
                if (equals(relop, "==")) {
                    cs.emit(Machine.CEQ);
                } else if (equals(relop, "!=")) {
                    cs.emit(Machine.SUB);
                } else if (equals(relop, "<")) {
                    cs.emit(Machine.SWAP);
                    cs.emit(Machine.CGT);
                } else if (equals(relop, "<=")) {
                    cs.emit(Machine.SWAP);
                    cs.emit(Machine.CGE);
                } else if (equals(relop, ">")) {
                    cs.emit(Machine.CGT);
                } else if (equals(relop, ">=")) {
                    cs.emit(Machine.CGE);
                }
                
        }
    }
    
    private void compileSExpr1() {
        boolean isminus = false;
        if (equals(theToken, "+")) accept("+");  // doesn't do anything
        else if (equals(theToken, "-")) {accept("-"); isminus = true;}
        compileTerm1();
        if (isminus) cs.emit(Machine.NEG);
        while (equals(theToken, "+") || equals(theToken, "-") || equals(theToken, "||")) {
            String addop = theToken;
            theToken = t.token();
            compileTerm1();
            if (equals(addop, "+"))       cs.emit(Machine.ADD);
            else if (equals(addop, "-"))  cs.emit(Machine.SUB);
            else if (equals(addop, "||")) cs.emit(Machine.LOR);
        }
    }
    
    private void compileTerm1() {
        compileFactor1();
        while (equals(theToken, "*") || equals(theToken, "/") || equals(theToken, "%") || equals(theToken, "&&")) {
            String mulop = theToken;
            theToken = t.token();
            compileFactor1();
            if (equals(mulop, "*"))       cs.emit(Machine.MUL);
            else if (equals(mulop, "/"))  cs.emit(Machine.DIV);
            else if (equals(mulop, "%"))  cs.emit(Machine.MOD);
            else if (equals(mulop, "&&")) cs.emit(Machine.LAND);
        }
    }
    
    private void compileFactor1() {
        if (isIdent(theToken)) {
            String ident = theToken;
            theToken = t.token();
            IdentInfo theInfo = st.lookup(ident);
            /*if theInfo == null, we have an undeclared variable! */
            if (theInfo == null) {
		    /*t.error ("undeclared variable: " + ident);*/
	    } else {
		    int theAddr = -1; boolean isGlobal = true;
		    theAddr = theInfo.getAddr();
		    isGlobal = theInfo.getIsGlobal();
		    if (theAddr == -1) {
			    /*t.error("undeclared identifier used in expr: "+ident);*/
		    } else {
		    if (isGlobal) cs.emit(Machine.LOAD, theAddr);
		    else cs.emit(Machine.LOADF, theAddr);}
	    }
        } else if (isNumber(theToken)) {
            int theNumber = new Integer(theToken).intValue();
            cs.emitLOADINT(theNumber);
            theToken = t.token();
        } else if (equals(theToken, "(")) {  // nothing to do to generate code!
            accept("(");
            compileExpr1();
            accept(")");
        }
    }
    /*# compileExpr2, compileSExpr2, compileTerm2, compileFactor2:
     * If the expr/term/factor is constant (including formulas based on final ints),
     * then the *value* is returned. 
     * If code is generated to load the stack, then null is returned.
     * 
     * Note that if we need to load the stack, we need to generate code.
     * This is done with 
     *      cs.emitLOADINT(value)
     */

    private Integer compileExpr2() {
        Integer ci1 = compileSExpr2();
        if (equals(theToken, "==") || equals(theToken, "<=") || equals(theToken, "<")
            || equals(theToken, ">=") || equals(theToken, ">") || equals(theToken, "!=")) {
                if (ci1!=null) cs.emitLOADINT(ci1);
                String relop = theToken;
                theToken = t.token();
                Integer ci2 = compileSExpr2();
                if (ci2!=null) cs.emitLOADINT(ci2);
                // two exprs now on the stack
                if (equals(relop, "==")) {
                    cs.emit(Machine.CEQ);
                } else if (equals(relop, "!=")) {
                    cs.emit(Machine.SUB);
                } else if (equals(relop, "<")) {
                    cs.emit(Machine.SWAP);
                    cs.emit(Machine.CGT);
                } else if (equals(relop, "<=")) {
                    cs.emit(Machine.SWAP);
                    cs.emit(Machine.CGE);
                } else if (equals(relop, ">")) {
                    cs.emit(Machine.CGT);
                } else if (equals(relop, ">=")) {
                    cs.emit(Machine.CGE);
                }
                
        }
        return null;
    }
    
    private Integer compileSExpr2() {
        boolean isminus = false;
        if (equals(theToken, "+")) accept("+");  // doesn't do anything
        else if (equals(theToken, "-")) {accept("-"); isminus = true;}
        Integer ci1 = compileTerm2();
        if (isminus) {
            if (ci1!=null) ci1 = -ci1;
            else cs.emit(Machine.NEG);
        }
        while (equals(theToken, "+") || equals(theToken, "-") || equals(theToken, "||")) {
            String addop = theToken;
            theToken = t.token();
            Integer ci2 = compileTerm2();
            if (ci1!=null && ci2!=null) { // both nonconstants
            }
            if (equals(addop, "+"))       cs.emit(Machine.ADD);
            else if (equals(addop, "-"))  cs.emit(Machine.SUB);
            else if (equals(addop, "||")) cs.emit(Machine.LOR);
        }
        return null;
    }
    
    private Integer compileTerm2() {
        compileFactor2();
        while (equals(theToken, "*") || equals(theToken, "/") || equals(theToken, "%") || equals(theToken, "&&")) {
            String mulop = theToken;
            theToken = t.token();
            compileFactor2();
            if (equals(mulop, "*"))       cs.emit(Machine.MUL);
            else if (equals(mulop, "/"))  cs.emit(Machine.DIV);
            else if (equals(mulop, "%"))  cs.emit(Machine.MOD);
            else if (equals(mulop, "&&")) cs.emit(Machine.LAND);
        }
        return null;
    }
    
    private Integer compileFactor2() {
        if (isIdent(theToken)) {
            String ident = theToken;
            theToken = t.token();
            IdentInfo theInfo = st.lookup(ident);
            /*# if theInfo == null, we have an undeclared variable! */
            int theAddr = -1; boolean isGlobal = true;
            // theAddr = theInfo.???
            // isGlobal = theInfo.???
            if (theInfo == null) {
                theAddr = GHack(ident);
                isGlobal = true;
            }
            if (theAddr == -1) {
                theAddr = LHack(ident);
                isGlobal = false;
            }
            if (theAddr == -1) t.error("undeclared identifier used in expr: "+ident);
            if (isGlobal) cs.emit(Machine.LOAD, theAddr);
            else cs.emit(Machine.LOADF, theAddr);
        } else if (isNumber(theToken)) {
            int theNumber = new Integer(theToken).intValue();
            cs.emitLOADINT(theNumber);
            theToken = t.token();
        } else if (equals(theToken, "(")) {  // nothing to do to generate code!
            accept("(");
            compileExpr2();
            accept(")");
        }
        return null;
    }
        /*# compileExpr3, compileSExpr3, compileTerm3, compileFactor3:
     * These return something of class ExprNode, with subclasses
     *      ConstNode
     *      VarNode
     *      UnopNode
     *      BinopNode
     * code is not generated.
     * 
     * constant evaluation
     * common subexpressions
     * 
     * 
     */

    private ExprNode compileExpr3() {
        ExprNode expr1 = compileSExpr3();
        if (equals(theToken, "==") || equals(theToken, "<=") || equals(theToken, "<")
            || equals(theToken, ">=") || equals(theToken, ">") || equals(theToken, "!=")) {
                String relop = theToken;
                theToken = t.token();
                ExprNode expr2 = compileSExpr3();
                return new BinOpNode(expr1, Tokenizer.strToToken(relop), expr2);
        }
        else return expr1;
    }
    
    private ExprNode compileSExpr3() {
        boolean isminus = false;
        if (equals(theToken, "+")) accept("+");  // doesn't do anything
        else if (equals(theToken, "-")) {accept("-"); isminus = true;}
        ExprNode term1 = compileTerm3();
        if (isminus) {
            if (term1 instanceof ConstNode) {
                int value = ((ConstNode) term1).getValue();
                value = - value;
                term1 = new ConstNode(value);
            } else {
                term1 = new UnOpNode(Tokenizer.Tokens.MINUS, term1);
            }
        }
        while (equals(theToken, "+") || equals(theToken, "-") || equals(theToken, "||")) {
            String addop = theToken;
            theToken = t.token();
            ExprNode term2 = compileTerm3();
            if (term1 instanceof ConstNode && term2 instanceof ConstNode) {
                int val1 = ((ConstNode) term1).getValue();
                int val2 = ((ConstNode) term2).getValue();
                term1 = new ConstNode(theResult(val1, addop, val2));
            } else {   
                term1 = new BinOpNode(term1, Tokenizer.strToToken(addop), term2);
            }
        }
        return term1;
    }
    
    private ExprNode compileTerm3() {
        ExprNode factor1 = compileFactor3();
        while (equals(theToken, "*") || equals(theToken, "/") || equals(theToken, "%") || equals(theToken, "&&")) {
            String mulop = theToken;
            theToken = t.token();
            ExprNode factor2 = compileFactor3();
            if (factor1 instanceof ConstNode && factor2 instanceof ConstNode) {
                int val1 = ((ConstNode) factor1).getValue();
                int val2 = ((ConstNode) factor2).getValue();
                factor1 = new ConstNode(theResult(val1, mulop, val2));
            } else {
                factor1 = new BinOpNode(factor1, Tokenizer.strToToken(mulop), factor2);
            }
        }
        return factor1;
    }
    
    private ExprNode compileFactor3() {
        if (isIdent(theToken)) {
            String ident = theToken;
            VarNode theVar;
            theToken = t.token();
            IdentInfo theInfo = st.lookup(ident);
            /*if theInfo == null, we have an undeclared variable! */
            if (theInfo == null) t.error ("undeclared variable: " + ident);
            int theAddr = -1; boolean isGlobal = true;
            if (theInfo == null) {
                t.error("problem! theInfo == null in compileFactor3()");
                return new VarNode(isGlobal, theAddr);
            } else {
                theAddr = theInfo.getAddr();
                isGlobal = theInfo.getIsGlobal();
	    }
            if (theAddr == -1) {
                theAddr = LHack(ident);
                isGlobal = false;
            }
            if (theAddr == -1) t.error("undeclared identifier used in expr: "+ident);
            return new VarNode(isGlobal, theAddr);
        } else if (isNumber(theToken)) {
            int theNumber = new Integer(theToken).intValue();
            theToken = t.token();
            return new ConstNode(theNumber);
        } else if (equals(theToken, "(")) {  // nothing to do to generate code!
            accept("(");
            ExprNode expr = compileExpr3();
            accept(")");
            return expr;
        }
        t.error ("Illegal start of expression");
        return null;
    }
    
    
    /**
     * theResult does compile-time evaluation
     */
    
    private static int theResult(int arg1, String oper, int arg2) {
        Tokenizer.Tokens op = Tokenizer.strToToken(oper);
        if (op == Tokenizer.Tokens.PLUS) return arg1 + arg2;
        if (op == Tokenizer.Tokens.MINUS) return arg1 - arg2;
        if (op == Tokenizer.Tokens.TIMES) return arg1 * arg2;
        if (op == Tokenizer.Tokens.DIV) return arg1 / arg2;
        if (op == Tokenizer.Tokens.MOD) return arg1 % arg2;
        if (op == Tokenizer.Tokens.LOGICAND) return (arg1!=0 && arg2!=0 ? 1 : 0);
        if (op == Tokenizer.Tokens.LOGICOR) return (arg1!=0 || arg2!=0 ? 1 : 0);
        return -99999999;
    }
        
    /**
     * symbol table stuff goes here
     */
    
    // hack for GLOBAL vars of form Gnn, where nn is two digits; location = 0xF000+nn
    private int GHack(String ident) {
        if (ident.length() != 3 || ident.charAt(0) != 'G') return -1;
        if (!Character.isDigit(ident.charAt(1)) || !Character.isDigit(ident.charAt(2))) return -1;
        int nn = new Integer(ident.substring(1)).intValue();
        return 0xF000 + nn;
    }
    
    // hack for LOCAL vars of form Ln, where n is one digit; location = FP+n
    private int LHack(String ident) {
        if (ident.length() != 2 || ident.charAt(0) != 'L') return -1;
        if (!Character.isDigit(ident.charAt(1))) return -1;
        int n = new Integer(ident.substring(1)).intValue();
        return n;
    }
    
    private boolean isLHack = false;         // set this to false to disable
    
    // labels for functions, f1, f2, f3, f4
    // note that each of these MUST be declared BEFORE first call!
   /*
    * private int FHack(String ident) {
        if (equals(ident, "f1")) return f1entry;
        if (equals(ident, "f2")) return f2entry;
        if (equals(ident, "f3")) return f3entry;
        if (equals(ident, "f4")) return f4entry;
        return -1;
    }
    */
    /**
     * puts a symbol in the symbol table.
     * it can be either a variable, with attribute its memory location, 
     * OR a constant (final int), in which case its attribute is its numeric value,
     * OR a function, in which case its attribute is the number of parameters
     * 
    
    private IdentInfo allocate(String idname) {
        return null;
    }
   
    private IdentInfo lookup(String idname) {
        return null;
    }
    */
    private int stackFrameSize() {
        return 0;
    }
        
    private void newScope() {
	     st.newScope();
    }
    
    private void endScope() {
	    st.endScope();
    }
}
