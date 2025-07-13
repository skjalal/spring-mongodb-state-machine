package com.example;

import com.example.domain.Payment;
import com.example.domain.PaymentEvent;
import com.example.domain.PaymentState;
import com.example.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class SpringStateMachineApplicationTests {

  @Autowired
  StateMachineService<PaymentState, PaymentEvent> stateMachineService;

  @Autowired
  PaymentRepository paymentRepository;

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

}
