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
    public static List<Set<String>> findFrequentItemsetWithSuffix(Node head,int minSupportCount) {
        List<Set<String>> frequentItemset = new ArrayList<Set<String>>();
        Set<String> result = new HashSet<>();
        for(Node it:head.getChildren())
        {
            frequentItemset.add(getCFAP(it, minSupportCount, result));
        }
        return frequentItemset;
    }
}
