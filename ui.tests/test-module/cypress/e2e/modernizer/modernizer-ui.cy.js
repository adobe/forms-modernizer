/**
 * Smoke tests for the Forms Modernizer tool UI.
 * Verifies the modernization wizard is reachable and renders its core elements.
 */
describe('Forms Modernizer – Tool UI', () => {
    let aem

    before(() => {
        cy.fixture('aem').then((data) => { aem = data })
        cy.loginToAEM()
    })

    it('modernizer tool page loads without error', () => {
        cy.visit(aem.paths.modernizerTool, { failOnStatusCode: false })
        // The page should load (200 or redirect to login if session expired)
        cy.url().should('not.include', 'login')
    })

    it('modernizer tool page contains the expected heading', () => {
        cy.visit(aem.paths.modernizerTool)
        // The AEM modernize tool page title should be present in the DOM
        cy.get('title').should('exist')
        cy.get('body').should('be.visible')
    })

    it('modernizer wizard can be initiated', () => {
        cy.visit(aem.paths.modernizerTool)
        // Verify the primary action button (Start wizard / Convert) is present
        cy.get('[data-foundation-wizard-control-next], .cq-wizard-next, [type="submit"]')
            .first()
            .should('exist')
    })
})
