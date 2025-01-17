package uk.gov.hmcts.probate.service;

import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.server.LDClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FeatureToggleService {

    private final LDClient ldClient;
    private final LDUser ldUser;
    private final LDUser.Builder ldUserBuilder;

    @Autowired
    public FeatureToggleService(LDClient ldClient, @Value("${ld.user.key}") String ldUserKey,
                                @Value("${ld.user.firstName}") String ldUserFirstName,
                                @Value("${ld.user.lastName}") String ldUserLastName) {
        this.ldClient = ldClient;
       
        this.ldUserBuilder = new LDUser.Builder(ldUserKey)
            .firstName(ldUserFirstName)
            .lastName(ldUserLastName)
            .custom("timestamp", String.valueOf(System.currentTimeMillis()));
        this.ldUser = this.ldUserBuilder.build();
    }

    public boolean isNewFeeRegisterCodeEnabled() {
        return this.ldClient.boolVariation("probate-newfee-register-code", this.ldUser, true);
    }

    public boolean enableNewMarkdownFiltering() {
        return this.ldClient.boolVariation("probate-enable-new-markdown-filtering", this.ldUser, false);
    }

    public boolean isFeatureToggleOn(String featureToggleCode, boolean defaultValue) {
        return this.ldClient.boolVariation(featureToggleCode, this.ldUser, defaultValue);
    }

}
