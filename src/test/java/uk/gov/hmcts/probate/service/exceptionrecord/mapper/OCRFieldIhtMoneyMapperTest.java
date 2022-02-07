package uk.gov.hmcts.probate.service.exceptionrecord.mapper;

import uk.gov.hmcts.probate.exception.OCRMappingException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class OCRFieldIhtMoneyMapperTest {

    OCRFieldIhtMoneyMapper ocrFieldIhtMoneyMapper = new OCRFieldIhtMoneyMapper();

    private static final String MONETARY_TEST_VALUE_INPUT = "125.50";
    private static final Long MONETARY_TEST_VALUE_PENNIES = 12550L;
    private static final String MONETARY_TEST_UNKNOWN_VALUE = "Twenty two pounds";
    
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testPoundsToPennies() {
        Long response = ocrFieldIhtMoneyMapper.poundsToPennies(MONETARY_TEST_VALUE_INPUT);
        assertEquals(MONETARY_TEST_VALUE_PENNIES, response);
    }

    @Test
    public void testExceptionForToPenniesNotNumeric() throws Exception {
        expectedEx.expect(OCRMappingException.class);
        expectedEx
            .expectMessage("Monetary field '" + MONETARY_TEST_UNKNOWN_VALUE + "' could not be converted to a number");
        Long response = ocrFieldIhtMoneyMapper.poundsToPennies(MONETARY_TEST_UNKNOWN_VALUE);
    }
}
