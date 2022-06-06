package uk.gov.hmcts.probate.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ExceptedEstateDateOfDeathCheckerTest {

    private static final String PRE_EE_DECEASED_DATE_OF_DEATH = "01012021";
    private static final String POST_EE_DECEASED_DATE_OF_DEATH = "01012022";

    @Autowired
    ExceptedEstateDateOfDeathChecker exceptedEstateDateOfDeathChecker;

    @Test
    public void shouldReturnFalse() {
        assertFalse(exceptedEstateDateOfDeathChecker.isOnOrAfterSwitchDate(PRE_EE_DECEASED_DATE_OF_DEATH));
    }

    @Test
    public void shouldReturnTrue() {
        assertTrue(exceptedEstateDateOfDeathChecker.isOnOrAfterSwitchDate(POST_EE_DECEASED_DATE_OF_DEATH));
    }
}
