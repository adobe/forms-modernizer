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

    it('modernizer tool page returns 200', () => {
        cy.aemRequest({
            url: aem.paths.modernizerTool,
            failOnStatusCode: false
        }).then((response) => {
            expect(response.status, `Tool page at ${aem.paths.modernizerTool} should return 200`).to.eq(200)
        })
    })

    it('modernizer tool page loads in the browser without redirect to login', () => {
        cy.visit(aem.paths.modernizerTool)
        cy.url().should('not.include', 'login')
        cy.get('body').should('be.visible')
    })

    it('modernizer wizard action button is present', () => {
        cy.visit(aem.paths.modernizerTool)
        // Scope to foundation wizard controls only — avoids matching generic submit buttons
        cy.get('[data-foundation-wizard-control-next], .cq-wizard-next')
            .should('exist')
    })
})
