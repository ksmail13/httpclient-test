package io.github.ksmail13.echoserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.time.Duration;

@SpringBootApplication
public class EchoserverApplication {

    public static void main(String[] args) {
        SpringApplication.run(EchoserverApplication.class, args);
    }

    @Bean
    public RouterFunction<ServerResponse> router() {
        return RouterFunctions.route()
                .GET("/",
                        request -> ServerResponse.ok()
                                .body(BodyInserters.fromValue("test"))
                                .delayElement(Duration.ofMillis(500)))
                .build();
    }
}
