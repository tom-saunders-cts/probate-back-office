package uk.gov.hmcts.probate.service.tasklist;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.probate.businessrule.IhtEstate207BusinessRule;
import uk.gov.hmcts.probate.businessrule.PA14FormBusinessRule;
import uk.gov.hmcts.probate.businessrule.PA15FormBusinessRule;
import uk.gov.hmcts.probate.businessrule.PA16FormBusinessRule;
import uk.gov.hmcts.probate.businessrule.PA17FormBusinessRule;
import uk.gov.hmcts.probate.htmlrendering.DetailsComponentRenderer;
import uk.gov.hmcts.probate.htmlrendering.GridRenderer;
import uk.gov.hmcts.probate.htmlrendering.LinkRenderer;
import uk.gov.hmcts.probate.model.Constants;
import uk.gov.hmcts.probate.model.caseprogress.TaskListState;
import uk.gov.hmcts.probate.model.caseprogress.TaskState;
import uk.gov.hmcts.probate.model.ccd.raw.AdditionalExecutorNotApplying;
import uk.gov.hmcts.probate.model.ccd.raw.request.CaseData;
import uk.gov.hmcts.probate.model.ccd.raw.request.CaseDetails;
import uk.gov.hmcts.probate.model.htmltemplate.SendDocumentsDetailsHtmlTemplate;
import uk.gov.hmcts.probate.model.htmltemplate.StateChangeDateHtmlTemplate;
import uk.gov.hmcts.probate.model.htmltemplate.StatusTagHtmlTemplate;
import uk.gov.hmcts.probate.service.SendDocumentsRenderer;
import uk.gov.hmcts.probate.service.solicitorexecutor.NotApplyingExecutorsMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static uk.gov.hmcts.probate.model.Constants.GRANT_TYPE_INTESTACY;
import static uk.gov.hmcts.probate.model.Constants.IHT_ESTATE_207_TEXT;
import static uk.gov.hmcts.probate.model.Constants.REASON_FOR_NOT_APPLYING_RENUNCIATION;
import static uk.gov.hmcts.probate.model.Constants.REASON_FOR_NOT_APPLYING_MENTALLY_INCAPABLE;
import static uk.gov.hmcts.probate.model.Constants.YES;
import static uk.gov.hmcts.probate.model.PageTextConstants.IHT_ESTATE_207;
import static uk.gov.hmcts.probate.model.PageTextConstants.IHT_FORM;
import static uk.gov.hmcts.probate.model.PageTextConstants.IHT_TEXT;
import static uk.gov.hmcts.probate.model.PageTextConstants.ORIGINAL_WILL;
import static uk.gov.hmcts.probate.model.PageTextConstants.PA14_FORM;
import static uk.gov.hmcts.probate.model.PageTextConstants.PA15_FORM;
import static uk.gov.hmcts.probate.model.PageTextConstants.PA16_FORM;
import static uk.gov.hmcts.probate.model.PageTextConstants.PA17_FORM;
import static uk.gov.hmcts.probate.model.caseprogress.UrlConstants.ADD_APPLICATION_DETAILS_URL_TEMPLATE_ADMON_WILL;
import static uk.gov.hmcts.probate.model.caseprogress.UrlConstants.ADD_APPLICATION_DETAILS_URL_TEMPLATE_GOP;
import static uk.gov.hmcts.probate.model.caseprogress.UrlConstants.ADD_APPLICATION_DETAILS_URL_TEMPLATE_INTESTACY;
import static uk.gov.hmcts.probate.model.caseprogress.UrlConstants.DECEASED_DETAILS_URL_TEMPLATE;
import static uk.gov.hmcts.probate.model.caseprogress.UrlConstants.REVIEW_OR_SUBMIT_URL_TEMPLATE;
import static uk.gov.hmcts.probate.model.caseprogress.UrlConstants.SOLICITOR_DETAILS_URL_TEMPLATE;
import static uk.gov.hmcts.probate.model.caseprogress.UrlConstants.TL_COVERSHEET_URL_TEMPLATE;

// Renders links / text and also the status tag - i.e. details varying by state
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskStateRenderer {
    private static final String ADD_SOLICITOR_DETAILS_TEXT = "Add Probate practitioner details";
    private static final String ADD_DECEASED_DETAILS_TEXT = "Add deceased details";
    private static final String ADD_APPLICATION_DETAILS_TEXT = "Add application details";
    private static final String REVIEW_OR_SUBMIT_TEXT = "Review and sign legal statement and submit application";
    private static final String SEND_DOCS_DETAILS_TITLE = "View the documents needed by HM Courts and Tribunal Service";
    private static final String AUTH_DOCS_TEXT = "Authenticate documents";
    private static final String EXAMINE_APP_TEXT = "Examine application";
    private static final String ISSUE_GRANT_TEXT = "Issue grant of representation<";
    private static final String COVERSHEET = "coversheet";
    private static final String IHT_400421 = "IHT400421";

    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private final PA14FormBusinessRule pa14FormBusinessRule;
    private final PA15FormBusinessRule pa15FormBusinessRule;
    private final PA16FormBusinessRule pa16FormBusinessRule;
    private final PA17FormBusinessRule pa17FormBusinessRule;
    private final IhtEstate207BusinessRule ihtEstate207BusinessRule;
    private final NotApplyingExecutorsMapper notApplyingExecutorsMapper;
    private final SendDocumentsRenderer sendDocumentsRenderer;

    // isProbate - true if application for probate, false if for caveat
    public String renderByReplace(TaskListState currState, String html, Long caseId,
                                         String willType, String solSOTNeedToUpdate,
                                         LocalDate authDate, LocalDate submitDate, CaseDetails details) {
        final TaskState addSolState = getTaskState(currState, TaskListState.TL_STATE_ADD_SOLICITOR_DETAILS,
                solSOTNeedToUpdate);
        final TaskState addDeceasedState = getTaskState(currState, TaskListState.TL_STATE_ADD_DECEASED_DETAILS,
                solSOTNeedToUpdate);
        final TaskState addAppState = getTaskState(currState, TaskListState.TL_STATE_ADD_APPLICATION_DETAILS,
                solSOTNeedToUpdate);
        final TaskState rvwState = getTaskState(currState, TaskListState.TL_STATE_REVIEW_AND_SUBMIT,
                solSOTNeedToUpdate);
        final TaskState sendDocsState = getTaskState(currState, TaskListState.TL_STATE_SEND_DOCUMENTS,
                solSOTNeedToUpdate);
        final TaskState authDocsState = getTaskState(currState, TaskListState.TL_STATE_AUTHENTICATE_DOCUMENTS,
                solSOTNeedToUpdate);
        final TaskState examineState = getTaskState(currState, TaskListState.TL_STATE_EXAMINE_APPLICATION,
                solSOTNeedToUpdate);
        final TaskState issueState = getTaskState(currState, TaskListState.TL_STATE_ISSUE_GRANT, solSOTNeedToUpdate);

        // the only time caseId will be null is when running unit tests!
        final String caseIdStr = caseId == null ? "" : caseId.toString();

        return html == null ? null : html
                .replaceFirst("<addSolicitorLink/>", renderLinkOrText(TaskListState.TL_STATE_ADD_SOLICITOR_DETAILS,
                        currState, addSolState, ADD_SOLICITOR_DETAILS_TEXT, caseIdStr, willType, details))
                .replaceFirst("<status-addSolicitor/>", renderTaskStateTag(addSolState))
                .replaceFirst("<addDeceasedLink/>", renderLinkOrText(TaskListState.TL_STATE_ADD_DECEASED_DETAILS,
                        currState, addDeceasedState, ADD_DECEASED_DETAILS_TEXT, caseIdStr, willType, details))
                .replaceFirst("<status-addDeceasedDetails/>", renderTaskStateTag(addDeceasedState))
                .replaceFirst("<addAppLink/>", renderLinkOrText(TaskListState.TL_STATE_ADD_APPLICATION_DETAILS,
                        currState, addAppState, ADD_APPLICATION_DETAILS_TEXT, caseIdStr, willType, details))
                .replaceFirst("<status-addApplicationDetails/>", renderTaskStateTag(addAppState))
                .replaceFirst("<rvwLink/>", renderLinkOrText(TaskListState.TL_STATE_REVIEW_AND_SUBMIT,
                        currState, rvwState, REVIEW_OR_SUBMIT_TEXT, caseIdStr, willType, details))
                .replaceFirst("<status-reviewAndSubmit/>", renderTaskStateTag(rvwState))
                .replaceFirst("<reviewAndSubmitDate/>", renderSubmitDate(submitDate))
                .replaceFirst("<sendDocsLink/>", renderSendDocsDetails(sendDocsState, caseIdStr, details))
                .replaceFirst("<status-sendDocuments/>", renderTaskStateTag(sendDocsState))
                .replaceFirst("<authDocsLink/>", renderLinkOrText(TaskListState.TL_STATE_EXAMINE_APPLICATION,
                        currState, authDocsState, AUTH_DOCS_TEXT, caseIdStr, willType, details))
                .replaceFirst("<authenticatedDate/>", renderAuthenticatedDate(authDate))
                .replaceFirst("<status-authDocuments/>", renderTaskStateTag(authDocsState))
                .replaceFirst("<examAppLink/>", renderLinkOrText(TaskListState.TL_STATE_EXAMINE_APPLICATION,
                        currState, examineState, EXAMINE_APP_TEXT, caseIdStr, willType, details))
                .replaceFirst("<status-examineApp/>", renderTaskStateTag(examineState))
                .replaceFirst("<issueGrantLink/>", renderLinkOrText(TaskListState.TL_STATE_ISSUE_GRANT,
                        currState, issueState, ISSUE_GRANT_TEXT, caseIdStr, willType, details))
                .replaceFirst("<status-issueGrant/>", renderTaskStateTag(issueState))
                .replaceFirst("<coversheet/>", renderLinkOrText(TaskListState.TL_STATE_SEND_DOCUMENTS,
                        currState, sendDocsState, COVERSHEET, caseIdStr, willType, details));
    }

    private TaskState getTaskState(TaskListState currState, TaskListState renderState,
                                          String solSOTNeedToUpdate) {
        if (solSOTNeedToUpdate != null && solSOTNeedToUpdate.equals(Constants.YES)
                && renderState.compareTo(TaskListState.TL_STATE_REVIEW_AND_SUBMIT) <= 0) {
            if (currState.compareTo(renderState) > 0) {
                return TaskState.COMPLETED;
            }
            return TaskState.IN_PROGRESS;
        }

        if (currState == renderState) {
            return currState.isMultiState ? TaskState.IN_PROGRESS : TaskState.NOT_STARTED;
        }
        if (currState.compareTo(renderState) > 0) {
            return TaskState.COMPLETED;
        }
        return TaskState.NOT_AVAILABLE;
    }

    private String renderTaskStateTag(TaskState taskState) {
        if (taskState == TaskState.NOT_AVAILABLE) {
            return "";
        }
        return StatusTagHtmlTemplate.STATUS_TAG
                .replaceFirst("<imgSrc/>", taskState.imageUrl)
                .replaceFirst("<imgAlt/>", taskState.displayText)
                .replaceFirst("<imgTitle/>", taskState.displayText);
    }

    private String renderSendDocsDetails(TaskState sendDocsState, String caseId, CaseDetails details) {
        Map<String, String> keyValues = getKeyValues(details.getData());
        return sendDocsState == TaskState.NOT_AVAILABLE ? "" :
                DetailsComponentRenderer.renderByReplace(SEND_DOCS_DETAILS_TITLE,
                        SendDocumentsDetailsHtmlTemplate.DOC_DETAILS.replaceFirst("<refNum/>", caseId)
                .replaceFirst(ORIGINAL_WILL, keyValues.getOrDefault("originalWill", ""))
                .replaceFirst(IHT_TEXT, keyValues.getOrDefault("ihtText", ""))
                .replaceFirst(IHT_FORM, keyValues.getOrDefault("ihtForm", ""))
                .replaceFirst(PA14_FORM, keyValues.getOrDefault("pa14Form", ""))
                .replaceFirst(PA15_FORM, keyValues.getOrDefault("pa15Form", ""))
                .replaceFirst(PA16_FORM, keyValues.getOrDefault("pa16Form", ""))
                .replaceFirst(PA17_FORM, keyValues.getOrDefault("pa17Form", ""))
                .replaceFirst(IHT_ESTATE_207, keyValues.getOrDefault("ihtEstate207", ""))
                );
    }

    private String renderLinkOrText(TaskListState taskListState, TaskListState currState,
                                           TaskState currTaskState, String linkText, String caseId,
                                           String willType, CaseDetails details) {

        String linkUrlTemplate = getLinkUrlTemplate(taskListState, willType);
        String coversheetUrl = details.getData().getSolsCoversheetDocument() == null ? "#" : details
            .getData().getSolsCoversheetDocument().getDocumentBinaryUrl();

        if (linkUrlTemplate != null && currState == taskListState
            && (currState == TaskListState.TL_STATE_SEND_DOCUMENTS)) {
            return LinkRenderer.renderOutside(linkText, linkUrlTemplate.replaceFirst("<CASE_ID>", caseId)
                .replaceFirst("<DOCUMENT_LINK>", coversheetUrl));
        }

        return linkUrlTemplate != null && currState == taskListState
                && (currTaskState == TaskState.NOT_STARTED || currTaskState == TaskState.IN_PROGRESS)
                ? LinkRenderer.render(linkText, linkUrlTemplate.replaceFirst("<CASE_ID>", caseId)) : linkText;
    }

    private static String renderAuthenticatedDate(LocalDate authDate) {
        if (authDate == null) {
            return ""; // mustn't be null as we are chaining .replaceFirst methods
        }
        String authDateTemplate = StateChangeDateHtmlTemplate.STATE_CHANGE_DATE_TEMPLATE
                .replaceFirst("<stateChangeDateText/>",
                    format("Authenticated on %s", authDate.format(dateFormat)));
        return GridRenderer.renderByReplace(authDateTemplate);
    }

    private String renderSubmitDate(LocalDate submitDate) {
        if (submitDate == null) {
            return ""; // mustn't be null as we are chaining .replaceFirst methods
        }
        String submitDateTemplate = StateChangeDateHtmlTemplate.STATE_CHANGE_DATE_TEMPLATE
                .replaceFirst("<stateChangeDateText/>",
                    format("Submitted on %s", submitDate.format(dateFormat)));
        return GridRenderer.renderByReplace(submitDateTemplate);
    }

    private String getLinkUrlTemplate(TaskListState taskListState, String willType) {
        switch (taskListState) {
            case TL_STATE_ADD_SOLICITOR_DETAILS:
                return SOLICITOR_DETAILS_URL_TEMPLATE;
            case TL_STATE_ADD_DECEASED_DETAILS:
                return DECEASED_DETAILS_URL_TEMPLATE;
            case TL_STATE_ADD_APPLICATION_DETAILS:
                switch (willType) {
                    case "NoWill":
                        return ADD_APPLICATION_DETAILS_URL_TEMPLATE_INTESTACY;
                    case "WillLeftAnnexed":
                        return ADD_APPLICATION_DETAILS_URL_TEMPLATE_ADMON_WILL;
                    default:
                        return ADD_APPLICATION_DETAILS_URL_TEMPLATE_GOP;
                }
            case TL_STATE_REVIEW_AND_SUBMIT:
                return REVIEW_OR_SUBMIT_URL_TEMPLATE;
            case TL_STATE_SEND_DOCUMENTS:
                return TL_COVERSHEET_URL_TEMPLATE;
            default:
                return null;
        }
    }

    private Map<String, String> getKeyValues(CaseData data) {
        Map<String, String> keyValue = new HashMap<>();
        String solsWillType = data.getSolsWillType() == null ? "" : data.getSolsWillType();
        String willHasCodicils = data.getWillHasCodicils() == null ? "" : data.getWillHasCodicils();
        String originalWill = "<li>the original will</li>";
        if (solsWillType.equals(GRANT_TYPE_INTESTACY)) {
            originalWill = "";
        } else if ("Yes".equals(willHasCodicils)) {
            originalWill = "<li>the original will and any codicils</li>";
        }

        keyValue.put("originalWill", originalWill);

        String ihtFormValue = data.getIhtFormId() == null ? "" : data.getIhtFormId();
        String ihtText = "";
        String ihtForm = "";
        if (!ihtFormValue.contentEquals(IHT_400421) && !"".equals(ihtFormValue)) {
            ihtText = "<li>the inheritance tax form ";
            if (YES.equals(data.getIht217())) {
                ihtForm = "IHT205 and IHT217</li>";
            } else {
                ihtForm = ihtFormValue + "</li>";
            }
        }

        keyValue.put("ihtText", ihtText);
        keyValue.put("ihtForm", ihtForm);
        String pa14Form = "";
        if (pa14FormBusinessRule.isApplicable(data)) {
            pa14Form = buildPA14NotApplyingExecutorsLinks(data, REASON_FOR_NOT_APPLYING_MENTALLY_INCAPABLE);
        }
        keyValue.put("pa14Form", pa14Form);
        String pa15Form = "";
        if (pa15FormBusinessRule.isApplicable(data)) {
            pa15Form = buildPA15NotApplyingExecutorsLinks(data, REASON_FOR_NOT_APPLYING_RENUNCIATION);
        }
        keyValue.put("pa15Form", pa15Form);
        String pa16Form = "";
        if (pa16FormBusinessRule.isApplicable(data)) {
            pa16Form = "<li>" + sendDocumentsRenderer.getPA16FormText() + "</li>";
        }
        keyValue.put("pa16Form", pa16Form);
        String pa17Form = "";
        if (pa17FormBusinessRule.isApplicable(data)) {
            pa17Form = "<li>" + sendDocumentsRenderer.getPA17FormText() + "</li>";
        }
        keyValue.put("pa17Form", pa17Form);
        String ihtEstate207 = "";
        if (ihtEstate207BusinessRule.isApplicable(data)) {
            ihtEstate207 = "<li>" + IHT_ESTATE_207_TEXT + "</li>";
        }
        keyValue.put("ihtEstate207", ihtEstate207);
        return keyValue;
    }

    private String buildPA14NotApplyingExecutorsLinks(CaseData caseData, String reason) {
        List<AdditionalExecutorNotApplying> notApplyingExecs =
            notApplyingExecutorsMapper.getAllExecutorsNotApplying(caseData, reason);
        return notApplyingExecs.stream()
            .map(executor -> buildPA14NotApplyingExecLabel(executor.getNotApplyingExecutorName()))
            .collect(Collectors.joining());

    }

    private String buildPA15NotApplyingExecutorsLinks(CaseData caseData, String reason) {
        List<AdditionalExecutorNotApplying> notApplyingExecs =
            notApplyingExecutorsMapper.getAllExecutorsNotApplying(caseData, reason);
        return notApplyingExecs.stream()
            .map(executor -> buildPA15NotApplyingExecLabel(executor.getNotApplyingExecutorName()))
            .collect(Collectors.joining());

    }

    private String buildPA15NotApplyingExecLabel(String renouncingExecutorName) {
        return "<li>" + sendDocumentsRenderer.getPA15NotApplyingExecutorText(renouncingExecutorName) +  "</li>";
    }
    
    private String buildPA14NotApplyingExecLabel(String renouncingExecutorName) {
        return "<li>" + sendDocumentsRenderer.getPA14NotApplyingExecutorText(renouncingExecutorName) +  "</li>";
    }
}
