package uk.gov.hmcts.probate.service.template.pdf.caseextra.decorator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.probate.businessrule.PA16FormBusinessRule;
import uk.gov.hmcts.probate.model.ccd.raw.request.CaseData;
import uk.gov.hmcts.probate.service.template.pdf.caseextra.PA16FormCaseExtra;

import static uk.gov.hmcts.probate.model.Constants.PA16_FORM_TEXT;
import static uk.gov.hmcts.probate.model.Constants.PA16_FORM_URL;
import static uk.gov.hmcts.probate.model.Constants.YES;

@Component
@AllArgsConstructor
public class SolicitorCoversheetPDFDecorator {
    private final CaseExtraDecorator caseExtraDecorator;
    private final PA16FormBusinessRule pa16FormBusinessRule;

    public String decorate(CaseData caseData) {

        if (pa16FormBusinessRule.isApplicable(caseData)) {
            PA16FormCaseExtra pa16FormCaseExtra = PA16FormCaseExtra.builder()
                .pa16FormText(PA16_FORM_TEXT)
                .pa16FormUrl(PA16_FORM_URL)
                .showPa16Form(YES)
                .build();
            return caseExtraDecorator.decorate(pa16FormCaseExtra);
        } else {
            return "";
        }
    }
}
