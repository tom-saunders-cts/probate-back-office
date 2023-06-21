'use strict';

const commonConfig = require('src/test/end-to-end/pages/common/commonConfig');
const testConfig = require('src/test/config.js');

module.exports = async function (transferToState) {

    const I = this;
    await I.waitForEnabled({css: '#transferToState'}, testConfig.WaitForTextTimeout || 60);
    await I.selectOption('#transferToState', transferToState);
    await I.waitForEnabled(commonConfig.submitButton, testConfig.WaitForTextTimeout || 60);
    await I.wait(testConfig.CaseworkerGoButtonClickDelay);
    await I.waitForNavigationToComplete(commonConfig.submitButton);
};
