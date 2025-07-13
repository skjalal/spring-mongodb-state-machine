package com.example;

import com.example.domain.Payment;
import com.example.domain.PaymentEvent;
import com.example.domain.PaymentState;
import com.example.repository.PaymentRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
class SpringStateMachineApplicationTests {

  @Container
  static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0")
          .withNetworkAliases("test-mongodb")
          .withExposedPorts(27017);

  @Autowired
  StateMachineService<PaymentState, PaymentEvent> stateMachineService;

  @Autowired
  PaymentRepository paymentRepository;

  @DynamicPropertySource
  static void containersProperties(DynamicPropertyRegistry registry) {
    mongoDBContainer.start();
    registry.add("spring.data.mongodb.uri", SpringStateMachineApplicationTests::getDbUrl);
  }

  static String getDbUrl() {
    return mongoDBContainer.getReplicaSetUrl("payment-state-machine");
  }

  @Test
  void contextLoads() {
    Payment payment = Payment.builder().id(UUID.randomUUID()).state("NEW").amount(45.25).build();
    paymentRepository.save(payment);

    StateMachine<PaymentState, PaymentEvent> sm = stateMachineService.acquireStateMachine(payment.getId().toString());

    sm.sendEvent(Mono.just(MessageBuilder.withPayload(PaymentEvent.PRE_AUTHORIZE).build()))
                    .subscribe(result -> System.out.println(result.getResultType()));

    payment.setState(sm.getState().getId().toString());
    paymentRepository.save(payment);

    sm.sendEvent(Mono.just(MessageBuilder.withPayload(PaymentEvent.PRE_AUTH_APPROVED).build()))
            .subscribe(result -> System.out.println(result.getResultType()));

    payment.setState(sm.getState().getId().toString());
    paymentRepository.save(payment);

    sm.sendEvent(Mono.just(MessageBuilder.withPayload(PaymentEvent.AUTHORIZE).build()))
            .subscribe(result -> System.out.println(result.getResultType()));

    payment.setState(sm.getState().getId().toString());
    paymentRepository.save(payment);

    sm.sendEvent(Mono.just(MessageBuilder.withPayload(PaymentEvent.AUTH_APPROVED).build()))
            .subscribe(result -> System.out.println(result.getResultType()));

    payment.setState(sm.getState().getId().toString());
    paymentRepository.save(payment);

    System.out.println("**** Final state: " + payment.getState() + " *****");
    assertEquals(PaymentState.AUTH, PaymentState.valueOf(payment.getState()));

    stateMachineService.releaseStateMachine(payment.getId().toString());
  }


  @AfterAll
  static void afterAll() {
    mongoDBContainer.stop();
  }
}
