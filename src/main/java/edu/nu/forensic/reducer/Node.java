package edu.nu.forensic.reducer;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private String fileName = null;
    private List<Node> children = new ArrayList<>();
    private Node parent = null;
    private Integer counter = 1;
    private double UtilizationRatio = 0;

    public Node(String fileName) {
        this.fileName = fileName;
    }

    public void addChild(Node child) {
        child.setParent(this);
        if(this.children.contains(child)) children.remove(child);
        this.children.add(child);
    }

    public Node addChild(String data) {
        Node newChild = new Node(data);
        this.addChild(newChild);
        return newChild;
    }

    public void addChildren(List<Node> children) {
        for(Node t : children) {
            t.setParent(this);
        }
        this.children.addAll(children);
    }

    public double getUtilizationRatio() {return UtilizationRatio;}
    public void setUtilizationRatio(double Ratio) {this.UtilizationRatio = Ratio;}

    public List<Node> getChildren() {
        return children;
    }

    public Integer getCounter() {return counter; }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    private void setParent(Node parent) {
        this.parent = parent;
    }

    public Node getParent() {
        return parent;
    }

    public void insert(List<String> files, Node node){
        if(files.size()==0)
            return;
        if(node.fileName==null){
            node.setFileName(files.get(0));
            insert(files.subList(1,files.size()),node);
        }
        else if(files.get(0).equals(node.getFileName())){
            node.counter++;
            insert(files.subList(1,files.size()),node);
        }
        else{
            Node newChild= new Node(files.get(0));
            files = files.subList(1,files.size());
            insert(files,newChild);
            node.addChild(newChild);
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o) return true;
        if(o instanceof Node) {
            if(((Node) o).fileName.equals(this.fileName)) return true;
        }
        return false;
    }

    @Override
    public int hashCode(){ return this.getFileName().hashCode();}

    public void delete(Node head, Node deletedNode) {
        if(head.getChildren().contains(deletedNode)) {
            head.children.remove(deletedNode);
        }
        else
        {
            for(Node it:head.getChildren()) {
                delete(it, deletedNode);
            }
        }
    }

}