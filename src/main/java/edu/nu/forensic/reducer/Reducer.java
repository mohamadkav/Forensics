package edu.nu.forensic.reducer;


import com.sun.javafx.iio.ios.IosDescriptor;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.IoEvent;
import edu.nu.forensic.db.entity.Object;
import edu.nu.forensic.db.entity.Subject;
import edu.nu.forensic.db.repository.EventRepository;
import edu.nu.forensic.db.repository.IoEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class Reducer {
    @Autowired
    EventRepository eventRepository;

    @Autowired
    IoEventRepository ioEventRepository;

    private Set<IoEvent> ioEventSet = new HashSet<>();
    private List<IoEvent> toBeSavedIoEvents = new ArrayList<>();
    private HashMap<String, HashSet<Integer>> fileToProcessesWhichHaveAccessedIt = new HashMap<>();
    //private HashSet<Integer> test=new HashSet<>();

    private boolean forward_check(IoEvent e_p, IoEvent e, String v) {
        for (IoEvent ioEvent : ioEventSet) {
            if (!(ioEvent.getEndTimestampNanos() <= e_p.getStartTimestampNanos() || e.getStartTimestampNanos() <= ioEvent.getStartTimestampNanos()))
                return false;
        }
        //System.out.println("get forward");
        return true;
    }

    private boolean backward_check(IoEvent e_p, IoEvent e, String u) {
        for (IoEvent ioEvent : ioEventSet) {
            if (!(ioEvent.getEndTimestampNanos() <= e_p.getEndTimestampNanos() || e.getEndTimestampNanos() <= ioEvent.getStartTimestampNanos()))
                return false;
        }
        //System.out.println("get backward");
        return true;
    }

    public void reduce(){
        Set<Event> eventSet = eventRepository.findByNamesEqualsOrNamesEqualsOrderByTimestampNanosAsc("FileIoRead","FileIoWrite");
        System.out.println("Size of eventSet: "+eventSet.size());

        String lastNames = "";
        String lastPredicateObjectPath = "";
        int lastThreadId = -1;
        IoEvent toBeSavedIoEvent = null;
        boolean dirty = true;

        for (Event event : eventSet) {
            // if current event has same type, object_path, thread_id as the last event, we are still in same time window.
            if (event.getNames().compareTo(lastNames) == 0 && event.getPredicateObjectPath().compareTo(lastPredicateObjectPath) == 0 && event.getThreadId() == lastThreadId) {
                // update the ioEvent's time window.
                toBeSavedIoEvent.setEndTimestampNanos(event.getTimestampNanos());
            }
            // o.w., we hit another time window.
            else {
                if (dirty) {
                    dirty = false;
                }
                else {
                    // save the ioEvent into buffer toBeSavedIoEvents.
                    toBeSavedIoEvents.add(toBeSavedIoEvent);
                    ioEventSet.add(toBeSavedIoEvent);
                }
                // create a new ioEvent
                toBeSavedIoEvent = new IoEvent(event.getId(), event.getSequence(), event.getType(), event.getThreadId(), event.getSubject(), event.getPredicateObject(), event.getPredicateObjectPath(), event.getPredicateObject2(), event.getPredicateObject2Path(), event.getTimestampNanos(), event.getTimestampNanos(), event.getNames(), event.getLocation(), event.getSize(), event.getProgramPoint());
                lastNames = event.getNames();
                lastPredicateObjectPath = event.getPredicateObjectPath();
                lastThreadId = event.getThreadId();
            }
            if(toBeSavedIoEvents.size() >= 1000) {
                System.out.println("Saving 1000 ioEvents...");
                ioEventRepository.saveAll(toBeSavedIoEvents);
                toBeSavedIoEvents = new ArrayList<>();
            }
        }
        toBeSavedIoEvents.add(toBeSavedIoEvent);
        ioEventSet.add(toBeSavedIoEvent);
        System.out.println("Saving the rest ioEvents...");
        ioEventRepository.saveAll(toBeSavedIoEvents);

        // debug
        /*int counter = 0;
        for (IoEvent ioEvent : ioEventSet) {
            counter += 1;
            System.out.println(counter+": "+ioEvent.getStartTimestampNanos()+", "+ioEvent.getEndTimestampNanos());
        }*/

        // CPR algorithm
        // step 1. find all possible edges.
        for (IoEvent ioEvent : ioEventSet) {
            if (fileToProcessesWhichHaveAccessedIt.containsKey(ioEvent.getPredicateObjectPath())) {
                HashSet<Integer> threadSet = fileToProcessesWhichHaveAccessedIt.get(ioEvent.getPredicateObjectPath());
                if (!threadSet.contains(ioEvent.getThreadId())) {
                    threadSet.add(ioEvent.getThreadId());
                }
            }
            else {
                HashSet<Integer> threadSet = new HashSet<>();
                threadSet.add(ioEvent.getThreadId());
                fileToProcessesWhichHaveAccessedIt.put(ioEvent.getPredicateObjectPath(), threadSet);
            }
        }
        // step 2. generate stacks for all possible edges.
        HashMap<String, HashMap<String, Stack<IoEvent>>> stackMaps = new HashMap<>();
        fileToProcessesWhichHaveAccessedIt.forEach((key, value) -> {
            value.forEach((threadId) -> {
                String file = key;
                String thread = threadId.toString();
                // create stack(file, thread)
                if (!stackMaps.containsKey(file)) {
                    HashMap<String, Stack<IoEvent>> stackMap = new HashMap<>();
                    stackMaps.put(file, stackMap);
                }
                HashMap<String, Stack<IoEvent>> stackMap = stackMaps.get(file);
                if (!stackMap.containsKey(thread)) {
                    Stack<IoEvent> stack = new Stack<>();
                    stackMap.put(thread, stack);
                }
                // create stack(thread, file)
                if (!stackMaps.containsKey(thread)) {
                    stackMap = new HashMap<>();
                    stackMaps.put(thread, stackMap);
                }
                stackMap = stackMaps.get(thread);
                if (!stackMap.containsKey(file)) {
                    Stack<IoEvent> stack = new Stack<>();
                    stackMap.put(file, stack);
                }
            });
        });
        // step 3. merge aggregable events.
        // debug
        int counter = 0;
        Set<IoEvent> toBeRemovedEventSet = new HashSet<>();
        for (IoEvent ioEvent : ioEventSet) {
            counter += 1;
            System.out.println(counter);
            String u;
            String v;
            if (ioEvent.getNames().compareTo("FileIoRead") == 0) {
                // fetch stack(file, thread)
                u = ioEvent.getPredicateObjectPath();
                v = ioEvent.getThreadId().toString();
            }
            else{ // FileIoWrite // fetch stack(thread, file)
                u = ioEvent.getThreadId().toString();
                v = ioEvent.getPredicateObjectPath();
            }
            Stack<IoEvent> stack = stackMaps.get(u).get(v);
            if (stack.empty()) {
                stack.push(ioEvent);
            }
            else {
                IoEvent priorEvent = stack.pop();
                if (forward_check(priorEvent, ioEvent, v) && backward_check(priorEvent, ioEvent, u)) {
                    // priorEvent may be different from event in ioEventSet?
                    priorEvent.setEndTimestampNanos(ioEvent.getEndTimestampNanos());
                    // tune attributes of priorEvent ?
                    toBeRemovedEventSet.add(ioEvent);
                    stack.push(priorEvent);
                }
                else {
                    stack.push(ioEvent);
                }
            }
        }
        System.out.println("Before remove we have:");
        System.out.println(ioEventSet.size());
        for (IoEvent toBeRemovedEvent : toBeRemovedEventSet) {
            ioEventSet.remove(toBeRemovedEvent);
        }
        System.out.println("After remove we have");
        System.out.println(ioEventSet.size());
    }
}