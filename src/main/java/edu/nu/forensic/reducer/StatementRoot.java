package edu.nu.forensic.reducer;

import java.util.*;

public class StatementRoot
{
    private Set<FSA.Statement> Head;
    private List<FSA.Statement> PointerTable;

    public StatementRoot(){
        Head = new HashSet<>();
        PointerTable = new LinkedList<>();
    }

    public Set<FSA.Statement> getHead (){return getHead();}
    public void putHead(FSA.Statement temp) {
        Head.add(temp);
    }

    public List<FSA.Statement> getPointerTable(){return getPointerTable();}
    public void putPointerTable(FSA.Statement temp) {
        PointerTable.add(temp);
    }
    public void UpdatePointerTable(FSA.Statement oldStatement, FSA.Statement newStatement){
        PointerTable.remove(oldStatement);
        PointerTable.add(newStatement);
    }

    public static void printFSA(StatementRoot root) {
        Queue<FSA.Statement> q = new LinkedList<>();
        q.addAll(root.Head);
        while(q.size()!=0) {
            FSA.Statement statement = q.poll();
            for(FSA.Transfer it:statement.getNext()) {
                System.out.println(statement.getNum()+"->"+it.getNext().getNum());
                q.add(it.getNext());
            }
        }
     }
}