const testConfig = require('src/test/config.js');

exports.config = {
    'tests': testConfig.TestPathToRun,
    'output': testConfig.TestOutputDir,
    'helpers': {
        'Playwright': {
            'waitForTimeout': 60000,
            'getPageTimeout': 60000,
            'waitForAction': 1000,
            'show': testConfig.TestShowBrowserWindow,
            'waitForNavigation': 'domcontentloaded',
            'chrome': {
                'ignoreHTTPSErrors': true,
                'ignore-certificate-errors': true,
                'defaultViewport': {
                    'width': 1280,
                    'height': 960
                },
                args: [
                    // '--headless',
                    '--disable-gpu',
                    '--no-sandbox',
                    '--allow-running-insecure-content',
                    '--ignore-certificate-errors',
                    // '--proxy-server=proxyout.reform.hmcts.net:8080',
                    // '--proxy-bypass-list=*beta*LB.reform.hmcts.net',
                    '--window-size=1440,1400'
                ]
            },

        },
        'PlaywrightHelper': {
            'require': './helpers/PlaywrightHelper.js'
        },
        'JSWait': {
            require: './helpers/JSWait.js'
        },
        'Mochawesome': {
            uniqueScreenshotNames: 'true'
        }
    },
    'include': {
        'I': './pages/steps.js'
    },
    'plugins': {
        'autoDelay': {
            'enabled': testConfig.TestAutoDelayEnabled
        },
        screenshotOnFail: {
            enabled: true,
            fullPageScreenshots: 'true'
        }
    },
    'mocha': {
        'reporterOptions': {
            'reportDir': testConfig.TestOutputDir,
            'reportName': 'index',
            'inlineAssets': true
        }
    },
    'name': 'Codecept Tests'
};
