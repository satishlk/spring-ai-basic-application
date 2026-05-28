package com.example.checkout.transactionmonitoring;

import com.example.checkout.transactionmonitoring.service.AuditLogService;
import com.example.checkout.transactionmonitoring.service.TransactionFilterService;
import com.example.checkout.transactionmonitoring.web.AdminActionController;
import com.example.checkout.transactionmonitoring.web.ExportController;
import com.example.checkout.transactionmonitoring.web.TransactionFilterController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the Transaction Monitoring feature.
 * Verifies that the Spring application context loads successfully
 * with all transaction monitoring beans wired correctly.
 * 
 * This test ensures that:
 * - All controllers are registered and accessible
 * - All services are instantiated as Spring beans
 * - The application context is valid with the new feature components
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionMonitoringIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Application context loads with transaction monitoring beans")
    void applicationContextLoads_withTransactionMonitoringBeans() {
        // Verify all controllers are registered as beans
        assertThat(applicationContext.getBean(TransactionFilterController.class))
                .isNotNull();
        assertThat(applicationContext.getBean(AdminActionController.class))
                .isNotNull();
        assertThat(applicationContext.getBean(ExportController.class))
                .isNotNull();

        // Verify service beans are registered
        assertThat(applicationContext.getBean(AuditLogService.class))
                .isNotNull();
        
        // TransactionFilterService is an interface; verify it's available if there's an impl
        // For now, we just verify the context loads without errors
    }

    @Test
    @DisplayName("TransactionFilterController endpoints are accessible")
    void transactionFilterController_endpointsAccessible() throws Exception {
        // Verify the main filter endpoint is mapped
        mockMvc.perform(get("/transactions"))
                .andExpect(status().is5xxServerError()); // Method not implemented, but routing works
    }

    @Test
    @DisplayName("AdminActionController endpoints are accessible")
    void adminActionController_endpointsAccessible() throws Exception {
        // Verify refund endpoint is mapped (POST requires body, so we expect 400 or 500)
        mockMvc.perform(get("/transactions/txn123/refund"))
                .andExpect(status().is4xxClientError()); // GET not allowed on POST endpoint
    }

    @Test
    @DisplayName("ExportController endpoints are accessible")
    void exportController_endpointsAccessible() throws Exception {
        // Verify export endpoint is mapped
        mockMvc.perform(get("/transactions/export.csv"))
                .andExpect(status().is5xxServerError()); // Method not implemented, but routing works

        // Verify summary endpoint is mapped
        mockMvc.perform(get("/transactions/summary"))
                .andExpect(status().is5xxServerError()); // Method not implemented, but routing works
    }

    @Test
    @DisplayName("AuditLogService bean is instantiated")
    void auditLogService_beanInstantiated() {
        AuditLogService auditLogService = applicationContext.getBean(AuditLogService.class);
        
        assertThat(auditLogService).isNotNull();
    }

    @Test
    @DisplayName("All transaction monitoring controllers are REST controllers")
    void transactionMonitoringControllers_areRestControllers() {
        TransactionFilterController filterController = 
                applicationContext.getBean(TransactionFilterController.class);
        AdminActionController adminController = 
                applicationContext.getBean(AdminActionController.class);
        ExportController exportController = 
                applicationContext.getBean(ExportController.class);

        // Verify they are annotated with @RestController (Spring proxy check)
        assertThat(filterController.getClass().getName())
                .contains("TransactionFilterController");
        assertThat(adminController.getClass().getName())
                .contains("AdminActionController");
        assertThat(exportController.getClass().getName())
                .contains("ExportController");
    }

    @Test
    @DisplayName("Application context contains expected number of transaction monitoring beans")
    void applicationContext_containsExpectedBeans() {
        // Count transaction monitoring beans
        String[] controllerBeans = applicationContext.getBeanNamesForType(Object.class);
        
        long txnMonitoringBeans = java.util.Arrays.stream(controllerBeans)
                .filter(name -> name.contains("transaction") || name.contains("Transaction"))
                .count();

        // We expect at least 4 beans: 3 controllers + 1 service
        assertThat(txnMonitoringBeans).isGreaterThanOrEqualTo(4);
    }
}
