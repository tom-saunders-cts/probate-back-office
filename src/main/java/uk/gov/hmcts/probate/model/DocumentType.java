package uk.gov.hmcts.probate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum DocumentType {
    @JsonProperty("legalStatement")
    LEGAL_STATEMENT("legalStatement"),

    @JsonProperty("digitalGrant")
    DIGITAL_GRANT("digitalGrant"),

    @JsonProperty("digitalGrantDraft")
    DIGITAL_GRANT_DRAFT("digitalGrantDraft"),

    @JsonProperty("intestacyGrant")
    INTESTACY_GRANT("intestacyGrant"),

    @JsonProperty("intestacyGrant")
    INTESTACY_GRANT_DRAFT("intestacyGrant"),

    @JsonProperty("admonWillGrant")
    ADMON_WILL_GRANT("admonWillGrant"),

    @JsonProperty("admonWillGrant")
    ADMON_WILL_GRANT_DRAFT("admonWillGrant"),

    @JsonProperty("sentEmail")
    SENT_EMAIL("sentEmail"),

    @JsonProperty("email")
    EMAIL("email"),

    @JsonProperty("IHT")
    IHT("IHT"),

    @JsonProperty("other")
    OTHER("other"),

    @JsonProperty("deathCertificate")
    DEATH_CERT("deathCertificate"),

    @JsonProperty("correspondence")
    CORRESPONDENCE("correspondence");

    private final String templateName;

    DocumentType(String templateName) {
        this.templateName = templateName;
    }
}
