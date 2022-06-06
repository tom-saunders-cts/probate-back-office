package uk.gov.hmcts.probate.service.exceptionrecord.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.probate.model.exceptionrecord.ExceptionRecordOCRFields;
import uk.gov.hmcts.probate.service.ExceptedEstateDateOfDeathChecker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
public class OCRFieldDeceasedHadLateSpouseOrCivilPartnerMapperTest {

    private static final String POST_EE_DECEASED_DATE_OF_DEATH = "01012022";

    @Mock
    ExceptedEstateDateOfDeathChecker exceptedEstateDateOfDeathChecker;

    @BeforeEach
    public void setUp() {
        when(exceptedEstateDateOfDeathChecker
            .isOnOrAfterSwitchDate(POST_EE_DECEASED_DATE_OF_DEATH)).thenReturn(true);
    }

    @InjectMocks
    OCRFieldDeceasedHadLateSpouseOrCivilPartnerMapper ocrFieldDeceasedHadLateSpouseOrCivilPartnerMapper
        = new OCRFieldDeceasedHadLateSpouseOrCivilPartnerMapper();



    @Test
    public void shouldReturnTrue() {
        ExceptionRecordOCRFields ocrFields = ExceptionRecordOCRFields.builder()
            .formVersion("2")
            .deceasedDateOfDeath(POST_EE_DECEASED_DATE_OF_DEATH)
            .deceasedMartialStatus("widowed")
            .iht400421Completed("false")
            .iht207Completed("false")
            .build();
        Boolean response = ocrFieldDeceasedHadLateSpouseOrCivilPartnerMapper
            .decasedHadLateSpouseOrCivilPartner(ocrFields);
        assertTrue(response);
    }

    @Test
    public void shouldReturnFalse() {
        ExceptionRecordOCRFields ocrFields = ExceptionRecordOCRFields.builder()
            .formVersion("2")
            .deceasedDateOfDeath(POST_EE_DECEASED_DATE_OF_DEATH)
            .deceasedMartialStatus("divorced")
            .iht400421Completed("false")
            .iht207Completed("false")
            .build();
        Boolean response = ocrFieldDeceasedHadLateSpouseOrCivilPartnerMapper
            .decasedHadLateSpouseOrCivilPartner(ocrFields);
        assertFalse(response);
    }

    @Test
    public void shouldReturnNull() {
        ExceptionRecordOCRFields ocrFields = ExceptionRecordOCRFields.builder()
            .build();
        Boolean response = ocrFieldDeceasedHadLateSpouseOrCivilPartnerMapper
            .decasedHadLateSpouseOrCivilPartner(ocrFields);
        assertNull(response);
    }
}
