package edu.nu.forensic.JsonFormat;

import java.util.EnumSet;

public enum LabelType {
    PT1,
    PT2,
    PT3,
    PT4,
    PT5,
    PT6,
    PT7,
    PT8,
    PT9,
    PT10,
    PT11,
    PT12,
    PT13,
    PT14,
    PT15,
    PT16,
    PT17,
    PT18,
    PT19,
    PT20,
    PT21,
    PT22,
    PT23,
    PT24,
    PT25,
    PT26,
    PT27,
    PT28,
    PT29,
    PT30,
    PT31,
    PT32,
    PT33,
    PT34,
    PT35,
    PT36,
    PT37,
    PT38,
    PT39,
    PT40,

    PF1,
    PF2,
    PF3,
    PF4,

    FT1,
    FT2,
    FT3,
    FT4,
    FT5,
    FT6,
    FT7,
    FT8,
    FT9,
    FT10,
    FT11,
    FT12,
    FT13,
    FT14,
    FT15,
    FT16,
    FT17,
    FT18,
    FT19,
    FT20,

    ECHO;

    private static EnumSet<LabelType> processLabels = EnumSet.of(PT1, PT2, PT3, PT4, PT5, PT6, PT7, PT8, PT9, PT10, PT11, PT12, PT13, PT14, PT15, PT16, PT17, PT18, PT19, PT20, PT21, PT22, PT23, PT24, PT25, PT26, PT27, PT28, PT29, PT30, PT31, PT32, PT33, PT34, PT35, PT36, PT37, PT38, PT39, PT40, PF1, PF2, PF3, PF4);
    private static EnumSet<LabelType> fileLabels = EnumSet.of(FT1, FT2, FT3, FT4, FT5, FT6, FT7, FT8, FT9, FT10, FT11, FT12, FT13, FT14, FT15, FT16, FT17, FT18, FT19, FT20);

    private static EnumSet<LabelType> phfLabels = EnumSet.noneOf(LabelType.class);

    static boolean isProcessLabel(LabelType type) {
        return processLabels.contains(type);
    }

    static boolean isFileLabel(LabelType type) {
        return fileLabels.contains(type);
    }

    public static void addPhfLabel(LabelType type) {
        phfLabels.add(type);
    }

    public static boolean isPhfLabel(LabelType type) {
        return phfLabels.contains(type);
    }

}
