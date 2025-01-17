package uk.gov.hmcts.probate.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.probate.model.caseaccess.FindUsersByOrganisation;
import uk.gov.hmcts.probate.model.caseaccess.SolicitorUser;
import uk.gov.hmcts.probate.model.ccd.raw.AddedRepresentative;
import uk.gov.hmcts.probate.model.ccd.raw.ChangeOfRepresentative;
import uk.gov.hmcts.probate.model.ccd.raw.ChangeOrganisationRequest;
import uk.gov.hmcts.probate.model.ccd.raw.CollectionMember;
import uk.gov.hmcts.probate.model.ccd.raw.RemovedRepresentative;
import uk.gov.hmcts.probate.model.ccd.raw.SolsAddress;
import uk.gov.hmcts.probate.model.payments.pba.OrganisationEntityResponse;
import uk.gov.hmcts.probate.security.SecurityDTO;
import uk.gov.hmcts.probate.security.SecurityUtils;
import uk.gov.hmcts.probate.service.caseaccess.AssignCaseAccessClient;
import uk.gov.hmcts.probate.service.caseaccess.OrganisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.probate.model.caseaccess.DecisionRequest.decisionRequest;

@Slf4j
@RequiredArgsConstructor
@Service
public class PrepareNocService {

    private final AuthTokenGenerator tokenGenerator;
    private final AssignCaseAccessClient assignCaseAccessClient;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;
    private final OrganisationApi organisationApi;

    public AboutToStartOrSubmitCallbackResponse applyDecision(CallbackRequest callbackRequest, String authorisation) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        Map<String, Object> caseData = caseDetails.getData();
        ChangeOrganisationRequest changeOrganisationRequest = getChangeOrganisationRequest(caseData);
        List<CollectionMember<ChangeOfRepresentative>> representatives = getChangeOfRepresentations(caseData);
        log.info("Change of Representatives before for case {} ", caseDetails.getId().toString());
        ChangeOfRepresentative representative = buildChangeOfRepresentative(caseData, changeOrganisationRequest);
        representatives.add(new CollectionMember<>(null, representative));
        log.info("Change of Representatives after for case {} ", caseDetails.getId().toString());
        representatives.sort((m1, m2) -> {
            LocalDateTime dt1 = m1.getValue().getAddedDateTime();
            LocalDateTime dt2 = m2.getValue().getAddedDateTime();
            return dt2.compareTo(dt1);
        });
        getNewSolicitorDetails(securityUtils.getUserBySchedulerTokenAndServiceSecurityDTO(),
                        changeOrganisationRequest, caseData, caseDetails.getId().toString());
        SolsAddress solsAddress =
                getNewSolicitorAddress(securityUtils.getUserBySchedulerTokenAndServiceSecurityDTO(),
                        changeOrganisationRequest.getOrganisationToAdd().getOrganisationID(),
                        caseData, caseDetails.getId().toString());
        caseData.put("changeOfRepresentatives", representatives);
        caseData.put("solsSolicitorAddress", solsAddress);
        caseData.put("lastModifiedDateForDormant", LocalDateTime.now(ZoneOffset.UTC));
        caseDetails.getData().putAll(caseData);
        return assignCaseAccessClient.applyDecision(
                authorisation,
                tokenGenerator.generate(),
                decisionRequest(caseDetails)
        );
    }

    public SolsAddress getNewSolicitorAddress(SecurityDTO securityDTO, String orgId,
                                               Map<String, Object> caseData, String caseId) {
        try {
            log.info("Get OrganisationEntityResponse for caseId {}", caseId);
            OrganisationEntityResponse organisationResponse =
                    organisationApi.findOrganisationByOrgId(securityDTO.getAuthorisation(),
                            securityDTO.getServiceAuthorisation(), orgId);

            log.info("Found OrganisationEntityResponse for caseId {}", caseId);
            return convertSolicitorAddress(organisationResponse, caseData);
        } catch (Exception e) {
            log.error("Exception when looking up OrganisationEntityResponse for case {} for exception {}",
                    caseId, e.getMessage());
        }
        log.info("No OrganisationEntityResponse for caseId {}", caseId);
        return null;
    }

    private void getNewSolicitorDetails(SecurityDTO securityDTO,
                                                           ChangeOrganisationRequest changeOrganisationRequest,
                                                       Map<String, Object> caseData, String id) {
        FindUsersByOrganisation organisationUser = findOrganisationDetails(securityDTO, changeOrganisationRequest, id);
        Optional<SolicitorUser> solicitorDetails = Optional.empty();
        if (null != organisationUser
                && null != organisationUser.getUsers()
                && !organisationUser.getUsers().isEmpty()) {
            solicitorDetails = organisationUser.getUsers()
                    .stream()
                    .filter(x -> changeOrganisationRequest.getCreatedBy().equalsIgnoreCase(
                            x.getEmail()))
                    .findFirst();
        }

        if (solicitorDetails.isPresent()) {
            SolicitorUser solicitorUser = solicitorDetails.get();
            caseData.put("solsSOTForenames", solicitorUser.getFirstName());
            caseData.put("solsSOTSurname", solicitorUser.getLastName());
            caseData.put("solsSolicitorEmail", solicitorUser.getEmail());
            caseData.put("solsSOTName", solicitorUser.getFirstName() + " " + solicitorUser.getLastName());

        } else {
            log.error(
                    "Notice of change: Solicitor {} does not belong to organisation id {}",
                    changeOrganisationRequest.getCreatedBy(),
                    changeOrganisationRequest.getOrganisationToAdd().getOrganisationID()
            );
        }
    }

    private FindUsersByOrganisation findOrganisationDetails(SecurityDTO securityDTO,
                                                            ChangeOrganisationRequest changeOrganisationRequest,
                                                            String caseId) {
        try {
            log.info("Get OrganisationUser for caseId {}", caseId);
            FindUsersByOrganisation organisationUser = organisationApi
                    .findSolicitorOrganisation(securityDTO.getAuthorisation(),
                    securityDTO.getServiceAuthorisation(),
                    changeOrganisationRequest.getOrganisationToAdd().getOrganisationID());
            log.info("Found OrganisationUser for caseId {}", caseId);
            return organisationUser;
        } catch (Exception e) {
            log.error("Exception when looking up organisationUser for case {} for exception {}",
                    caseId, e.getMessage());
        }
        log.info("No OrganisationUser for caseId {}", caseId);
        return null;
    }

    public ChangeOfRepresentative buildChangeOfRepresentative(Map<String, Object> caseData,
                                                              ChangeOrganisationRequest changeOrganisationRequest) {
        RemovedRepresentative removeRepresentative = setRemovedRepresentative(caseData, changeOrganisationRequest);
        AddedRepresentative addRepresentative = setAddRepresentative(changeOrganisationRequest);
        return ChangeOfRepresentative.builder()
                .addedDateTime(LocalDateTime.now())
                .addedRepresentative(addRepresentative)
                .removedRepresentative(removeRepresentative)
                .build();
    }

    private AddedRepresentative setAddRepresentative(ChangeOrganisationRequest changeOrganisationRequest) {
        return AddedRepresentative.builder()
                .organisationID(changeOrganisationRequest.getOrganisationToAdd().getOrganisationID())
                .updatedBy(changeOrganisationRequest.getCreatedBy())
                .updatedVia("NOC")
                .build();
    }

    private RemovedRepresentative setRemovedRepresentative(Map<String, Object> caseData,
                                                           ChangeOrganisationRequest changeOrganisationRequest) {
        RemovedRepresentative removed;
        if (caseData.get("caveatorEmailAddress") != null) {
            removed = RemovedRepresentative.builder()
                    .organisationID(changeOrganisationRequest.getOrganisationToRemove().getOrganisationID())
                    .solicitorEmail(caseData.get("caveatorEmailAddress").toString())
                    .organisation(changeOrganisationRequest.getOrganisationToRemove())
                    .build();
        } else {
            removed = RemovedRepresentative.builder()
                    .organisationID(changeOrganisationRequest.getOrganisationToRemove().getOrganisationID())
                    .solicitorFirstName(caseData.get("solsSOTForenames").toString())
                    .solicitorLastName(caseData.get("solsSOTSurname").toString())
                    .solicitorEmail(caseData.get("solsSolicitorEmail").toString())
                    .organisation(changeOrganisationRequest.getOrganisationToRemove())
                    .build();
        }
        caseData.put("removedRepresentative", removed);
        return removed;
    }

    private SolsAddress convertSolicitorAddress(OrganisationEntityResponse organisationResponse,
                                                Map<String, Object> caseData) {
        caseData.put("solsSolicitorFirmName", organisationResponse.getName());
        return objectMapper.convertValue(SolsAddress.builder()
                .addressLine1(organisationResponse.getContactInformation().get(0).getAddressLine1())
                .addressLine2(organisationResponse.getContactInformation().get(0).getAddressLine2())
                .addressLine3(organisationResponse.getContactInformation().get(0).getAddressLine3())
                .county(organisationResponse.getContactInformation().get(0).getCounty())
                .country(organisationResponse.getContactInformation().get(0).getCountry())
                .postTown(organisationResponse.getContactInformation().get(0).getTownCity())
                .postCode(organisationResponse.getContactInformation().get(0).getPostCode())
                .build(), SolsAddress.class);
    }

    private ChangeOrganisationRequest getChangeOrganisationRequest(Map<String, Object> caseData) {

        return objectMapper.convertValue(caseData.get("changeOrganisationRequestField"),
                ChangeOrganisationRequest.class);
    }

    public List<CollectionMember<ChangeOfRepresentative>> getChangeOfRepresentations(Map<String, Object> caseData) {
        Object changeOfRepresentativesValue = caseData.get("changeOfRepresentatives");
        if (changeOfRepresentativesValue == null) {
            log.info("Change of reps - " + changeOfRepresentativesValue);
            return new ArrayList<>();
        }
        TypeReference<List<CollectionMember<ChangeOfRepresentative>>> typeRef =
                new TypeReference<>() {};
        return objectMapper.convertValue(changeOfRepresentativesValue, typeRef);
    }
}
