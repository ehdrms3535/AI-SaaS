package com.example.saas.api;

import com.example.saas.billing.PlanLimitService;
import com.example.saas.api.error.NotFoundException;
import com.example.saas.customer.dto.CreateCustomerRequest;
import com.example.saas.customer.dto.CustomerResponse;
import com.example.saas.customer.dto.UpdateCustomerRequest;
import com.example.saas.domain.Customer;
import com.example.saas.repo.CustomerRepository;
import com.example.saas.security.JwtPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customers;
    private final PlanLimitService planLimitService;

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@RequestBody @Valid CreateCustomerRequest req,
                                                   Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        planLimitService.assertCanCreateCustomer(principal.orgId());

        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setOrganizationId(principal.orgId());
        customer.setName(req.name());
        customer.setPhone(req.phone());
        customer.setEmail(req.email());
        customer.setMemo(req.memo());

        customers.save(customer);
        return ResponseEntity.status(201).body(CustomerResponse.from(customer));
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> list(@RequestParam(required = false) String q,
                                                       Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();

        List<Customer> found = (q == null || q.isBlank())
                ? customers.findByOrganizationIdOrderByCreatedAtDesc(principal.orgId())
                : customers.searchByOrganizationId(principal.orgId(), q.trim());

        return ResponseEntity.ok(found.stream().map(CustomerResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> get(@PathVariable UUID id, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();

        Customer customer = customers.findByIdAndOrganizationId(id, principal.orgId())
                .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "고객을 찾을 수 없습니다."));

        return ResponseEntity.ok(CustomerResponse.from(customer));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CustomerResponse> update(@PathVariable UUID id,
                                                   @RequestBody @Valid UpdateCustomerRequest req,
                                                   Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();

        Customer customer = customers.findByIdAndOrganizationId(id, principal.orgId())
                .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "고객을 찾을 수 없습니다."));

        if (req.name() != null) {
            customer.setName(req.name());
        }
        if (req.phone() != null) {
            customer.setPhone(req.phone());
        }
        if (req.email() != null) {
            customer.setEmail(req.email());
        }
        if (req.memo() != null) {
            customer.setMemo(req.memo());
        }

        customers.save(customer);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }
}
