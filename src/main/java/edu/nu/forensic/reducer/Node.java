package edu.nu.forensic.reducer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Node {
    private String fileName = null;
    private List<Node> children = new ArrayList<>();
    private Node parent = null;
    public Integer counter = 1;
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

    public void insert(Map<String, Integer> fileFrequences, Node node){
        if(fileFrequences.size()==0)
            return;
        for(String it:fileFrequences.keySet()){
            if(node.fileName==null){
                node.setFileName(it);
                System.out.println(it+" "+fileFrequences.get(it));
                node.counter = fileFrequences.get(it);
            }
            else if(it.equals(node.getFileName())){
                node.counter +=fileFrequences.get(it);
            }
            else {
                Node newChild = new Node(it);
                newChild.counter = fileFrequences.get(it);
                node.addChild(newChild);
                node = newChild;
            }
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