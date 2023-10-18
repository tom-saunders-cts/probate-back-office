'use strict';

const requireDirectory = require('require-directory');
const steps = requireDirectory(module);

module.exports = function () {
    return actor({

        // Login

        authenticateWithIdamIfAvailable: steps.IDAM.signIn,
        selectNewCase: steps.newCase.newCase,
        selectCaseTypeOptions: steps.createCase.createCase,
        handleEvidence: steps.handleEvidence.handleEvidence,
        registrarsDecision: steps.registrarsDecision.registrarsDecision,
        enterWillLodgementPage1: steps.createWillLodgement.page1,
        enterWillLodgementPage2: steps.createWillLodgement.page2,
        enterWillLodgementPage3: steps.createWillLodgement.page3,
        enterCaveatPage1: steps.createCaveat.page1,
        enterCaveatPage2: steps.createCaveat.page2,
        enterCaveatPage3: steps.createCaveat.page3,
        enterCaveatPage4: steps.createCaveat.page4,
        emailCaveator: steps.emailNotifications.caveat.emailCaveator,
        reopenCaveat: steps.reopenningCases.caveat.reopenCaveat,
        withdrawCaveatPage1: steps.withdrawCaveat.page1,
        enterGrantOfProbatePage1: steps.createGrantOfProbate.page1,
        enterGrantOfProbatePage2: steps.createGrantOfProbate.page2,
        enterGrantOfProbatePage3: steps.createGrantOfProbate.page3,
        enterGrantOfProbatePage4: steps.createGrantOfProbate.page4,
        enterGrantOfProbatePage5: steps.createGrantOfProbate.page5,
        enterGrantOfProbatePage6: steps.createGrantOfProbate.page6,
        enterGrantOfProbatePage7: steps.createGrantOfProbate.page7,
        enterGrantOfProbatePage8: steps.createGrantOfProbate.page8,
        enterGrantOfProbatePage9: steps.createGrantOfProbate.page9,
        enterGrantOfProbateManualPage1: steps.createGrantOfProbateManual.page1,
        enterGrantOfProbateManualPage2: steps.createGrantOfProbateManual.page2,
        enterGrantOfProbateManualPage3: steps.createGrantOfProbateManual.page3,
        enterGrantOfProbateManualForProbateManPage1: steps.createGrantOfProbateManualForProbateMan.page1,
        enterGrantOfProbateManualForProbateManPage2: steps.createGrantOfProbateManualForProbateMan.page2,
        enterGrantOfProbateManualForProbateManPage3: steps.createGrantOfProbateManualForProbateMan.page3,
        cwEnterSolsGoPPage1: steps.createGrantOfProbateSolicitor.page1,
        cwEnterSolsGoPPage2: steps.createGrantOfProbateSolicitor.page2,
        cwEnterSolsGoPPage3: steps.createGrantOfProbateSolicitor.page3,
        cwEnterSolsGoPPage4: steps.createGrantOfProbateSolicitor.page4,
        cwEnterSolsGoPPage5: steps.createGrantOfProbateSolicitor.page5,
        cwEnterSolsGoPPage6: steps.createGrantOfProbateSolicitor.page6,
        cwEnterSolsGoPPage7: steps.createGrantOfProbateSolicitor.page7,
        cwEnterSolsGoPPage8: steps.createGrantOfProbateSolicitor.page8,
        cwEnterSolsGoPPage9: steps.createGrantOfProbateSolicitor.page9,
        cwEnterSolsGoPPage10: steps.createGrantOfProbateSolicitor.page10,

        caseProgressSolicitorDetails: steps.caseProgress.solicitorDetails,
        caseProgressSolicitorDetailsCheckAnswers: steps.caseProgress.solicitorDetailsCheckAnswers,
        caseProgressDeceasedDetails: steps.caseProgress.deceasedDetails,
        caseProgressDeceasedDetails2: steps.caseProgress.deceasedDetails2,
        caseProgressCheckCaseProgressTab: steps.caseProgress.checkCaseProgressTab,
        caseProgressClickElementsAndContinue: steps.caseProgress.clickElementsAndContinue,
        caseProgressClickSelectOrFillElementsAndContinue: steps.caseProgress.clickSelectOrFillElementsAndContinue,
        caseProgressWaitForElementThenContinue: steps.caseProgress.waitForElementThenContinue,
        caseProgressCheckYourAnswers: steps.caseProgress.checkYourAnswers,
        caseProgressConfirmApplication: steps.caseProgress.confirmApplication,
        caseProgressCompleteApplication: steps.caseProgress.completeApplication,
        caseProgressSubmittedConfirmation: steps.caseProgress.submittedConfirmation,
        caseProgressCaseworkerChooseNextStepAndGo: steps.caseProgress.caseworkerChooseNextStepAndGo,
        caseProgressSelectPenultimateNextStepAndGo: steps.caseProgress.selectPenultimateNextStepAndGo,
        caseProgressClickSubmitAndSignOut: steps.caseProgress.clickSubmitAndSignOut,
        caseProgressFeePayment: steps.caseProgress.feePayment,
        caseProgressContinueWithoutChangingAnything: steps.caseProgress.continueWithoutChangingAnything,
        caseProgressAppStoppedDetails: steps.caseProgressAppStopped.appStoppedDetails,
        caseProgressAppStoppedTabCheck: steps.caseProgressAppStopped.appStoppedTabCheck,
        caseProgressStandardDeceasedDetailsCheck: steps.caseProgressStandard.deceasedDetailsCheck,
        caseProgressStopEscalateIssueDeceasedDetailsCheck: steps.caseProgressStopEscalateIssue.deceasedDetailsCheck,
        caseProgressStopEscalateIssueStoppedTabCheck: steps.caseProgressStopEscalateIssue.stoppedTabCheck,
        caseProgressStopEscalateIssueAddCaseStoppedReason: steps.caseProgressStopEscalateIssue.addCaseStoppedReason,
        caseProgressStopEscalateIssueCaseStopAgainReason: steps.caseProgressStopEscalateIssue.addCaseStoppedAgainReason,
        caseProgressStopEscalateIssueAddCaseResolveStop: steps.caseProgressStopEscalateIssue.addResolveStopDetails,
        caseProgressStopEscalateIssueEscalatedTabCheck: steps.caseProgressStopEscalateIssue.escalatedTabCheck,
        checkMyAnswers: steps.checkYourAnswers.checkYourAnswers,
        seeCaseDetails: steps.caseDetails.caseDetails,
        dontSeeCaseDetails: steps.caseDetails.checkCaseDetailsDoNotAppear,
        seeUpdatesOnCase: steps.caseDetails.caseDetailsUpdated,
        chooseNextStep: steps.nextStep.nextStep,
        printCase: steps.printCase.printCase,
        enterEventSummary: steps.eventSummary.eventSummary,
        uploadDocument: steps.documentUpload.documentUpload,
        enterComment: steps.eventSummary.eventSummary,
        markForExamination: steps.markForExamination.markForExamination,
        markForIssue: steps.markForIssue.markForIssue,
        issueGrant: steps.issueGrant.issueGrant,
        selectCaseMatchesForWillLodgement: steps.caseMatches.caseMatches,
        selectCaseMatchesForGrantOfProbate: steps.caseMatches.caseMatches,
        selectProbateManCaseMatchesForGrantOfProbate: steps.caseMatches.probateManCaseMatches,
        verifyProbateManCcdCaseNumber: steps.probateManCaseUrlVerification.probateManCaseUrlVerification,
        selectCaseMatchesForCaveat: steps.caseMatches.caseMatches,
        selectWithdrawalReason: steps.withdrawal.withdrawal,
        applyForProbatePage1: steps.solicitorApplyProbate.applyProbate.page1,
        applyForProbatePage2: steps.solicitorApplyProbate.applyProbate.page2,
        cyaPage: steps.solicitorApplyProbate.cyaPage,
        seeEndState: steps.solicitorApplyProbate.endState,
        deceasedDetailsPage1: steps.solicitorApplyProbate.deceasedDetails.page1,
        deceasedDetailsPage2: steps.solicitorApplyProbate.deceasedDetails.page2,
        deceasedDetailsPage3: steps.solicitorApplyProbate.deceasedDetails.page3,
        deceasedDetailsPage4: steps.solicitorApplyProbate.deceasedDetails.page4,
        grantOfProbatePage1: steps.solicitorApplyProbate.grantOfProbate.page1,
        grantOfProbatePage2: steps.solicitorApplyProbate.grantOfProbate.page2,
        verifyTitleAndClearingTypeOptionPage: steps.solicitorApplyProbate.grantOfProbate.verifyTitleAndClearingTypeOption,
        verifyTitleAndClearingTypeOptionsPage: steps.solicitorApplyProbate.grantOfProbate.verifyTitleAndClearingTypeOptions,
        grantOfProbatePage3: steps.solicitorApplyProbate.grantOfProbate.page3,
        grantOfProbatePage4: steps.solicitorApplyProbate.grantOfProbate.page4,
        grantOfProbatePage5: steps.solicitorApplyProbate.grantOfProbate.page5,
        grantOfProbatePage6: steps.solicitorApplyProbate.grantOfProbate.page6,
        completeApplicationPage1: steps.solicitorApplyProbate.completeApplication.page1,
        completeApplicationPage2: steps.solicitorApplyProbate.completeApplication.page2,
        completeApplicationPage3: steps.solicitorApplyProbate.completeApplication.page3,
        completeApplicationPage4: steps.solicitorApplyProbate.completeApplication.page4,
        completeApplicationPage5: steps.solicitorApplyProbate.completeApplication.page5,
        completeApplicationPage6: steps.solicitorApplyProbate.completeApplication.page6,
        completeApplicationPage7: steps.solicitorApplyProbate.completeApplication.page7,
        completeApplicationPage8: steps.solicitorApplyProbate.completeApplication.page8,
        makePaymentPage1: steps.solicitorApplyProbate.makePayment.paymentPage1,
        reviewPaymentDetails: steps.solicitorApplyProbate.makePayment.reviewPaymentPage,
        postPaymentReviewDetails: steps.solicitorApplyProbate.makePayment.postPaymentReviewPage,
        makePaymentPage2: steps.solicitorApplyProbate.makePayment.paymentPage2,
        viewPaymentStatus: steps.solicitorApplyProbate.makePayment.viewPaymentStatusPage,
        makeCaveatPaymentPage1: steps.solicitorApplyProbate.makePayment.caveatPaymentPage1,
        admonWillDetailsPage1: steps.solicitorApplyProbate.admonWillDetails.page1,
        admonWillDetailsPage2: steps.solicitorApplyProbate.admonWillDetails.page2,
        admonWillDetailsPage3: steps.solicitorApplyProbate.admonWillDetails.page3,
        admonWillDetailsPage4: steps.solicitorApplyProbate.admonWillDetails.page4,
        admonWillDetailsPage5: steps.solicitorApplyProbate.admonWillDetails.page5,
        admonWillDetailsPage6: steps.solicitorApplyProbate.admonWillDetails.page6,
        intestacyDetailsPage1: steps.solicitorApplyProbate.intestacyDetails.page1,
        intestacyDetailsPage2: steps.solicitorApplyProbate.intestacyDetails.page2,
        intestacyDetailsPage3: steps.solicitorApplyProbate.intestacyDetails.page3,
        intestacyDetailsPage4: steps.solicitorApplyProbate.intestacyDetails.page4,
        applyCaveatPage1: steps.solicitorApplyCaveat.applyCaveat.page1,
        applyCaveatPage2: steps.solicitorApplyCaveat.applyCaveat.page2,
        caveatApplicationDetailsPage1: steps.solicitorApplyCaveat.applicationDetails.page1,
        caveatApplicationDetailsPage2: steps.solicitorApplyCaveat.applicationDetails.page2,
        completeCaveatApplicationPage1: steps.solicitorApplyCaveat.completeApplication.page1,
        completeCaveatApplicationPage2: steps.solicitorApplyCaveat.completeApplication.page2,
        completeCaveatApplicationPage3: steps.solicitorApplyCaveat.completeApplication.page3,
        getCaseRefFromUrl: steps.utility.getCaseRefFromUrl,
        navigateToCase: steps.utility.navigateToCase,
        rejectCookies: steps.utility.rejectCookies,
        checkAmendApplicantDetailsForSolCreatedApp: steps.createGrantOfProbate.checkAmendApplicantDetailsForSolCreatedApp,
        checkAmendDomAndAssetsForSolCreatedApp: steps.createGrantOfProbate.checkAmendDomAndAssetsForSolCreatedApp,
        shareCaseSelection: steps.shareCase.shareCaseSelection,
        authenticateUserShareCase: steps.IDAM.signInShareCase,
        verifyShareCase: steps.shareCase.verifyShareCase,
        shareCaseVerifyUserRemove: steps.shareCase.shareCaseVerifyUserRemove,
        shareCaseDelete: steps.shareCase.shareCaseDelete,
        authenticateUserNoc: steps.IDAM.signInNoC,
        nocNavigation: steps.noticeOfChange.navigateToNoc,
        nocPage1: steps.noticeOfChange.nocPage1,
        nocPage2: steps.noticeOfChange.nocPage2,
        nocPage3: steps.noticeOfChange.nocPage3,
        nocConfirmationPage: steps.noticeOfChange.nocConfirmationPage,
        chooseResolveStop: steps.resolveStop.resolveStop,
        probateStopCase: steps.stopCaseProbate.stopCaseProbate
    });
};
