package com.btg.fondos.config;

import com.btg.fondos.model.Fund;
import com.btg.fondos.repository.FundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final FundRepository fundRepository;

    @Override
    public void run(String... args) {
        if (fundRepository.count() > 0) {
            log.info("Fondos ya existentes, omitiendo seed data");
            return;
        }

        List<Fund> funds = List.of(
                Fund.builder().id("1").name("FPV_BTG_PACTUAL_RECAUDADORA").minimumAmount(75_000).category("FPV").build(),
                Fund.builder().id("2").name("FPV_BTG_PACTUAL_ECOPETROL").minimumAmount(125_000).category("FPV").build(),
                Fund.builder().id("3").name("DEUDAPRIVADA").minimumAmount(50_000).category("FIC").build(),
                Fund.builder().id("4").name("FDO-ACCIONES").minimumAmount(250_000).category("FIC").build(),
                Fund.builder().id("5").name("FPV_BTG_PACTUAL_DINAMICA").minimumAmount(100_000).category("FPV").build()
        );

        fundRepository.saveAll(funds);
        log.info("Seed data: {} fondos cargados exitosamente", funds.size());
    }
}
