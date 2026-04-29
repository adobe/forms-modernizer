const { defineConfig } = require('cypress')
const { installLogsPrinter } = require('cypress-terminal-report/src/installLogsPrinter')
const fs = require('fs')

module.exports = defineConfig({
  e2e: {
    baseUrl: process.env.AEM_AUTHOR_URL || 'http://localhost:4502',
    specPattern: 'cypress/e2e/**/*.cy.{js,jsx}',
    supportFile: 'cypress/support/e2e.js',
    fixturesFolder: 'cypress/fixtures',
    screenshotsFolder: 'target/screenshots',
    videosFolder: 'target/videos',
    viewportWidth: 1440,
    viewportHeight: 900,
    pageLoadTimeout: 120000,
    responseTimeout: 60000,
    retries: {
      runMode: 2,
      openMode: 0
    },
    video: true,
    // videoUploadOnPasses was removed in Cypress 13; delete passing-test recordings via after:spec below
    reporters: ['junit', 'spec'],
    reporterOptions: {
      mochaFile: 'target/cypress-results/results-[hash].xml',
      toConsole: true
    },
    setupNodeEvents(on, config) {
      installLogsPrinter(on, {
        printLogsToConsole: 'onFail'
      })

      on('after:spec', (_spec, results) => {
        if (results && results.video && results.stats.failures === 0) {
          fs.unlinkSync(results.video)
        }
      })

      return config
    }
  }
})
