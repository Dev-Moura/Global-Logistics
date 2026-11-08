package com.michael.global_logistics_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.michael.global_logistics_api.identity.domain.User;
import com.michael.global_logistics_api.identity.domain.UserRole;
import com.michael.global_logistics_api.identity.infrastructure.UserRepository;
import com.michael.global_logistics_api.organization.domain.Organization;
import com.michael.global_logistics_api.organization.domain.OrganizationStatus;
import com.michael.global_logistics_api.organization.infrastructure.OrganizationRepository;
import com.michael.global_logistics_api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RbacIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void operatorCannotUpdateOrganizationOrCreateOrganization() throws Exception {
        Organization organization = createOrganization();
        String email = uniqueEmail("operator");
        createUser(organization, UserRole.LOGISTICS_OPERATOR, email);
        String accessToken = login(email);

        mockMvc.perform(patch("/api/v1/organizations/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Forbidden Rename\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Forbidden Org\",\"countryCode\":\"BR\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void operatorCanListOwnOrganizationUsers() throws Exception {
        Organization organization = createOrganization();
        String email = uniqueEmail("operator-list");
        createUser(organization, UserRole.LOGISTICS_OPERATOR, email);
        String accessToken = login(email);

        mockMvc.perform(get("/api/v1/organizations/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(email));
    }

    @Test
    void managerCanUpdateOrganization() throws Exception {
        Organization organization = createOrganization();
        String email = uniqueEmail("manager");
        createUser(organization, UserRole.LOGISTICS_MANAGER, email);
        String accessToken = login(email);

        mockMvc.perform(patch("/api/v1/organizations/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed Logistics\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Logistics"));
    }

    private Organization createOrganization() {
        Organization organization = new Organization();
        organization.setName("RBAC Logistics");
        organization.setCountryCode("US");
        organization.setTaxIdentifier("RBAC-" + UUID.randomUUID());
        organization.setStatus(OrganizationStatus.ACTIVE);
        return organizationRepository.save(organization);
    }

    private User createUser(Organization organization, UserRole role, String email) {
        User user = new User();
        user.setOrganization(organization);
        user.setFullName(role.name());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("StrongPass123!"));
        user.setRole(role);
        user.setActive(true);
        return userRepository.save(user);
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"StrongPass123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
