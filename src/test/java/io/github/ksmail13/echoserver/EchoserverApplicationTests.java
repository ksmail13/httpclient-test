package io.github.ksmail13.echoserver;

import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EchoserverApplicationTests {
	private final static Logger log = LoggerFactory.getLogger(EchoserverApplicationTests.class);
	public static final int CNT = 10000;

	@Test
	@DisplayName("Test with apache http components")
	void syncTest() throws ExecutionException, InterruptedException {
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
		RestTemplate restTemplate = new RestTemplate(factory);
		ExecutorService executorService = Executors.newFixedThreadPool(100);

		long start = new Date().getTime();
		List<Future<String>> futures = new ArrayList<>();

		for (int i = 0; i< CNT; i++) {
			futures.add(executorService.submit(() -> restTemplate
					.exchange("http://localhost:8000/", HttpMethod.GET, HttpEntity.EMPTY, String.class).getBody()));

		}

		log.info("after trigger {} millis", new Date().getTime() - start);
		for (Future<String> future : futures) {
			assertThat(future.get()).isEqualTo("test");
		}
		log.info("result {} millis", new Date().getTime() - start);
	}

	@Test
	@DisplayName("Test with AsyncRestTemplate divide request thread")
	void asyncTest1() throws ExecutionException, InterruptedException {
		Netty4ClientHttpRequestFactory factory = new Netty4ClientHttpRequestFactory();
		AsyncRestTemplate restTemplate = new AsyncRestTemplate(factory);

		ExecutorService executorService = Executors.newFixedThreadPool(100);
		long start = new Date().getTime();
		List<Future<Future<ResponseEntity<String>>>> futures = new ArrayList<>();
		List<Future<ResponseEntity<String>>> resultFutures = new ArrayList<>();

		for (int i = 0; i< CNT; i++) {
			futures.add(executorService.submit(() -> restTemplate
					.exchange("http://localhost:8000/", HttpMethod.GET, HttpEntity.EMPTY, String.class)));
		}

		log.info("after job trigger {} millis", new Date().getTime() - start);

		for (Future<Future<ResponseEntity<String>>> future : futures) {
			resultFutures.add(future.get());
		}

		log.info("after trigger {} millis", new Date().getTime() - start);

		for (Future<ResponseEntity<String>> resultFuture : resultFutures) {
			assertThat(resultFuture.get().getBody()).isEqualTo("test");
		}
		log.info("result {} millis", new Date().getTime() - start);
	}

	@Test
	@DisplayName("Test with AsyncRestTemplate")
	void asyncTest2() throws ExecutionException, InterruptedException {
		Netty4ClientHttpRequestFactory factory = new Netty4ClientHttpRequestFactory();
		AsyncRestTemplate restTemplate = new AsyncRestTemplate(factory);
		CountDownLatch latch = new CountDownLatch(CNT);
		long start = new Date().getTime();
		List<ListenableFuture<ResponseEntity<String>>> futures = new ArrayList<>();

		for (int i = 0; i< CNT; i++) {
			futures.add(restTemplate
					.exchange("http://localhost:8000/", HttpMethod.GET, HttpEntity.EMPTY, String.class));
		}

		log.info("after trigger {} millis", new Date().getTime() - start);

		for (ListenableFuture<ResponseEntity<String>> future : futures) {
			future.completable().join();
		}

		log.info("result {} millis", new Date().getTime() - start);
	}

	@Test
	@DisplayName("Test with WebClient")
	void asyncTest3() {
		WebClient webClient = WebClient.create();
		long start = new Date().getTime();

		Scheduler scheduler = Schedulers.newParallel("test", 100, true);
		Flux<String> stringFlux = Flux.fromStream(Stream.generate(() -> "http://localhost:8000"))
				.take(CNT)
				.publishOn(scheduler)
				.flatMap(url -> webClient.get().uri(url).exchange().subscribeOn(scheduler))
				.flatMap(res -> res.bodyToMono(String.class));

		log.info("after trigger {} millis", new Date().getTime() - start);

		stringFlux
				.then().block();
		log.info("result {} millis", new Date().getTime() - start);
	}
}
