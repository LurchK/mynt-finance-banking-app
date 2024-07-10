package com.mynt.banking.currency_cloud.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.mynt.banking.currency_cloud.dto.CreateAccountRequest;
import com.mynt.banking.currency_cloud.dto.CreateAccountResponse;
import com.mynt.banking.currency_cloud.dto.FindAccountRequest;
import com.mynt.banking.currency_cloud.dto.FindAccountResponse;
import com.mynt.banking.currency_cloud.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/currency-cloud/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/create")
    public Mono<ResponseEntity<JsonNode>> createAccount(@RequestBody CreateAccountRequest request) {
        // TODO: add account id to contact
        return accountService.createAccount(request);
    }

    @PostMapping("/find")
    public FindAccountResponse findAccount(@RequestBody FindAccountRequest request) {
        FindAccountResponse response = accountService.findAccount(request);
        return response;
    }
}
