package org.jelled.core;

public class LCode {
    int ops[];
    int argc;
    String name;
    
    public LCode(int ops[], int nOps, String name, int argc) {
        this.name = name;
        this.argc = argc;
        this.ops = new int[nOps];
        System.arraycopy(ops, 0, this.ops, 0, nOps);
    }

    private String dumpCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("ops(");
        sb.append(ops.length);
        sb.append("): ");
        for (int op : ops) {
            sb.append(" ");
            sb.append(op);
        }
        sb.append("\n");
        return sb.toString();
    }
    private String constant(int i) {
        return Value.toString(LVM.debugInstance.constants[i]);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb, "");
        return sb.toString();
    }

    private void toString(StringBuilder sb, String indent) {
        sb.append("compiled procedure '" + name + "', argc = " + argc + ", ops = " + dumpCode());
        int pc = 0;
        int i, j;
        LCode sub;
        
        while (pc < ops.length) {
            sb.append(indent);
            switch (ops[pc++]) {
            case LVM.OPCODE_LOCAL:
                i = ops[pc++];
                j = ops[pc++];
                sb.append(pc +  "\tLOCAL\t" + i + "," + j + "\n");
                break;
            case LVM.OPCODE_PRIMITIVE:
                i = ops[pc++];
                j = ops[pc++];
                sb.append(pc +  "\tPRIMOP\n");
                break;
            case LVM.OPCODE_JUMPFALSE:
                i = ops[pc++];
                sb.append(pc +  "\tJUMPF\t" + i + "\t; " + (i + pc) + "\n");
                break;
            case LVM.OPCODE_JUMP:
                i = ops[pc++];
                sb.append(pc +  "\tJUMP\t" + i + "\t; " + (i + pc) + "\n");
                break;
            case LVM.OPCODE_TAILCALL:
                i = ops[pc++];
                sb.append(pc +  "\tTCALL\t" + i + "\n");
                break;
            case LVM.OPCODE_CALL:
                i = ops[pc++];
                sb.append(pc +  "\tCALL\t" + i + "\n");
                break;
            case LVM.OPCODE_RETURN:
                sb.append(pc +  "\tRETURN\n");
                break;
            case LVM.OPCODE_CLOSURE:
                i = ops[pc++];
                sb.append(pc +  "\tCLOSURE\t" + i + "\t; ");
                sub = (LCode)LVM.debugInstance.constants[i];
                sub.toString(sb, indent + "\t");
                sb.append("\n");
                break;
            case LVM.OPCODE_GLOBAL:
                i = ops[pc++];
                sb.append(pc +  "\tGLOBAL\t" + i + "\t; " + constant(i) + "\n");
                break;
            case LVM.OPCODE_CONSTANT:
                i = ops[pc++];
                sb.append(pc +  "\tCONST\t" + i + "\t; " + constant(i) + "\n");
                break;
            case LVM.OPCODE_POP:
                sb.append(pc +  "\tPOP\n");
                break;
            case LVM.OPCODE_SET_LOCAL:
                i = ops[pc++];
                j = ops[pc++];
                sb.append(pc +  "\tSETLOC\t" + i + "," + j + "\n");
                break;
            case LVM.OPCODE_SET_GLOBAL:
                i = ops[pc++];
                sb.append(pc +  "\tSETGLOB\t" + i + "\t; " + constant(i) + "\n");
                break;
            case LVM.OPCODE_DEF_GLOBAL:
                i = ops[pc++];
                sb.append(pc +  "\tDEFGLOB\t" + i + "\t; " + constant(i) + "\n");
                break;
            default:
                sb.append("?: " + ops[--pc] + "\n");
                break;
            }
        }
    }
}

