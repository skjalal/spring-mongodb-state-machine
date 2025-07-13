package com.example.repository;

import com.example.domain.Payment;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, UUID> {

}
