package uk.gov.hmcts.probate.transformer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.probate.model.ccd.raw.AdditionalExecutorApplying;
import uk.gov.hmcts.probate.model.ccd.raw.AdditionalExecutorNotApplying;
import uk.gov.hmcts.probate.model.ccd.raw.CollectionMember;
import uk.gov.hmcts.probate.model.ccd.raw.request.CallbackRequest;
import uk.gov.hmcts.probate.transformer.reset.ResetCaseDataTransformer;
import uk.gov.hmcts.probate.transformer.solicitorexecutors.LegalStatementExecutorTransformer;
import uk.gov.hmcts.probate.transformer.solicitorexecutors.SolicitorApplicationCompletionTransformer;

import java.util.List;

import static uk.gov.hmcts.reform.probate.model.cases.CaseState.Constants.CASE_PRINTED_NAME;

@Component
@RequiredArgsConstructor
public class CaseDataTransformer {

    private final SolicitorApplicationCompletionTransformer solicitorApplicationCompletionTransformer;
    private final ResetCaseDataTransformer resetCaseDataTransformer;
    private final LegalStatementExecutorTransformer legalStatementExecutorTransformer;
    private final EvidenceHandledTransformer evidenceHandledTransformer;
    private final AttachDocumentsTransformer attachDocumentsTransformer;

    public void transformCaseDataForSolicitorApplicationCompletion(CallbackRequest callbackRequest) {

        final var caseData = callbackRequest.getCaseDetails().getData();
        resetCaseDataTransformer.resetExecutorLists(caseData);
        solicitorApplicationCompletionTransformer.setFieldsIfSolicitorIsNotNamedInWillAsAnExecutor(caseData);
        solicitorApplicationCompletionTransformer
                .mapSolicitorExecutorFieldsOnCompletion(caseData);

        // Remove the solicitor exec lists. Will not be needed now mapped onto caseworker exec lists.
        solicitorApplicationCompletionTransformer.clearSolicitorExecutorLists(caseData);
    }

    public void transformCaseDataForSolicitorApplicationCompletion(CallbackRequest callbackRequest,
                                                                   String serviceRequestReference) {

        final var caseData = callbackRequest.getCaseDetails().getData();
        resetCaseDataTransformer.resetExecutorLists(caseData);
        solicitorApplicationCompletionTransformer.setFieldsIfSolicitorIsNotNamedInWillAsAnExecutor(caseData);
        solicitorApplicationCompletionTransformer
                .mapSolicitorExecutorFieldsOnCompletion(caseData);

        // Remove the solicitor exec lists. Will not be needed now mapped onto caseworker exec lists.
        solicitorApplicationCompletionTransformer.clearSolicitorExecutorLists(caseData);
        solicitorApplicationCompletionTransformer.setFieldsOnServiceRequest(callbackRequest.getCaseDetails(),
                serviceRequestReference);
    }


    public void transformCaseDataForValidateProbate(CallbackRequest callbackRequest) {
        final var caseData = callbackRequest.getCaseDetails().getData();
        resetCaseDataTransformer.resetExecutorLists(caseData);
        solicitorApplicationCompletionTransformer.setFieldsIfSolicitorIsNotNamedInWillAsAnExecutor(caseData);
        solicitorApplicationCompletionTransformer.mapSolicitorExecutorFieldsOnAppDetailsComplete(caseData);
        solicitorApplicationCompletionTransformer.eraseCodicilAddedDateIfWillHasNoCodicils(caseData);
    }

    public void transformCaseDataForValidateAdmon(CallbackRequest callbackRequest) {
        final var caseData = callbackRequest.getCaseDetails().getData();
        legalStatementExecutorTransformer.formatFields(caseData);
        solicitorApplicationCompletionTransformer.eraseCodicilAddedDateIfWillHasNoCodicils(caseData);
    }


    public void transformCaseDataForLegalStatementRegeneration(CallbackRequest callbackRequest) {
        final var caseData = callbackRequest.getCaseDetails().getData();

        // we don't really need to do this, as the temp lists prior to sol journey completion should
        // be empty by this stage, however it makes functional testing a lot simpler to
        // always invoke this method, so we can simulate completion before legal statement generation
        List<CollectionMember<AdditionalExecutorApplying>> execsApplying =
                solicitorApplicationCompletionTransformer.createCaseworkerApplyingList(caseData);
        List<CollectionMember<AdditionalExecutorNotApplying>> execsNotApplying =
                solicitorApplicationCompletionTransformer.createCaseworkerNotApplyingList(caseData);

        solicitorApplicationCompletionTransformer.formatFields(caseData);
        solicitorApplicationCompletionTransformer.createLegalStatementExecutorLists(execsApplying,
                execsNotApplying, caseData);
    }

    public void transformCaseDataForSolicitorExecutorNames(CallbackRequest callbackRequest) {
        final var caseData = callbackRequest.getCaseDetails().getData();
        resetCaseDataTransformer.resetExecutorLists(caseData);
    }

    public void transformCaseDataForEvidenceHandled(CallbackRequest callbackRequest) {
        if (CASE_PRINTED_NAME.equals(callbackRequest.getCaseDetails().getState())) {
            evidenceHandledTransformer.updateEvidenceHandled(callbackRequest.getCaseDetails().getData());
        }
    }

    public void transformCaseDataForEvidenceHandledForManualCreateByCW(CallbackRequest callbackRequest) {
        if (CASE_PRINTED_NAME.equals(callbackRequest.getCaseDetails().getState())) {
            evidenceHandledTransformer.updateEvidenceHandledToNo(callbackRequest.getCaseDetails().getData());
        }
    }

    public void transformCaseDataForEvidenceHandledForCreateBulkscan(CallbackRequest callbackRequest) {
        if (CASE_PRINTED_NAME.equals(callbackRequest.getCaseDetails().getState())) {
            evidenceHandledTransformer.updateEvidenceHandledToNo(callbackRequest.getCaseDetails().getData());
        }
    }

    public void transformCaseDataForAttachDocuments(CallbackRequest callbackRequest) {
        if (CASE_PRINTED_NAME.equals(callbackRequest.getCaseDetails().getState())) {
            attachDocumentsTransformer.updateAttachDocuments(callbackRequest.getCaseDetails().getData());
        }
    }
}
