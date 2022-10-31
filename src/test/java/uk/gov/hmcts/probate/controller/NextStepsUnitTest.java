package uk.gov.hmcts.probate.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import uk.gov.hmcts.probate.exception.BadRequestException;
import uk.gov.hmcts.probate.exception.BusinessValidationException;
import uk.gov.hmcts.probate.insights.AppInsights;
import uk.gov.hmcts.probate.model.DocumentType;
import uk.gov.hmcts.probate.model.ccd.CCDData;
import uk.gov.hmcts.probate.model.ccd.Fee;
import uk.gov.hmcts.probate.model.ccd.InheritanceTax;
import uk.gov.hmcts.probate.model.ccd.raw.Document;
import uk.gov.hmcts.probate.model.ccd.raw.request.CallbackRequest;
import uk.gov.hmcts.probate.model.ccd.raw.request.CaseData;
import uk.gov.hmcts.probate.model.ccd.raw.request.CaseDetails;
import uk.gov.hmcts.probate.model.ccd.raw.response.CallbackResponse;
import uk.gov.hmcts.probate.model.fee.FeesResponse;
import uk.gov.hmcts.probate.model.payments.CreditAccountPayment;
import uk.gov.hmcts.probate.model.payments.PaymentResponse;
import uk.gov.hmcts.probate.service.ConfirmationResponseService;
import uk.gov.hmcts.probate.service.EventValidationService;
import uk.gov.hmcts.probate.service.StateChangeService;
import uk.gov.hmcts.probate.service.fee.FeeService;
import uk.gov.hmcts.probate.service.payments.CreditAccountPaymentTransformer;
import uk.gov.hmcts.probate.service.payments.PaymentsService;
import uk.gov.hmcts.probate.service.template.pdf.PDFManagementService;
import uk.gov.hmcts.probate.transformer.CCDDataTransformer;
import uk.gov.hmcts.probate.transformer.CallbackResponseTransformer;
import uk.gov.hmcts.probate.transformer.HandOffLegacyTransformer;
import uk.gov.hmcts.probate.transformer.ServiceRequestTransformer;
import uk.gov.hmcts.probate.validator.CreditAccountPaymentValidationRule;
import uk.gov.hmcts.probate.validator.ServiceRequestAlreadyCreatedValidationRule;
import uk.gov.hmcts.probate.validator.SolicitorPaymentMethodValidationRule;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class NextStepsUnitTest {

    private NextStepsController underTest;

    @Mock
    private CallbackRequest callbackRequestMock;
    @Mock
    private CCDDataTransformer ccdBeanTransformerMock;
    @Mock
    private ConfirmationResponseService confirmationResponseServiceMock;
    @Mock
    private CallbackResponseTransformer callbackResponseTransformerMock;
    @Mock
    private ObjectMapper objectMapperMock;
    @Mock
    private FeeService feeServiceMock;

    @Mock
    private BindingResult bindingResultMock;
    @Mock
    private HttpServletRequest httpServletRequestMock;
    @Mock
    private CaseDetails caseDetailsMock;
    @Mock
    private CaseData caseDataMock;
    @Mock
    private CCDData ccdDataMock;
    @Mock
    private InheritanceTax inheritanceTaxMock;
    @Mock
    private Fee feeMock;
    @Mock
    private FeesResponse feesResponseMock;
    @Mock
    private CallbackResponse callbackResponseMock;
    @Mock
    private StateChangeService stateChangeServiceMock;
    @Mock
    private PaymentsService paymentsService;
    @Mock
    private CreditAccountPaymentTransformer creditAccountPaymentTransformer;
    @Mock
    private EventValidationService eventValidationService;
    @Mock
    private CreditAccountPaymentValidationRule creditAccountPaymentValidationRule;
    @Mock
    private SolicitorPaymentMethodValidationRule solicitorPaymentMethodValidationRule;
    @Mock
    private PDFManagementService pdfManagementServiceMock;
    @Mock
    private CreditAccountPayment creditAccountPaymentMock;
    @Mock
    private PaymentResponse paymentResponseMock;
    @Mock
    Document coversheetMock;
    @Mock
    private HandOffLegacyTransformer handOffLegacyTransformerMock;
    @Mock
    private ServiceRequestTransformer serviceRequestTransformer;
    @Mock
    private ServiceRequestAlreadyCreatedValidationRule serviceRequestAlreadyCreatedValidationRuleMock;

    private static final String AUTH = "Auth";

    @MockBean
    private AppInsights appInsights;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        underTest = new NextStepsController(ccdBeanTransformerMock,
            confirmationResponseServiceMock, callbackResponseTransformerMock, serviceRequestTransformer,
                objectMapperMock, feeServiceMock, stateChangeServiceMock, paymentsService, pdfManagementServiceMock,
                handOffLegacyTransformerMock, serviceRequestAlreadyCreatedValidationRuleMock);

        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(creditAccountPaymentTransformer.transform(caseDetailsMock, feesResponseMock))
            .thenReturn(creditAccountPaymentMock);
        when(pdfManagementServiceMock.generateAndUpload(callbackRequestMock, DocumentType.SOLICITOR_COVERSHEET))
                .thenReturn(coversheetMock);
        when(callbackResponseTransformerMock
            .transformForSolicitorComplete(callbackRequestMock, feesResponseMock, null, coversheetMock))
            .thenReturn(callbackResponseMock);

        when(feeServiceMock.getAllFeesData(null, 0L, 0L)).thenReturn(feesResponseMock);
        when(paymentsService.getCreditAccountPaymentResponse(AUTH, creditAccountPaymentMock))
            .thenReturn(paymentResponseMock);
    }

    @Test
    void shouldValidateWithNoErrors() {
        when(stateChangeServiceMock.getChangedStateForCaseReview(caseDataMock)).thenReturn(Optional.empty());
        when(ccdBeanTransformerMock.transform(callbackRequestMock)).thenReturn(ccdDataMock);
        when(ccdDataMock.getIht()).thenReturn(inheritanceTaxMock);
        when(ccdDataMock.getFee()).thenReturn(feeMock);
        when(creditAccountPaymentTransformer.transform(caseDetailsMock, feesResponseMock))
            .thenReturn(creditAccountPaymentMock);
        when(feesResponseMock.getTotalAmount()).thenReturn(BigDecimal.valueOf(100000));
        when(callbackResponseTransformerMock
            .transformForSolicitorComplete(callbackRequestMock, feesResponseMock, null, coversheetMock))
            .thenReturn(callbackResponseMock);
        CallbackResponse creditPaymentResponseError = Mockito.mock(CallbackResponse.class);
        when(creditPaymentResponseError.getErrors()).thenReturn(Collections.emptyList());
        when(eventValidationService.validatePaymentResponse(caseDetailsMock, paymentResponseMock,
            creditAccountPaymentValidationRule)).thenReturn(creditPaymentResponseError);

        ResponseEntity<CallbackResponse> response = underTest.validate(AUTH, callbackRequestMock,
            bindingResultMock, httpServletRequestMock);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), is(callbackResponseMock));
    }

    @Test
    void shouldValidateWithNoFeeValueNoErrors() {
        when(stateChangeServiceMock.getChangedStateForCaseReview(caseDataMock)).thenReturn(Optional.empty());
        when(ccdBeanTransformerMock.transform(callbackRequestMock)).thenReturn(ccdDataMock);
        when(ccdDataMock.getIht()).thenReturn(inheritanceTaxMock);
        when(ccdDataMock.getFee()).thenReturn(feeMock);
        when(feesResponseMock.getTotalAmount()).thenReturn(BigDecimal.ZERO);
        when(callbackResponseTransformerMock
            .transformForSolicitorComplete(callbackRequestMock, feesResponseMock, null, coversheetMock))
            .thenReturn(callbackResponseMock);

        ResponseEntity<CallbackResponse> response = underTest.validate(AUTH, callbackRequestMock,
            bindingResultMock, httpServletRequestMock);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), is(callbackResponseMock));
    }

    @Test
    void shouldValidateWithServiceRequestAlreadyCreatedError() {
        assertThrows(BusinessValidationException.class, () -> {
            when(ccdBeanTransformerMock.transform(callbackRequestMock)).thenReturn(ccdDataMock);
            when(ccdDataMock.getIht()).thenReturn(inheritanceTaxMock);
            when(ccdDataMock.getFee()).thenReturn(feeMock);
            when(stateChangeServiceMock.getChangedStateForCaseReview(caseDataMock)).thenReturn(Optional.empty());
            when(feesResponseMock.getTotalAmount()).thenReturn(BigDecimal.valueOf(100000));

            doThrow(BusinessValidationException.class).when(serviceRequestAlreadyCreatedValidationRuleMock)
                    .validate(caseDetailsMock);

            underTest.validate(AUTH, callbackRequestMock,
                    bindingResultMock, httpServletRequestMock);
        });
    }

    @Test
    void shouldValidateWithError() {
        assertThrows(BadRequestException.class, () -> {
            when(caseDetailsMock.getData()).thenReturn(caseDataMock);
            when(bindingResultMock.hasErrors()).thenReturn(true);
            when(stateChangeServiceMock.getChangedStateForCaseReview(caseDataMock)).thenReturn(Optional.empty());

            underTest.validate(AUTH, callbackRequestMock,
                bindingResultMock, httpServletRequestMock);
        });
    }

    @Test
    void shouldValidateWithErrorAndLogRequest() throws JsonProcessingException {
        assertThrows(BadRequestException.class, () -> {
            when(caseDetailsMock.getData()).thenReturn(caseDataMock);
            when(bindingResultMock.hasErrors()).thenReturn(true);
            when(stateChangeServiceMock.getChangedStateForCaseReview(caseDataMock)).thenReturn(Optional.empty());
            when(objectMapperMock.writeValueAsString(callbackRequestMock)).thenThrow(JsonProcessingException.class);

            underTest.validate(AUTH, callbackRequestMock,
                bindingResultMock, httpServletRequestMock);
        });
    }

    @Test
    void shouldValidateWithNoErrorsForStateChange() {
        Optional<String> newState = Optional.of("changedState");
        when(stateChangeServiceMock.getChangedStateForCaseReview(caseDataMock)).thenReturn(newState);
        when(callbackResponseTransformerMock.transformWithConditionalStateChange(callbackRequestMock, newState))
            .thenReturn(callbackResponseMock);


        ResponseEntity<CallbackResponse> response = underTest.validate(AUTH, callbackRequestMock,
            bindingResultMock, httpServletRequestMock);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), is(callbackResponseMock));
    }

}
