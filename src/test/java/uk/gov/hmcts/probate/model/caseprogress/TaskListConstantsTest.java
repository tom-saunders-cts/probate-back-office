package uk.gov.hmcts.probate.model.caseprogress;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.probate.model.StateConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.probate.model.caseprogress.TaskListState.TL_STATE_ADD_SOLICITOR_DETAILS;

class TaskListConstantsTest {
    private final static String paymentToken = "paymentToken";

    @Test
    void testConstantsMapCorrectly() {
        assertEquals(TL_STATE_ADD_SOLICITOR_DETAILS, TaskListState.mapCaseState(null, paymentToken));
        assertEquals(TL_STATE_ADD_SOLICITOR_DETAILS,
                TaskListState.mapCaseState(StateConstants.STATE_SOL_APP_CREATED_SOLICITOR_DTLS, paymentToken));
        assertEquals(TaskListState.TL_STATE_ADD_DECEASED_DETAILS,
                TaskListState.mapCaseState(StateConstants.STATE_SOL_APP_CREATED_DECEASED_DTLS, paymentToken));
        assertEquals(TaskListState.TL_STATE_ADD_APPLICATION_DETAILS,
                TaskListState.mapCaseState(StateConstants.STATE_SOL_PROBATE_CREATED, paymentToken));
        assertEquals(TaskListState.TL_STATE_ADD_APPLICATION_DETAILS,
                TaskListState.mapCaseState(StateConstants.STATE_SOL_INTESTACY_CREATED, paymentToken));
        assertEquals(TaskListState.TL_STATE_ADD_APPLICATION_DETAILS,
                TaskListState.mapCaseState(StateConstants.STATE_SOL_ADMON_CREATED, paymentToken));
        assertEquals(TaskListState.TL_STATE_SEND_DOCUMENTS,
                TaskListState.mapCaseState(StateConstants.STATE_BO_REDEC_NOTIFICATION_SENT, paymentToken));
        assertEquals(TaskListState.TL_STATE_REVIEW_AND_SUBMIT,
                TaskListState.mapCaseState(StateConstants.STATE_SOL_APP_UPDATED, paymentToken));
        assertEquals(TaskListState.TL_STATE_MAKE_PAYMENT,
                TaskListState.mapCaseState(StateConstants.STATE_CASE_CREATED, paymentToken));
        assertEquals(TaskListState.TL_STATE_SEND_DOCUMENTS,
                TaskListState.mapCaseState(StateConstants.STATE_CASE_CREATED, "Yes"));
        assertEquals(TaskListState.TL_STATE_PAYMENT_ATTEMPTED,
                TaskListState.mapCaseState(StateConstants.STATE_CASE_CREATED, "No"));
        assertEquals(TaskListState.TL_STATE_SEND_DOCUMENTS,
                TaskListState.mapCaseState(StateConstants.STATE_CASE_PRINTED, paymentToken));
        assertEquals(TaskListState.TL_STATE_EXAMINE_APPLICATION,
                TaskListState.mapCaseState(StateConstants.STATE_BO_READY_FOR_EXAMINATION, paymentToken));
        assertEquals(TaskListState.TL_STATE_EXAMINE_APPLICATION,
                TaskListState.mapCaseState(StateConstants.STATE_BO_EXAMINING, paymentToken));
        assertEquals(TaskListState.TL_STATE_EXAMINE_APPLICATION,
                TaskListState.mapCaseState(StateConstants.STATE_BO_EXAMINING_REISSUE, paymentToken));
        assertEquals(TaskListState.TL_STATE_EXAMINE_APPLICATION,
                TaskListState.mapCaseState(StateConstants.STATE_BO_CASE_MATCHING_EXAMINING, paymentToken));
        assertEquals(TaskListState.TL_STATE_EXAMINE_APPLICATION,
                TaskListState.mapCaseState(StateConstants.STATE_BO_READY_TO_ISSUE, paymentToken));
        assertEquals(TaskListState.TL_STATE_ISSUE_GRANT,
                TaskListState.mapCaseState(StateConstants.STATE_BO_CASE_MATCHING_ISSUE_GRANT, paymentToken));
        assertEquals(TaskListState.TL_STATE_COMPLETE,
                TaskListState.mapCaseState(StateConstants.STATE_BO_GRANT_ISSUED, paymentToken));
    }
}
