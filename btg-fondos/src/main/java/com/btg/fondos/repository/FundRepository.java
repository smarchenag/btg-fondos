package com.btg.fondos.repository;

import com.btg.fondos.model.Fund;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FundRepository extends MongoRepository<Fund, String> {
}
