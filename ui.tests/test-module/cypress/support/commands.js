const aemUser = () => Cypress.env('AEM_AUTHOR_USERNAME') || 'admin'
const aemPass = () => Cypress.env('AEM_AUTHOR_PASSWORD') || 'admin'

/**
 * Log into AEM via the login page and store the session cookie.
 * Uses cy.session() so login only happens once per spec unless credentials change.
 */
Cypress.Commands.add('loginToAEM', (username = aemUser(), password = aemPass()) => {
    cy.session([username, password], () => {
        cy.visit('/libs/granite/core/content/login.html')
        cy.get('#username').clear().type(username)
        cy.get('#password').clear().type(password)
        cy.get('#submit-button').click()
        cy.url().should('not.include', 'login')
    })
})

/**
 * Query the OSGi bundle REST endpoint and assert the bundle is in "Active" state.
 *
 * @param {string} symbolicName - OSGi Bundle-SymbolicName
 */
Cypress.Commands.add('assertBundleActive', (symbolicName) => {
    cy.request({
        method: 'GET',
        url: `/system/console/bundles/${symbolicName}.json`,
        auth: { user: aemUser(), pass: aemPass() }
    }).then((response) => {
        expect(response.status).to.eq(200)
        const bundle = response.body.data && response.body.data[0]
        expect(bundle, `Bundle "${symbolicName}" not found in OSGi console`).to.exist
        expect(bundle.state, `Bundle "${symbolicName}" is not Active`).to.eq('Active')
    })
})

/**
 * Perform an authenticated AEM HTTP request (bypasses CSRF for read-only checks).
 */
Cypress.Commands.add('aemRequest', (options) => {
    cy.request({
        ...options,
        auth: { user: aemUser(), pass: aemPass() }
    })
})
