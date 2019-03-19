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

    public Set<FSA.Statement> getHead (){return Head;}
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
        for(FSA.Statement tempstatement: root.Head) {
            System.out.println("noderoot -> node"+tempstatement.getNum()+";");
        }
        List<FSA.Statement> temp = new LinkedList<>();
        while(q.size()!=0) {
            FSA.Statement statement = q.poll();
            if(!temp.contains(statement)) {
                for (FSA.Transfer it : statement.getNext()) {
                    System.out.println("node" + statement.getNum() + " -> node" + it.getNext().getNum() + ";");
                    q.add(it.getNext());
                }
                temp.add(statement);
            }
        }
     }

     public void RemoveHead(StatementRoot root, FSA.Statement statement) {
         if(root.Head.contains(statement)) root.Head.remove(statement);
     }
}