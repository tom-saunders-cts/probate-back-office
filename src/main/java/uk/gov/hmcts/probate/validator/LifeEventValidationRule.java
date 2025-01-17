package uk.gov.hmcts.probate.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.probate.exception.BusinessValidationException;
import uk.gov.hmcts.probate.model.ccd.raw.CollectionMember;
import uk.gov.hmcts.probate.model.ccd.raw.DeathRecord;
import uk.gov.hmcts.probate.model.ccd.raw.request.CaseData;
import uk.gov.hmcts.probate.model.ccd.raw.request.CaseDetails;
import uk.gov.hmcts.probate.service.BusinessValidationMessageRetriever;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class LifeEventValidationRule implements CaseDetailsValidationRule {
    
    private final BusinessValidationMessageRetriever businessValidationMessageRetriever;
    
    @Override
    public void validate(CaseDetails caseDetails) {
        final CaseData data = caseDetails.getData();
        final List<CollectionMember<DeathRecord>> deathRecords = data.getDeathRecords();
        if (deathRecords.size() != data.getNumberOfDeathRecords()
            || deathRecords.stream().anyMatch(r -> r.getValue().getSystemNumber() == null)) {
            final String message = businessValidationMessageRetriever.getMessage("dontAddOrRemoveRecords", null,
                Locale.UK);
            final String userMessageWelsh = businessValidationMessageRetriever
                    .getMessage("dontAddOrRemoveRecordsWelsh", null, Locale.UK);
            throw new BusinessValidationException(message, message, userMessageWelsh);
        }
        if (1 != deathRecords.stream().filter(r -> r.getValue().getValid().equalsIgnoreCase("Yes")).count()) {
            final String message = businessValidationMessageRetriever.getMessage("selectOneDeathRecord",
                null, Locale.UK);
            final String userMessageWelsh = businessValidationMessageRetriever.getMessage("selectOneDeathRecordWelsh",
                    null, Locale.UK);
            throw new BusinessValidationException(message, message, userMessageWelsh);
        }
    }
}
