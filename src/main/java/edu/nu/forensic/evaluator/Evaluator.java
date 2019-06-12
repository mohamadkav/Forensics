package edu.nu.forensic.evaluator;

import edu.nu.forensic.db.entity.IoEvent;
import edu.nu.forensic.db.entity.IoEventAfterCPR;
import edu.nu.forensic.db.repository.IoEventAfterCPRRepository;
import edu.nu.forensic.db.repository.IoEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Evaluator {

    @Autowired
    IoEventRepository ioEventRepository;

    @Autowired
    IoEventAfterCPRRepository ioEventAfterCPRRepository;

    public void evaluate(){
        // Hardcode: edit to change evaluated event
        String names = "FileIoWrite";
        String predicate_object_path = "C:\\Users\\admin\\Desktop\\doit.exe";
        Integer thread_id = 2128;
        Long start_timestamp_nanos = 1542160605119618500l;
        Long end_timestamp_nanos = 1542160605119618500l;
        // Debug info
        System.out.println("Now evaluating event: "+"event_type: "+names+", thread_id: "+thread_id+", file_path: "+predicate_object_path+", time_window: ["+start_timestamp_nanos+", "+end_timestamp_nanos+"]");
        // Check whether forward dependent events and backward dependent events have been preserved
        if (names.equals("FileIoWrite")) {
            System.out.println("Before CPR:");
            Long countBackward = ioEventRepository.countBackwardDependentReadEvents(thread_id, end_timestamp_nanos);
            System.out.println("countBackwardDependentReadEvents = "+countBackward);
            Long countForward = ioEventRepository.countForwardDependentReadEvents(predicate_object_path, start_timestamp_nanos);
            System.out.println("countForwardDependentReadEvents = "+countForward);
            System.out.println("After CPR:");
            countBackward = ioEventAfterCPRRepository.countBackwardDependentReadEvents(thread_id, end_timestamp_nanos);
            System.out.println("countBackwardDependentReadEvents = "+countBackward);
            countForward = ioEventAfterCPRRepository.countForwardDependentReadEvents(predicate_object_path, start_timestamp_nanos);
            System.out.println("countForwardDependentReadEvents = "+countForward);
        }
        else if (names.equals("FileIoRead")) {
            System.out.println("Before CPR:");
            Long countBackward = ioEventRepository.countBackwardDependentWriteEvents(predicate_object_path, end_timestamp_nanos);
            System.out.println("countBackwardDependentWriteEvents = "+countBackward);
            Long countForward = ioEventRepository.countForwardDependentWriteEvents(thread_id, start_timestamp_nanos);
            System.out.println("countForwardDependentWriteEvents = "+countForward);
            System.out.println("After CPR:");
            countBackward = ioEventAfterCPRRepository.countBackwardDependentWriteEvents(predicate_object_path, end_timestamp_nanos);
            System.out.println("countBackwardDependentWriteEvents = "+countBackward);
            countForward = ioEventAfterCPRRepository.countForwardDependentWriteEvents(thread_id, start_timestamp_nanos);
            System.out.println("countForwardDependentWriteEvents = "+countForward);
        }
    }

}
