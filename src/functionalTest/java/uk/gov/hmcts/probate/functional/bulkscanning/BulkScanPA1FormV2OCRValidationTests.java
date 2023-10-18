package uk.gov.hmcts.probate.functional.bulkscanning;

import io.restassured.RestAssured;
import static java.util.Collections.emptyList;
import java.util.List;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import static org.hamcrest.Matchers.containsString;
import org.junit.Before;
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.runner.RunWith;
import uk.gov.hmcts.probate.functional.IntegrationTestBase;

@RunWith(SpringIntegrationSerenityRunner.class)
public class BulkScanPA1FormV2OCRValidationTests extends IntegrationTestBase {

    private static final String VALIDATE_OCR_DATA = "/forms/%s/validate-ocr";
    private static final String PA1P = "PA1P";
    private static final String PA1A = "PA1A";
    private static final String SUCCESS = "SUCCESS";
    private static final String WARNINGS = "WARNINGS";

    @Before
    public void setUp() {
        initialiseConfig();
    }

    @Test
    public void testPost2022PA1PAllMandatoryFieldsPresentReturnNoWarning() {
        String jsonRequest =
            utils.getStringFromFile(
                "/json/bulkscan/version2/validation/requestPayload/Post2022PA1PAllMandatoryFilled.json");
        List<String> expectedWarnings = emptyList();
        validateOCRWarnings(PA1P, jsonRequest, SUCCESS, expectedWarnings);
    }

    @Test
    public void testPost2022PA1PMissingMandatoryFieldsPresentReturnSomeWarnings() {
        String jsonRequest =
            utils.getStringFromFile("/json/bulkscan/version2/validation/requestPayload/Post2022PA1PMissingNQV.json");
        List<String> expectedWarnings =
            utils.getLinesFromFile("/json/bulkscan/version2/validation/expectedWarnings/missingNQV.txt");
        validateOCRWarnings(PA1P, jsonRequest, WARNINGS, expectedWarnings);
    }

    @Test
    public void testPost2022PA1AAllMandatoryFieldsPresentReturnNoWarning() {
        String jsonRequest =
            utils.getStringFromFile(
                "/json/bulkscan/version2/validation/requestPayload/Post2022PA1AAllMandatoryFilled.json");
        List<String> expectedWarnings = emptyList();
        validateOCRWarnings(PA1A, jsonRequest, SUCCESS, expectedWarnings);
    }

    @Test
    public void testPost2022PA1AMissingMandatoryFieldsPresentReturnSomeWarnings() {
        String jsonRequest =
            utils.getStringFromFile(
                "/json/bulkscan/version2/validation/requestPayload/Post2022PA1AMissingNQV.json");
        List<String> expectedWarnings =
            utils.getLinesFromFile("/json/bulkscan/version2/validation/expectedWarnings/missingNQV.txt");
        validateOCRWarnings(PA1A, jsonRequest, WARNINGS, expectedWarnings);
    }

    @Test
    public void shouldWarnWhenIHT400421Missing() {
        String jsonRequest =
            utils.getStringFromFile("/json/bulkscan/version2/validation/requestPayload/"
                + "Post2022PA1PMissingIHT400421completed.json");
        List<String> expectedWarnings =
            utils.getLinesFromFile(
                "/json/bulkscan/version2/validation/expectedWarnings/missingIHT400421completed.txt");
        validateOCRWarnings(PA1P, jsonRequest, WARNINGS, expectedWarnings);
    }

    @Test
    public void shouldWarnWhenIHT207Missing() {
        String jsonRequest =
            utils.getStringFromFile("/json/bulkscan/version2/validation/requestPayload/"
                + "Post2022PA1PMissingIHT207completed.json");
        List<String> expectedWarnings =
            utils.getLinesFromFile(
                "/json/bulkscan/version2/validation/expectedWarnings/missingIHT207completed.txt");
        validateOCRWarnings(PA1P, jsonRequest, WARNINGS, expectedWarnings);
    }

    @Test
    public void shouldWarnWhenDiedAfterSwitchDateMissing() {
        String jsonRequest =
            utils.getStringFromFile("/json/bulkscan/version2/validation/requestPayload/"
                + "Post2022PA1PMissingDiedAfterSwitchDate.json");
        List<String> expectedWarnings =
            utils.getLinesFromFile("/json/bulkscan/version2/validation/expectedWarnings"
                + "/missingDiedAfterSwitchDate.txt");
        validateOCRWarnings(PA1P, jsonRequest, WARNINGS, expectedWarnings);
    }

    @Test
    public void shouldWarnWhenDiedAfterSwitchDateInconsistentWithDOD() {
        String jsonRequest =
            utils.getStringFromFile("/json/bulkscan/version2/validation/requestPayload/"
                + "Post2022PA1PDiedAfterSwitchDateWrong.json");
        List<String> expectedWarnings =
            utils.getLinesFromFile("/json/bulkscan/version2/validation/expectedWarnings/wrongDiedAfterSwitchDate.txt");
        validateOCRWarnings(PA1P, jsonRequest, WARNINGS, expectedWarnings);
    }

    @Test
    public void shouldWarnForMissingEstateValues() {
        String jsonRequest =
            utils.getStringFromFile("/json/bulkscan/version2/validation/requestPayload/"
                + "Post2022PA1PMissingEstateValues.json");
        List<String> expectedWarnings =
            utils.getLinesFromFile("/json/bulkscan/version2/validation/expectedWarnings/"
                + "missingEstateValues.txt");
        validateOCRWarnings(PA1P, jsonRequest, WARNINGS, expectedWarnings);
    }

    @Test
    public void shouldWarnForMissingIHT205CompletedOnline() {
        String jsonRequest =
            utils.getStringFromFile("/json/bulkscan/version2/validation/requestPayload/"
                + "Post2022PA1PMissingIHT205CompletedOnline.json");
        List<String> expectedWarnings =
            utils.getLinesFromFile("/json/bulkscan/version2/validation/expectedWarnings/"
                + "missingIHT205CompletedOnline.txt");
        validateOCRWarnings(PA1P, jsonRequest, WARNINGS, expectedWarnings);
    }

    @Test
    public void invalidApplyingExecutor0EmailAddress() {
        String jsonRequest =
                utils.getStringFromFile("/json/bulkscan/version2/validation/requestPayload/"
                        + "Post2022PA1PInvalidExecutor0EmailAddress.json");
        List<String> expectedWarnings =
                utils.getLinesFromFile("/json/bulkscan/version2/validation/expectedWarnings/"
                        + "invalidApplyingExecutor0EmailAddress.txt");
        validateOCRWarnings(PA1P, jsonRequest, WARNINGS, expectedWarnings);
    }

    @Test
    public void validApplyingExecutor0EmailAddress() {
        String jsonRequest =
                utils.getStringFromFile("/json/bulkscan/version2/validation/requestPayload/"
                        + "Post2022PA1PValidExecutor0EmailAddress.json");
        List<String> expectedWarnings = emptyList();
        validateOCRWarnings(PA1A, jsonRequest, SUCCESS, expectedWarnings);
    }

    @Test
    public void shouldWarnForMissingIhtUnusedAllowanceClaimed() {
        String jsonRequest =
            utils.getStringFromFile("/json/bulkscan/version2/validation/requestPayload/"
                + "Post2022PA1AMissingUnusedAllowanceClaimed.json");
        List<String> expectedWarnings =
            utils.getLinesFromFile("/json/bulkscan/version2/validation/expectedWarnings/"
                + "missingUnusedAllowanceClaimed.txt");
        validateOCRWarnings(PA1A, jsonRequest, WARNINGS, expectedWarnings);
    }

    @Test
    public void shouldNotWarnForMissingIhtUnusedAllowanceClaimedIfNotWidowed() {
        String jsonRequest =
            utils.getStringFromFile("/json/bulkscan/version2/validation/requestPayload/"
                + "Post2022PA1AMissingUnusedAllowanceClaimedNotWidowed.json");
        List<String> expectedWarnings = emptyList();
        validateOCRWarnings(PA1A, jsonRequest, SUCCESS, expectedWarnings);
    }

    @Test
    public void shouldWarnForMissingIhtUnusedAllowanceClaimedIfNQVOutOfRange() {
        String jsonRequest =
            utils.getStringFromFile("/json/bulkscan/version2/validation/requestPayload/"
                + "Post2022PA1AMissingUnusedAllowanceClaimedNQVOutOfRange.json");
        List<String> expectedWarnings = emptyList();
        validateOCRWarnings(PA1A, jsonRequest, SUCCESS, expectedWarnings);
    }

    private void validateOCRWarnings(String formName, String bodyText, String containsText,
                                     List<String> expectedWarnings) {
        List<String> warnings = RestAssured.given()
            .config(config)
            .relaxedHTTPSValidation()
            .headers(utils.getHeaders())
            .body(bodyText)
            .when().post(String.format(VALIDATE_OCR_DATA, formName))
            .then().assertThat().statusCode(200)
            .and().body(containsString(containsText)).extract().body().jsonPath().get("warnings");
        assertEquals(expectedWarnings, warnings);
    }
}
