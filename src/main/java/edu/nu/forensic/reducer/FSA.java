package edu.nu.forensic.reducer;

import java.util.*;

public class FSA {
    public static class Statement {
        private int num;
        private Set<Transfer> next;
        private Boolean LeafNode = false;

        public Statement(){
            next = new HashSet<>();
        }

        public int getNum(){return num;}
        public void setNum(int num){this.num = num;}

        public Set<Transfer> getNext(){return next;}
        public void putNext(Transfer temp){
            next.add(temp);
        }

        public void SetLeafNode(Boolean temp){ this.LeafNode = temp;}
        public Boolean getLeafNode(){ return this.LeafNode;}
        public boolean equals(Statement temp) {
            if( this.getNum() == temp.getNum()) return true;
            else return false;
        }
    }

    public static class Transfer {
        private int parameter;
        private Statement next;

        public Transfer() {}

        private int function(int num) {return num + this.parameter;}

        public int getParameter(){return parameter;}
        public void setParameter(int numpre, int num) {this.parameter = num - numpre;}
        public void setParameter(int num){this.parameter = num;}

        public Statement getNext(){return next;}
        public void putNext(Statement temp){next = temp;}

        public Boolean transferStatement(int num) {
            if(function(num)==getNext().getNum()) return true;
            return false;
        }
    }


    public static StatementRoot buildFSA(Map<String, Integer> FileNameToFileNum, Set<Set<String>>FileSequence) {
        StatementRoot root = new StatementRoot();
        for(Set<String>it: FileSequence) {
            Statement headnode = new Statement();
            headnode = buildFSANode(FileNameToFileNum,it,headnode);
            root.putHead(headnode);
        }
        return root;
    }

    public static Statement buildFSANode(Map<String, Integer>FileNameToFileNum, Set<String>FileSequence, Statement lastNode) {
        Transfer t = new Transfer();
        String temp = FileSequence.iterator().next();
        int n = FileNameToFileNum.get(temp);
        FileSequence.remove(temp);

        t.setParameter(lastNode.getNum(),n);
        lastNode.putNext(t);

        Statement nextNode = new Statement();
        nextNode.setNum(n);
        if(FileSequence.size()==0) {
            nextNode.SetLeafNode(true);
        }
        else nextNode = buildFSANode(FileNameToFileNum, FileSequence, nextNode);
        t.putNext(nextNode);
        return lastNode;
    }

    public static String FSAreduce(String filename, StatementRoot root, Map<String, Integer> FileNameToFileNum) {
        String NotMatch = "not match";
        String Match = "match";
        String canReduce = "initial process";
        if(!FileNameToFileNum.containsKey(filename)) return NotMatch;
        else {
            int fileNum = FileNameToFileNum.get(filename);
            for(Statement statement: root.getPointerTable()) {
                for(Transfer transfer: statement.getNext()) {
                    if(transfer.transferStatement(fileNum)) {
                        root.UpdatePointerTable(statement, transfer.next);
                        if(transfer.next.getLeafNode()) return canReduce;
                        else return Match;
                    }
                }
            }
            for(Statement statement: root.getHead()) {
                if(statement.getNum()==fileNum) {
                    root.putPointerTable(statement);
                    return Match;
                }
            }
        }
        return NotMatch;
    }
}