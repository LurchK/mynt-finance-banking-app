package com.mynt.banking.mPesa.flutterwave;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mynt.banking.currency_cloud.CurrencyCloudEntity;
import com.mynt.banking.currency_cloud.CurrencyCloudRepository;
import com.mynt.banking.currency_cloud.collect.demo.DemoService;
import com.mynt.banking.currency_cloud.collect.demo.requests.DemoFundingDto;
import com.mynt.banking.currency_cloud.collect.funding.FundingService;
import com.mynt.banking.currency_cloud.collect.funding.requests.FindAccountDetails;
import com.mynt.banking.currency_cloud.manage.authenticate.AuthenticationService;
import com.mynt.banking.mPesa.flutterwave.requests.MPesaToCurrencyCloudDto;
import com.mynt.banking.mPesa.flutterwave.requests.MPesaToFlutterWearDto;
import com.mynt.banking.mPesa.flutterwave.requests.SendMpesaDto;
import com.mynt.banking.mPesa.flutterwave.requests.Wallet2WalletDto;
import com.mynt.banking.user.User;
import com.mynt.banking.user.UserContextService;
import com.mynt.banking.user.UserRepository;
import com.mynt.banking.util.HashMapToQuiryPrams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FlutterwaveService {

    private final FlutterwaveWebClientConfig webClient;

    private final FundingService fundingService;

    private final DemoService demoService;

    @Value("${flutterwave.api.secretKey}")
    private String secretKey;

    private final UserRepository userRepository;

    private final CurrencyCloudRepository currencyCloudRepository;

    public Mono<ResponseEntity<JsonNode>> mPesaToFlutterwave(MPesaToFlutterWearDto mPesaToFlutterWearDto) {

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode jsonNode = mapper.valueToTree(mPesaToFlutterWearDto);

        jsonNode.put("tx_ref", jsonNode.get("email").toString()+LocalDateTime.now().toString());

        return webClient.webClientFW()
                .post()
                .uri("/v3/charges?type=mpesa")
                .header("Authorization", secretKey)
                .bodyValue(jsonNode)
                .exchangeToMono(response -> response.toEntity(JsonNode.class))
                .flatMap(request -> { return Mono.just(request); });
    }

    public Mono<ResponseEntity<JsonNode>> wallet2Wallet(Wallet2WalletDto dto) {

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode jsonNode = mapper.createObjectNode();
        jsonNode.put("amount", dto.getAmount());
        jsonNode.put("account_bank","flutterwave");
        jsonNode.put("account_number","200527841");
        jsonNode.put("currency","KES");
        jsonNode.put("debit_currency","NGN");

        String tx_ref = dto.getEmail().toString()+LocalDateTime.now().toString();
        jsonNode.put("tx_ref", tx_ref);

        return webClient.webClientFW()
                .post()
                .uri("/v3/transfers")
                .header("Authorization", secretKey)
                .bodyValue(jsonNode)
                .exchangeToMono(response -> response.toEntity(JsonNode.class))
                .flatMap(request -> { return Mono.just(request); });
    }

    public Mono<ResponseEntity<JsonNode>> sendMPesa(SendMpesaDto dto) {

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode meta = mapper.createObjectNode();
        meta.put("sender", dto.getSender());
        meta.put("sender_country", dto.getSenderCountry());
        meta.put("mobile_number", dto.getMobileNumber());

        ObjectNode jsonNode = mapper.createObjectNode();
        jsonNode.put("amount", dto.getAmount());
        jsonNode.put("account_bank","MPS");
        jsonNode.put("account_number","2540700000000");
        jsonNode.put("currency","KES");
        jsonNode.put("beneficiary_name", dto.getBeneficiaryName());
        jsonNode.put("debit_currency",dto.getDebitCurrency());
        jsonNode.set("meta",meta);

        String tx_ref = this.genTx_ref(dto.getEmail());
        jsonNode.put("reference", tx_ref);

        return webClient.webClientFW()
                .post()
                .uri("/v3/transfers")
                .header("Authorization", secretKey)
                .bodyValue(jsonNode)
                .exchangeToMono(response -> response.toEntity(JsonNode.class))
                .flatMap(request -> { return Mono.just(request); });
    }

    public String genTx_ref(String email){
        return email
                .replace(".", "_")
                .replace("@","_")
                +LocalDateTime.now().toString()
                .replace(":","_")
                .replace(".","_");
    }

    public Mono<ResponseEntity<JsonNode>> depoistTransactionCheck(String id) {

        String url = "/v3/transactions/"+id+"/verify";
        return webClient.webClientFW()
                .get()
                .uri(url)
                .header("Authorization", secretKey)
                .exchangeToMono(response -> response.toEntity(JsonNode.class))
                .flatMap(request -> { return Mono.just(request); });

    }

    public Mono<ResponseEntity<JsonNode>> transactionCheck(String id) {

        String url = "/v3/transfers/"+id;
        return webClient.webClientFW()
                .get()
                .uri(url)
                .header("Authorization", secretKey)
                .exchangeToMono(response -> response.toEntity(JsonNode.class))
                .flatMap(request -> { return Mono.just(request); });

    }

    //TODO: Intergrate - mpesa to CC including CC methrods
    public ResponseEntity<JsonNode> mpesaToCloudCurrency(MPesaToCurrencyCloudDto dto,
                                                        String email
                                                        ) {

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode finalResponse = mapper.createObjectNode();

        Optional<User> user = userRepository.findByEmail(email);

        if(!user.isPresent()) {return null;}

        User userExsists = user.get();

        // mPesaToFlutterwave()
        ResponseEntity<JsonNode> response = mPesaToFlutterwaveCall(dto, email, userExsists);
        if(!response.getStatusCode().is2xxSuccessful()) { return response; }

        // depoistTransactionCheck()
        response = depoistTransactionCheckCall(response.getBody());
        if(!response.getStatusCode().is2xxSuccessful()) { return response; }

        // cc get account details end point
        response = ccFundAccountDetails(userExsists);
        if(!response.getStatusCode().is2xxSuccessful()) { return response; }

        //cc demo fund account
        response =  demoFundAccount(userExsists, response.getBody(),dto);
        if(!response.getStatusCode().is2xxSuccessful()) { return response; }
        //TODO: create cutome responce

        return response;
    }

    private ResponseEntity<JsonNode> mPesaToFlutterwaveCall(MPesaToCurrencyCloudDto dto, String email, User userExsists){
        MPesaToFlutterWearDto mPesaToFlutterWearDto = MPesaToFlutterWearDto.builder()
                .amount(dto.getAmount())
                .email(userExsists.getEmail())
                .phone_number(userExsists.getPhone_number())
                .fullname(userExsists.getFirstname()+" "+userExsists.getLastname())
                .build();
        ResponseEntity<JsonNode> response = this.mPesaToFlutterwave(mPesaToFlutterWearDto).block();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode errorResponse = mapper.createObjectNode();

        if(!response.getStatusCode().is2xxSuccessful()) {
            errorResponse.put("Error", " with mPesaToFlutterwave()");
            return ResponseEntity.status(400).body(errorResponse);
        }

        return response;
    }

    private ResponseEntity<JsonNode> depoistTransactionCheckCall(JsonNode response){


        ResponseEntity<JsonNode> response1 =  this.depoistTransactionCheck(response
                        .get("data")
                        .get("id")
                        .asText())
                        .block();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode errorResponse = mapper.createObjectNode();

        if(!response1.getStatusCode().is2xxSuccessful()) {
            errorResponse.put("Error", " with depoistTransactionCheck()");
            return ResponseEntity.status(400).body(errorResponse);
        } else if (!(Objects.equals(response1.getBody().get("status").asText(), "success"))) {
            errorResponse.put("Error", " depoistTransactionCheck() is not sucessfull");
            return ResponseEntity.status(400).body(errorResponse);
        }

        return response1;

    }

    private ResponseEntity<JsonNode> ccFundAccountDetails(User user){

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode errorResponse = mapper.createObjectNode();

        // get UUID
        Optional<CurrencyCloudEntity> currencyCloudData = currencyCloudRepository.findByUser(user);
        String uuid = "";
        if(currencyCloudData.isPresent()) {
            uuid = currencyCloudData.get().getUuid();
        }

        FindAccountDetails data = FindAccountDetails.builder()
                .currency("KES")
                .onBehalfOf(uuid)
                .build();

        ResponseEntity<JsonNode> response = fundingService.find(data).block();

        // check for more then one account if so fail the test
        if(!Objects.equals(response.getBody().get("pagination").get("total_entries").toString(), "1")) {
            errorResponse.put("Error", " with ccFundAccountDetails()");
            return ResponseEntity.status(400).body(errorResponse);
        }

        if(!response.getStatusCode().is2xxSuccessful()) {
            errorResponse.put("Error", " with ccFundAccountDetails()");
            return ResponseEntity.status(400).body(errorResponse);
        }

        return response;
    }

    private ResponseEntity<JsonNode> demoFundAccount(User user, JsonNode ccFundAccountDetails, MPesaToCurrencyCloudDto dto){

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode errorResponse = mapper.createObjectNode();

        // get UUID
        Optional<CurrencyCloudEntity> currencyCloudData = currencyCloudRepository.findByUser(user);
        String uuid = "";
        if(currencyCloudData.isPresent()) {
            uuid = currencyCloudData.get().getUuid();
        }

        DemoFundingDto demoFundingDto = DemoFundingDto.builder()
                .id(ccFundAccountDetails.get("funding_accounts").get(0).get("id").asText())
                .receiverAccountNumber(ccFundAccountDetails.get("funding_accounts").get(0).get("account_number").asText())
                .currency("KES")
                .amount(Integer.valueOf(dto.getAmount()))
                .onBehalfOf(uuid)
                .build();
        ResponseEntity<JsonNode> response =  demoService.create(demoFundingDto).block();

        if(!response.getStatusCode().is2xxSuccessful()) {
            errorResponse.put("Error", " with ccFundAccountDetails()");
            return ResponseEntity.status(400).body(errorResponse);
        }

        return response;

    }

}
