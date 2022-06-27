package uk.gov.hmcts.probate.service.ocr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.probate.model.ocr.OCRField;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NonMandatoryFieldsValidatorTest {
    private OCRFieldTestUtils ocrFieldTestUtils = new OCRFieldTestUtils();

    @Mock
    private OcrEmailValidator ocrEmailValidator;

    @InjectMocks
    private NonMandatoryFieldsValidator nonMandatoryFieldsValidator;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFlagSolsWillTypeCaseWarningPA1A() {
        List<OCRField> ocrFields = ocrFieldTestUtils.addAllMandatoryGORSolicitorFields();

        ocrFields.add(OCRField.builder().name("solsWillType").value("Grant I think").description("Will Type").build());
        List<String> warningsResponse = nonMandatoryFieldsValidator.ocrToCCDNonMandatoryWarnings(ocrFields,
            FormType.PA1A);
        assertEquals("The form has been flagged as a Solictor case.", warningsResponse.get(0));
        assertEquals(
            "An application type and/or reason has been provided, this will need to be reviewed as it will not be "
                + "mapped to the case.", warningsResponse.get(1));
        assertEquals(2, warningsResponse.size());
    }

    @Test
    void testFlagSolsWillTypeReasonCaseWarningPA1A() {
        List<OCRField> ocrFields = ocrFieldTestUtils.addAllMandatoryGORSolicitorFields();
        ocrFields.add(
            OCRField.builder().name("solsWillTypeReason").value("Because they died").description("Will Type").build());
        List<String> warningsResponse = nonMandatoryFieldsValidator.ocrToCCDNonMandatoryWarnings(ocrFields,
            FormType.PA1A);
        assertEquals("The form has been flagged as a Solictor case.", warningsResponse.get(0));
        assertEquals(
            "An application type and/or reason has been provided, this will need to be reviewed as it will not be "
                + "mapped to the case.", warningsResponse.get(1));
        assertEquals(2, warningsResponse.size());
    }

    @Test
    void testFlagAsSolicitorCaseWarningPA1A() {
        List<OCRField> ocrFields = ocrFieldTestUtils.addAllMandatoryIntestacySolicitorFields();

        assertEquals("The form has been flagged as a Solictor case.",
            nonMandatoryFieldsValidator.ocrToCCDNonMandatoryWarnings(ocrFields, FormType.PA1A).get(0));
    }

    @Test
    void testFlagAsSolicitorCaseWarningPA8A() {
        List<OCRField> ocrFields = ocrFieldTestUtils.addAllMandatoryCaveatSolicitorFields();
        List<String> warningsResult = nonMandatoryFieldsValidator.ocrToCCDNonMandatoryWarnings(ocrFields,
            FormType.PA8A);
        assertEquals(1, warningsResult.size());
        assertEquals("The form has been flagged as a Solictor case.", warningsResult.get(0));
    }

    @Test
    void testEmailFieldWarning() {
        List<OCRField> ocrFields = new ArrayList<>();
        final OCRField field = OCRField
            .builder()
            .name("primaryApplicantEmailAddress")
            .value("invalidEmailAddress")
            .build();
        ocrFields.add(field);
        nonMandatoryFieldsValidator.ocrToCCDNonMandatoryWarnings(ocrFields, FormType.PA8A);
        verify(ocrEmailValidator, times(1)).validateField(ocrFields);
    }

    @Test
    void testFlagAsSolicitorCaseWarningPA1P() {
        List<OCRField> ocrFields = ocrFieldTestUtils.addAllMandatoryGORSolicitorFields();
        List<String> warningsResult = nonMandatoryFieldsValidator.ocrToCCDNonMandatoryWarnings(ocrFields,
            FormType.PA1P);
        assertEquals("The form has been flagged as a Solictor case.", warningsResult.get(0));
    }

    @Test
    void testFlagSolsWillTypeCaseWarningPA1P() {
        List<OCRField> ocrFields = ocrFieldTestUtils.addAllMandatoryGORSolicitorFields();
        ocrFields.add(OCRField.builder().name("solsWillType").value("Grant I think").description("Will Type").build());
        List<String> warningsResponse = nonMandatoryFieldsValidator.ocrToCCDNonMandatoryWarnings(ocrFields,
            FormType.PA1P);

        assertEquals("The form has been flagged as a Solictor case.", warningsResponse.get(0));
        assertEquals(
            "An application type and/or reason has been provided, this will need to be reviewed as it will not be "
                + "mapped to the case.", warningsResponse.get(1));
        assertEquals(2, warningsResponse.size());
    }

    @Test
    void testFlagSolsWillTypeReasonCaseWarningPA1P() {
        List<OCRField> ocrFields = ocrFieldTestUtils.addAllMandatoryGORSolicitorFields();
        ocrFields.add(
            OCRField.builder().name("solsWillTypeReason").value("Because they died").description("Will Type").build());
        List<String> warningsResponse = nonMandatoryFieldsValidator.ocrToCCDNonMandatoryWarnings(ocrFields,
            FormType.PA1P);

        assertEquals("The form has been flagged as a Solictor case.", warningsResponse.get(0));
        assertEquals(
            "An application type and/or reason has been provided, this will need to be reviewed as it will not be "
                + "mapped to the case.", warningsResponse.get(1));
        assertEquals(2, warningsResponse.size());
    }

}
