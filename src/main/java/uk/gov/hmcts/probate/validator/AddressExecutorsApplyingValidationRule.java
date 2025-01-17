package uk.gov.hmcts.probate.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.probate.exception.BusinessValidationException;
import uk.gov.hmcts.probate.model.ccd.raw.request.CaseData;
import uk.gov.hmcts.probate.model.ccd.raw.request.CaseDetails;
import uk.gov.hmcts.probate.service.BusinessValidationMessageRetriever;

import java.util.Locale;

import static uk.gov.hmcts.probate.model.Constants.YES;

@Component
@RequiredArgsConstructor
public class AddressExecutorsApplyingValidationRule implements CaseDetailsValidationRule {

    private final BusinessValidationMessageRetriever businessValidationMessageRetriever;

    private static final String ADDRESS_NOT_FOUND = "multipleAddressNotProvidedPA";
    private static final String ADDRESS_NOT_FOUND_WELSH = "multipleAddressNotProvidedPAWelsh";

    @Override
    public void validate(CaseDetails caseDetails) {
        CaseData caseData = caseDetails.getData();
        String[] args = {caseDetails.getId().toString()};
        String userMessage = businessValidationMessageRetriever.getMessage(ADDRESS_NOT_FOUND, args, Locale.UK);
        String userMessageWelsh = businessValidationMessageRetriever.getMessage(ADDRESS_NOT_FOUND_WELSH, args,
                Locale.UK);

        caseData.getExecutorsApplyingNotifications().forEach(executor -> {
            if (executor.getValue().getNotification().equals(YES)) {
                if (executor.getValue().getAddress().getAddressLine1() == null
                        || executor.getValue().getAddress().getPostCode() == null) {
                    throw new BusinessValidationException(userMessage,
                            "An applying exec address has null value for Address line 1 or postcode with case id "
                                    + caseDetails.getId(), userMessageWelsh);
                } else if (executor.getValue().getAddress().getAddressLine1().isEmpty()
                        || executor.getValue().getAddress().getPostCode().isEmpty()) {
                    throw new BusinessValidationException(userMessage,
                            "An applying exec address has empty value for Address line 1 or postcode with case id "
                                    + caseDetails.getId(), userMessageWelsh);
                }
            }
        });
    }
}
