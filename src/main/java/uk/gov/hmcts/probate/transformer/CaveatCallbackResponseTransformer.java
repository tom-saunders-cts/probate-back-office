package uk.gov.hmcts.probate.transformer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.probate.model.ApplicationType;
import uk.gov.hmcts.probate.model.DocumentType;
import uk.gov.hmcts.probate.model.caseaccess.Organisation;
import uk.gov.hmcts.probate.model.caseaccess.OrganisationPolicy;
import uk.gov.hmcts.probate.model.ccd.CaseMatch;
import uk.gov.hmcts.probate.model.ccd.caveat.request.CaveatCallbackRequest;
import uk.gov.hmcts.probate.model.ccd.caveat.request.CaveatData;
import uk.gov.hmcts.probate.model.ccd.caveat.request.CaveatDetails;
import uk.gov.hmcts.probate.model.ccd.caveat.response.CaveatCallbackResponse;
import uk.gov.hmcts.probate.model.ccd.caveat.response.ResponseCaveatData;
import uk.gov.hmcts.probate.model.ccd.caveat.response.ResponseCaveatData.ResponseCaveatDataBuilder;
import uk.gov.hmcts.probate.model.ccd.raw.BulkPrint;
import uk.gov.hmcts.probate.model.ccd.raw.CollectionMember;
import uk.gov.hmcts.probate.model.ccd.raw.Document;
import uk.gov.hmcts.probate.model.ccd.raw.Payment;
import uk.gov.hmcts.probate.model.exceptionrecord.CaseCreationDetails;
import uk.gov.hmcts.probate.model.payments.PaymentResponse;
import uk.gov.hmcts.probate.model.payments.pba.OrganisationEntityResponse;
import uk.gov.hmcts.probate.service.organisations.OrganisationsRetrievalService;
import uk.gov.hmcts.reform.probate.model.cases.RegistryLocation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.probate.model.ApplicationType.PERSONAL;
import static uk.gov.hmcts.probate.model.ApplicationType.SOLICITOR;
import static uk.gov.hmcts.probate.model.Constants.CAVEAT_EXPIRY_EXTENSION_PERIOD_IN_MONTHS;
import static uk.gov.hmcts.probate.model.Constants.CTSC;
import static uk.gov.hmcts.probate.model.Constants.NO;
import static uk.gov.hmcts.probate.model.Constants.YES;
import static uk.gov.hmcts.probate.model.DocumentType.CAVEAT_EXTENDED;
import static uk.gov.hmcts.probate.model.DocumentType.CAVEAT_RAISED;
import static uk.gov.hmcts.probate.model.DocumentType.CAVEAT_WITHDRAWN;

@Component
@RequiredArgsConstructor
public class CaveatCallbackResponseTransformer {

    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final ApplicationType DEFAULT_APPLICATION_TYPE = PERSONAL;
    public static final String DEFAULT_REGISTRY_LOCATION = "Leeds";
    public static final String EXCEPTION_RECORD_CASE_TYPE_ID = "Caveat";
    public static final String EXCEPTION_RECORD_EVENT_ID = "raiseCaveatFromBulkScan";
    private static final String PBA_PAYMENT_METHOD = "pba";
    public static final RegistryLocation EXCEPTION_RECORD_REGISTRY_LOCATION = RegistryLocation.CTSC;
    private final DocumentTransformer documentTransformer;
    private final SolicitorPBADefaulter solicitorPBADefaulter;
    private final OrganisationsRetrievalService organisationsRetrievalService;

    public CaveatCallbackResponse caveatRaised(CaveatCallbackRequest caveatCallbackRequest,
                                               PaymentResponse paymentResponse, List<Document> documents,
                                               String letterId) {
        CaveatDetails caveatDetails = caveatCallbackRequest.getCaseDetails();
        CaveatData caveatData = caveatDetails.getData();
        documents.forEach(document -> documentTransformer.addDocument(caveatCallbackRequest, document));
        ResponseCaveatDataBuilder responseCaveatDataBuilder = getResponseCaveatData(caveatDetails);

        updateBulkPrint(documents, letterId, caveatData, responseCaveatDataBuilder, CAVEAT_RAISED);

        List<CollectionMember<Payment>> paymentsList = null;
        if (caveatData.getPayments() != null) {
            paymentsList = new ArrayList<>();
            paymentsList.addAll(caveatData.getPayments());
        }

        if (caveatData.getApplicationType() != null) {
            if (SOLICITOR.equals(caveatData.getApplicationType()) && paymentResponse != null) {
                if (paymentsList == null) {
                    paymentsList = new ArrayList<>();
                }
                Payment payment = Payment.builder()
                    .reference(paymentResponse.getReference())
                    .status(paymentResponse.getStatus())
                    .method(PBA_PAYMENT_METHOD)
                    .build();
                paymentsList.add(new CollectionMember<Payment>(payment));
            }

            responseCaveatDataBuilder
                .payments(paymentsList)
                .applicationSubmittedDate(dateTimeFormatter.format(LocalDate.now()))
                .paperForm(caveatData.getApplicationType().equals(SOLICITOR) ? NO : YES);
        } else {
            responseCaveatDataBuilder
                .applicationSubmittedDate(dateTimeFormatter.format(LocalDate.now()))
                .paperForm(YES);
        }

        return transformResponse(responseCaveatDataBuilder.build());
    }

    public CaveatCallbackResponse caveatExtendExpiry(CaveatCallbackRequest caveatCallbackRequest,
                                                     List<Document> documents, String letterId) {
        CaveatDetails caveatDetails = caveatCallbackRequest.getCaseDetails();
        CaveatData caveatData = caveatDetails.getData();
        documents.forEach(document -> documentTransformer.addDocument(caveatCallbackRequest, document));
        ResponseCaveatDataBuilder responseCaveatDataBuilder = getResponseCaveatData(caveatDetails);

        updateBulkPrint(documents, letterId, caveatData, responseCaveatDataBuilder, CAVEAT_EXTENDED);

        return transformResponse(responseCaveatDataBuilder.build());
    }

    public CaveatCallbackResponse withdrawn(final CaveatCallbackRequest caveatCallbackRequest, List<Document> documents,
                                            String letterId) {
        CaveatDetails caveatDetails = caveatCallbackRequest.getCaseDetails();
        CaveatData caveatData = caveatDetails.getData();
        documents.forEach(document -> documentTransformer.addDocument(caveatCallbackRequest, document));
        ResponseCaveatDataBuilder responseCaveatDataBuilder = getResponseCaveatData(caveatDetails);

        updateBulkPrint(documents, letterId, caveatData, responseCaveatDataBuilder, CAVEAT_WITHDRAWN);

        return transformResponse(responseCaveatDataBuilder.build());
    }

    private void updateBulkPrint(List<Document> documents, String letterId, CaveatData caveatData,
                                 ResponseCaveatDataBuilder responseCaveatDataBuilder, DocumentType documentType) {
        if (documentTransformer.hasDocumentWithType(documents, documentType) && letterId != null) {
            CollectionMember<BulkPrint> bulkPrint = buildBulkPrint(letterId, documentType.getTemplateName());
            caveatData.getBulkPrintId().add(bulkPrint);

            responseCaveatDataBuilder.bulkPrintId(caveatData.getBulkPrintId());
        }
    }

    public CaveatCallbackResponse defaultCaveatValues(CaveatCallbackRequest caveatCallbackRequest) {
        CaveatDetails caveatDetails = caveatCallbackRequest.getCaseDetails();

        ResponseCaveatData responseCaveatData = getResponseCaveatData(caveatDetails)
            .caveatRaisedEmailNotificationRequested(
                caveatCallbackRequest.getCaseDetails().getData().getCaveatRaisedEmailNotification())
            .sendToBulkPrintRequested(caveatCallbackRequest.getCaseDetails().getData().getSendToBulkPrint())
            .build();

        return transformResponse(responseCaveatData);
    }

    public CaveatCallbackResponse generalMessage(CaveatCallbackRequest caveatCallbackRequest, Document document) {
        CaveatDetails caveatDetails = caveatCallbackRequest.getCaseDetails();

        caveatDetails.getData().getDocumentsGenerated().add(new CollectionMember<>(null, document));

        ResponseCaveatData responseCaveatData = getResponseCaveatData(caveatDetails)
            .messageContent("")
            .build();

        return transformResponse(responseCaveatData);
    }

    public CaveatCallbackResponse transformForSolicitor(CaveatCallbackRequest callbackRequest) {
        ResponseCaveatData responseCaveatData = getResponseCaveatData(callbackRequest.getCaseDetails())
                .applicationType(SOLICITOR)
                .paperForm(NO)
                .registryLocation(CTSC)
                .build();

        return transformResponse(responseCaveatData);
    }

    public CaveatCallbackResponse transformForSolicitor(CaveatCallbackRequest callbackRequest, String authToken) {
        ResponseCaveatDataBuilder responseCaveatDataBuilder = getResponseCaveatData(callbackRequest.getCaseDetails())
            .applicationType(SOLICITOR)
            .paperForm(NO)
            .registryLocation(CTSC);
        OrganisationPolicy organisationPolicy =
            buildOrganisationPolicy(callbackRequest.getCaseDetails().getData(), authToken);
        if (null != organisationPolicy) {
            responseCaveatDataBuilder.applicantOrganisationPolicy(organisationPolicy);
        }
        return transformResponse(responseCaveatDataBuilder.build());
    }

    public OrganisationPolicy buildOrganisationPolicy(CaveatData caveatData, String authToken) {
        OrganisationEntityResponse organisationEntityResponse = null;
        if (null != authToken) {
            organisationEntityResponse = organisationsRetrievalService.getOrganisationEntity(authToken);
        }
        if (null != organisationEntityResponse && null != caveatData.getApplicantOrganisationPolicy()) {
            return OrganisationPolicy.builder()
                .organisation(Organisation.builder()
                    .organisationID(organisationEntityResponse.getOrganisationIdentifier())
                    .organisationName(organisationEntityResponse.getName())
                    .build())
                .orgPolicyReference(caveatData.getApplicantOrganisationPolicy().getOrgPolicyReference())
                .orgPolicyCaseAssignedRole(caveatData.getApplicantOrganisationPolicy().getOrgPolicyCaseAssignedRole())
                .build();
        }
        return null;
    }

    public CaveatCallbackResponse addMatches(CaveatCallbackRequest request, List<CaseMatch> newMatches) {
        List<CollectionMember<CaseMatch>> storedMatches = request.getCaseDetails().getData().getCaseMatches();

        // Removing case matches that have been already added
        storedMatches.stream()
            .map(CollectionMember::getValue).forEach(newMatches::remove);

        storedMatches.addAll(newMatches.stream().map(CollectionMember::new).collect(Collectors.toList()));

        storedMatches.sort(
            Comparator.comparingInt(m -> ofNullable(m.getValue().getValid()).orElse("").length()));

        ResponseCaveatData.ResponseCaveatDataBuilder responseCaseDataBuilder =
            getResponseCaveatData(request.getCaseDetails());

        return transformResponse(responseCaseDataBuilder.build());
    }

    public CaveatCallbackResponse transformResponseWithExtendedExpiry(CaveatCallbackRequest caveatCallbackRequest) {
        ResponseCaveatData.ResponseCaveatDataBuilder responseCaseDataBuilder =
            getResponseCaveatData(caveatCallbackRequest.getCaseDetails());

        String defaultExpiry = dateTimeFormatter.format(caveatCallbackRequest.getCaseDetails()
            .getData().getExpiryDate().plusMonths(CAVEAT_EXPIRY_EXTENSION_PERIOD_IN_MONTHS));
        return transformResponse(responseCaseDataBuilder.expiryDate(defaultExpiry).build());
    }

    public CaveatCallbackResponse transformResponseWithNoChanges(CaveatCallbackRequest caveatCallbackRequest) {
        ResponseCaveatData.ResponseCaveatDataBuilder responseCaseDataBuilder =
            getResponseCaveatData(caveatCallbackRequest.getCaseDetails());

        return transformResponse(responseCaseDataBuilder.build());
    }

    private CaveatCallbackResponse transformResponse(ResponseCaveatData responseCaveatData) {
        return CaveatCallbackResponse.builder().caveatData(responseCaveatData).build();
    }

    private ResponseCaveatDataBuilder getResponseCaveatData(CaveatDetails caveatDetails) {
        CaveatData caveatData = caveatDetails.getData();

        return ResponseCaveatData.builder()

            .applicationType(ofNullable(caveatData.getApplicationType()).orElse(DEFAULT_APPLICATION_TYPE))
            .registryLocation(ofNullable(caveatData.getRegistryLocation()).orElse(DEFAULT_REGISTRY_LOCATION))
            .deceasedForenames(caveatData.getDeceasedForenames())
            .deceasedSurname(caveatData.getDeceasedSurname())
            .deceasedDateOfDeath(formatDateOfDeath(caveatData.getDeceasedDateOfDeath()))
            .deceasedDateOfBirth(transformToString(caveatData.getDeceasedDateOfBirth()))
            .deceasedAnyOtherNames(caveatData.getDeceasedAnyOtherNames())
            .deceasedFullAliasNameList(caveatData.getDeceasedFullAliasNameList())
            .deceasedAddress(caveatData.getDeceasedAddress())

            .languagePreferenceWelsh(caveatData.getLanguagePreferenceWelsh())
            .solsSolicitorFirmName(caveatData.getSolsSolicitorFirmName())
            .solsSolicitorPhoneNumber(caveatData.getSolsSolicitorPhoneNumber())
            .solsSolicitorAppReference(caveatData.getSolsSolicitorAppReference())

            .solsPaymentMethods(caveatData.getSolsPaymentMethods())
            .solsFeeAccountNumber(caveatData.getSolsFeeAccountNumber())
            .solsPBANumber(caveatData.getSolsPBANumber())
            .solsPBAPaymentReference(caveatData.getSolsPBAPaymentReference())

            .caveatorForenames(caveatData.getCaveatorForenames())
            .caveatorSurname(caveatData.getCaveatorSurname())
            .caveatorEmailAddress(caveatData.getCaveatorEmailAddress())
            .caveatorAddress(caveatData.getCaveatorAddress())

            .caseMatches(caveatData.getCaseMatches())
            .applicationSubmittedDate(transformToString(caveatData.getApplicationSubmittedDate()))
            .expiryDate(transformToString(caveatData.getExpiryDate()))
            .messageContent(caveatData.getMessageContent())
            .caveatReopenReason(caveatData.getCaveatReopenReason())

            .documentsUploaded(caveatData.getDocumentsUploaded())
            .documentsGenerated(caveatData.getDocumentsGenerated())
            .scannedDocuments(caveatData.getScannedDocuments())
            .notificationsGenerated(caveatData.getNotificationsGenerated())
            .recordId(caveatData.getRecordId())
            .paperForm(caveatData.getPaperForm())
            .legacyCaseViewUrl(caveatData.getLegacyCaseViewUrl())
            .legacyType(caveatData.getLegacyType())
            .sendToBulkPrintRequested(caveatData.getSendToBulkPrintRequested())
            .caveatRaisedEmailNotificationRequested(caveatData.getCaveatRaisedEmailNotificationRequested())
            .bulkPrintId(caveatData.getBulkPrintId())
            .bulkScanCaseReference((caveatData.getBulkScanCaseReference()))
            .applicationSubmittedDate(transformToString(caveatData.getApplicationSubmittedDate()))
            .autoClosedExpiry(caveatData.getAutoClosedExpiry())
            .pcqId(caveatData.getPcqId())
            .bulkScanEnvelopes(caveatData.getBulkScanEnvelopes())
            .payments(caveatData.getPayments())
            .applicantOrganisationPolicy(caveatData.getApplicantOrganisationPolicy());
    }

    public CaseCreationDetails bulkScanCaveatCaseTransform(
        uk.gov.hmcts.reform.probate.model.cases.caveat.CaveatData caveatData) {

        if (caveatData.getApplicationType() == null) {
            caveatData.setApplicationType(uk.gov.hmcts.reform.probate.model.cases.ApplicationType.PERSONAL);
        }

        if (caveatData.getRegistryLocation() == null) {
            caveatData.setRegistryLocation(EXCEPTION_RECORD_REGISTRY_LOCATION);
        }

        if (caveatData.getPaperForm() == null) {
            caveatData.setPaperForm(true);
        }

        if (caveatData.getApplicationSubmittedDate() == null) {
            caveatData.setApplicationSubmittedDate(LocalDate.now());
        }

        if (caveatData.getCaveatorEmailAddress() == null || caveatData.getCaveatorEmailAddress().isEmpty()) {
            caveatData.setSendToBulkPrintRequested(Boolean.TRUE);
            caveatData.setCaveatRaisedEmailNotificationRequested(Boolean.FALSE);
        } else {
            caveatData.setCaveatRaisedEmailNotificationRequested(Boolean.TRUE);
            caveatData.setSendToBulkPrintRequested(Boolean.FALSE);
        }

        caveatData.setBulkScanCaseReference((caveatData.getBulkScanCaseReference()));

        return CaseCreationDetails.builder().<ResponseCaveatData>
            eventId(EXCEPTION_RECORD_EVENT_ID).caseData(caveatData).caseTypeId(EXCEPTION_RECORD_CASE_TYPE_ID).build();
    }

    public CaveatCallbackResponse transformCaseForSolicitorPBANumbers(CaveatCallbackRequest caveatCallbackRequest,
                                                                      String authToken) {
        ResponseCaveatDataBuilder responseCaseDataBuilder =
            getResponseCaveatData(caveatCallbackRequest.getCaseDetails());
        solicitorPBADefaulter.defaultCaveatFeeAccounts(caveatCallbackRequest.getCaseDetails().getData(),
            responseCaseDataBuilder, authToken);

        return transformResponse(responseCaseDataBuilder.build());
    }

    private String transformToString(LocalDate dateValue) {
        return ofNullable(dateValue)
            .map(String::valueOf)
            .orElse(null);
    }

    private String formatDateOfDeath(LocalDate dod) {
        return dod != null ? dateTimeFormatter.format(dod) : null;
    }

    private CollectionMember<BulkPrint> buildBulkPrint(String letterId, String templateName) {
        return new CollectionMember<>(null, BulkPrint.builder()
            .sendLetterId(letterId)
            .templateName(templateName)
            .build());
    }
}
