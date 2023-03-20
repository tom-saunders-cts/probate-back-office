package uk.gov.hmcts.probate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import uk.gov.hmcts.probate.exception.BusinessValidationException;
import uk.gov.hmcts.probate.model.ccd.caveat.request.CaveatCallbackRequest;
import uk.gov.hmcts.probate.model.ccd.caveat.request.CaveatData;
import uk.gov.hmcts.probate.model.ccd.caveat.request.CaveatDetails;
import uk.gov.hmcts.probate.model.ccd.caveat.response.CaveatCallbackResponse;
import uk.gov.hmcts.probate.model.fee.FeeResponse;
import uk.gov.hmcts.probate.model.payments.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.probate.service.CaveatNotificationService;
import uk.gov.hmcts.probate.service.ConfirmationResponseService;
import uk.gov.hmcts.probate.service.EventValidationService;
import uk.gov.hmcts.probate.service.NotificationService;
import uk.gov.hmcts.probate.service.RegistrarDirectionService;
import uk.gov.hmcts.probate.service.fee.FeeService;
import uk.gov.hmcts.probate.service.payments.PaymentsService;
import uk.gov.hmcts.probate.transformer.CaveatCallbackResponseTransformer;
import uk.gov.hmcts.probate.transformer.CaveatDataTransformer;
import uk.gov.hmcts.probate.transformer.ServiceRequestTransformer;
import uk.gov.hmcts.probate.validator.CaveatsEmailValidationRule;
import uk.gov.hmcts.probate.validator.CaveatsExpiryValidationRule;
import uk.gov.hmcts.probate.validator.ServiceRequestAlreadyCreatedValidationRule;
import uk.gov.service.notify.NotificationClientException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaveatControllerUnitTest {

    private CaveatController underTest;

    @Mock
    private List<CaveatsEmailValidationRule> validationRuleCaveats;
    @Mock
    private List<CaveatsExpiryValidationRule> validationRuleCaveatsExpiry;
    @Mock
    private CaveatDataTransformer caveatDataTransformer;
    @Mock
    private CaveatCallbackResponseTransformer caveatCallbackResponseTransformer;
    @Mock
    private EventValidationService eventValidationService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private CaveatNotificationService caveatNotificationService;
    @Mock
    private ConfirmationResponseService confirmationResponseService;
    @Mock
    private PaymentsService paymentsService;
    @Mock
    private FeeService feeService;

    private static final String SERVICE_REQUEST_REFERENCE = "Service Request Ref";
    private static final String USER_ID = "User-ID";

    @Mock
    private CaveatCallbackRequest caveatCallbackRequest;
    @Mock
    private CaveatDetails caveatDetailsMock;
    @Mock
    private CaveatData caveatDataMock;
    @Mock
    private FeeResponse feeResponseMock;
    @Mock
    private CaveatCallbackResponse caveatCallbackResponse;
    @Mock
    private BindingResult bindingResultMock;
    @Mock
    private ServiceRequestTransformer serviceRequestTransformer;
    @Mock
    private ServiceRequestAlreadyCreatedValidationRule serviceRequestAlreadyCreatedValidationRuleMock;
    @Mock
    private ServiceRequestDto serviceRequestDtoMock;
    @Mock
    private RegistrarDirectionService registrarDirectionService;
    @Mock
    private HttpServletRequest httpServletRequestMock;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        underTest = new CaveatController(validationRuleCaveats, validationRuleCaveatsExpiry, caveatDataTransformer,
            caveatCallbackResponseTransformer, serviceRequestTransformer, eventValidationService, notificationService,
            caveatNotificationService, confirmationResponseService, paymentsService, feeService,
            registrarDirectionService, serviceRequestAlreadyCreatedValidationRuleMock);

    }

    @Test
    void shouldValidateWithNoErrors() throws NotificationClientException {
        when(feeService.getCaveatFeesData()).thenReturn(feeResponseMock);
        when(caveatCallbackRequest.getCaseDetails()).thenReturn(caveatDetailsMock);
        when(serviceRequestTransformer.buildServiceRequest(caveatDetailsMock, feeResponseMock))
                .thenReturn(serviceRequestDtoMock);
        when(paymentsService.createServiceRequest(serviceRequestDtoMock))
            .thenReturn(SERVICE_REQUEST_REFERENCE);
        when(httpServletRequestMock.getHeader("user-id")).thenReturn(USER_ID);
        when(caveatCallbackResponseTransformer.transformResponseWithServiceRequest(caveatCallbackRequest,
                SERVICE_REQUEST_REFERENCE, USER_ID)).thenReturn(caveatCallbackResponse);
        ResponseEntity<CaveatCallbackResponse> response = underTest.solsCompleteApplication(caveatCallbackRequest,
            bindingResultMock, httpServletRequestMock);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), is(caveatCallbackResponse));
    }

    @Test
    void shouldValidateWithServiceRequestAlreadyCreatedErrors() {
        assertThrows(BusinessValidationException.class, () -> {
            when(caveatCallbackRequest.getCaseDetails()).thenReturn(caveatDetailsMock);
            doThrow(BusinessValidationException.class).when(serviceRequestAlreadyCreatedValidationRuleMock)
                    .validate(caveatDetailsMock);
            underTest.solsCompleteApplication(caveatCallbackRequest, bindingResultMock, httpServletRequestMock);
        });
    }

    @Test
    void shouldDefaultSolsPBA() {
        when(caveatCallbackResponseTransformer.transformCaseForSolicitorPayment(caveatCallbackRequest))
            .thenReturn(caveatCallbackResponse);
        ResponseEntity<CaveatCallbackResponse> response = underTest.defaultSolicitorNextStepsForPayment(
                caveatCallbackRequest);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), is(caveatCallbackResponse));
    }

    @Test
    void shouldInvokeRegistrarsDecision() {
        when(caveatDetailsMock.getData()).thenReturn(caveatDataMock);
        when(caveatCallbackRequest.getCaseDetails()).thenReturn(caveatDetailsMock);
        when(bindingResultMock.hasErrors()).thenReturn(false);

        ResponseEntity<CaveatCallbackResponse> response =
                underTest.registrarsDecision(caveatCallbackRequest);
        verify(registrarDirectionService, times(1)).addAndOrderDirectionsToCaveat(caveatDataMock);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }
}
