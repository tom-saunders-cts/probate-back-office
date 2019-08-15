package uk.gov.hmcts.probate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.probate.model.ccd.raw.Document;
import uk.gov.hmcts.probate.model.ccd.raw.request.CaseDetails;
import uk.gov.hmcts.probate.validator.EmailAddressExecutorsApplyingValidationRule;
import uk.gov.service.notify.NotificationClientException;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.probate.model.State.CASE_STOPPED_REQUEST_INFORMATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class InformationRequestService {

    private final NotificationService notificationService;
    private final EmailAddressExecutorsApplyingValidationRule emailAddressExecutorsApplyingValidationRule;

    public List<Document> emailInformationRequest(CaseDetails caseDetails)throws NotificationClientException {
        List<Document> documents = new ArrayList<>();

        emailAddressExecutorsApplyingValidationRule.validateEmails(caseDetails);

        caseDetails.getData().getExecutorsApplyingNotifications().forEach(executor -> {
            log.info("Initiate call to send request for information email for case id {} ",
                    caseDetails.getId());
            documents.add(notificationService.sendEmail(CASE_STOPPED_REQUEST_INFORMATION, caseDetails, executor));
            log.info("Successful response for request for information email for case id {} ",
                    caseDetails.getId());
        });

        return documents;


    }
}
