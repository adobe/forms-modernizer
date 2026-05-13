/**
 * Verifies that the forms-modernizer core OSGi bundle is deployed and Active.
 * These tests run against a live AEM instance and do not require browser UI.
 */
describe('Forms Modernizer – Bundle Status', () => {
    let aem

    before(() => {
        cy.fixture('aem').then((data) => { aem = data })
    })

    it('core bundle is in Active state', () => {
        cy.assertBundleActive(aem.bundles.core)
    })

    it('rules node exists under /apps/forms-modernizer', () => {
        cy.aemRequest({
            url: `${aem.paths.rulesRoot}.json`,
            failOnStatusCode: false
        }).then((response) => {
            expect(response.status, 'Rules node should be accessible').to.eq(200)
        })
    })

    it('proxy-rules node exists under /apps/forms-modernizer', () => {
        cy.aemRequest({
            url: `${aem.paths.proxyRulesRoot}.json`,
            failOnStatusCode: false
        }).then((response) => {
            expect(response.status, 'Proxy-rules node should be accessible').to.eq(200)
        })
    })
})
