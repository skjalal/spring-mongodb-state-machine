package com.example.config;

import com.example.domain.PaymentEvent;
import com.example.domain.PaymentState;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.data.mongodb.MongoDbPersistingStateMachineInterceptor;
import org.springframework.statemachine.data.mongodb.MongoDbStateMachineRepository;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.service.DefaultStateMachineService;
import org.springframework.statemachine.service.StateMachineService;

import java.util.EnumSet;

@Configuration
@RequiredArgsConstructor
@EnableStateMachineFactory
public class StateMachineConfig extends
        EnumStateMachineConfigurerAdapter<PaymentState, PaymentEvent> {

    private final ApplicationContext applicationContext;

    @Bean
    public StateMachineRuntimePersister<PaymentState, PaymentEvent, String> stateMachineRuntimePersister(MongoDbStateMachineRepository jpaStateMachineRepository) {
        return new MongoDbPersistingStateMachineInterceptor<>(jpaStateMachineRepository);
    }

    @Override
    public void configure(StateMachineStateConfigurer<PaymentState, PaymentEvent> states)
            throws Exception {
        states.withStates().initial(PaymentState.NEW).states(EnumSet.allOf(PaymentState.class))
                .end(PaymentState.AUTH).end(PaymentState.PRE_AUTH_ERROR).end(PaymentState.AUTH_ERROR);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<PaymentState, PaymentEvent> transitions)
            throws Exception {
        transitions.withExternal().source(PaymentState.NEW).target(PaymentState.NEW)
                .event(PaymentEvent.PRE_AUTHORIZE).and()
                .withExternal().source(PaymentState.NEW).target(PaymentState.PRE_AUTH)
                .event(PaymentEvent.PRE_AUTH_APPROVED).and().withExternal()
                .source(PaymentState.NEW).target(PaymentState.PRE_AUTH_ERROR)
                .event(PaymentEvent.PRE_AUTH_DECLINED)
                //preauth to auth
                .and().withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.PRE_AUTH)
                .event(PaymentEvent.AUTHORIZE).and().withExternal()
                .source(PaymentState.PRE_AUTH).target(PaymentState.AUTH).event(PaymentEvent.AUTH_APPROVED)
                .and().withExternal().source(PaymentState.PRE_AUTH)
                .target(PaymentState.AUTH_ERROR).event(PaymentEvent.AUTH_DECLINED);
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public void configure(StateMachineConfigurationConfigurer<PaymentState, PaymentEvent> config)
            throws Exception {
        StateMachineRuntimePersister<PaymentState, PaymentEvent, String> runtimePersister =
                (StateMachineRuntimePersister<PaymentState, PaymentEvent, String>) applicationContext.getBean(StateMachineRuntimePersister.class);
        config.withPersistence().runtimePersister(runtimePersister);
    }

    @Bean
    public StateMachineService<PaymentState, PaymentEvent> stateMachineService(
            StateMachineFactory<PaymentState, PaymentEvent> stateMachineFactory,
            StateMachinePersist<PaymentState, PaymentEvent, String> stateMachinePersist) {
        return new DefaultStateMachineService<>(stateMachineFactory, stateMachinePersist);
    }
}
