package edu.nu.forensic.reducer;

import java.util.*;

public class FPGrowth {

    public static List<String> getCFAP (Node node, int minSupportCount, List<String> result)
    {
        if(node.counter>minSupportCount) {
            if(!result.contains(node.getFileName())) result.add(node.getFileName().substring(1));
            for(Node item: node.getChildren()) {
                getCFAP(item, minSupportCount, result);
            }
        }
        return result;
    }
    public static Set<List<String>> findFrequentItemsetWithSuffix(Node head, int minSupportCount) {
        Set<List<String>> frequentItemset = new HashSet<>();
        for(Node it:head.getChildren()) {
            List<String> result = new LinkedList<>();
            result = getCFAP(it, minSupportCount, result);
            if(result.size()!=0) frequentItemset.add(result);
        }
        return frequentItemset;
    }

    public Map<String, Integer> getFileNum(Set<Set<String>> FileSequence) {
        Integer i = 1;
        Map<String, Integer> temp = new HashMap<>();
        for(Set<String> filesequence:FileSequence) {
            for(String it: filesequence) {
                if(!temp.containsKey(it)) {
                    temp.put(it,i);
                    i++;
                }
            }
        }
        return temp;
    }

    public static void CalculateUtilizationRatio(Node head)
    {
        int rootNum = head.counter;
        for(Node it:head.getChildren())
        {
            calculateUtilizationRatio(rootNum, it);
        }
    }
    private static void calculateUtilizationRatio(int rootNum, Node node)
    {
        node.setUtilizationRatio(node.counter/rootNum);
        for(Node it: node.getChildren())
        {
            calculateUtilizationRatio(rootNum, it);
        }
    }

    public static void RemoveInfrequentlyAccessedPath(double threshold, Node head)    //Remember calling CalculateUtilizationRatio function after calling this one.
    {
        for(Node it:head.getChildren())
        {
            if(it.getUtilizationRatio()<threshold) it.delete(head, it);
            else RemoveInfrequentlyAccessedPath(threshold, it);
        }
    }

}
