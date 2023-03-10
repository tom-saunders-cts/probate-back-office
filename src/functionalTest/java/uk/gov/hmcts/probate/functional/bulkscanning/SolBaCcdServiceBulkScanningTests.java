package uk.gov.hmcts.probate.functional.bulkscanning;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.probate.functional.IntegrationTestBase;
import uk.gov.hmcts.probate.transformer.CaveatCallbackResponseTransformer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@RunWith(SpringIntegrationSerenityRunner.class)
public class SolBaCcdServiceBulkScanningTests extends IntegrationTestBase {

    protected static final String S_S_DOES_NOT_APPEAR_TO_BE_A_VALID_EMAIL_ADDRESS =
        "%s (%s) does not appear to be a valid email address";
    private static final String SUCCESS = "SUCCESS";
    private static final String WARNINGS = "WARNINGS";
    private static final String DOB_MISSING = "Deceased date of birth (deceasedDateOfBirth) is mandatory.";
    private static final String DOD_MISSING = "Deceased date of death (deceasedDateOfDeath) is mandatory.";
    private static final String SOLICITOR_EMAIL_MISSING = "Solictor email address (solsSolicitorEmail) is mandatory.";
    private static final String SOLICITOR_FLAG = "The form has been flagged as a Solictor case.";
    private static final String VALIDATE_OCR_DATA = "/forms/%s/validate-ocr";
    private static final String PA1A = "PA1A";
    private static final String PA1P = "PA1P";
    private static final String PA8A = "PA8A";
    private static final String VALIDATE_OCR_DATA_UNKNOWN_FORM_TYPE = "/forms/XZY/validate-ocr";
    private static final String TRANSFORM_EXCEPTON_RECORD = "/transform-scanned-data";
    private static final String UPDATE_CASE_FROM_EXCEPTON_RECORD = "/update-case";
    private static final DateTimeFormatter CCD_DATE_FORMAT = CaveatCallbackResponseTransformer.dateTimeFormatter;
    private String jsonRequest;
    private String jsonResponse;

    @Before
    public void setUp() {
        initialiseConfig();
    }

    private void validateOCRDataPostSuccess(String formName, String bodyText, String containsText,
                                            String warningMessage, int warningSize, int warningItem) {
        RestAssured.given()
            .config(config)
            .relaxedHTTPSValidation()
            .headers(utils.getHeadersWithCaseworkerUser())
            .body(bodyText)
            .when().post(String.format(VALIDATE_OCR_DATA, formName))
            .then().assertThat().statusCode(200)
            .and().body("warnings", hasSize(warningSize))
            .and().body("warnings[" + warningItem + "]", equalTo(warningMessage))
            .and().body(containsString(containsText));
    }

    private void validateOCRDataPostError(String bodyText) {
        RestAssured.given()
            .config(config)
            .relaxedHTTPSValidation()
            .headers(utils.getHeadersWithCaseworkerUser())
            .body(bodyText)
            .when().post(VALIDATE_OCR_DATA_UNKNOWN_FORM_TYPE)
            .then().assertThat().statusCode(404);
    }

    private void transformExceptionPostSuccess(String bodyText, String containsText) {
        String actualResponse = RestAssured.given()
                .config(config)
                .relaxedHTTPSValidation()
                .headers(utils.getHeadersWithCaseworkerUser())
                .body(bodyText)
                .when().post(TRANSFORM_EXCEPTON_RECORD)
                .then().assertThat().statusCode(200)
                .and().extract().body().asPrettyString();
        JSONAssert.assertEquals(containsText, actualResponse, JSONCompareMode.STRICT);
    }

    private void updateCaseFromExceptionPostSuccess(String bodyText, String containsText) {
        RestAssured.given()
            .config(config)
            .relaxedHTTPSValidation()
            .headers(utils.getHeadersWithCaseworkerUser())
            .body(bodyText)
            .when().post(UPDATE_CASE_FROM_EXCEPTON_RECORD)
            .then().assertThat().statusCode(422)
            .and().body(containsString(containsText));
    }

    private JsonPath fetchJsonPathUpdatedCaveatDetailsFromCaseFromException(String bodyText) throws IOException {
        final ValidatableResponse response = RestAssured.given()
            .config(config)
            .relaxedHTTPSValidation()
            .headers(utils.getHeadersWithCaseworkerUser())
            .body(bodyText)
            .when().post(UPDATE_CASE_FROM_EXCEPTON_RECORD)
            .then().assertThat().statusCode(200);
        return response.extract().body().jsonPath();
    }

    @Test
    public void testAllMandatoryFieldsPresentReturnNoWarnings() throws IOException {
        jsonRequest = utils.getJsonFromFile("expectedOCRDataAllMandatoryFields.json");
        validateOCRDataPostSuccess(PA1P, jsonRequest, SUCCESS, null, 0, 0);
    }

    @Test
    public void testMissingMandatoryFieldsReturnWarnings() throws IOException {
        jsonRequest = utils.getJsonFromFile("expectedOCRDataMissingMandatoryFields.json");
        validateOCRDataPostSuccess(PA1P, jsonRequest, WARNINGS, DOB_MISSING, 2, 0);
        validateOCRDataPostSuccess(PA1P, jsonRequest, WARNINGS, DOD_MISSING, 2, 1);
    }

    @Test
    public void testMissingSolicitorEmailPA1AReturnsWarning() throws IOException {
        jsonRequest = utils.getJsonFromFile("expectedOCRDataMissingMandatoryFieldsSolPA1.json");
        validateOCRDataPostSuccess(PA1A, jsonRequest, WARNINGS, SOLICITOR_EMAIL_MISSING, 2, 0);
        validateOCRDataPostSuccess(PA1A, jsonRequest, WARNINGS, SOLICITOR_FLAG, 2, 1);
    }

    @Test
    public void testMissingSolicitorEmailPA1PReturnsWarning() throws IOException {
        jsonRequest = utils.getJsonFromFile("expectedOCRDataMissingMandatoryFieldsSolPA1.json");
        validateOCRDataPostSuccess(PA1P, jsonRequest, WARNINGS, SOLICITOR_EMAIL_MISSING, 2, 0);
        validateOCRDataPostSuccess(PA1P, jsonRequest, WARNINGS, SOLICITOR_FLAG, 2, 1);
    }

    @Test
    public void testInvalidEmailFieldsReturnWarnings() throws IOException {
        jsonRequest = utils.getJsonFromFile("expectedOCRDataAllInvalidEmailAddress.json");
        validateOCRDataPostSuccess(PA1P, jsonRequest, WARNINGS,
            format(S_S_DOES_NOT_APPEAR_TO_BE_A_VALID_EMAIL_ADDRESS, "Primary applicant email address",
                "primaryApplicantEmailAddress"), 3, 0);
        validateOCRDataPostSuccess(PA1P, jsonRequest, WARNINGS,
            format(S_S_DOES_NOT_APPEAR_TO_BE_A_VALID_EMAIL_ADDRESS, "Caveator email address", "caveatorEmailAddress"),
            3, 1);
        validateOCRDataPostSuccess(PA1P, jsonRequest, WARNINGS,
            format(S_S_DOES_NOT_APPEAR_TO_BE_A_VALID_EMAIL_ADDRESS, "Solicitor email address", "solsSolicitorEmail"),
                3, 2);
    }

    @Test
    public void testInvalidFormTypeReturnError() throws IOException {
        jsonRequest = utils.getJsonFromFile("expectedOCRDataAllMandatoryFields.json");
        validateOCRDataPostError(jsonRequest);
    }

    @Test
    public void testTransformPA8AReturnSuccessfulJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordPA8A.json");
        jsonResponse = utils.getJsonFromFile("expectedBulkScanTransformExceptionRecordOutputPA8A.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformPA8Av2ReturnSuccessfulJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordPA8Av2.json");
        jsonResponse = utils.getJsonFromFile("expectedBulkScanTransformExceptionRecordOutputPA8Av2.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
            applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformCombinedCitizenPA8Av2ReturnSuccessfulJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CaveatCallbackResponseTransformer.dateTimeFormatter);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordCombCitizenPA8Av2.json");
        jsonResponse = utils.getJsonFromFile(
                "expectedBulkScanTransformExceptionRecordOutputCombCitizenPA8Av2.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformCombinedSolicitorPA8AReturnSuccessfulJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CaveatCallbackResponseTransformer.dateTimeFormatter);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordCombSolicitorPA8A.json");
        jsonResponse = utils.getJsonFromFile(
                "expectedBulkScanTransformExceptionRecordOutputCombSolicitorPA8A.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformPA1PReturnSuccessfulJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordPA1P.json");
        jsonResponse = utils.getJsonFromFile("expectedBulkScanTransformExceptionRecordOutputPA1P.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformCombinedCitizenPA1PReturnSuccessfulJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordCombCitizenPA1P.json");
        jsonResponse = utils.getJsonFromFile(
                "expectedBulkScanTransformExceptionRecordOutputCombCitizenPA1P.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformCombinedSolicitorPA1PReturnSuccessfulJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordCombSolicitorPA1P.json");
        jsonResponse = utils.getJsonFromFile(
                "expectedBulkScanTransformExceptionRecordOutputCombSolicitorPA1P.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformSolicitorPA1PReturnSuccessfulJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordSolicitorPA1P.json");
        jsonResponse = utils.getJsonFromFile(
                "expectedBulkScanTransformExceptionRecordOutputSolicitorPA1P.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformMissingMandatoryPA1PReturnUnprocessedJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordMissingMandatoryPA1P.json");
        jsonResponse = utils.getJsonFromFile("expectedBulkScanExceptionRecordMissingMandatoryPA1P.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostUnprocessed(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformMissingMandatoryPA1AReturnUnprocessedJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordMissingMandatoryPA1A.json");
        jsonResponse = utils.getJsonFromFile("expectedBulkScanExceptionRecordMissingMandatoryPA1A.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostUnprocessed(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformCitizenPA1PReturnUnprocessedJSON() throws IOException {
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordPA1PUnprocessed.json");
        jsonResponse = utils.getJsonFromFile(
                "expectedBulkScanTransformExceptionRecordOutputPA1PUnprocessed.json");
        transformExceptionPostUnprocessed(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformPA1AReturnSuccessfulJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordPA1A.json");
        jsonResponse = utils.getJsonFromFile("expectedBulkScanTransformExceptionRecordOutputPA1A.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testUpdateCaseExtendCaveatPA8AReturnSuccessfulJSON() throws IOException {
        final String expiryDate7DaysFromNow =
            LocalDate.now().plusDays(7).format(CaveatCallbackResponseTransformer.dateTimeFormatter);
        final String expectedExpiryDate6MonthsFromNow =
            LocalDate.now().plusDays(7).plusMonths(6).format(CaveatCallbackResponseTransformer.dateTimeFormatter);
        final String expiryDate = "\"expiryDate\":\"" + expiryDate7DaysFromNow + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanUpdateCaseExceptionRecordExtendExpiryPA8A.json");
        jsonRequest = jsonRequest.replaceAll("\"expiryDate\":\"[0-9-]+\"", expiryDate);
        final JsonPath jsonPath = fetchJsonPathUpdatedCaveatDetailsFromCaseFromException(jsonRequest);

        // Unable to use static file as documents are generated in the response, picking out specific values instead.
        Assert.assertEquals("Correct applicationType", "Personal",
            jsonPath.get("case_update_details.case_data.applicationType"));
        Assert.assertEquals("Correct paperForm", "Yes",
            jsonPath.get("case_update_details.case_data.paperForm"));
        Assert.assertEquals("Correct expiry date", expectedExpiryDate6MonthsFromNow,
            jsonPath.get("case_update_details.case_data.expiryDate"));
        Assert.assertEquals("Correct registry", "ctsc",
            jsonPath.get("case_update_details.case_data.registryLocation"));

        // Checked Scanned Documents
        Assert.assertEquals("Correct number scanned docs", 2,
            jsonPath.getList("case_update_details.case_data.scannedDocuments").size());
        Assert.assertEquals("Correct DCN Scan Doc 1", "19365040100100002",
            jsonPath.get("case_update_details.case_data.scannedDocuments[0].value.controlNumber"));
        Assert.assertEquals("Correct DCN Scan Doc 2", "123135453645",
            jsonPath.get("case_update_details.case_data.scannedDocuments[1].value.controlNumber"));

        // Checked Generated Notification Documents
        Assert.assertEquals("Correct number generated notifications", 2,
            jsonPath.getList("case_update_details.case_data.notificationsGenerated").size());
        Assert.assertEquals("Correct DocumentType Doc 1", "sentEmail",
            jsonPath.get("case_update_details.case_data.notificationsGenerated[0].value.DocumentType"));
        Assert.assertEquals("Correct DocumentType Doc 2", "sentEmail",
            jsonPath.get("case_update_details.case_data.notificationsGenerated[1].value.DocumentType"));
    }

    @Test
    public void testUpdateCaseExtendCaveatPA8AReturnExpiredErrorJSON() throws IOException {
        jsonRequest = utils.getJsonFromFile("bulkScanUpdateCaseExceptionRecordExtendExpiryPA8A.json");
        jsonResponse = utils.getJsonFromFile(
                "expectedBulkScanUpdateCaseExceptionRecordExpiredCaveatErrorPA8A.json");
        updateCaseFromExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testUpdateCaseExtendCaveatPA8AReturnOutsideOneMonthExpiryErrorJSON() throws IOException {
        final String expiryDate3MonthsFromNow =
            LocalDate.now().plusMonths(3).format(CaveatCallbackResponseTransformer.dateTimeFormatter);
        final String expireDate = "\"expiryDate\":\"" + expiryDate3MonthsFromNow + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanUpdateCaseExceptionRecordExtendExpiryPA8A.json");
        jsonRequest = jsonRequest.replaceAll("\"expiryDate\":\"[0-9-]+\"", expireDate);
        jsonResponse = utils.getJsonFromFile(
                    "expectedBulkScanUpdateCaseExceptionRecordExpiryOutsideOneMonthErrorPA8A.json");
        updateCaseFromExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformCombinedCitizenPA1AReturnSuccessfulJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordCombCitizenPA1A.json");
        jsonResponse = utils.getJsonFromFile(
                "expectedBulkScanTransformExceptionRecordOutputCombCitizenPA1A.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformCombinedSolicitorPA1AReturnSuccessfulJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordCombSolicitorPA1A.json");
        jsonResponse = utils.getJsonFromFile(
                "expectedBulkScanTransformExceptionRecordOutputCombSolicitorPA1A.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformPA8AReturnTransformErrorJSON() throws IOException {
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordError.json");
        jsonResponse = utils.getJsonFromFile("expectedBulkScanTransformExceptionRecordOutputError.json");
        transformExceptionPostUnprocessed(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformSolicitorPA8AReturnTransformErrorJSON() throws IOException {
        jsonRequest = utils.getJsonFromFile("bulkScanTransformSolicitorExceptionRecordError.json");
        jsonResponse = utils.getJsonFromFile("expectedBulkScanTransformExceptionRecordOutputError.json");
        transformExceptionPostUnprocessed(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformSolicitorPA8AReturnTransformErrorAutomatedJSON() throws IOException {
        jsonRequest = utils.getJsonFromFile("bulkScanTransformSolicitorExceptionRecordErrorAutomated.json");
        jsonResponse = utils.getJsonFromFile("expectedBulkScanTransformExceptionRecordOutputError.json");
        transformExceptionPostUnprocessed(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformPA8AReturnSuccessfulAutomatedJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordPA8AAutomated.json");
        jsonResponse = utils.getJsonFromFile("expectedBulkScanTransformExceptionRecordOutputPA8A.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformCombinedCitizenPA8Av2ReturnSuccessfulAutomatedJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CaveatCallbackResponseTransformer.dateTimeFormatter);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordCombCitizenPA8Av2Automated.json");
        jsonResponse = utils.getJsonFromFile(
                "expectedBulkScanTransformExceptionRecordOutputCombCitizenPA8Av2.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformCombinedSolicitorPA8AReturnSuccessfulAutomatedJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CaveatCallbackResponseTransformer.dateTimeFormatter);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordCombSolicitorPA8AAutomated.json");
        jsonResponse = utils.getJsonFromFile(
                "expectedBulkScanTransformExceptionRecordOutputCombSolicitorPA8A.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformPA1PReturnSuccessfulAutomatedJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordPA1PAutomated.json");
        jsonResponse = utils.getJsonFromFile("expectedBulkScanTransformExceptionRecordOutputPA1P.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformCombinedCitizenPA1PReturnSuccessfulAutomatedJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordCombCitizenPA1PAutomated.json");
        jsonResponse = utils.getJsonFromFile(
                "expectedBulkScanTransformExceptionRecordOutputCombCitizenPA1P.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformCombinedSolicitorPA1PReturnSuccessfulAutomatedJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordCombSolicitorPA1PAutomated.json");
        jsonResponse = utils.getJsonFromFile(
                "expectedBulkScanTransformExceptionRecordOutputCombSolicitorPA1P.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformSolicitorPA1PSingleExecReturnSuccessfulAutomatedJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordSolicitorPA1PAutomated.json");
        jsonResponse = utils.getJsonFromFile(
                "expectedBulkScanTransformExceptionRecordOutputSolicitorPA1P.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformPA1AReturnSuccessfulAutomatedJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordPA1AAutomated.json");
        jsonResponse = utils.getJsonFromFile("expectedBulkScanTransformExceptionRecordOutputPA1A.json");
        jsonResponse = jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"",
                applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testUpdateCaseExtendCaveatPA8AReturnSuccessfulAutomatedJSON() throws IOException {
        final String expiryDate7DaysFromNow = LocalDate.now().plusDays(7).format(
            CaveatCallbackResponseTransformer.dateTimeFormatter);
        final String expiryDate = "\"expiryDate\":\"" + expiryDate7DaysFromNow + "\"";
        jsonRequest = utils.getJsonFromFile("bulkScanUpdateCaseExceptionRecordExtendExpiryPA8AAutomated.json");
        jsonRequest = jsonRequest.replaceAll("\"expiryDate\":\"[0-9-]+\"", expiryDate);
        JsonPath jsonPath = fetchJsonPathUpdatedCaveatDetailsFromCaseFromException(jsonRequest);
        String expectedExpiryDate6MonthsFromNow = LocalDate.now().plusDays(7).plusMonths(6).format(
                CaveatCallbackResponseTransformer.dateTimeFormatter);
        // Unable to use static file as documents are generated in the response, picking out specific values instead.
        Assert.assertEquals("Correct applicationType", "Personal",
            jsonPath.get("case_update_details.case_data.applicationType"));
        Assert.assertEquals("Correct paperForm", "Yes",
            jsonPath.get("case_update_details.case_data.paperForm"));
        Assert.assertEquals("Correct expiry date", expectedExpiryDate6MonthsFromNow,
            jsonPath.get("case_update_details.case_data.expiryDate"));
        Assert.assertEquals("Correct registry", "ctsc",
            jsonPath.get("case_update_details.case_data.registryLocation"));

        // Checked Scanned Documents
        Assert.assertEquals("Correct number scanned docs", 2,
            jsonPath.getList("case_update_details.case_data.scannedDocuments").size());
        Assert.assertEquals("Correct DCN Scan Doc 1", "19365040100100002",
            jsonPath.get("case_update_details.case_data.scannedDocuments[0].value.controlNumber"));
        Assert.assertEquals("Correct DCN Scan Doc 2", "123135453645",
            jsonPath.get("case_update_details.case_data.scannedDocuments[1].value.controlNumber"));

        // Checked Generated Notification Documents
        Assert.assertEquals("Correct number generated notifications", 2,
            jsonPath.getList("case_update_details.case_data.notificationsGenerated").size());
        Assert.assertEquals("Correct DocumentType Doc 1", "sentEmail",
            jsonPath.get("case_update_details.case_data.notificationsGenerated[0].value.DocumentType"));
        Assert.assertEquals("Correct DocumentType Doc 2", "sentEmail",
            jsonPath.get("case_update_details.case_data.notificationsGenerated[1].value.DocumentType"));
    }

    @Test
    public void testUpdateCaseExtendCaveatPA8AReturnExpiredErrorAutomatedJSON() throws IOException {
        jsonRequest =
            utils.getJsonFromFile("bulkScanUpdateCaseExceptionRecordExtendExpiryPA8AAutomated.json");
        jsonResponse =
            utils.getJsonFromFile("expectedBulkScanUpdateCaseExceptionRecordExpiredCaveatErrorPA8A.json");
        updateCaseFromExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testUpdateCaseExtendCaveatPA8AReturnOutsideOneMonthExpiryErrorAutomatedJSON() throws IOException {
        final String expiryDate3MonthsFromNow = LocalDate.now().plusMonths(3).format(
            CaveatCallbackResponseTransformer.dateTimeFormatter);
        final String expireDate = "\"expiryDate\":\"" + expiryDate3MonthsFromNow + "\"";
        jsonRequest =
            utils.getJsonFromFile("bulkScanUpdateCaseExceptionRecordExtendExpiryPA8AAutomated.json");
        jsonRequest =
            jsonRequest.replaceAll("\"expiryDate\":\"[0-9-]+\"", expireDate);
        jsonResponse =
            utils.getJsonFromFile(
                "expectedBulkScanUpdateCaseExceptionRecordExpiryOutsideOneMonthErrorPA8A.json");
        updateCaseFromExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformCombinedCitizenPA1AReturnSuccessfulAutomatedJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest =
            utils.getJsonFromFile("bulkScanTransformExceptionRecordCombCitizenPA1AAutomated.json");
        jsonResponse =
            utils.getJsonFromFile("expectedBulkScanTransformExceptionRecordOutputCombCitizenPA1A.json");
        jsonResponse =
            jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"", applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformCombinedSolicitorPA1AReturnSuccessfulAutomatedJSON() throws IOException {
        final String currentDate = LocalDate.now().format(CCD_DATE_FORMAT);
        final String applicationSubmittedDate = "\"applicationSubmittedDate\":\"" + currentDate + "\"";
        jsonRequest =
            utils.getJsonFromFile("bulkScanTransformExceptionRecordCombSolicitorPA1AAutomated.json");
        jsonResponse =
            utils.getJsonFromFile("expectedBulkScanTransformExceptionRecordOutputCombSolicitorPA1A.json");
        jsonResponse =
            jsonResponse.replaceAll("\"applicationSubmittedDate\":\"[0-9-]+\"", applicationSubmittedDate);
        transformExceptionPostSuccess(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformPA8AReturnTransformErrorAutomatedJSON() throws IOException {
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordErrorAutomated.json");
        jsonResponse = utils.getJsonFromFile("expectedBulkScanTransformExceptionRecordOutputError.json");
        transformExceptionPostUnprocessed(jsonRequest, jsonResponse);
    }

    @Test
    public void testTransformPA1AReturnTransformForbiddenJSON() throws IOException {
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordCombSolicitorPA1AAutomated.json");
        transformExceptionPostForbidden(jsonRequest);
    }

    @Test
    public void testUpdatePA1AReturnTransformForbiddenJSON() throws IOException {
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordCombSolicitorPA1AAutomated.json");
        updateExceptionPostForbidden(jsonRequest);
    }

    @Test
    public void testTransformPA8AReturnTransformForbiddenJSON() throws IOException {
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordPA8A.json");
        transformExceptionPostForbidden(jsonRequest);
    }

    @Test
    public void testUpdatePA8AReturnTransformForbiddenJSON() throws IOException {
        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordPA8A.json");
        updateExceptionPostForbidden(jsonRequest);
    }

    private void transformExceptionPostUnprocessed(String bodyText, String containsText) {
        RestAssured.given()
            .config(config)
            .relaxedHTTPSValidation()
            .headers(utils.getHeadersWithCaseworkerUser())
            .body(bodyText)
            .when().post(TRANSFORM_EXCEPTON_RECORD)
            .then().assertThat().statusCode(422)
            .and().body(containsString(containsText));
    }

    private void transformExceptionPostForbidden(String bodyText) {
        RestAssured.given()
            .config(config)
            .relaxedHTTPSValidation()
            .headers(utils.getHeadersForUnauthorisedService())
            .body(bodyText)
            .when().post(TRANSFORM_EXCEPTON_RECORD)
            .then().assertThat().statusCode(403);
    }


    private void updateExceptionPostForbidden(String bodyText) {
        RestAssured.given()
            .config(config)
            .relaxedHTTPSValidation()
            .headers(utils.getHeadersForUnauthorisedService())
            .body(bodyText)
            .when().post(UPDATE_CASE_FROM_EXCEPTON_RECORD)
            .then().assertThat().statusCode(403);
    }


    //    @Test
    //    public void test401test() {
    //        jsonRequest = utils.getJsonFromFile("bulkScanTransformExceptionRecordCombSolicitorPA1AAutomated.json");
    //        test401(jsonRequest);
    //    }
    //
    //    private void test401(String bodyText) {
    //        RestAssured.given()
    //                .relaxedHTTPSValidation()
    //                .headers(utils.getHeadersWithUserId())
    //                .body(bodyText)
    //                .when().post(TRANSFORM_EXCEPTON_RECORD)
    //                .then().assertThat().statusCode(401);
    //    }
}
