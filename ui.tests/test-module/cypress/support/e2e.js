import './commands'
import installLogsCollector from 'cypress-terminal-report/src/installLogsCollector'

installLogsCollector()

// Suppress known AEM framework errors that are unrelated to test assertions
Cypress.on('uncaught:exception', (err) => {
    const ignored = [
        'ResizeObserver loop limit exceeded',
        'ResizeObserver loop completed with undelivered notifications',
        'Cannot read properties of null',
        'Non-Error promise rejection captured'
    ]
    if (ignored.some((msg) => err.message.includes(msg))) {
        return false
    }
})
