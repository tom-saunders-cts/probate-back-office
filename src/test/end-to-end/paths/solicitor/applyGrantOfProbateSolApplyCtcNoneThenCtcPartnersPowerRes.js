'use strict';

const testConfig = require('src/test/config');
const createCaseConfig = require('src/test/end-to-end/pages/createCase/createCaseConfig');

const commonConfig = require('src/test/end-to-end/pages/common/commonConfig');

Feature('Solicitor - Apply Grant of probate').retry(testConfig.TestRetryFeatures);
const scenarioName = 'Solicitor - Apply Grant of Probate - SolApply - CTC NoneOfThese -> CTC Partners PowerRes';
Scenario(scenarioName, async function ({I}) {

    const applyFN = 'SolApplyFN';
    const applyLN = 'SolApplyLN';
    const applyName = applyFN + ' ' + applyLN;

    const sigFN = 'SolSignFN';
    const sigLN = 'SolSignLN';
    const sigName = sigFN + ' ' + sigLN;

    const decFN = 'DecFN';
    const decLN = 'DecLN';
    const decName = decFN + ' ' + decLN;

    const decDobDay = 1;
    const decDobMon = 1;
    const decDobYear = 1990;

    const decDodDay = 10;
    const decDodMonth = 10;
    const decDodYear = 2024;

    const firmName = 'Solicitor Applying Firm';
    const address = {
        'line1': 'Buckingham Palace',
        'town': 'London',
        'postcode': 'SW1A 1AA',
        'country': 'United Kingdom',
    };
    const solEmail = 'probatesolicitortestorgtest2@gmail.com';
    const solPhone = '01234 012345';
    const probRef = 'ProbRef';

    const ihtGrossValue = 100;
    const ihtNetValue = 90;

    const uniqueHmrcCode = 'CTS 040523 1104 3tpp s8e9';

    const willSignedDay = 10;
    const willSignedMonth = 10;
    const willSignedYear = 2024;

    const firstExecFN = 'FirstExecFN';
    const firstExecLN = 'FirstExecLN';
    const firstExecName = firstExecFN + ' ' + firstExecLN;

    const secondExecFN = 'SecondExecFN';
    const secondExecLN = 'SecondExecLN';
    const secondExecName = secondExecFN + ' ' + secondExecLN;

    const firstPartnerExecFN = 'FirstPartnerExecFN';
    const firstPartnerExecLN = 'FirstPartnerExecLN';
    const firstPartnerExecName = firstPartnerExecFN + ' ' + firstPartnerExecLN;

    const secondPartnerExecFN = 'SecondPartnerExecFN';
    const secondPartnerExecLN = 'SecondPartnerExecLN';
    const secondPartnerExecName = secondPartnerExecFN + ' ' + secondPartnerExecLN;

    await I.logInfo(scenarioName, 'Login as Solicitor');
    await I.authenticateWithIdamIfAvailable(true);

    const firstStep = 'Create application';
    await I.logInfo(scenarioName, firstStep);
    await I.selectNewCase();
    await I.selectCaseTypeOptions(createCaseConfig.list2_text_gor, createCaseConfig.list3_text_solGor);

    // applyForProbate Page 1
    {
        await I.waitForElement('#solsStartPage');
        await I.waitForNavigationToComplete(commonConfig.submitButton, true);
    }

    // applyForProbate Page 2
    {
        await I.waitForElement('#solsApplyPage');

        await I.waitForElement(`#solsSolicitorWillSignSOT_No`);
        await I.click(`#solsSolicitorWillSignSOT_No`);
        await I.fillField('#solsForenames', applyFN);
        await I.fillField('#solsSurname', applyLN);

        await I.fillField('#solsSOTForenames', sigFN);
        await I.fillField('#solsSOTSurname', sigLN);

        await I.click({css: '#solsSolicitorIsExec_Yes'});
        await I.waitForVisible({css: '#applyForProbatePageHint1'});

        await I.click({css: '#solsSolicitorIsApplying_Yes'});
        await I.waitForVisible({css: '#applyForProbatePageHint1'});

        await I.fillField('#solsSolicitorFirmName', firmName);

        // Manually enter the address
        await I.click('.manual-link');
        await I.fillField('#solsSolicitorAddress__detailAddressLine1', address.line1);
        await I.fillField('#solsSolicitorAddress__detailPostTown', address.town);
        await I.fillField('#solsSolicitorAddress__detailPostCode', address.postcode);
        await I.fillField('#solsSolicitorAddress__detailCountry', address.country);

        await I.fillField('#solsSolicitorEmail', solEmail);
        await I.fillField('#solsSolicitorPhoneNumber', solPhone);
        await I.fillField('#solsSolicitorAppReference', probRef);

        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    await I.cyaPage();

    const firstEndState = 'Application created';
    const secondStep = 'Deceased details'
    await I.seeEndState(firstEndState);

    const caseRef = await I.getCaseRefFromUrl();

    await I.logInfo(scenarioName, secondStep, caseRef);
    await I.chooseNextStep(secondStep);
    // deceasedDetails Page 1
    {
        await I.waitForElement('#deceasedForenames');
        await I.fillField('#deceasedForenames', decFN);
        await I.fillField('#deceasedSurname', decLN);

        await I.fillField('#deceasedDateOfBirth-day', decDobDay);
        await I.fillField('#deceasedDateOfBirth-month', decDobMon);
        await I.fillField('#deceasedDateOfBirth-year', decDobYear);

        await I.fillField('#deceasedDateOfDeath-day', decDodDay);
        await I.fillField('#deceasedDateOfDeath-month', decDodMonth);
        await I.fillField('#deceasedDateOfDeath-year', decDodYear);

        await I.click(`#deceasedDomicileInEngWales_Yes`);

        // Manually enter address
        await I.click('.manual-link');
        await I.fillField('#deceasedAddress__detailAddressLine1', address.line1);
        await I.fillField('#deceasedAddress__detailPostTown', address.town);
        await I.fillField('#deceasedAddress__detailPostCode', address.postcode);
        await I.fillField('#deceasedAddress__detailCountry', address.country);

        await I.click(`#deceasedAnyOtherNames_No`);

        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // deceasedDetails Page 2
    {
        await I.click({css: '#ihtFormEstateValuesCompleted_Yes'});
        await I.click({css: '#ihtFormEstate-IHT400'});
        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // deceasedDetails Page 3
    {
        await I.click({css: '#hmrcLetterId_Yes'});
        await I.fillField({css: '#uniqueProbateCodeId'}, uniqueHmrcCode);
        await I.waitForNavigationToComplete(commonConfig.continueButton);
    }
    // deceasedDetails Page 4
    {
        await I.waitForElement({css: '#ihtGrossValue'});
        await I.fillField({css: '#ihtGrossValue'}, ihtGrossValue);
        await I.fillField({css: '#ihtFormNetValue'}, ihtNetValue);
        await I.waitForNavigationToComplete(commonConfig.continueButton);
    }
    // deceasedDetails Page 5
    {
        await I.waitForElement('#solsWillType');
        await I.runAccessibilityTest();
        await I.click(`#solsWillType-WillLeft`);
        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // deceasedDetails Page 6
    {
        await I.waitForElement('#willDispose');

        await I.click('#willDispose_Yes');
        await I.click('#englishWill_Yes');
        await I.click('#appointExec_Yes');

        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    await I.cyaPage();

    const secondEndState = 'Grant of probate created';
    await I.seeEndState(secondEndState);

    const thirdStep = 'Grant of probate details';
    await I.logInfo(scenarioName, thirdStep, caseRef);
    await I.chooseNextStep(thirdStep);
    // grantOfProbate Page 1
    {
        await I.waitForElement({css: '#willAccessOriginal'});

        await I.click({css: '#willAccessOriginal_Yes'});

        await I.fillField({css: '#originalWillSignedDate-day'}, willSignedDay);
        await I.fillField({css: '#originalWillSignedDate-month'}, willSignedMonth);
        await I.fillField({css: '#originalWillSignedDate-year'}, willSignedYear);

        await I.click({css: '#willHasCodicils_No'});

        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // grantOfProbate Page 2
    {
        await I.click({css: '#dispenseWithNotice_No'});
        await I.click({css: '#titleAndClearingType-TCTNoT'});
        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // grantOfProbate Page 3
    {
        await I.click({css: '#otherExecutorExists_Yes'});

        await I.click('#solsAdditionalExecutorList > div > button');
        await I.fillField({css: '#solsAdditionalExecutorList_0_additionalExecForenames'}, firstExecFN);
        await I.fillField({css: '#solsAdditionalExecutorList_0_additionalExecLastname'}, firstExecLN);
        await I.click({css: '#solsAdditionalExecutorList_0_additionalExecNameOnWill_No'});
        await I.click({css: '#solsAdditionalExecutorList_0_additionalApplying_Yes'});

        // Manually enter address
        await I.click('.manual-link');
        await I.fillField('#solsAdditionalExecutorList_0_additionalExecAddress__detailAddressLine1', address.line1);
        await I.fillField('#solsAdditionalExecutorList_0_additionalExecAddress__detailPostTown', address.town);
        await I.fillField('#solsAdditionalExecutorList_0_additionalExecAddress__detailPostCode', address.postcode);
        await I.fillField('#solsAdditionalExecutorList_0_additionalExecAddress__detailCountry', address.country);

        await I.click('#solsAdditionalExecutorList > div > button');
        await I.fillField({css: '#solsAdditionalExecutorList_1_additionalExecForenames'}, secondExecFN);
        await I.fillField({css: '#solsAdditionalExecutorList_1_additionalExecLastname'}, secondExecLN);
        await I.click({css: '#solsAdditionalExecutorList_1_additionalExecNameOnWill_No'});
        await I.click({css: '#solsAdditionalExecutorList_1_additionalApplying_No'});
        await I.click({css: '#solsAdditionalExecutorList_1_additionalExecReasonNotApplying-Renunciation'});

        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // grantOfProbate Page 4
    {
        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // grantOfProbate Page 5
    {
        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    await I.cyaPage();

    const thirdEndState = 'Application updated';
    await I.seeEndState(thirdEndState);

    const fourthStep = 'Complete application';
    await I.logInfo(scenarioName, fourthStep, caseRef);
    await I.chooseNextStep(fourthStep);

    // completeApplication Page 1
    {
        await I.click({css: '#solsSOTNeedToUpdate_Yes'});
        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // completeApplication Page 2
    {
        await I.selectOption('#solsAmendLegalStatmentSelect', '3: WillLeft');
        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // completeApplication Page 3
    {
        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }

    const fourthEndState = 'Grant of probate created';
    await I.seeEndState(fourthEndState);

    const fifthStep = 'Grant of probate details';
    await I.logInfo(scenarioName, fifthStep, caseRef);
    await I.chooseNextStep(fifthStep);

    // grantOfProbate Page 1
    {
        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // grantOfProbate Page 2
    {
        await I.click({css: '#titleAndClearingType-TCTPartPowerRes'});
        await I.fillField({css: '#nameOfFirmNamedInWill'}, firmName);

        await I.click('.manual-link');
        await I.fillField('#addressOfFirmNamedInWill__detailAddressLine1', address.line1);
        await I.fillField('#addressOfFirmNamedInWill__detailPostTown', address.town);
        await I.fillField('#addressOfFirmNamedInWill__detailPostCode', address.postcode);
        await I.fillField('#addressOfFirmNamedInWill__detailCountry', address.country);

        await I.click({css: '#anyOtherApplyingPartners_Yes'});
        await I.click('#otherPartnersApplyingAsExecutors > div > button');
        await I.fillField('#otherPartnersApplyingAsExecutors_0_additionalExecForenames', firstPartnerExecFN);
        await I.fillField('#otherPartnersApplyingAsExecutors_0_additionalExecLastname', firstPartnerExecLN);

        await I.click('.manual-link');
        await I.fillField('#otherPartnersApplyingAsExecutors_0_additionalExecAddress__detailAddressLine1', address.line1);
        await I.fillField('#otherPartnersApplyingAsExecutors_0_additionalExecAddress__detailPostTown', address.town);
        await I.fillField('#otherPartnersApplyingAsExecutors_0_additionalExecAddress__detailPostCode', address.postcode);
        await I.fillField('#otherPartnersApplyingAsExecutors_0_additionalExecAddress__detailCountry', address.country);

        await I.click({css: '#morePartnersHoldingPowerReserved_No'});

        await I.click({css: '#whoSharesInCompanyProfits-partner'});

        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // grantOfProbate Page 3
    {
        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // grantOfProbate Page 4
    {
        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // grantOfProbate Page 5
    {
        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    await I.cyaPage();

    const fifthEndState = 'Application updated';
    await I.seeEndState(fifthEndState);

    const sixthStep = 'Complete application';
    await I.logInfo(scenarioName, sixthStep, caseRef);
    await I.chooseNextStep(sixthStep);

    // completeApplication Page 1
    {
        await I.click({css: '#solsSOTNeedToUpdate_No'});
        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // completeApplication Page 2
    {
        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // completeApplication Page 3
    {
        await I.click({css: '#solsReviewSOTConfirmCheckbox1-BelieveTrue'});
        await I.click({css: '#solsReviewSOTConfirmCheckbox2-BelieveTrue'});

        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // completeApplication Page 4
    {
        await I.fillField({css: '#extraCopiesOfGrant'}, "0");
        await I.fillField({css: '#outsideUKGrantCopies'}, "0");

        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // completeApplication Page 5
    {
        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }
    // completeApplication Page 6
    {
        await I.waitForNavigationToComplete(commonConfig.continueButton, true);
    }

    await I.logInfo(scenarioName, `${testConfig.TestBackOfficeUrl}` + '/cases/case-details/' + caseRef.replaceAll('-', '') + '#Legal%20Statement');
});