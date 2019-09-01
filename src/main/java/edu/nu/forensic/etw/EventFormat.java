package edu.nu.forensic.etw;

import edu.nu.forensic.GlobalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class EventFormat {
    static final Logger logger = LoggerFactory.getLogger(EventFormat.class);
    // eventKey is (providerId + opcode * 31)
    static Map<Long, EtwEventType> eventKey2EventType = new HashMap<>();
    static Map<EtwEventType, Integer> eventType2ArgSize = new HashMap<>();
    static Map<EtwEventType, List<String>> eventType2ArgNames = new HashMap<>();

    static {
        loadFormat();
    }

    public static EtwEventType getEventTypeUsingEventKey(long eventKey) {
        return eventKey2EventType.getOrDefault(eventKey, EtwEventType.Unknown);
    }

    public static int getArgSizeUsingEventType(EtwEventType etwEventType) {
        return eventType2ArgSize.getOrDefault(etwEventType, 0);
    }

    public static List<String> getArgNamesUsingEventType(EtwEventType etwEventType) {
        return eventType2ArgNames.getOrDefault(etwEventType, new ArrayList<>());
    }

    private static void loadFormat() {
        try (Scanner scanner = new Scanner(new FileReader(GlobalConfig.FORMAT_FILE_LOCATION))) {
            while (scanner.hasNextLine()) {
                // headLine looks like: 2429279289 67 FileIoRead
                // which represents providerId, opcode, EventName
                String headLine = scanner.nextLine();
                String[] splitHeadLine = headLine.split(" ");
                long providerId = Long.parseLong(splitHeadLine[0]);
                int opcode = Integer.parseInt(splitHeadLine[1]);
                long key = providerId + 31 * opcode;
                EtwEventType etwEventType = EtwEventType.valueOf(splitHeadLine[2]);
                eventKey2EventType.put(key, etwEventType);

                // secondLine looks like: 8
                // which represents argSize
                String secondLine = scanner.nextLine();
                int argSize = Integer.parseInt(secondLine);
                eventType2ArgSize.put(etwEventType, argSize);

                // argLine looks like: Offset 0 8
                // only the first String matters, it represents the name of that argument
                List<String> argNames = new ArrayList<>(argSize);
                for (int i = 0; i < argSize; i++) {
                    String argLine = scanner.nextLine();
                    String[] splitArgLine = argLine.split(" ");
                    String argName = splitArgLine[0];
                    argNames.add(argName);
                }
                eventType2ArgNames.put(etwEventType, argNames);
            }
        } catch (IOException e) {
            logger.error("scanner error", e);
            System.exit(1);
        } catch (IllegalArgumentException e) {
            logger.error("format error", e);
            System.exit(1);
        }
    }
}