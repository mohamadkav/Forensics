package edu.nu.forensic.reducer;


import edu.nu.forensic.db.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class Reducer {
    @Autowired
    EventRepository eventRepository;

    public void reduce(){
    }
}