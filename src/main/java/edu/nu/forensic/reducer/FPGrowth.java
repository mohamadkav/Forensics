package edu.nu.forensic.reducer;

import java.util.*;

public class FPGrowth {

    public static Set<String> getCFAP (Node node, int minSupportCount, Set<String> result)
    {
        if(node.getCounter()>minSupportCount)
        {
            result.add(node.getFileName());
            for(Node it: node.getChildren())
            {
                getCFAP(it, minSupportCount, result);
            }
        }
        return result;
    }
    public static Set<Set<String>> findFrequentItemsetWithSuffix(Node head, int minSupportCount) {
        Set<Set<String>> frequentItemset = new LinkedHashSet<Set<String>>();
        Set<String> result = new LinkedHashSet<>();
        for(Node it:head.getChildren())
        {
            frequentItemset.add(getCFAP(it, minSupportCount, result));
        }
        return frequentItemset;
    }

    public Map<String, Integer> getFileNum(Set<Set<String>> FileSequence)
    {
        Integer i = 1;
        Map<String, Integer> temp = new HashMap<>();
        for(Set<String> filesequence:FileSequence)
        {
            for(String it: filesequence)
            {
                if(!temp.containsKey(it))
                {
                    temp.put(it,i);
                    i++;
                }
            }
        }
        return temp;
    }

    public static void CalculateUtilizationRatio(Node head)
    {
        int rootNum = head.getCounter();
        for(Node it:head.getChildren())
        {
            calculateUtilizationRatio(rootNum, it);
        }
    }
    private static void calculateUtilizationRatio(int rootNum, Node node)
    {
        node.setUtilizationRatio(node.getCounter()/rootNum);
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
