package uk.gov.hmcts.probate.transformer.solicitorexecutors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.probate.model.ccd.raw.AdditionalExecutorApplying;
import uk.gov.hmcts.probate.model.ccd.raw.CollectionMember;
import uk.gov.hmcts.probate.model.ccd.raw.request.CaseData;
import uk.gov.hmcts.probate.service.DateFormatterService;
import uk.gov.hmcts.probate.service.solicitorexecutor.ExecutorListMapperService;

import java.util.List;

import static uk.gov.hmcts.probate.model.Constants.NO;

@Component
@Slf4j
// Handles some casedata mappings for when a solicitor application becomes a case
// for caseworker or solicitor journeys
public class SolicitorApplicationCompletionTransformer extends LegalStatementExecutorTransformer {

    private static final String NOT_APPLICABLE = "NotApplicable";

    public SolicitorApplicationCompletionTransformer(ExecutorListMapperService executorListMapperService,
                                                     DateFormatterService dateFormatterService) {
        super(executorListMapperService, dateFormatterService);
    }

    /**
     * Map all executors into executors applying and executors not applying lists for the solicitor legal statement.
     */
    public void mapSolicitorExecutorFieldsOnCompletion(CaseData caseData) {

        mapSolicitorExecutorFieldsToCaseworkerExecutorFields(caseData);
        formatFields(caseData);
        createLegalStatementExecutorListsFromTransformedLists(caseData);
    }

    public void mapSolicitorExecutorFieldsOnAppDetailsComplete(CaseData caseData) {
        if (isSolicitorApplying(caseData)) {
            List<CollectionMember<AdditionalExecutorApplying>> execsApplying = createCaseworkerApplyingList(caseData);
            mapExecutorToPrimaryApplicantFields(execsApplying.get(0).getValue(), caseData);
        } else if (!isSolicitorApplying(caseData) && isSolicitorNamedInWillAsAnExecutor(caseData)) {
            createCaseworkerApplyingList(caseData);
            mapExecutorToPrimaryApplicantFieldsNotApplying(caseData);
        }
        formatFields(caseData);
        mapSolicitorExecutorFieldsToLegalStatementExecutorFields(caseData);
    }

    public void eraseCodicilAddedDateIfWillHasNoCodicils(CaseData caseData) {
        if (NO.equals(caseData.getWillHasCodicils())) {
            caseData.setCodicilAddedDateList(null);
            caseData.setCodicilAddedFormattedDateList(null);
        }
    }

    public void setFieldsOnServiceRequest(CaseData caseData, String serviceRequestReference) {
        if (serviceRequestReference != null) {
            caseData.setServiceRequestReference(serviceRequestReference);
        } else {
            caseData.setPaymentTaken(NOT_APPLICABLE);
        }
    }
}
