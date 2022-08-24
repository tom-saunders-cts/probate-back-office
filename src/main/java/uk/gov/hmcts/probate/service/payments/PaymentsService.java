package uk.gov.hmcts.probate.service.payments;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.probate.exception.BusinessValidationException;
import uk.gov.hmcts.probate.model.ccd.CcdCaseType;
import uk.gov.hmcts.probate.model.ccd.EventId;
import uk.gov.hmcts.probate.model.payments.CreditAccountPayment;
import uk.gov.hmcts.probate.model.payments.PaymentResponse;
import uk.gov.hmcts.probate.model.payments.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.probate.model.payments.servicerequest.ServiceRequestUpdateResponseDto;
import uk.gov.hmcts.probate.security.SecurityDTO;
import uk.gov.hmcts.probate.security.SecurityUtils;
import uk.gov.hmcts.probate.service.BusinessValidationMessageRetriever;
import uk.gov.hmcts.probate.service.IdamApi;
import uk.gov.hmcts.probate.service.ccd.CcdClientApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.probate.model.PaymentStatus;
import uk.gov.hmcts.reform.probate.model.cases.CaseData;
import uk.gov.hmcts.reform.probate.model.cases.CasePayment;
import uk.gov.hmcts.reform.probate.model.cases.CollectionMember;
import uk.gov.hmcts.reform.probate.model.cases.caveat.CaveatData;
import uk.gov.hmcts.reform.probate.model.cases.grantofrepresentation.GrantOfRepresentationData;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Locale.UK;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentsService {

    private static final String SERVICE_REQUEST_REFERENCE_KEY = "service_request_reference";
    private static final String PAYMENT_ERROR_404 = "Account information could not be found";
    private static final String PAYMENT_ERROR_422 = "Invalid or missing attribute";
    private static final String PAYMENT_ERROR_400 = "Payment Failed";
    private static final String PAYMENT_ERROR_5XX = "Unable to retrieve account information, please try again later";
    private static final String PAYMENT_ERROR_OTHER = "Unexpected Exception";
    private static final String DUPLICANT_PAYMENT_ERROR_KEY = "duplicate payment";
    private final RestTemplate restTemplate;
    private final AuthTokenGenerator authTokenGenerator;
    private final BusinessValidationMessageRetriever businessValidationMessageRetriever;
    private final ServiceRequestClient serviceRequestClient;
    private final SecurityUtils securityUtils;
    private final CcdClientApi ccdClientApi;
    private final IdamApi idamApi;

    @Value("${payment.url}")
    private String payUri;
    @Value("${payment.api}")
    private String payApi;
    @Value("${payment.pba.siteId}")
    private String siteId;

    public String createServiceRequest(ServiceRequestDto serviceRequestDto) {
        SecurityDTO securityDTO = securityUtils.getSecurityDTO();
        String serviceRequestResponse = serviceRequestClient.createServiceRequest(securityDTO.getAuthorisation(),
                securityDTO.getServiceAuthorisation(), serviceRequestDto);
        System.out.println("serviceRequestResponse:" + serviceRequestResponse);
        DocumentContext jsonContext = JsonPath.parse(serviceRequestResponse);
        String readPath = "$['" + SERVICE_REQUEST_REFERENCE_KEY + "']";
        System.out.println("readPath:" + jsonContext.read(readPath).toString());
        return jsonContext.read(readPath);
    }

    public void updateCaseFromServiceRequest(ServiceRequestUpdateResponseDto response, CcdCaseType ccdCaseType) {
        String caseId = response.getCcdCaseNumber();
        log.info("Updating case for Service Request, caseId: {}", caseId);

        CasePayment casePayment = CasePayment.builder()
                .amount(response.getServiceRequesAmount().longValue() * 100)
                .date(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .method(getCasePyamentMethod(response))
                .reference(response.getServiceRequestReference())
                .siteId(siteId)
                .transactionId(response.getServiceRequestReference())
                .status(getPaymentStatusByServiceRequestStatus(response.getServiceRequestStatus()))
                .build();
        List<CollectionMember<CasePayment>> payments = new ArrayList<>();

        CaseData caseData = null;
        if (CcdCaseType.GRANT_OF_REPRESENTATION == ccdCaseType) {
//            List<CollectionMember<CasePayment>> gorPayments = ((GrantOfRepresentationData) caseData).getPayments();
//            payments.addAll(gorPayments);
            payments.add(new CollectionMember<CasePayment>(null, casePayment));
            caseData = GrantOfRepresentationData.builder()
                    .payments(payments)
                    .paymentTaken(Boolean.TRUE)
                    .build();
        } else if (CcdCaseType.CAVEAT == ccdCaseType) {
//            List<CollectionMember<CasePayment>> caveatPayments = ((CaveatData) caseData).getPayments();
//            payments.addAll(caveatPayments);
            payments.add(new CollectionMember<CasePayment>(null, casePayment));
            caseData = CaveatData.builder()
                    .payments(payments)
                    .paymentTaken(Boolean.TRUE)
                    .build();
        } else {
            throw new IllegalArgumentException("Service request payment for Case:" + caseId + " not valid CaseType:"
                    + ccdCaseType);
        }

        securityUtils.setSecurityContextUserAsCaseworker();
        ResponseEntity<Map<String, Object>> userResponse = idamApi.getUserDetails(securityUtils.getAuthorisation());
        Map<String, Object> result = Objects.requireNonNull(userResponse.getBody());
        String userId = result.get("id").toString().toLowerCase();
        SecurityDTO securityDTO = SecurityDTO.builder().authorisation(securityUtils.getAuthorisation())
                .serviceAuthorisation(securityUtils.generateServiceToken())
                .userId(userId)
                .build();
        ccdClientApi.updateCaseAsCaseworker(ccdCaseType, caseId,
                caseData, EventId.SERVICE_REQUEST_PAYMENT_UPDATE,
                securityDTO, "Service request payment details updated on case",
                "Service request payment details updated on case");
        log.info("Updated Service Request on case:{}", caseId);

    }

    private String getCasePyamentMethod(ServiceRequestUpdateResponseDto response) {
        //"cheque", online", "card", "pba"
        if ("Payment by account".equals(response.getServiceRequestPaymentResponseDto().getPaymentMethod())) {
            return "pba";
        } else if ("card".equals(response.getServiceRequestPaymentResponseDto().getPaymentMethod())) {
            return "card";
        }

        return "pba";
    }

    private PaymentStatus getPaymentStatusByServiceRequestStatus(String serviceRequestStatus) {
        if ("Paid".equals(serviceRequestStatus)) {
            return PaymentStatus.SUCCESS;
        } else if ("Not Paid".equals(serviceRequestStatus)) {
            return PaymentStatus.FAILED;
        } else if ("Partially paid".equals(serviceRequestStatus)) {
            return PaymentStatus.INITIATED;
        }

        throw new IllegalArgumentException("serviceRequestStatus not a valid value");
    }

    public PaymentResponse getCreditAccountPaymentResponse(String authToken,
                                                           CreditAccountPayment creditAccountPayment) {
        URI uri = fromHttpUrl(payUri + payApi).build().toUri();
        HttpEntity<CreditAccountPayment> request = buildRequest(authToken, creditAccountPayment);

        PaymentResponse paymentResponse = null;
        try {
            ResponseEntity<PaymentResponse> responseEntity = restTemplate.exchange(uri, POST,
                    request, PaymentResponse.class);
            paymentResponse = Objects.requireNonNull(responseEntity.getBody());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == FORBIDDEN.value()) {
                throw getNewBusinessValidationException(e);
            } else if (e.getStatusCode().value() == NOT_FOUND.value()) {
                throw new BusinessValidationException(PAYMENT_ERROR_404, e.getMessage());
            } else if (e.getStatusCode().value() == UNPROCESSABLE_ENTITY.value()) {
                throw new BusinessValidationException(PAYMENT_ERROR_422, e.getMessage());
            } else if (e.getStatusCode().value() == BAD_REQUEST.value()) {
                throw getExceptionForDuplicatePayment(e);
            } else if (e.getStatusCode().is5xxServerError()) {
                throw new BusinessValidationException(PAYMENT_ERROR_5XX, e.getMessage());
            } else {
                throw new BusinessValidationException(PAYMENT_ERROR_OTHER, e.getMessage());
            }
        }
        return paymentResponse;
    }

    protected BusinessValidationException getExceptionForDuplicatePayment(HttpClientErrorException e) {
        String message = e.getMessage();
        if (message != null && message.contains(DUPLICANT_PAYMENT_ERROR_KEY)) {
            String[] empty = {};
            String duplicateMessage = businessValidationMessageRetriever.getMessage(
                    "creditAccountPaymentErrorMessageDuplicatePayment", empty, UK);
            String duplicateMessage2 = businessValidationMessageRetriever.getMessage(
                    "creditAccountPaymentErrorMessageDuplicatePayment2", empty, UK);
            return new BusinessValidationException(duplicateMessage, e.getMessage(), duplicateMessage2);
        } else {
            return new BusinessValidationException(PAYMENT_ERROR_400, e.getMessage());
        }
    }

    private BusinessValidationException getNewBusinessValidationException(HttpClientErrorException e) {
        String[] payError = {getErrorMessage(e)};
        String error1 = businessValidationMessageRetriever.getMessage("creditAccountPaymentErrorMessage", payError,
                UK);
        String[] empty = {};
        String error2 = businessValidationMessageRetriever.getMessage("creditAccountPaymentErrorMessage2",
                empty, UK);
        String error3 = businessValidationMessageRetriever.getMessage("creditAccountPaymentErrorMessage3",
                empty, UK);
        String error4 = businessValidationMessageRetriever.getMessage("creditAccountPaymentErrorMessage4",
                empty, UK);
        String error5 = businessValidationMessageRetriever.getMessage("creditAccountPaymentErrorMessage5",
                empty, UK);
        return new BusinessValidationException(error1, e.getMessage(), error2, error3,
                error4, error5);
    }

    private String getErrorMessage(HttpClientErrorException e) {

        String body = e.getResponseBodyAsString();
        log.info("getErrorMessage.body:" + body);
        JSONObject json = new JSONObject(body);
        JSONArray jsonArray = json.getJSONArray("status_histories");
        String statusHistory = jsonArray.get(0).toString();
        json = new JSONObject(statusHistory);
        return json.getString("error_message");
    }

    private HttpEntity<CreditAccountPayment> buildRequest(String authToken, CreditAccountPayment creditAccountPayment) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authToken);
        headers.add("Content-Type", "application/json");
        String sa = authTokenGenerator.generate();
        headers.add("ServiceAuthorization", sa);

        return new HttpEntity<>(creditAccountPayment, headers);
    }

    private HttpEntity<ServiceRequestDto> buildRequest(String authToken, ServiceRequestDto serviceRequestDto) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authToken);
        headers.add("Content-Type", "application/json");
        String sa = authTokenGenerator.generate();
        headers.add("ServiceAuthorization", sa);

        return new HttpEntity<ServiceRequestDto>(serviceRequestDto, headers);
    }
}
