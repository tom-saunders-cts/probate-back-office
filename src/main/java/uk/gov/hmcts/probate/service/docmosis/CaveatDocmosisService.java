package uk.gov.hmcts.probate.service.docmosis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.probate.config.PDFServiceConfiguration;
import uk.gov.hmcts.probate.config.properties.registries.RegistriesProperties;
import uk.gov.hmcts.probate.config.properties.registries.Registry;
import uk.gov.hmcts.probate.model.ccd.caveat.request.CaveatDetails;
import uk.gov.hmcts.probate.service.FileSystemResourceService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaveatDocmosisService {
    private static final String DATE_INPUT_FORMAT = "ddMMyyyy";
    private final RegistriesProperties registriesProperties;
    private final PDFServiceConfiguration pdfServiceConfiguration;
    private final FileSystemResourceService fileSystemResourceService;

    public Map<String, Object> caseDataAsPlaceholders(CaveatDetails caveatDetails) {

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> placeholders = mapper.convertValue(caveatDetails.getData(), Map.class);

        Registry registry = registriesProperties.getRegistries().get(
                caveatDetails.getData().getRegistryLocation().toLowerCase());
        Map<String, Object> registryPlaceholders = mapper.convertValue(registry, Map.class);

        DateFormat generatedDateFormat = new SimpleDateFormat(DATE_INPUT_FORMAT);

        placeholders.put("caseReference", getFormattedCaseReference(caveatDetails.getId().toString()));
        placeholders.put("generatedDate", generatedDateFormat.format(new Date()));
        placeholders.put("registry", registryPlaceholders);
        placeholders.put("PA8AURL", "www.citizensadvice.org.uk|https://www.citizensadvice.org.uk/");
        placeholders.put("PA8BURL", "www.gov.uk|https://www.gov.uk/wills-probate-inheritance/stopping-a-grant-of-representation");
        return placeholders;
    }

    private String getFormattedCaseReference(String caseId) {
        return "#" + caseId.substring(0, 4) + "-"
                + caseId.substring(4, 8) + "-"
                + caseId.substring(8, 12) + "-"
                + caseId.substring(12, 16);
    }

}