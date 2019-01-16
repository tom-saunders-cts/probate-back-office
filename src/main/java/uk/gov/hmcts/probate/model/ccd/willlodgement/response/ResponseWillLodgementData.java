package uk.gov.hmcts.probate.model.ccd.willlodgement.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.probate.model.ApplicationType;
import uk.gov.hmcts.probate.model.ccd.ProbateAddress;
import uk.gov.hmcts.probate.model.ccd.ProbateFullAliasName;
import uk.gov.hmcts.probate.model.ccd.raw.CollectionMember;
import uk.gov.hmcts.probate.model.ccd.raw.Document;
import uk.gov.hmcts.probate.model.ccd.raw.UploadDocument;

import java.util.List;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseWillLodgementData {

    private final ApplicationType applicationType;
    private final String registryLocation;

    // ... lodgementType
    private final String lodgedDate;
    private final String willDate;
    private final String codicilDate;
    private final String numberOfCodicils;
    private final String jointWill;

    private final String deceasedForenames;
    private final String deceasedSurname;
    // ... deceasedGender
    private final String deceasedDateOfBirth;
    private final String deceasedDateOfDeath;
    private final String deceasedTypeOfDeath;
    private final String deceasedAnyOtherNames;
    private final List<CollectionMember<ProbateFullAliasName>> deceasedFullAliasNameList;
    private final ProbateAddress deceasedAddress;
    private final String deceasedEmailAddress;

    private final String executorTitle;
    private final String executorForenames;
    private final String executorSurname;
    private final ProbateAddress executorAddress;
    private final String executorEmailAddress;
    // ... additionalExecutorList

    // ... wlWithdrawReason
    private final List<CollectionMember<Document>> wlDocumentsGenerated;
    private final List<CollectionMember<UploadDocument>> documentsUploaded;
}
