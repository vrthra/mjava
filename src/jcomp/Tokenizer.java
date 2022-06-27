package jcomp;
import java.io.*;
import java.lang.Character;
import java.util.HashMap;
/**
 * implements the token() method, which returns the next token.
 * comments are skipped over.
 * 
 * @author peter dordal 
 * @version november 2009
 */
public class Tokenizer
{
    // instance variables - replace the example below with your own
    private FileReader f;
    private BufferedReader br;
    String currentLine;     // is null at EOF
    private int currentPos;         // index of next char to be read
    private static String WHITESPACE = " \t\n\r";    // space, tab, newline
    private static String PUNCTUATION=",;(){}[]+-*/%=!<>|&";
    private boolean EOF = false;    // end of file
    private int lineNum = 0;
    private boolean DEBUG = false;
    private static HashMap<String, Tokens> strToTokenMap = null;

    /**
     * Constructor for objects of class Token
     */
    public Tokenizer(String filename)
    {
        // initialise instance variables
        try {
            f = new FileReader(filename);
        }catch (FileNotFoundException e) {
            System.out.println("file \"" + filename + "\" not found");
            System.out.println("current directory is " + System.getProperty("user.dir"));
            System.exit(1);
        }
        br = new BufferedReader(f);
        try {
            currentLine = br.readLine(); lineNum++;
        } catch (IOException e) {
            System.err.println("I/O failure");
            EOF = true;
        }
        currentPos  = 0;
        if (strToTokenMap == null) initStrToToken();
    }
    
    public void setDebug(boolean value) {DEBUG=value;}
    
    /**
     * skips ahead until currentPos/currentLine is a nonblank, and returns true,
     * OR we reach EOF, and returns false.
     */
    private boolean skipWhite() {
        if (EOF) return false;
        while (true) {
            if (currentLine == null) {
                EOF = true;
                return false;
            }
            if (currentPos >= currentLine.length()) {   // end of line
                skipLine();
                continue;
            }
            char ch = currentLine.charAt(currentPos);
            if (! isWhitespace(ch)) {
                return true;                // found a nonblank
            }
            currentPos++;
        }   
    }
    
    /**
     * skips to start of next line, or else sets EOF to true
     */
    private void skipLine() {
        try {
            currentLine = br.readLine();
        } catch (IOException e) {
            System.err.println("I/O failure");
            EOF = true;
        }
        currentPos = 0;
        lineNum ++;
    }
    
    private char currChar() {
        if (currentPos < currentLine.length()) return currentLine.charAt(currentPos);
        else return ' ';
    }
    
    private char nextChar() {
        int nextPos = currentPos+1;
        if (nextPos < currentLine.length()) return currentLine.charAt(nextPos);
        else return ' ';
    }
         
    /**
     * this returns the next token in the file, as a string, or null for EOF
     */
    private String rawtoken() {
        if (!skipWhite()) return null;  // reached EOF
        int start = currentPos;
        char ch = currentLine.charAt(currentPos);
        if (ch == '"') { return getstring(); }
        else if (isIn(ch, PUNCTUATION)) {    
            // check two-char tokens ==, !=, <=, >=, ||, &&, //
            if (ch == '=') {
                if (nextChar() == '=') {currentPos+=2; return "==";}
            } else if (ch =='<') {
                if (nextChar() == '=') {currentPos+=2; return "<=";}
            } else if (ch == '>') {
                if (nextChar() == '=') {currentPos +=2;return ">=";}
            } else if (ch == '!') {
                if (nextChar() == '=') {currentPos+=2;return "!=";}
            } else if (ch == '&') {
                if (nextChar() == '&') {currentPos+=2; return "&&";}
            } else if (ch == '|') {
                if (nextChar() == '|') {currentPos+=2; return "||";}
            } else if (ch == '/') {
                if (nextChar() == '/') {skipLine(); return "//";}
            }
            currentPos++;
            return ""+ch;
        } else if (Character.isLetter(ch)) {
            while (Character.isLetterOrDigit(currChar())) {
                currentPos++;
            }
            return currentLine.substring(start, currentPos);
        } else if (Character.isDigit(ch)) {
            while (Character.isDigit(currChar())) {
                currentPos++;
            }
            return currentLine.substring(start, currentPos);
             
        } else {
            currentPos++;
            return "ILLEGAL TOKEN";     
        }
    }
    
    private String getstring() {
        char ch;
        String theString = "\"";  // first char is '"'
        while (true) {
            currentPos++;
            if (currentPos >=currentLine.length()) error ("unclosed string: "+ theString);
            ch = currentLine.charAt(currentPos);
             if (ch == '"') {   // end of string
                theString += ch;
                currentPos++;
                return theString;
            } 
            if (ch == '\\') { // ignore backslash but next char is special
                currentPos++;
                if (currentPos >=currentLine.length()) error ("unclosed string: "+ theString);
                ch = currentLine.charAt(currentPos);    
                if (ch=='n') ch = '\n';
                else if (ch=='t') ch='\t';
            } 
            theString +=ch;
        }                
    }
    
    // returns non-comment tokens only
    public String token() {
        String s;
        do {
            s = rawtoken();
        } while (s!=null && s.equals("//"));
        if (DEBUG) System.out.println("token="+s);
        return s;
    }
    
    private static boolean isWhitespace(char ch) {
        return WHITESPACE.indexOf(ch) != -1;
    }

    private static boolean isIn(char ch, String s) {
        return s.indexOf(ch) != -1;
    }
    
    public void error(String s) {
        System.err.println(s);
        System.err.println("At line " + lineNum);
        System.err.println(currentLine);
        System.exit(1);
    }
    
    public static void demo(String filename) {
        Tokenizer t = new Tokenizer(filename);
        String s;
        while ((s = t.token())!= null) System.out.println(s);
    }
    
    /*# Note that, in the following, IDENTIFIER, STRING, and NUMBER
     * still make use of a String attribute.
     */
    public static enum Tokens {COMMA, SEMICOLON, PERIOD, COLON,
        LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET, 
        ASSIGN, 
        PLUS, MINUS, TIMES, DIV, MOD, LOGICAND, LOGICOR, BITAND, BITOR,
        EQUAL, LESS, LESSEQUAL, GREATER, GREATEREQUAL, NOTEQUAL,
        /*# reserved words */
        DO, WHILE, IF, ELSE, FUNCTION, FINAL, INT, PRINT,
        IDENTIFIER, STRING, NUMBER, EOF, UNKNOWN
    }
        
    public void initStrToToken() {
        strToTokenMap = new HashMap<String, Tokens>();
        strToTokenMap.put(",", Tokens.COMMA);
        strToTokenMap.put(";", Tokens.SEMICOLON);
        strToTokenMap.put(".", Tokens.PERIOD);
        strToTokenMap.put(":", Tokens.COLON);

        strToTokenMap.put("(", Tokens.LPAREN);
        strToTokenMap.put(")", Tokens.RPAREN);
        strToTokenMap.put("{", Tokens.LBRACE);
        strToTokenMap.put("}", Tokens.RBRACE);
        strToTokenMap.put("[", Tokens.LBRACKET);
        strToTokenMap.put("]", Tokens.RBRACKET);

        strToTokenMap.put("=", Tokens.ASSIGN);
        
        strToTokenMap.put("+", Tokens.PLUS);
        strToTokenMap.put("-", Tokens.MINUS);
        strToTokenMap.put("*", Tokens.TIMES);
        strToTokenMap.put("/", Tokens.DIV);
        strToTokenMap.put("%", Tokens.MOD);
        strToTokenMap.put("&&", Tokens.LOGICAND);
        strToTokenMap.put("||", Tokens.LOGICOR);
        strToTokenMap.put("&", Tokens.BITAND);
        strToTokenMap.put("|", Tokens.BITOR);

        strToTokenMap.put("==", Tokens.EQUAL);
        strToTokenMap.put("<", Tokens.LESS);
        strToTokenMap.put("<=", Tokens.LESSEQUAL);
        strToTokenMap.put(">", Tokens.GREATER);
        strToTokenMap.put(">=", Tokens.GREATEREQUAL);
        strToTokenMap.put("!=", Tokens.NOTEQUAL);

        strToTokenMap.put("do", Tokens.DO);
        strToTokenMap.put("while", Tokens.WHILE);
        strToTokenMap.put("if", Tokens.IF);
        strToTokenMap.put("else", Tokens.ELSE);
        strToTokenMap.put("function", Tokens.FUNCTION);
        strToTokenMap.put("final", Tokens.FINAL);
        strToTokenMap.put("int", Tokens.INT);
        strToTokenMap.put("print", Tokens.PRINT);
        
        // IDENTIFIER, STRING, NUMBER, EOF, UNKNOWN can't be converted
    }
    
    public static Tokens strToToken(String s) {
        Tokens result = strToTokenMap.get(s);
        if (result!=null) return result;
        return Tokens.UNKNOWN;
    }
}
