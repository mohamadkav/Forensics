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


    public static StatementRoot buildFSA(Map<String, Integer> FileNameToFileNum, Set<Set<String>>FileSequence, Integer n) {
        StatementRoot root = new StatementRoot();
        Map<Integer, Statement> filenumTofileNode = new HashMap<>();
        for(Set<String>it: FileSequence) {
            Statement headnode = new Statement();
            String head = it.iterator().next();
            it.remove(head);
            int headnum;
            if(FileNameToFileNum.containsKey(head)) headnum = FileNameToFileNum.get(head);
            else{
                n++;
                headnum = n;
                FileNameToFileNum.put(head,headnum);
            }
            headnode.setNum(headnum);
            headnode = buildFSANode(FileNameToFileNum, it, headnode, n, filenumTofileNode);
            filenumTofileNode.put(headnum, headnode);
            root.putHead(headnode);
        }
        return root;
    }

    public static Statement buildFSANode(Map<String, Integer>FileNameToFileNum, Set<String>FileSequence, Statement lastNode, Integer n, Map<Integer, Statement> FileNumToFileNode) {
        if(FileSequence.size()==0) {
            lastNode.SetLeafNode(true);
            return lastNode;
        }
        Boolean repeat = false;
        Transfer transfer = new Transfer();
        String temp = FileSequence.iterator().next();
        FileSequence.remove(temp);
        Statement nextNode = new Statement();
        int i;

        if(FileNameToFileNum.containsKey(temp)) i = FileNameToFileNum.get(temp);
        else{
            n++;
            i = n;
            FileNameToFileNum.put(temp,i);
        }
        if(FileNameToFileNum.containsKey(i)) nextNode = FileNumToFileNode.get(i);
        else nextNode.setNum(i);


        nextNode = buildFSANode(FileNameToFileNum, FileSequence, nextNode, n, FileNumToFileNode);
        FileNumToFileNode.put(i, nextNode);

        for(Transfer t :lastNode.next)
        {
            if(t.next!=null&&t.next.getNum()==i) {
                repeat = true;
                break;
            }
        }
        if(!repeat){
            transfer.putNext(nextNode);
            transfer.setParameter(lastNode.getNum(), i);
            lastNode.next.add(transfer);
        }
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