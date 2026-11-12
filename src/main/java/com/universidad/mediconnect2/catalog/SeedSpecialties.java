package com.universidad.mediconnect2.catalog;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev","docker","prod"})
public class SeedSpecialties implements ApplicationRunner {
    private final SpecialtyRepository specialties;
    public SeedSpecialties(SpecialtyRepository specialties) { this.specialties=specialties; }
    @Override
    public void run(ApplicationArguments args) {
        for (String name : new String[]{"Medicina general", "Pediatria", "Cardiologia"}) {
            if (!specialties.existsByNameIgnoreCase(name)) specialties.save(new Specialty(name));
        }
    }
}
