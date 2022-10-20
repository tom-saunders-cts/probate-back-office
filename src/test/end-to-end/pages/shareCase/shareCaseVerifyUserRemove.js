'use strict';

const testConfig = require('src/test/config.js');
const commonConfig = require('src/test/end-to-end/pages/common/commonConfig');

module.exports = async function (verifyUnsharedCase){
    const I = this;
    await I.waitForText('Your cases', 20);
    await I.wait(testConfig.CreateCaseDelay);
    await I.click('//div[normalize-space()="Case reference"]');
    await I.wait(2);
    await I.dontSeeElement('//input[@id="select-'+caseRefNumber+'"]');
    await I.click('//a[normalize-space()="Sign out"]');
   // await I.logInfo(scenarioName, 'PP1 User verified unshared caseRef: '+caseRef+'');
};
