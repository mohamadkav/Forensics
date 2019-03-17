package edu.nu.forensic.reducer;

<<<<<<< HEAD
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

=======
>>>>>>> d55f6e4596dcafc9652073a782e21e3e3e2694fa
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
<<<<<<< HEAD
=======

>>>>>>> d55f6e4596dcafc9652073a782e21e3e3e2694fa
    }


    public static StatementRoot buildFSA(Map<String, Integer> FileNameToFileNum, Set<List<String>>FileSequence, Integer n) {
        StatementRoot root = new StatementRoot();
        Map<Integer, Statement> filenumTofileNode = new HashMap<>();
        for(List<String>its: FileSequence) {
            List<Integer> tempFileNumSequence = new LinkedList<>();
            for(String it:its)
            {
                if(FileNameToFileNum.containsKey(it)) tempFileNumSequence.add(FileNameToFileNum.get(it));
                else{
                    n++;
                    tempFileNumSequence.add(n);
                    FileNameToFileNum.put(it,n);
                }
            }
            tempFileNumSequence.sort((o1, o2) -> o1-o2);
<<<<<<< HEAD
            System.out.println(tempFileNumSequence.toString());
            Statement headnode = new Statement();
            Integer head = tempFileNumSequence.get(0);
            tempFileNumSequence = tempFileNumSequence.subList(1, tempFileNumSequence.size());
=======
            Statement headnode = new Statement();
            Integer head = tempFileNumSequence.iterator().next();
            tempFileNumSequence.remove(head);
>>>>>>> d55f6e4596dcafc9652073a782e21e3e3e2694fa
            if(filenumTofileNode.containsKey(head)) {
                headnode = filenumTofileNode.get(head);
                filenumTofileNode.remove(head);
                root.RemoveHead(root, headnode);
            }
            else headnode.setNum(head);
<<<<<<< HEAD
            headnode = buildFSANode(tempFileNumSequence, headnode, filenumTofileNode);
=======
            headnode = buildFSANode(tempFileNumSequence, headnode, n, filenumTofileNode);
>>>>>>> d55f6e4596dcafc9652073a782e21e3e3e2694fa
            filenumTofileNode.put(head, headnode);
            root.putHead(headnode);
        }
        return root;
    }

<<<<<<< HEAD
    public static Statement buildFSANode( List<Integer>FilenumSequence, Statement lastNode, Map<Integer, Statement> FileNumToFileNode) {
=======
    public static Statement buildFSANode( List<Integer>FilenumSequence, Statement lastNode, Integer n, Map<Integer, Statement> FileNumToFileNode) {
>>>>>>> d55f6e4596dcafc9652073a782e21e3e3e2694fa
        if(FilenumSequence.size()==0) {
            lastNode.SetLeafNode(true);
            return lastNode;
        }

<<<<<<< HEAD
        Transfer transfer = new Transfer();
        Integer temp = FilenumSequence.get(0);
        FilenumSequence = FilenumSequence.subList(1,FilenumSequence.size());
        Statement nextNode = new Statement();

        if(FileNumToFileNode.containsKey(temp)) {
            nextNode = FileNumToFileNode.get(temp);
            FileNumToFileNode.remove(temp);
        }
        else nextNode.setNum(temp);

        nextNode = buildFSANode(FilenumSequence, nextNode, FileNumToFileNode);
        FileNumToFileNode.put(temp, nextNode);
        transfer.putNext(nextNode);

        for(Transfer t :lastNode.next) {
            if(t.next!=null&&t.next.getNum()==temp) {
                lastNode.next.remove(t);
                break;
            }
        }
        transfer.setParameter(lastNode.getNum(), temp);
        lastNode.next.add(transfer);

=======
        Boolean repeat = false;
        Transfer transfer = new Transfer();
        Integer temp = FilenumSequence.iterator().next();
        FilenumSequence.remove(temp);
        Statement nextNode = new Statement();

        for(Transfer t :lastNode.next)
        {
            if(t.next!=null&&t.next.getNum()==temp) {
                repeat = true;
                break;
            }
        }
        if(!repeat){
            if(FileNumToFileNode.containsKey(temp)) {
                nextNode = FileNumToFileNode.get(temp);
                FileNumToFileNode.remove(temp);
            }
            else nextNode.setNum(temp);

            nextNode = buildFSANode(FilenumSequence, nextNode, n, FileNumToFileNode);
            FileNumToFileNode.put(temp, nextNode);

            transfer.putNext(nextNode);
            transfer.setParameter(lastNode.getNum(), temp);
            lastNode.next.add(transfer);
        }
>>>>>>> d55f6e4596dcafc9652073a782e21e3e3e2694fa
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