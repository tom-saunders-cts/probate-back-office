'use strict';

const testConfig = require('src/test/config.js');

module.exports = async function (signInDelay = testConfig.SignInDelayDefault) {

    const I = this;
    I.amOnLoadedPage(`${testConfig.TestBackOfficeUrl}/`);
    await I.wait(testConfig.ManualDelayMedium);
    await I.waitForText('Sign in', 600);
    await I.waitForText('Email address');
    await I.waitForText('Password');
    await I.fillField('#username',  testConfig.TestEnvCwSuperUser);
    await I.fillField('#password',  testConfig.TestEnvCwSuperPassword);
    await I.waitForNavigationToComplete('input[type="submit"]', signInDelay);
    await I.dontSee({css: '#username'});
    await I.rejectCookies();
    await I.wait(signInDelay);
};
