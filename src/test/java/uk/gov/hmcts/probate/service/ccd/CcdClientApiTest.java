package uk.gov.hmcts.probate.service.ccd;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.probate.model.ccd.CcdCaseType;
import uk.gov.hmcts.probate.model.ccd.EventId;
import uk.gov.hmcts.probate.model.ccd.JurisdictionId;
import uk.gov.hmcts.probate.model.ccd.raw.request.CaseData;
import uk.gov.hmcts.probate.security.SecurityDTO;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.probate.model.cases.grantofrepresentation.GrantOfRepresentationData;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class CcdClientApiTest {

    private static final String AUTHORISATION = "XXJDHFHF";
    private static final String SERVICE_AUTHORISATION = "uifhuhfsd";
    private static final String USER_ID = "33";
    private static final String EVENT_ID = "EVENTID324234";

    @Mock
    private CoreCaseDataApi coreCaseDataApi;

    @InjectMocks
    private CcdClientApi ccdClientApi;

    @Test
    void shouldCreateCase() {
        CcdCaseType ccdCaseType = CcdCaseType.GRANT_OF_REPRESENTATION;
        CaseData caseData = CaseData.builder().build();
        EventId eventId = EventId.IMPORT_GOR_CASE;
        SecurityDTO securityDTO = SecurityDTO.builder()
            .authorisation(AUTHORISATION)
            .serviceAuthorisation(SERVICE_AUTHORISATION)
            .userId(USER_ID)
            .build();
        StartEventResponse startEventResponse = StartEventResponse.builder()
            .eventId(EVENT_ID)
            .build();
        CaseDetails caseDetails = CaseDetails.builder().build();

        when(coreCaseDataApi.startForCaseworker(
            AUTHORISATION,
            SERVICE_AUTHORISATION,
            USER_ID,
            JurisdictionId.PROBATE.name(),
            ccdCaseType.getName(),
            eventId.getName())).thenReturn(startEventResponse);

        when(coreCaseDataApi.submitForCaseworker(
            eq(AUTHORISATION),
            eq(SERVICE_AUTHORISATION),
            eq(USER_ID),
            eq(JurisdictionId.PROBATE.name()),
            eq(ccdCaseType.getName()),
            eq(false),
            any(CaseDataContent.class))).thenReturn(caseDetails);

        CaseDetails actualCaseDetails = ccdClientApi.createCase(caseData, ccdCaseType, eventId, securityDTO);

        assertThat(actualCaseDetails, equalTo(caseDetails));


        verify(coreCaseDataApi, times(1)).startForCaseworker(
            AUTHORISATION,
            SERVICE_AUTHORISATION,
            USER_ID,
            JurisdictionId.PROBATE.name(),
            ccdCaseType.getName(),
            eventId.getName());

        verify(coreCaseDataApi, times(1)).submitForCaseworker(
            eq(AUTHORISATION),
            eq(SERVICE_AUTHORISATION),
            eq(USER_ID),
            eq(JurisdictionId.PROBATE.name()),
            eq(ccdCaseType.getName()),
            eq(false),
            any(CaseDataContent.class));
    }

    @Test
    void shouldRetrieveCase() {
        CcdCaseType ccdCaseType = CcdCaseType.GRANT_OF_REPRESENTATION;
        Long legacyId = 1L;
        CaseDetails caseDetails = CaseDetails.builder().build();

        when(coreCaseDataApi.searchForCaseworker(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(ImmutableMap.class))).thenReturn(Arrays.asList(caseDetails));

        SecurityDTO securityDTO = SecurityDTO.builder()
            .authorisation(AUTHORISATION)
            .serviceAuthorisation(SERVICE_AUTHORISATION)
            .userId(USER_ID)
            .build();

        Optional<CaseDetails> actualCaseDetails =
            ccdClientApi.retrieveCaseByLegacyId(ccdCaseType.getName(), legacyId, securityDTO);

        assertThat(actualCaseDetails.get(), equalTo(caseDetails));


    }

    @Test
    void shouldNotFindCase() {
        CcdCaseType ccdCaseType = CcdCaseType.GRANT_OF_REPRESENTATION;
        Long legacyId = 1L;

        when(coreCaseDataApi.searchForCaseworker(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(ImmutableMap.class))).thenReturn(Collections.EMPTY_LIST);

        SecurityDTO securityDTO = SecurityDTO.builder()
            .authorisation(AUTHORISATION)
            .serviceAuthorisation(SERVICE_AUTHORISATION)
            .userId(USER_ID)
            .build();

        Optional<CaseDetails> actualCaseDetails =
            ccdClientApi.retrieveCaseByLegacyId(ccdCaseType.getName(), legacyId, securityDTO);

        assertThat(actualCaseDetails.isPresent(), equalTo(false));


    }

    @Test
    void shouldThrowExceptionWhenMorethan1CaseFoundForRetrieveCase() {
        assertThrows(IllegalStateException.class, () -> {
            CcdCaseType ccdCaseType = CcdCaseType.GRANT_OF_REPRESENTATION;
            Long legacyId = 1L;
            CaseDetails caseDetails1 = CaseDetails.builder().build();
            CaseDetails caseDetails2 = CaseDetails.builder().build();

            when(coreCaseDataApi.searchForCaseworker(
                    any(String.class),
                    any(String.class),
                    any(String.class),
                    any(String.class),
                    any(String.class),
                    any(ImmutableMap.class))).thenReturn(Arrays.asList(caseDetails1, caseDetails2));

            SecurityDTO securityDTO = SecurityDTO.builder()
                    .authorisation(AUTHORISATION)
                    .serviceAuthorisation(SERVICE_AUTHORISATION)
                    .userId(USER_ID)
                    .build();

            Optional<CaseDetails> actualCaseDetails =
                    ccdClientApi.retrieveCaseByLegacyId(ccdCaseType.getName(), legacyId, securityDTO);
        });
    }

    @Test
    void shouldReadCaseDetails() {
        CcdCaseType ccdCaseType = CcdCaseType.GRANT_OF_REPRESENTATION;
        Long legacyId = 1L;

        CaseDetails caseDetails = Mockito.mock(CaseDetails.class);
        when(coreCaseDataApi.readForCaseWorker(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class))).thenReturn(caseDetails);

        SecurityDTO securityDTO = SecurityDTO.builder()
            .authorisation(AUTHORISATION)
            .serviceAuthorisation(SERVICE_AUTHORISATION)
            .userId(USER_ID)
            .build();

        CaseDetails actualCaseDetails = ccdClientApi.readForCaseWorker(ccdCaseType, "1", securityDTO);

        assertThat(actualCaseDetails, equalTo(caseDetails));
    }

    @Test
    void updateCaseAsCitizen() {
        CcdCaseType ccdCaseType = CcdCaseType.GRANT_OF_REPRESENTATION;

        CaseDetails caseDetails = Mockito.mock(CaseDetails.class);
        StartEventResponse startEventResponse = StartEventResponse.builder().build();
        EventId eventId = EventId.DEATH_RECORD_VERIFIED;

        when(coreCaseDataApi.startEventForCitizen(
                eq(AUTHORISATION),
                eq(SERVICE_AUTHORISATION),
                eq(USER_ID),
                eq(JurisdictionId.PROBATE.name()),
                eq(ccdCaseType.getName()),
                any(),
                any())).thenReturn(startEventResponse);

        GrantOfRepresentationData grantOfRepresentationData = GrantOfRepresentationData.builder()
                .build();

        when(coreCaseDataApi.submitEventForCitizen(
                eq(AUTHORISATION),
                eq(SERVICE_AUTHORISATION),
                eq(USER_ID),
                eq(JurisdictionId.PROBATE.name()),
                eq(ccdCaseType.getName()),
                eq("1"),
                eq(false),
                any(CaseDataContent.class))).thenReturn(caseDetails);

        SecurityDTO securityDTO = SecurityDTO.builder()
                .authorisation(AUTHORISATION)
                .serviceAuthorisation(SERVICE_AUTHORISATION)
                .userId(USER_ID)
                .build();


        CaseDetails actualCaseDetails = ccdClientApi.updateCaseAsCitizen(
                CcdCaseType.GRANT_OF_REPRESENTATION,
                "1",
                grantOfRepresentationData,
                eventId,
                securityDTO,
                "Description",
                "Summary");

        assertThat(actualCaseDetails, equalTo(caseDetails));
    }

    @Test
    void updateCaseAsCaseworker() {
        CcdCaseType ccdCaseType = CcdCaseType.GRANT_OF_REPRESENTATION;

        CaseDetails caseDetails = Mockito.mock(CaseDetails.class);
        StartEventResponse startEventResponse = StartEventResponse.builder().build();
        EventId eventId = EventId.SERVICE_REQUEST_PAYMENT_SUCCESS;

        when(coreCaseDataApi.startEventForCaseWorker(
                eq(AUTHORISATION),
                eq(SERVICE_AUTHORISATION),
                eq(USER_ID),
                eq(JurisdictionId.PROBATE.name()),
                eq(ccdCaseType.getName()),
                any(),
                any())).thenReturn(startEventResponse);

        GrantOfRepresentationData grantOfRepresentationData = GrantOfRepresentationData.builder()
                .build();

        when(coreCaseDataApi.submitEventForCaseWorker(
                eq(AUTHORISATION),
                eq(SERVICE_AUTHORISATION),
                eq(USER_ID),
                eq(JurisdictionId.PROBATE.name()),
                eq(ccdCaseType.getName()),
                eq("1"),
                eq(false),
                any(CaseDataContent.class))).thenReturn(caseDetails);

        SecurityDTO securityDTO = SecurityDTO.builder()
                .authorisation(AUTHORISATION)
                .serviceAuthorisation(SERVICE_AUTHORISATION)
                .userId(USER_ID)
                .build();


        CaseDetails actualCaseDetails = ccdClientApi.updateCaseAsCaseworker(
                CcdCaseType.GRANT_OF_REPRESENTATION,
                "1",
                grantOfRepresentationData,
                eventId,
                securityDTO,
                "Description",
                "Summary");

        assertThat(actualCaseDetails, equalTo(caseDetails));
    }


    @Test
    void updateCaseAsCaseworkerGeneric() {
        CcdCaseType ccdCaseType = CcdCaseType.GRANT_OF_REPRESENTATION;

        CaseDetails caseDetails = Mockito.mock(CaseDetails.class);
        StartEventResponse startEventResponse = StartEventResponse.builder().build();
        EventId eventId = EventId.SCHEDULED_UPDATE_GRANT_DELAY_NOTIFICATION_SENT;

        when(coreCaseDataApi.startEventForCaseWorker(
                eq(AUTHORISATION),
                eq(SERVICE_AUTHORISATION),
                eq(USER_ID),
                eq(JurisdictionId.PROBATE.name()),
                eq(ccdCaseType.getName()),
                any(),
                any())).thenReturn(startEventResponse);

        GrantOfRepresentationData grantOfRepresentationData = GrantOfRepresentationData.builder()
                .build();

        when(coreCaseDataApi.submitEventForCaseWorker(
                eq(AUTHORISATION),
                eq(SERVICE_AUTHORISATION),
                eq(USER_ID),
                eq(JurisdictionId.PROBATE.name()),
                eq(ccdCaseType.getName()),
                eq("1"),
                eq(false),
                any(CaseDataContent.class))).thenReturn(caseDetails);

        SecurityDTO securityDTO = SecurityDTO.builder()
                .authorisation(AUTHORISATION)
                .serviceAuthorisation(SERVICE_AUTHORISATION)
                .userId(USER_ID)
                .build();


        CaseDetails actualCaseDetails = ccdClientApi.updateCaseAsCaseworker(
                CcdCaseType.GRANT_OF_REPRESENTATION,
                "1",
                grantOfRepresentationData,
                eventId,
                securityDTO);

        assertThat(actualCaseDetails, equalTo(caseDetails));
    }
}
