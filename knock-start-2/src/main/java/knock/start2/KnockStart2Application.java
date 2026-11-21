package knock.start2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KnockStart2Application {

	public static void main(String[] args) {
		SpringApplication.run(KnockStart2Application.class, args);
	}

}
