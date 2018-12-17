package edu.nu.forensic;

import edu.nu.forensic.db.entity.Customer;
import edu.nu.forensic.db.repository.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Main {


    public static void main(String[] args) {
        SpringApplication.run(Main.class,args);
    }

/*    @Bean
    public CommandLineRunner demo(CustomerRepository repository) {
        return (args) -> {
            // save a couple of customers
        //    repository.save(new Customer("Jack", "Bauer"));
            for (Customer customer : repository.findAll()) {
                System.out.println(customer.toString());
            }
        };
    }*/

}