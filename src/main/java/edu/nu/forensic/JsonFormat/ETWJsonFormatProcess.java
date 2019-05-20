package edu.nu.forensic.JsonFormat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ETWJsonFormatProcess {
    String processName;
    String uuid;
    Map<LabelType, Set<String>> labelToEvent = new HashMap<>();
}
