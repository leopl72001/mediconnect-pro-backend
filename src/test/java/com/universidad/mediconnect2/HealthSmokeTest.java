package com.universidad.mediconnect2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.profiles.active=dev"})
@AutoConfigureMockMvc
class HealthSmokeTest {
    @Autowired MockMvc mvc;
    @Test void healthIsUpWithoutDocker() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
    @Test void specialtiesArePublic() throws Exception {
        mvc.perform(get("/api/v1/specialties"))
                .andExpect(status().isOk());
    }
    @Test void appointmentsRequireToken() throws Exception {
        mvc.perform(get("/api/v1/appointments/me"))
                .andExpect(status().isUnauthorized());
    }
}
