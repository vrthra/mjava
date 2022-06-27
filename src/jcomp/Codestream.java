package jcomp;
import java.util.ArrayList;
/**
 * Write a description of class Codestream here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class Codestream
{
    // instance variables - replace the example below with your own
    private byte [] program;
    private ArrayList<String> strings;
    private int pos = 0;
    private int entryPoint=0;

    /**
     * Constructor for objects of class Codestream
     */
    public Codestream()  {
        program = new byte[65536];
        strings = new ArrayList<String>();
        pos = 0;
    }

    public int addString(String s) {
        int spos = strings.size();
        strings.add(s);
        return spos;
    }
    
    // this is for labels
    public int getPos() {
        return pos;
    }
    
    public void setEntry(int entry){
        entryPoint = entry;
    }
    
    public int getEntry(){
        return entryPoint;
    }
    
    // updates a previous label with the correct value
    public void pokeLabel(int theLabel, int thePos) {
        program[thePos++] = (byte) ((theLabel >> 8) & 0xFF);
        program[thePos++] = (byte) (theLabel & 0xFF);   // low-order byte
    }
    
    public void emit(int opcode) {
        emit1(opcode);
        if (Machine.hasArg(opcode)) {
            System.err.println("warning: opcode needs arg, wasn't given one: " + Machine.op2str(opcode));
        }
    }
    
    public void emit1(int opcode) {
        program[pos] = (byte) opcode;
        pos++;
    }
    
    // returns position of arg
    public int emit(int opcode, int arg) {
        emit1(opcode);
        program[pos++] = (byte) ((arg >> 8) & 0xFF);
        program[pos++] = (byte) (arg & 0xFF);   // low-order byte
        if ((arg & 0xFFFF0000) != 0) {
            System.err.println("code emission warning for opcode " + Machine.op2str(opcode) 
                + " at pos " + (pos-3) + ": oversized arg="+arg);
        }
        if (!Machine.hasArg(opcode)) {
            System.err.println("warning: opcode doesn't take arg, but was given one: " + Machine.op2str(opcode));
        }
        return pos-2;
    }
    
    // this is for loading possible 4-byte ints
    public void emitLOADINT(int arg) {
        int lo = arg & 0xFFFF;
        int hi = (arg >>16) & 0xFFFF;
        emit(Machine.LOADI, lo);
        if (hi!=0) emit(Machine.LOADH, hi);
    }
    
    public ArrayList<String> getStrings() {return strings;}
    public byte[] getCode() {return program;}
    
    public void dump() {
        int i = 0;
        while (i<=pos && program[i]!=Machine.HALT) {
            int addr = i;
            int opcode = program[i++];
            int arg = -1;
            if (Machine.hasArg(opcode)) {
                arg = ((program[i] & 0xFF) << 8) | (program[i+1] & 0xFF);
                i+=2;
            }
            String line = "" + addr;
            line = rpad(line, 8) + Machine.op2str(opcode);
            if (arg != -1) {
                line = rpad(line, 16) + arg;
            }
            if (0xF000<= arg && arg < 0xF000+100) {
                arg = arg - 0xF000;
                char d1 = (char)((arg / 10) + '0');
                char d2 = (char)((arg % 10) + '0');
                line = rpad(line, 26) + "(G" + d1 + d2 + "??)";
            }
            System.out.println(line);
        }
    }
    
    private static String rpad(String str, int len) {
        for (int i = str.length(); i<len; i++) str = str+" ";
        return str;
    }
    
}
