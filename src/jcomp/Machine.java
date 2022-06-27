package jcomp;
import java.util.ArrayList;

/**
 * This class is the Machine Interpreter.
 * 
 * @author peter dordal 
 * @version november 2009
 */
public class Machine
{
    // TOP = top value on stack, PREV = prev value
    public static final int HALT= 1;
    public static final int NOP = HALT+1;
    public static final int ADD = NOP+1;
    public static final int SUB = ADD+1;    // push PREV - TOP
    public static final int MUL = SUB+1;
    public static final int DIV = MUL+1;    // push PREV / TOP
    public static final int MOD = DIV+1;
    public static final int NEG = MOD+1;
    public static final int AND = NEG+1;    // bitwise AND
    public static final int OR  = AND+1;    // bitwise OR
    public static final int LAND=  OR+1;    // logical AND
    public static final int LOR = LAND+1;   // logical OR
    public static final int NOT = LOR+1;
    public static final int XOR = NOT+1;
    public static final int CEQ = XOR+1;    // push boolean PREV == TOP
    public static final int CGE = CEQ+1;    // push boolean PREV >= TOP
    public static final int CGT = CGE+1;    // push boolean PREV > TOP
    public static final int DUP = CGT+1;
    public static final int SWAP= DUP+1;    // swaps top two items on stack
    public static final int RET = SWAP+1;   // SP=FP (where it started for this frame; =ALLOC 0)
                                            // pop FP, pop addr, branch to it
    
    public static final int PLOAD =RET+1;   // pop addr, push memory[addr]
    public static final int PSTOR = PLOAD+1; // pop value, pop addr, memory[addr]=value
    public static final int IPRINT= PSTOR+1;    // pop value and print as integer
    
    
/*# opcodes with a 16-bit operand, known here as ARG*/

    public static final int LOAD = 32;       // push value at given address ARG
    public static final int STOR = LOAD+1;   // store stacktop at given address ARG
    public static final int LOADF= STOR+1;   // push value at location FP+ARG
    public static final int STORF= LOADF+1;  // stor stacktop at location FP+ARG
    public static final int ALLOC= STORF+1;  // SP = FP+ARG; creates/resizes frame
    public static final int JUMP = ALLOC+1;  // PC = given address
    public static final int JNZ  = JUMP+1;   // pop value; PC = addr if value!=0; jimp-if-true
    public static final int JZ   = JNZ+1;    // jump if false
    public static final int JGE  = JZ+1;
    public static final int JGT  = JGE+1;
    public static final int JSR  = JGT+1;   // push PC, push FP, FP=SP, jump to ARG
    public static final int LOADI = JSR+1;  // load 16-bit operand to top of stack
    public static final int LOADH = LOADI+1; // loads 16-bit operand to upper halfword of existing top of stack
    
    
    public static final int SPRINT = 64;   // print string[ARG]
    
    private static final int BIG = 0x10000;
    
    private byte[] program;
    private int[]  memory;
    private ArrayList<String> strings;       // special area for strings
    private int[]  stack;
    private int SP;         // stack pointer; points to next open slot on stack
    private int FP;         // Frame pointer
    private int PC;         // program counter
    private int entryPoint = 0;
    

    /**
     * Constructor for objects of class Machine
     */
    public Machine(byte[] prog, ArrayList<String> str)
    {
        program = prog;
        memory = new int[BIG];
        strings = str;
        stack = new int[BIG];
        SP = 0;                 // next open position
        FP = 0;
        PC = 0;                 // index of next instruction to be executed
        entryPoint = 0;
     }
     
     public Machine(Codestream cs) {
         this(cs.getCode(), cs.getStrings());
         entryPoint = cs.getEntry();
     }
     
     public static boolean hasArg(int opcode) { return (opcode >= 32); }
     private int arg() { return (((program[PC]& 0xFF)<<8) | (program[PC+1] & 0xFF)); }
     
     private int pop() {return stack[--SP];}
     private void push(int val) {
         try {stack[SP++]=val;}
         catch (ArrayIndexOutOfBoundsException e) {
             fail("stack arraybounds error on PUSH, SP = " + SP);
         }
     }
     
     /**
      * run the designated program
      */
     public void run(int entrypoint) {
         PC = entrypoint;
         while (true) {
             int opcode = program[PC];
             int arg = -1;
             PC++;
             if (hasArg(opcode)) {
                 arg=arg();
                 PC+=2;
             }
             switch(opcode) {
                 case ADD: {int second=pop(); push(pop() + second); break;}
                 case SUB: {int second=pop(); push(pop() - second); break;}
                 case MUL: {int second=pop(); push(pop() * second); break;}
                 case DIV: {int second=pop(); push(pop() / second); break;}
                 case MOD: {int second=pop(); push(pop() % second); break;}
                 case NEG: {push(-pop()); break;}
                 case AND: {int second=pop(); push(pop() & second); break;}
                 case OR:  {int second=pop(); push(pop() | second); break;}
                 case LAND:{int second=pop(); push(pop()!= 0 && second!=0 ? 1 : 0); break;}
                 case LOR: {int second=pop(); push(pop()!= 0 || second!=0 ? 1 : 0); break;}
                 case NOT: {push(pop() == 0 ? 1 : 0); break;}
                 case XOR: {int second=pop(); push(pop() ^ second); break;}
                 case CEQ: {int second=pop(); push(pop() == second ? 1 : 0); break;}
                 case CGE: {int second=pop(); push(pop() >= second ? 1 : 0); break;}
                 case CGT: {int second=pop(); push(pop() >  second ? 1 : 0); break;}
                 case DUP: {push(stack[SP-1]); break;}
                 case SWAP:{int second=pop(); int first = pop(); push(second); push(first); break;}
                 case RET: {SP=FP; FP=pop(); PC=pop(); break;}
                 case PLOAD: {int addr=pop(); push(memory[addr]); break;}
                 case PSTOR: {int val=pop(); int addr=pop(); memory[addr]=val; break;}
                 case IPRINT: {int val=pop(); System.out.print(" " + val); break;}
                 case NOP:  {break;}
                 case HALT: {
                     System.out.println("halting at PC="+(PC-1));
                     //if (SP!=FP) System.out.println("warning: program terminated with nonempty runtime stack!");
                     return;
                 }
                 
                 case LOAD: {push(memory[arg]); break;}
                 case STOR: {memory[arg] = pop(); break;}
                 case LOADF: {push(stack[FP+arg]); break;}
                 case STORF: {stack[FP+arg] = pop(); break;}
                 case ALLOC: {SP=SP+arg; break;}    // had been FP + arg
                 case JUMP: {PC=arg; break;}
                 case JNZ:  {if (pop() != 0) PC=arg; break;}
                 case JZ:   {if (pop() == 0) PC=arg; break;}
                 case JGE:  {if (pop() >= 0) PC=arg; break;}
                 case JGT:  {if (pop() > 0)  PC=arg; break;}
                 case JSR:  {push(PC); push(FP); FP=SP; PC=arg; break;}
                 case LOADI: {push(arg); break;}
                 case LOADH: {int st =pop() & 0xFFFF; push((arg<<16) | st); break;}
    
                 case SPRINT: {System.out.print(strings.get(arg)); break;}
    
                 default: fail("unknown opcode " + opcode);
             }
         }
     }
     
     public void run() {run(entryPoint);}

     public static void fail(String s) {
         System.err.println("FAIL: " + s);
         System.exit(1);
     }
     
     public static void demo1() {
        byte[] code = {LOADI, 0, 101, LOADI, 0, 66, MUL, IPRINT, HALT};
        Machine m = new Machine(code, null);
        m.run();
     }

     public static void demo2() {
         ArrayList<String> sa = new ArrayList<String>();
         sa.add("hello, world!\n");
         
        byte[] code = {SPRINT, 0, 0, HALT};
        Machine m = new Machine(code, sa);
        m.run();
     }
     public static void demo3() {
        byte[] code = {NOP, HALT};
        Machine m = new Machine(code, null);
        m.run();
     }
     public static void demo4() {
        byte[] code = {LOADI, 4, 0, LOADI, 0, 5, DUP, MUL, SUB, IPRINT, HALT};
        Machine m = new Machine(code, null);
        m.run();
     }

     private static void printopcodes_old() {
         System.out.println("HALT" + "\t" + HALT);
         System.out.println("NOP" + "\t" + NOP);
         System.out.println("ADD" + "\t" + ADD);
         System.out.println("SUB" + "\t" + SUB);
         System.out.println("MUL" + "\t" + MUL);
         System.out.println("DIV" + "\t" + DIV);
         System.out.println("MOD" + "\t" + MOD);
         System.out.println("NEG" + "\t" + NEG);
         System.out.println("AND" + "\t" + AND);
         System.out.println("OR" + "\t" + OR);
         System.out.println("LAND" + "\t" + LAND);
         System.out.println("LOR" + "\t" + LOR);
         System.out.println("NOT" + "\t" + NOT);
         System.out.println("XOR" + "\t" + XOR);
         System.out.println("CEQ" + "\t" + CEQ);
         System.out.println("CGE" + "\t" + CGE);
         System.out.println("CGT" + "\t" + CGT);
         System.out.println("DUP" + "\t" + DUP);
         System.out.println("SWAP" + "\t" + SWAP);
         System.out.println("RET" + "\t" + RET);
         System.out.println("PLOAD" + "\t" + PLOAD);
         System.out.println("PSTOR" + "\t" + PSTOR);
         System.out.println("IPRINT" + "\t" + IPRINT);
         System.out.println("LOAD" + "\t" + LOAD);
         System.out.println("STOR" + "\t" + STOR);
         System.out.println("LOADF" + "\t" + LOADF);
         System.out.println("STORF" + "\t" + STORF);
         System.out.println("ALLOC" + "\t" + ALLOC);
         System.out.println("JUMP" + "\t" + JUMP);
         System.out.println("JNZ" + "\t" + JNZ);
         System.out.println("JZ" + "\t" + JZ);
         System.out.println("JGE" + "\t" + JGE);
         System.out.println("JGT" + "\t" + JGT);
         System.out.println("JSR" + "\t" + JSR);
         System.out.println("LOADI" + "\t" + LOADI);
         System.out.println("LOADH" + "\t" + LOADH);
         System.out.println("SPRINT" + "\t" + SPRINT);
     }
     
     public static void printopcodes() {
         for (int i = 0; i<=SPRINT; i++) {
             String code = op2str(i);
             if (code!=null) {
                 System.out.println(code+"\t"+i);
             }
         }
     }
                

     public static String op2str(int opcode) {
         switch(opcode) {
             case HALT: return "HALT";
             case NOP: return "NOP"  ;
             case ADD: return "ADD"  ;
             case SUB: return "SUB"  ;
             case MUL: return "MUL"  ;
             case DIV: return "DIV"  ;
             case MOD: return "MOD"  ;
             case NEG: return "NEG"  ;
             case AND: return "AND"  ;
             case OR: return "OR"  ;
             case LAND: return "LAND"  ;
             case LOR: return "LOR"  ;
             case NOT: return "NOT"  ;
             case XOR: return "XOR"  ;
             case CEQ: return "CEQ"  ;
             case CGE: return "CGE"  ;
             case CGT: return "CGT"  ;
             case DUP: return "DUP"  ;
             case SWAP: return "SWAP"  ;
             case RET: return "RET"  ;
             case PLOAD: return "PLOAD"  ;
             case PSTOR: return "PSTOR"  ;
             case IPRINT: return "IPRINT"  ;
             case LOAD: return "LOAD"  ;
             case STOR: return "STOR"  ;
             case LOADF: return "LOADF"  ;
             case STORF: return "STORF"  ;
             case ALLOC: return "ALLOC"  ;
             case JUMP: return "JUMP"  ;
             case JNZ: return "JNZ"  ;
             case JZ: return "JZ"  ;
             case JGE: return "JGE"  ;
             case JGT: return "JGT"  ;
             case JSR: return "JSR"  ;
             case LOADI: return "LOADI"  ;
             case LOADH: return "LOADH"  ;
             case SPRINT: return "SPRINT"  ;
             default: return null;
        }
     }
    
}
