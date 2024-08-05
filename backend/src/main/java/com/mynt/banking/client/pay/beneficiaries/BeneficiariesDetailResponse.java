package com.mynt.banking.client.pay.beneficiaries;

import java.time.ZonedDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BeneficiariesDetailResponse {
    private List<Beneficiary> beneficiaries;
    private Pagination pagination;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Beneficiary {
        private String id;
        private String name;
        @JsonProperty("bank_account_holder_name")
        private String bankAccountHolderName;
        @JsonProperty("beneficiary_address")
        private List<String> beneficiaryAddress;
        @JsonProperty("beneficiary_country")
        private String beneficiaryCountry;
        @JsonProperty("beneficiary_city")
        private String beneficiaryCity;
        @JsonProperty("beneficiary_postcode")
        private String beneficiaryPostcode;
        @JsonProperty("bank_name")
        private String bankName;
        private String currency;
        private String iban;
        @JsonProperty("bicSwift")
        private String bic_swift;
        @JsonProperty("default_beneficiary")
        private Boolean defaultBeneficiary;
        @JsonProperty("created_at")
        private ZonedDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pagination {
        @JsonProperty("total_entries")
        private int totalEntries;
        @JsonProperty("total_pages")
        private int totalPages;
        @JsonProperty("current_page")
        private int currentPage;
        @JsonProperty("per_page")
        private int perPage;
        @JsonProperty("previous_page")
        private int previousPage;
        @JsonProperty("next_page")
        private int nextPage;
        private String order;
        @JsonProperty("order_asc_desc")
        private String orderAscDesc;
    }
}
