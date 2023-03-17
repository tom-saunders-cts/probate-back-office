package uk.gov.hmcts.probate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import uk.gov.hmcts.probate.exception.BadRequestException;
import uk.gov.hmcts.probate.exception.model.FieldErrorResponse;
import uk.gov.hmcts.probate.model.ApplicationType;
import uk.gov.hmcts.probate.model.CaseOrigin;
import uk.gov.hmcts.probate.model.ccd.CCDData;
import uk.gov.hmcts.probate.model.ccd.raw.Document;
import uk.gov.hmcts.probate.model.ccd.raw.request.CallbackRequest;
import uk.gov.hmcts.probate.model.ccd.raw.request.CaseData;
import uk.gov.hmcts.probate.model.ccd.raw.request.CaseDetails;
import uk.gov.hmcts.probate.model.ccd.raw.response.AfterSubmitCallbackResponse;
import uk.gov.hmcts.probate.model.ccd.raw.response.CallbackResponse;
import uk.gov.hmcts.probate.model.ccd.raw.response.ResponseCaseData;
import uk.gov.hmcts.probate.service.CaseEscalatedService;
import uk.gov.hmcts.probate.service.CaseStoppedService;
import uk.gov.hmcts.probate.service.ConfirmationResponseService;
import uk.gov.hmcts.probate.service.EventValidationService;
import uk.gov.hmcts.probate.transformer.HandOffLegacyTransformer;
import uk.gov.hmcts.probate.service.NotificationService;
import uk.gov.hmcts.probate.service.StateChangeService;
import uk.gov.hmcts.probate.service.caseaccess.AssignCaseAccessService;
import uk.gov.hmcts.probate.service.template.pdf.PDFManagementService;
import uk.gov.hmcts.probate.transformer.CallbackResponseTransformer;
import uk.gov.hmcts.probate.transformer.CaseDataTransformer;
import uk.gov.hmcts.probate.transformer.reset.ResetCaseDataTransformer;
import uk.gov.hmcts.probate.transformer.solicitorexecutors.LegalStatementExecutorTransformer;
import uk.gov.hmcts.probate.transformer.solicitorexecutors.SolicitorApplicationCompletionTransformer;
import uk.gov.hmcts.probate.validator.CaseworkerAmendAndCreateValidationRule;
import uk.gov.hmcts.probate.validator.CaseworkersSolicitorPostcodeValidationRule;
import uk.gov.hmcts.probate.validator.CheckListAmendCaseValidationRule;
import uk.gov.hmcts.probate.validator.CodicilDateValidationRule;
import uk.gov.hmcts.probate.validator.EmailAddressNotifyApplicantValidationRule;
import uk.gov.hmcts.probate.validator.FurtherEvidenceForApplicationValidationRule;
import uk.gov.hmcts.probate.validator.IHTFourHundredDateValidationRule;
import uk.gov.hmcts.probate.validator.IhtEstateValidationRule;
import uk.gov.hmcts.probate.validator.IHTValidationRule;
import uk.gov.hmcts.probate.validator.NumberOfApplyingExecutorsValidationRule;
import uk.gov.hmcts.probate.validator.OriginalWillSignedDateValidationRule;
import uk.gov.hmcts.probate.validator.RedeclarationSoTValidationRule;
import uk.gov.hmcts.probate.validator.SolicitorPostcodeValidationRule;
import uk.gov.hmcts.probate.validator.TitleAndClearingPageValidationRule;
import uk.gov.hmcts.probate.validator.ValidationRule;
import uk.gov.service.notify.NotificationClientException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.probate.model.DocumentType.LEGAL_STATEMENT_ADMON;
import static uk.gov.hmcts.probate.model.DocumentType.LEGAL_STATEMENT_INTESTACY;
import static uk.gov.hmcts.probate.model.DocumentType.LEGAL_STATEMENT_PROBATE_TRUST_CORPS;
import static uk.gov.hmcts.probate.model.State.APPLICATION_RECEIVED;
import static uk.gov.hmcts.reform.probate.model.cases.CaseState.Constants.CASE_PRINTED_NAME;

class BusinessValidationUnitTest {

    private static Optional<String> STATE_GRANT_TYPE_PROBATE = Optional.of("SolProbateCreated");
    @Mock
    private EmailAddressNotifyApplicantValidationRule emailAddressNotifyApplicantValidationRule;
    @Mock
    private EventValidationService eventValidationServiceMock;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private CallbackRequest callbackRequestMock;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private CaseDetails caseDetailsMock;
    @Mock
    private CaseData caseDataMock;
    @Mock
    private Document documentMock;
    @Mock
    private BindingResult bindingResultMock;
    @Mock
    private FieldError fieldErrorMock;
    @Mock
    private List<ValidationRule> validationRules;
    @Mock
    private List<CaseworkerAmendAndCreateValidationRule> caseworkerAmendAndCreateValidationRules;
    @Mock
    private List<CheckListAmendCaseValidationRule> checkListAmendCaseValidationRules;
    @Mock
    private CallbackResponseTransformer callbackResponseTransformerMock;
    @Mock
    private CallbackResponse callbackResponseMock;
    @Mock
    private CaseDataTransformer caseDataTransformerMock;
    @Mock
    private ConfirmationResponseService confirmationResponseServiceMock;
    @Mock
    private AfterSubmitCallbackResponse afterSubmitCallbackResponseMock;
    @Mock
    private StateChangeService stateChangeServiceMock;
    @Mock
    private RedeclarationSoTValidationRule redeclarationSoTValidationRuleMock;
    @Mock
    private NumberOfApplyingExecutorsValidationRule numberOfApplyingExecutorsValidationRuleMock;

    private FieldErrorResponse businessValidationErrorMock;
    @Mock
    private PDFManagementService pdfManagementServiceMock;
    @Mock
    private CaseStoppedService  caseStoppedServiceMock;
    @Mock
    private CaseEscalatedService caseEscalatedServiceMock;
    @Mock
    private IHTFourHundredDateValidationRule ihtFourHundredDateValidationRule;
    @Mock
    private IhtEstateValidationRule ihtEstateValidationRule;
    @Mock
    private IHTValidationRule ihtValidationRule;
    @Mock
    private CodicilDateValidationRule codicilDateValidationRuleMock;
    @Mock
    private OriginalWillSignedDateValidationRule originalWillSignedDateValidationRuleMock;
    @Mock
    private SolicitorApplicationCompletionTransformer solCompletionTransformer;
    @Mock
    private ResetCaseDataTransformer resetCdTransformer;
    @Mock
    private LegalStatementExecutorTransformer legalStatementExecutorTransformer;
    @Mock
    private List<TitleAndClearingPageValidationRule> allTitleAndClearingValidationRules;
    @Mock
    private SolicitorPostcodeValidationRule solicitorPostcodeValidationRule;
    @Mock
    private CaseworkersSolicitorPostcodeValidationRule caseworkersSolicitorPostcodeValidationRule;
    @Mock
    private AssignCaseAccessService assignCaseAccessService;
    @Mock
    private FurtherEvidenceForApplicationValidationRule furtherEvidenceForApplicationValidationRule;
    @Mock
    private HandOffLegacyTransformer handOffLegacyTransformer;

    private BusinessValidationController underTest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        businessValidationErrorMock = FieldErrorResponse.builder().build();
        underTest = new BusinessValidationController(eventValidationServiceMock,
            notificationService,
            objectMapper,
            validationRules,
            caseworkerAmendAndCreateValidationRules,
            checkListAmendCaseValidationRules,
            callbackResponseTransformerMock,
            caseDataTransformerMock,
            confirmationResponseServiceMock,
            stateChangeServiceMock,
            pdfManagementServiceMock,
            redeclarationSoTValidationRuleMock,
            numberOfApplyingExecutorsValidationRuleMock,
            codicilDateValidationRuleMock,
            originalWillSignedDateValidationRuleMock,
            allTitleAndClearingValidationRules,
            caseStoppedServiceMock,
            caseEscalatedServiceMock,
            emailAddressNotifyApplicantValidationRule,
            ihtFourHundredDateValidationRule,
            ihtEstateValidationRule,
            ihtValidationRule,
            solicitorPostcodeValidationRule,
            caseworkersSolicitorPostcodeValidationRule,
            assignCaseAccessService,
            furtherEvidenceForApplicationValidationRule,
            handOffLegacyTransformer);

        when(httpServletRequest.getRequestURI()).thenReturn("/test-uri");
    }

    @Test
    void shouldValidateWithNoErrors() {
        when(bindingResultMock.hasErrors()).thenReturn(false);
        when(callbackRequestMock.getCaseDetails())
                .thenReturn(caseDetailsMock);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, validationRules))
            .thenReturn(callbackResponseMock);
        when(stateChangeServiceMock.getChangedStateForGrantType(caseDataMock)).thenReturn(STATE_GRANT_TYPE_PROBATE);
        when(callbackResponseTransformerMock
            .transformForDeceasedDetails(callbackRequestMock, STATE_GRANT_TYPE_PROBATE))
            .thenReturn(callbackResponseMock);

        ResponseEntity<CallbackResponse> response = underTest.solsValidate(callbackRequestMock,
            bindingResultMock, httpServletRequest);

        assertThat(response.getBody(), is(callbackResponseMock));
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getErrors().isEmpty(), is(true));
    }

    @Test
    void shouldVerifySolsAccessWithNoErrors() {
        when(callbackRequestMock.getCaseDetails())
                .thenReturn(caseDetailsMock);

        ResponseEntity<AfterSubmitCallbackResponse> response = underTest.solicitorAccess("auth",
                "GrantOfRepresentation", callbackRequestMock);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    void shouldVerifySolsCreatedWithNoErrors() {
        when(callbackResponseTransformerMock.createSolsCase(callbackRequestMock, "auth"))
                .thenReturn(callbackResponseMock);
        ResponseEntity<CallbackResponse> response = underTest.createSolsCaseWithOrganisation("auth",
                callbackRequestMock);

        assertThat(response.getBody(), is(callbackResponseMock));
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getErrors().isEmpty(), is(true));
    }

    @Test
    void shouldValidateWithNoErrorsWithStateChange() {
        when(bindingResultMock.hasErrors()).thenReturn(false);
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, validationRules))
            .thenReturn(callbackResponseMock);
        Optional<String> changedState = Optional.of("changedState");
        when(stateChangeServiceMock.getChangedStateForGrantType(caseDataMock)).thenReturn(changedState);
        when(callbackResponseTransformerMock.transformForDeceasedDetails(callbackRequestMock, changedState))
            .thenReturn(callbackResponseMock);

        ResponseEntity<CallbackResponse> response = underTest.solsValidate(callbackRequestMock,
            bindingResultMock, httpServletRequest);

        assertThat(response.getBody(), is(callbackResponseMock));
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getErrors().isEmpty(), is(true));
    }

    @Test
    void shouldValidateProbateWithNoErrors() {
        when(bindingResultMock.hasErrors()).thenReturn(false);
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, validationRules))
            .thenReturn(callbackResponseMock);
        when(stateChangeServiceMock.getChangedStateForProbateUpdate(caseDataMock)).thenReturn(Optional.empty());
        when(pdfManagementServiceMock.generateAndUpload(callbackRequestMock, LEGAL_STATEMENT_PROBATE_TRUST_CORPS))
            .thenReturn(documentMock);
        when(callbackResponseTransformerMock.transform(callbackRequestMock, documentMock,
            "gop"))
            .thenReturn(callbackResponseMock);

        ResponseEntity<CallbackResponse> response = underTest.solsValidateProbate(callbackRequestMock,
            bindingResultMock, httpServletRequest);

        assertThat(response.getBody(), is(callbackResponseMock));
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getErrors().isEmpty(), is(true));
    }

    @Test
    void shouldValidateProbateWithNoErrorsWithStateChange() {
        when(bindingResultMock.hasErrors()).thenReturn(false);
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, validationRules))
            .thenReturn(callbackResponseMock);
        Optional<String> changedState = Optional.of("changedState");
        when(stateChangeServiceMock.getChangedStateForProbateUpdate(caseDataMock)).thenReturn(changedState);
        when(callbackResponseTransformerMock.transformWithConditionalStateChange(callbackRequestMock, changedState))
            .thenReturn(callbackResponseMock);

        ResponseEntity<CallbackResponse> response = underTest.solsValidateProbate(callbackRequestMock,
            bindingResultMock, httpServletRequest);

        assertThat(response.getBody(), is(callbackResponseMock));
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getErrors().isEmpty(), is(true));
    }

    @Test
    void shouldValidateIntestacyWithNoErrors() {
        when(bindingResultMock.hasErrors()).thenReturn(false);
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, validationRules))
            .thenReturn(callbackResponseMock);
        when(stateChangeServiceMock.getChangedStateForIntestacyUpdate(caseDataMock)).thenReturn(Optional.empty());
        when(pdfManagementServiceMock.generateAndUpload(callbackRequestMock, LEGAL_STATEMENT_INTESTACY))
            .thenReturn(documentMock);
        when(callbackResponseTransformerMock.transform(callbackRequestMock, documentMock,
            "intestacy"))
            .thenReturn(callbackResponseMock);

        ResponseEntity<CallbackResponse> response = underTest.solsValidateIntestacy(callbackRequestMock,
            bindingResultMock, httpServletRequest);

        assertThat(response.getBody(), is(callbackResponseMock));
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getErrors().isEmpty(), is(true));
    }

    @Test
    void shouldValidateIntestacyWithNoErrorsWithStateChange() {
        when(bindingResultMock.hasErrors()).thenReturn(false);
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, validationRules))
            .thenReturn(callbackResponseMock);
        Optional<String> changedState = Optional.of("changedState");
        when(stateChangeServiceMock.getChangedStateForIntestacyUpdate(caseDataMock)).thenReturn(changedState);
        when(callbackResponseTransformerMock.transformWithConditionalStateChange(callbackRequestMock, changedState))
            .thenReturn(callbackResponseMock);

        ResponseEntity<CallbackResponse> response = underTest.solsValidateIntestacy(callbackRequestMock,
            bindingResultMock, httpServletRequest);

        assertThat(response.getBody(), is(callbackResponseMock));
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getErrors().isEmpty(), is(true));
    }

    @Test
    void shouldValidateAdmonWithNoErrors() {
        when(bindingResultMock.hasErrors()).thenReturn(false);
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, validationRules))
            .thenReturn(callbackResponseMock);
        when(stateChangeServiceMock.getChangedStateForAdmonUpdate(caseDataMock)).thenReturn(Optional.empty());
        when(pdfManagementServiceMock.generateAndUpload(callbackRequestMock, LEGAL_STATEMENT_ADMON))
            .thenReturn(documentMock);
        when(callbackResponseTransformerMock.transform(callbackRequestMock, documentMock,
            "admonWill"))
            .thenReturn(callbackResponseMock);

        ResponseEntity<CallbackResponse> response = underTest.solsValidateAdmon(callbackRequestMock,
            bindingResultMock, httpServletRequest);

        assertThat(response.getBody(), is(callbackResponseMock));
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getErrors().isEmpty(), is(true));
    }

    @Test
    void shouldValidateAdmonWithNoErrorsWithStateChange() {
        when(bindingResultMock.hasErrors()).thenReturn(false);
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, validationRules))
            .thenReturn(callbackResponseMock);
        Optional<String> changedState = Optional.of("changedState");
        when(stateChangeServiceMock.getChangedStateForAdmonUpdate(caseDataMock)).thenReturn(changedState);
        when(callbackResponseTransformerMock.transformWithConditionalStateChange(callbackRequestMock, changedState))
            .thenReturn(callbackResponseMock);

        ResponseEntity<CallbackResponse> response = underTest.solsValidateAdmon(callbackRequestMock,
            bindingResultMock, httpServletRequest);

        assertThat(response.getBody(), is(callbackResponseMock));
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getErrors().isEmpty(), is(true));
    }

    @Test
    void shouldValidateWithFieldErrors() {
        assertThrows(BadRequestException.class, () -> {
            when(bindingResultMock.hasErrors()).thenReturn(true);
            when(bindingResultMock.getFieldErrors()).thenReturn(Collections.singletonList(fieldErrorMock));
            when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);

            ResponseEntity<CallbackResponse> response = underTest.solsValidate(callbackRequestMock,
                    bindingResultMock, httpServletRequest);

            assertThat(response.getStatusCode(), is(HttpStatus.OK));
            assertThat(response.getBody().getErrors().isEmpty(), is(false));
        });
    }

    @Test
    void shouldValidateWithBusinessErrors() {
        when(bindingResultMock.hasErrors()).thenReturn(false);
        List<FieldErrorResponse> businessErrors = Collections.singletonList(businessValidationErrorMock);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, validationRules))
            .thenReturn(callbackResponseMock);
        when(callbackResponseMock.getErrors())
            .thenReturn((businessErrors.stream().map(FieldErrorResponse::getMessage).collect(Collectors.toList())));
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);

        ResponseEntity<CallbackResponse> response = underTest.solsValidate(callbackRequestMock,
            bindingResultMock, httpServletRequest);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getErrors().isEmpty(), is(false));
    }

    @Test
    void shouldValidateAmendCaseWithNoErrors() {
        when(bindingResultMock.hasErrors()).thenReturn(false);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, caseworkerAmendAndCreateValidationRules))
            .thenReturn(callbackResponseMock);
        when(callbackResponseMock.getErrors()).thenReturn(Collections.emptyList());

        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);

        when(callbackResponseTransformerMock.transform(callbackRequestMock))
            .thenReturn(callbackResponseMock);

        ResponseEntity<CallbackResponse> response = underTest.validateCaseDetails(callbackRequestMock,
            bindingResultMock, httpServletRequest);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getErrors().isEmpty(), is(true));
    }


    @Test
    void shouldValidateAmendCaseWithFieldErrors() {
        assertThrows(BadRequestException.class, () -> {
            when(bindingResultMock.hasErrors()).thenReturn(true);
            when(bindingResultMock.getFieldErrors()).thenReturn(Collections.singletonList(fieldErrorMock));
            when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);

            ResponseEntity<CallbackResponse> response = underTest.validateCaseDetails(callbackRequestMock,
                bindingResultMock, httpServletRequest);

            assertThat(response.getStatusCode(), is(HttpStatus.OK));
            assertThat(response.getBody().getErrors().isEmpty(), is(false));
        });
    }

    @Test
    void shouldValidateAmendCaseWithBusinessErrors() {
        when(bindingResultMock.hasErrors()).thenReturn(false);
        List<FieldErrorResponse> businessErrors = Collections.singletonList(businessValidationErrorMock);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, caseworkerAmendAndCreateValidationRules))
            .thenReturn(callbackResponseMock);
        when(callbackResponseMock.getErrors())
            .thenReturn((businessErrors.stream().map(FieldErrorResponse::getMessage).collect(Collectors.toList())));
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);

        ResponseEntity<CallbackResponse> response = underTest.validateCaseDetails(callbackRequestMock,
            bindingResultMock, httpServletRequest);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getErrors().isEmpty(), is(false));
    }

    @Test
    void shouldErrorForConfirmation() {
        assertThrows(BadRequestException.class, () -> {
            when(bindingResultMock.hasErrors()).thenReturn(true);
            when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);

            ResponseEntity<AfterSubmitCallbackResponse> response = underTest.stopWithConfirmation(callbackRequestMock,
                bindingResultMock);

            assertThat(response.getStatusCode(), is(HttpStatus.OK));
        });
    }

    @Test
    void shouldPassConfirmation() {
        when(bindingResultMock.hasErrors()).thenReturn(false);
        when(confirmationResponseServiceMock.getStopConfirmation(Mockito.any(CallbackRequest.class)))
            .thenReturn(afterSubmitCallbackResponseMock);

        ResponseEntity<AfterSubmitCallbackResponse> response = underTest.stopWithConfirmation(callbackRequestMock,
            bindingResultMock);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    void shouldTransformCaseWithFieldErrors() {
        assertThrows(BadRequestException.class, () -> {
            when(bindingResultMock.hasErrors()).thenReturn(true);
            when(bindingResultMock.getFieldErrors()).thenReturn(Collections.singletonList(fieldErrorMock));
            when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);

            ResponseEntity<CallbackResponse> response = underTest.casePrinted(callbackRequestMock,
                    bindingResultMock);

            assertThat(response.getStatusCode(), is(HttpStatus.OK));
            assertThat(response.getBody().getErrors().isEmpty(), is(false));
        });
    }

    @Test
    void shouldTransformCaseWithNoErrors() {
        when(bindingResultMock.hasErrors()).thenReturn(false);
        when(callbackResponseTransformerMock.transformCase(callbackRequestMock))
            .thenReturn(callbackResponseMock);
        when(callbackRequestMock.getCaseDetails())
                .thenReturn(caseDetailsMock);
        when(caseDetailsMock.getState()).thenReturn(CASE_PRINTED_NAME);
        ResponseEntity<CallbackResponse> response = underTest.casePrinted(callbackRequestMock,
            bindingResultMock);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getErrors().isEmpty(), is(true));
    }

    @Test
    void shouldPaperFormWithFieldErrors() throws NotificationClientException {
        assertThrows(BadRequestException.class, () -> {
            when(bindingResultMock.hasErrors()).thenReturn(true);
            when(bindingResultMock.getFieldErrors()).thenReturn(Collections.singletonList(fieldErrorMock));
            when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);

            ResponseEntity<CallbackResponse> response = underTest.paperFormCaseDetails(callbackRequestMock,
                    bindingResultMock);

            assertThat(response.getStatusCode(), is(HttpStatus.OK));
            assertThat(response.getBody().getErrors().isEmpty(), is(false));
        });
    }

    @Test
    void shouldSubmitForPersonalWithEmail() throws NotificationClientException {
        String paperFormValue = "Any";
        ResponseCaseData responseCaseData = ResponseCaseData.builder().paperForm(paperFormValue).build();
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(caseDataMock.getApplicationType()).thenReturn(ApplicationType.PERSONAL);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, caseworkerAmendAndCreateValidationRules))
            .thenReturn(callbackResponseMock);
        when(callbackResponseMock.getData()).thenReturn(responseCaseData);
        Document documentMock = Mockito.mock(Document.class);
        when(notificationService.sendEmail(APPLICATION_RECEIVED, caseDetailsMock, Optional.of(CaseOrigin.CASEWORKER)))
            .thenReturn(documentMock);
        when(callbackResponseTransformerMock.paperForm(callbackRequestMock, documentMock))
            .thenReturn(callbackResponseMock);
        when(emailAddressNotifyApplicantValidationRule.validate(any(CCDData.class))).thenReturn(Collections.EMPTY_LIST);
        ResponseEntity<CallbackResponse> response = underTest.paperFormCaseDetails(callbackRequestMock,
            bindingResultMock);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getData().getPaperForm(), is(paperFormValue));
    }

    @Test
    void shouldSubmitForPersonalWithoutEmail() throws NotificationClientException {
        String paperFormValue = "Any";
        ResponseCaseData responseCaseData = ResponseCaseData.builder().paperForm(paperFormValue).build();
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(caseDataMock.getApplicationType()).thenReturn(ApplicationType.PERSONAL);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, caseworkerAmendAndCreateValidationRules))
            .thenReturn(callbackResponseMock);
        when(callbackResponseMock.getData()).thenReturn(responseCaseData);
        when(callbackResponseTransformerMock.paperForm(callbackRequestMock, null)).thenReturn(callbackResponseMock);
        when(emailAddressNotifyApplicantValidationRule.validate(any(CCDData.class)))
            .thenReturn(Arrays.asList(FieldErrorResponse.builder().build()));
        Document documentMock = Mockito.mock(Document.class);
        ResponseEntity<CallbackResponse> response = underTest.paperFormCaseDetails(callbackRequestMock,
            bindingResultMock);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getData().getPaperForm(), is(paperFormValue));
        verify(notificationService, times(0))
            .sendEmail(APPLICATION_RECEIVED, caseDetailsMock, Optional.of(CaseOrigin.CASEWORKER));
    }

    @Test
    void shouldSubmitPaperFormNoForPersonal() throws NotificationClientException {
        String paperFormValue = "No";
        ResponseCaseData responseCaseData = ResponseCaseData.builder().paperForm(paperFormValue).build();
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(caseDataMock.getApplicationType()).thenReturn(ApplicationType.PERSONAL);
        when(callbackResponseMock.getData()).thenReturn(responseCaseData);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, caseworkerAmendAndCreateValidationRules))
            .thenReturn(callbackResponseMock);
        Document documentMock = Mockito.mock(Document.class);
        when(notificationService.sendEmail(APPLICATION_RECEIVED, caseDetailsMock, Optional.of(CaseOrigin.CASEWORKER)))
            .thenReturn(documentMock);
        when(callbackResponseTransformerMock.paperForm(callbackRequestMock, documentMock))
            .thenReturn(callbackResponseMock);
        when(emailAddressNotifyApplicantValidationRule.validate(any(CCDData.class))).thenReturn(Collections.EMPTY_LIST);
        ResponseEntity<CallbackResponse> response = underTest.paperFormCaseDetails(callbackRequestMock,
            bindingResultMock);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getData().getPaperForm(), is(paperFormValue));
    }

    @Test
    void shouldSubmitForSolicitorWithoutEmail() throws NotificationClientException {
        String paperFormValue = "YesNo";
        ResponseCaseData responseCaseData = ResponseCaseData.builder().paperForm(paperFormValue).build();
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(caseDataMock.getApplicationType()).thenReturn(ApplicationType.SOLICITOR);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, caseworkerAmendAndCreateValidationRules))
            .thenReturn(callbackResponseMock);
        when(callbackResponseMock.getData()).thenReturn(responseCaseData);
        when(callbackResponseTransformerMock.paperForm(callbackRequestMock, null)).thenReturn(callbackResponseMock);
        when(emailAddressNotifyApplicantValidationRule.validate(any(CCDData.class)))
            .thenReturn(Arrays.asList(FieldErrorResponse.builder().build()));
        ResponseEntity<CallbackResponse> response = underTest.paperFormCaseDetails(callbackRequestMock,
            bindingResultMock);

        verify(notificationService, times(0))
            .sendEmail(APPLICATION_RECEIVED, caseDetailsMock, Optional.of(CaseOrigin.CASEWORKER));
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getData().getPaperForm(), is(paperFormValue));
    }

    @Test
    void shouldSubmitForSolicitorWithEmail() throws NotificationClientException {
        String paperFormValue = "YesNo";
        ResponseCaseData responseCaseData = ResponseCaseData.builder().paperForm(paperFormValue).build();
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(caseDataMock.getApplicationType()).thenReturn(ApplicationType.SOLICITOR);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, caseworkerAmendAndCreateValidationRules))
            .thenReturn(callbackResponseMock);
        when(callbackResponseMock.getData()).thenReturn(responseCaseData);
        when(callbackResponseTransformerMock.paperForm(callbackRequestMock, null)).thenReturn(callbackResponseMock);
        when(emailAddressNotifyApplicantValidationRule.validate(any(CCDData.class))).thenReturn(Collections.EMPTY_LIST);
        ResponseEntity<CallbackResponse> response = underTest.paperFormCaseDetails(callbackRequestMock,
            bindingResultMock);

        verify(notificationService, times(1))
            .sendEmail(APPLICATION_RECEIVED, caseDetailsMock, Optional.of(CaseOrigin.CASEWORKER));
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getData().getPaperForm(), is(paperFormValue));
    }

    @Test
    void shouldSubmitForSolicitorPaperFormNoWithEmail() throws NotificationClientException {
        String paperFormValue = "No";
        ResponseCaseData responseCaseData = ResponseCaseData.builder().paperForm(paperFormValue).build();
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(caseDataMock.getApplicationType()).thenReturn(ApplicationType.SOLICITOR);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, caseworkerAmendAndCreateValidationRules))
            .thenReturn(callbackResponseMock);
        when(callbackResponseMock.getData()).thenReturn(responseCaseData);
        when(callbackResponseTransformerMock.paperForm(callbackRequestMock, null)).thenReturn(callbackResponseMock);
        when(emailAddressNotifyApplicantValidationRule.validate(any(CCDData.class))).thenReturn(Collections.EMPTY_LIST);
        ResponseEntity<CallbackResponse> response = underTest.paperFormCaseDetails(callbackRequestMock,
                bindingResultMock);

        verify(notificationService, times(1))
                .sendEmail(APPLICATION_RECEIVED, caseDetailsMock, Optional.of(CaseOrigin.CASEWORKER));
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody().getData().getPaperForm(), is(paperFormValue));
    }

    @Test
    void shouldValidateIHT400Date() {
        ResponseEntity<CallbackResponse> response = underTest.solsValidateIHT400Date(callbackRequestMock);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        verify(callbackResponseTransformerMock).transform(any());
    }

    @Test
    void shouldDefaultIHT400421PageFlow() {
        ResponseEntity<CallbackResponse> response = underTest.defaultIht400DatePage(callbackRequestMock);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        verify(callbackResponseTransformerMock).defaultIht400421DatePageFlow(any());
    }

    @Test
    void shouldSetGrantStoppedDateAfterCaseFailQa() {
        ResponseEntity<CallbackResponse> response = underTest.caseFailQa(callbackRequestMock);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    void shouldDefaultPBAs() {
        ResponseEntity<CallbackResponse> response =
            underTest.defaultSolicitorNextStepsForPayment(callbackRequestMock);
        verify(callbackResponseTransformerMock, times(1))
            .transformCaseForSolicitorPayment(callbackRequestMock);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    void shouldDefaultIHT() {
        ResponseEntity<CallbackResponse> response =
            underTest.defaultIhtEstateFromDateOfDeath(callbackRequestMock);
        verify(callbackResponseTransformerMock, times(1))
            .defaultIhtEstateFromDateOfDeath(callbackRequestMock);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    void shouldValidateIHTEstateData() {
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(eventValidationServiceMock.validateRequest(any(), any())).thenReturn(callbackResponseMock);
        ResponseEntity<CallbackResponse> response =
            underTest.validateIhtEstateData(callbackRequestMock);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        verify(ihtEstateValidationRule, times(1)).validate(caseDetailsMock);
        verify(callbackResponseTransformerMock).transform(callbackRequestMock);
    }

    @Test
    void shouldValidateIHTEstateDataWithError() {
        List<String> errors = new ArrayList<>();
        errors.add("some error");
        when(callbackResponseMock.getErrors()).thenReturn(errors);
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(eventValidationServiceMock.validateRequest(any(), any())).thenReturn(callbackResponseMock);
        ResponseEntity<CallbackResponse> response =
                underTest.validateIhtEstateData(callbackRequestMock);
        verify(callbackResponseTransformerMock, times(0)).transform(callbackRequestMock);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    void shouldValidateSolPostCode() {
        when(eventValidationServiceMock.validateRequest(any(), any())).thenReturn(callbackResponseMock);
        ResponseEntity<CallbackResponse> response =  underTest.validateSolsCreate(callbackRequestMock);
        verify(callbackResponseTransformerMock).transform(callbackRequestMock);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    void shouldValidateSolPostCodeCaseworker() {
        when(eventValidationServiceMock.validateRequest(any(), any())).thenReturn(callbackResponseMock);
        ResponseEntity<CallbackResponse> response =  underTest.validateSolsCreateDefaultIhtEstate(callbackRequestMock);
        verify(callbackResponseTransformerMock, times(1))
                .defaultIhtEstateFromDateOfDeath(callbackRequestMock);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    void shouldValidateMissingSolPostCode() {
        List<String> errors = new ArrayList<>();
        errors.add("some error");
        when(callbackResponseMock.getErrors()).thenReturn(errors);
        when(eventValidationServiceMock.validateRequest(any(), any())).thenReturn(callbackResponseMock);
        ResponseEntity<CallbackResponse> response =  underTest.validateSolsCreate(callbackRequestMock);
        verify(callbackResponseTransformerMock, times(0)).transform(callbackRequestMock);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    void shouldValidateMissingSolPostCodeCaseworker() {
        List<String> errors = new ArrayList<>();
        errors.add("some error");
        when(callbackResponseMock.getErrors()).thenReturn(errors);
        when(eventValidationServiceMock.validateRequest(any(), any())).thenReturn(callbackResponseMock);
        ResponseEntity<CallbackResponse> response =  underTest.validateSolsCreateDefaultIhtEstate(callbackRequestMock);
        verify(callbackResponseTransformerMock, times(0)).defaultIhtEstateFromDateOfDeath(callbackRequestMock);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    void shouldTransformCaseDataForEvidenceHandledPACreateCaseOK() {
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(caseDetailsMock.getState()).thenReturn(CASE_PRINTED_NAME);
        ResponseEntity<CallbackResponse> response =  underTest.paCreate(callbackRequestMock, bindingResultMock);
        verify(callbackResponseTransformerMock).transformCase(callbackRequestMock);
        verify(caseDataTransformerMock).transformCaseDataForEvidenceHandled(callbackRequestMock);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    void shouldTransformCaseDataForEvidenceHandledCasePrinted() {
        when(bindingResultMock.hasErrors()).thenReturn(false);
        when(callbackResponseTransformerMock.transformCase(callbackRequestMock))
                .thenReturn(callbackResponseMock);
        when(callbackRequestMock.getCaseDetails())
                .thenReturn(caseDetailsMock);
        when(caseDetailsMock.getState()).thenReturn(CASE_PRINTED_NAME);
        ResponseEntity<CallbackResponse> response = underTest.casePrinted(callbackRequestMock,
                bindingResultMock);

        verify(caseDataTransformerMock).transformCaseDataForEvidenceHandled(callbackRequestMock);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    void shouldTransformCaseDataForEvidenceHandledCW() throws NotificationClientException {
        String paperFormValue = "Any";
        ResponseCaseData responseCaseData = ResponseCaseData.builder().paperForm(paperFormValue).build();
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(caseDataMock.getApplicationType()).thenReturn(ApplicationType.PERSONAL);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, caseworkerAmendAndCreateValidationRules))
                .thenReturn(callbackResponseMock);
        when(callbackResponseMock.getData()).thenReturn(responseCaseData);
        Document documentMock = Mockito.mock(Document.class);
        when(notificationService.sendEmail(APPLICATION_RECEIVED, caseDetailsMock, Optional.of(CaseOrigin.CASEWORKER)))
                .thenReturn(documentMock);
        when(callbackResponseTransformerMock.paperForm(callbackRequestMock, documentMock))
                .thenReturn(callbackResponseMock);
        when(emailAddressNotifyApplicantValidationRule.validate(any(CCDData.class))).thenReturn(Collections.EMPTY_LIST);
        ResponseEntity<CallbackResponse> response = underTest.paperFormCaseDetails(callbackRequestMock,
                bindingResultMock);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        verify(caseDataTransformerMock).transformCaseDataForEvidenceHandledForManualCreateByCW(callbackRequestMock);
    }

    @Test
    void shouldValidateFurtherEvidenceForApplication() {
        when(callbackRequestMock.getCaseDetails()).thenReturn(caseDetailsMock);
        when(bindingResultMock.hasErrors()).thenReturn(false);
        when(caseDetailsMock.getData()).thenReturn(caseDataMock);
        when(eventValidationServiceMock.validateRequest(callbackRequestMock, validationRules))
                .thenReturn(callbackResponseMock);
        ResponseEntity<CallbackResponse> response =
                underTest.solsValidateAdmon(callbackRequestMock, bindingResultMock, httpServletRequest);
        verify(furtherEvidenceForApplicationValidationRule, times(1))
                .validate(caseDetailsMock);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }
}
