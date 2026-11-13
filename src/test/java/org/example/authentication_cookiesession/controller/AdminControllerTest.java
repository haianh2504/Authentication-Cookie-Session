package org.example.authentication_cookiesession.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.authentication_cookiesession.service.AdminSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminSessionService adminSessionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminController(adminSessionService))
                .build();
    }

    @Test
    void returnsLogoutSuccessEnvelope() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/admin/sessions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("ADMIN_LOGOUT_SUCCESSFULLY"))
                .andExpect(jsonPath("$.data.authenticated").value(false));

        verify(adminSessionService).logout(any(HttpServletRequest.class));
    }
}
