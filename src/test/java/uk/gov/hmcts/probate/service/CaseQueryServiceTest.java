package uk.gov.hmcts.probate.service;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.probate.config.CCDDataStoreAPIConfiguration;
import uk.gov.hmcts.probate.exception.CaseMatchingException;
import uk.gov.hmcts.probate.insights.AppInsights;
import uk.gov.hmcts.probate.model.ccd.raw.request.CaseData;
import uk.gov.hmcts.probate.model.ccd.raw.request.CaseDetails;
import uk.gov.hmcts.probate.model.ccd.raw.request.ReturnedCases;
import uk.gov.hmcts.probate.service.evidencemanagement.header.HttpHeadersFactory;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class CaseQueryServiceTest {

    private static final String[] LAST_MODIFIED = {"2018", "1", "1", "0", "0", "0", "0"};

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private HttpHeadersFactory headers;

    @Mock
    private AppInsights appInsights;

    @Mock
    private CCDDataStoreAPIConfiguration ccdDataStoreAPIConfiguration;

    @InjectMocks
    private CaseQueryService caseQueryService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(headers.getAuthorizationHeaders())
                .thenReturn(new HttpHeaders());

        when(ccdDataStoreAPIConfiguration.getHost()).thenReturn("http://localhost");
        when(ccdDataStoreAPIConfiguration.getCaseMatchingPath()).thenReturn("/path");

        CaseData caseData = CaseData.builder()
                .deceasedSurname("Smith")
                .build();
        List<CaseDetails> caseList = new ImmutableList.Builder<CaseDetails>().add(new CaseDetails(caseData,
                LAST_MODIFIED, 1L))
                .build();
        ReturnedCases returnedCases = new ReturnedCases(caseList);

        when(restTemplate.postForObject(any(), any(), any())).thenReturn(returnedCases);

        doNothing().when(appInsights).trackEvent(any(), anyString());
    }

    @Test
    public void findCasesWithDatedDocumentReturnsCaseList() throws IOException {
        List<CaseDetails> cases = caseQueryService.findCasesWithDatedDocument("Test",
                "testDate");

        assertEquals(1, cases.size());
        assertThat(cases.get(0).getId(), is(1L));
        assertEquals("Smith", cases.get(0).getData().getDeceasedSurname());
    }

    @Test
    public void findCasesWithDateRangeReturnsCaseList() {
        List<CaseDetails> cases = caseQueryService.findCaseStateWithinTimeFrame("digitalGrant",
                "2019-02-05", "2019-02-22");

        assertEquals(1, cases.size());
        assertThat(cases.get(0).getId(), is(1L));
        assertEquals("Smith", cases.get(0).getData().getDeceasedSurname());
    }

    @Test
    public void testHttpExceptionCaughtWithBadPost() {
        when(restTemplate.postForObject(any(), any(), any())).thenThrow(HttpClientErrorException.class);

        Assertions.assertThatThrownBy(() -> caseQueryService.findCasesWithDatedDocument("test", "testDate"))
                .isInstanceOf(CaseMatchingException.class);
    }
}