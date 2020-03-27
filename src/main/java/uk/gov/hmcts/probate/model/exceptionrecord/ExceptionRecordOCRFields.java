package uk.gov.hmcts.probate.model.exceptionrecord;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class ExceptionRecordOCRFields {

    // Caveats
    private final String caveatorForenames;
    private final String caveatorSurnames;
    private final String caveatorEmailAddress;
    private final String caveatorAddressLine1;
    private final String caveatorAddressLine2;
    private final String caveatorAddressTown;
    private final String caveatorAddressCounty;
    private final String caveatorAddressPostCode;
    private final String solsSolicitorRepresentativeName;
    private final String solsSolicitorFirmName;
    private final String solsSolicitorAppReference;
    private final String solsFeeAccountNumber;
    private final String solsSolicitorAddressLine1;
    private final String solsSolicitorAddressLine2;
    private final String solsSolicitorAddressTown;
    private final String solsSolicitorAddressCounty;
    private final String solsSolicitorAddressPostCode;
    private final String solsSolicitorEmail;
    private final String solsSolicitorPhoneNumber;
    private final String deceasedForenames;
    private final String deceasedSurname;
    private final String deceasedDateOfDeath;
    private final String deceasedDateOfBirth;
    private final String deceasedAnyOtherNames;
    private final String deceasedAddressLine1;
    private final String deceasedAddressLine2;
    private final String deceasedAddressTown;
    private final String deceasedAddressCounty;
    private final String deceasedAddressPostCode;
    private final String caseReference;

    // PA1A and PA1P
    private final String extraCopiesOfGrant;
    private final String outsideUKGrantCopies;
    private final String applicationFeePaperForm;
    private final String feeForCopiesPaperForm;
    private final String totalFeePaperForm;
    private final String paperPaymentMethod;
    private final String solsFeeAccountNumber;
    private final String paymentReferenceNumberPaperform;
    private final String primaryApplicantForenames;
    private final String primaryApplicantSurname;
    private final String primaryApplicantHasAlias;
    private final String primaryApplicantAlias;
    private final String primaryApplicantAddressLine1;
    private final String primaryApplicantAddressLine2;
    private final String primaryApplicantAddressTown;
    private final String primaryApplicantAddressCounty;
    private final String primaryApplicantAddressPostCode;
    private final String primaryApplicantPhoneNumber;
    private final String primaryApplicantSecondPhoneNumber;
    private final String primaryApplicantEmailAddress;
    private final String executorsApplying0applyingExecutorName;
    private final String executorsApplying0applyingExecutorAddressLine1;
    private final String executorsApplying0applyingExecutorAddressLine2;
    private final String executorsApplying0applyingExecutorAddressTown;
    private final String executorsApplying0applyingExecutorAddressCounty;
    private final String executorsApplying0applyingExecutorAddressPostCode;
    private final String executorsApplying0applyingExecutorEmail;
    private final String executorsApplying0applyingExecutorDifferentNameToWill;
    private final String executorsApplying0applyingExecutorOtherNames;
    private final String executorsApplying1applyingExecutorName;
    private final String executorsApplying1applyingExecutorAddressLine1;
    private final String executorsApplying1applyingExecutorAddressLine2;
    private final String executorsApplying1applyingExecutorAddressTown;
    private final String executorsApplying1applyingExecutorAddressCounty;
    private final String executorsApplying1applyingExecutorAddressPostCode;
    private final String executorsApplying1applyingExecutorEmail;
    private final String executorsApplying1applyingExecutorDifferentNameToWill;
    private final String executorsApplying1applyingExecutorOtherNames;
    private final String executorsApplying2applyingExecutorName;
    private final String executorsApplying2applyingExecutorAddressLine1;
    private final String executorsApplying2applyingExecutorAddressLine2;
    private final String executorsApplying2applyingExecutorAddressTown;
    private final String executorsApplying2applyingExecutorAddressCounty;
    private final String executorsApplying2applyingExecutorAddressPostCode;
    private final String executorsApplying2applyingExecutorEmail;
    private final String executorsApplying2applyingExecutorDifferentNameToWill;
    private final String executorsApplying2applyingExecutorOtherNames;
    private final String solsSolicitorIsApplying;
    private final String solsSolicitorRepresentativeName;
    private final String solsSolicitorFirmName;
    private final String solsSolicitorAppReference;
    private final String solsSolicitorAddressLine1;
    private final String solsSolicitorAddressLine2;
    private final String solsSolicitorAddressTown;
    private final String solsSolicitorAddressCounty;
    private final String solsSolicitorAddressPostCode;
    private final String solsSolicitorEmail;
    private final String solsSolicitorPhoneNumber;
    private final String deceasedDomicileInEngWales;
    private final String deceasedMartialStatus;
    private final String dateOfMarriageOrCP;
    private final String dateOfDivorcedCPJudicially;
    private final String courtOfDecree;
    private final String foreignAsset;
    private final String foreignAssetEstateValue;
    private final String adopted;
    private final String adoptiveRelatives0name;
    private final String adoptiveRelatives0relationship;
    private final String adoptiveRelatives0adoptedInOrOut;
    private final String adoptiveRelatives1name;
    private final String adoptiveRelatives1relationship;
    private final String adoptiveRelatives1adoptedInOrOut;
    private final String adoptiveRelatives2name;
    private final String adoptiveRelatives2relationship;
    private final String adoptiveRelatives2adoptedInOrOut;
    private final String adoptiveRelatives3name;
    private final String adoptiveRelatives3relationship;
    private final String adoptiveRelatives3adoptedInOrOut;
    private final String adoptiveRelatives4name;
    private final String adoptiveRelatives4relationship;
    private final String adoptiveRelatives4adoptedInOrOut;
    private final String adoptiveRelatives5name;
    private final String adoptiveRelatives5relationship;
    private final String adoptiveRelatives5adoptedInOrOut;
    private final String solsWillType;
    private final String solsWillTypeReason;
    private final String bilingualGrantRequested;
    private final String spouseOrPartner;
    private final String childrenUnderEighteenSurvived;
    private final String childrenOverEighteenSurvived;
    private final String childrenDiedUnderEighteen;
    private final String childrenDiedOverEighteen;
    private final String grandChildrenSurvivedUnderEighteen;
    private final String grandChildrenSurvivedOverEighteen;
    private final String parentsExistUnderEighteenSurvived;
    private final String parentsExistOverEighteenSurvived;
    private final String wholeBloodSiblingsSurvivedUnderEighteen;
    private final String wholeBloodSiblingsSurvivedOverEighteen;
    private final String wholeBloodSiblingsDiedUnderEighteen;
    private final String wholeBloodSiblingsDiedOverEighteen;
    private final String wholeBloodNeicesAndNephewsUnderEighteen;
    private final String wholeBloodNeicesAndNephewsOverEighteen;
    private final String halfBloodSiblingsSurvivedUnderEighteen;
    private final String halfBloodSiblingsSurvivedOverEighteen;
    private final String halfBloodSiblingsDiedUnderEighteen;
    private final String halfBloodSiblingsDiedOverEighteen;
    private final String halfBloodNeicesAndNephewsUnderEighteen;
    private final String halfBloodNeicesAndNephewsOverEighteen;
    private final String grandparentsDiedUnderEighteen;
    private final String grandparentsDiedOverEighteen;
    private final String wholeBloodUnclesAndAuntsSurvivedUnderEighteen;
    private final String wholeBloodUnclesAndAuntsSurvivedOverEighteen;
    private final String wholeBloodUnclesAndAuntsDiedUnderEighteen;
    private final String wholeBloodUnclesAndAuntsDiedOverEighteen;
    private final String wholeBloodCousinsSurvivedUnderEighteen;
    private final String wholeBloodCousinsSurvivedOverEighteen;
    private final String halfBloodUnclesAndAuntsSurvivedUnderEighteen;
    private final String halfBloodUnclesAndAuntsSurvivedOverEighteen;
    private final String halfBloodUnclesAndAuntsDiedUnderEighteen;
    private final String halfBloodUnclesAndAuntsDiedOverEighteen;
    private final String halfBloodCousinsSurvivedUnderEighteen;
    private final String halfBloodCousinsSurvivedOverEighteen;
    private final String primaryApplicantRelationshipToDeceased;
    private final String secondApplicantRelationshipToDeceased;
    private final String thirdApplicantRelationshipToDeceased;
    private final String fourthApplicantRelationshipToDeceased;
    private final String applyingAsAnAttorney;
    private final String attorneyOnBehalfOfName;
    private final String attorneyOnBehalfOfAddressLine1;
    private final String attorneyOnBehalfOfAddressLine2;
    private final String attorneyOnBehalfOfAddressTown;
    private final String attorneyOnBehalfOfAddressCounty;
    private final String attorneyOnBehalfOfAddressPostCode;
    private final String mentalCapacity;
    private final String courtOfProtection;
    private final String epaOrLpa;
    private final String epaRegistered;
    private final String domicilityCountry;
    private final String domicilityEntrustingDocument;
    private final String domicilitySuccessionIHTCert;
    private final String willDate;
    private final String willHasCodicils;
    private final String willsOutsideOfUK;
    private final String deceasedMarriedAfterWillOrCodicilDate;
    private final String willGiftUnderEighteen;
    private final String executorsNotApplying0notApplyingExecutorName;
    private final String executorsNotApplying0notApplyingExecutorReason;
    private final String executorsNotApplying1notApplyingExecutorName;
    private final String executorsNotApplying1notApplyingExecutorReason;
    private final String executorsNotApplying2notApplyingExecutorName;
    private final String executorsNotApplying2notApplyingExecutorReason;
    private final String notifiedApplicants;
    private final String ihtFormCompletedOnline;
    private final String ihtReferenceNumber;
    private final String ihtFormId;
    private final String ihtGrossValue;
    private final String ihtNetValue;
}