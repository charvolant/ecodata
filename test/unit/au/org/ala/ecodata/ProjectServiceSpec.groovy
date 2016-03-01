package au.org.ala.ecodata

import com.github.fakemongo.Fongo
import grails.test.mixin.TestMixin
import grails.test.mixin.mongodb.MongoDbTestMixin
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */

@TestMixin(MongoDbTestMixin)
class ProjectServiceSpec extends Specification {

    ProjectService service = new ProjectService()
    def stubbedCollectoryService = Stub(CollectoryService)

    def setup() {
        Fongo fongo = new Fongo("ecodata-test")
        mongoDomain(fongo.mongo, [Project, Site, Organisation])

        Site.withTransaction {
            new Site(siteId: 1).save(flush: true)
        }
        Organisation.withTransaction {
            new Organisation(organisationId: 1, name: "organisation").save(flush: true)
        }

        defineBeans {
            commonService(CommonService)
        }
        grailsApplication.mainContext.commonService.grailsApplication = grailsApplication
        service.grailsApplication = grailsApplication
        service.collectoryService = stubbedCollectoryService
    }

    def "test create and update project"() {
        given:
        def projData = [name              : 'test proj', description: 'test proj description', dynamicProperty: 'dynamicProperty',
                        isExternal        : true,
                        name              : 'test1 name',
                        task              : 'test task',
                        aim               : 'test aim',
                        scienceType       : 'test scienceType',
                        difficulty        : 'Easy',
                        termsOfUseAccepted: true,
                        plannedStartDate  : new Date(),
                        organisationId    : 1,
                        projectSiteId     : 1]
        def dataProviderId = 'dp1'
        def dataResourceId = 'dr1'
        stubbedCollectoryService.createDataProviderAndResource(_, _) >> [dataProviderId: dataProviderId, dataResourceId: dataResourceId]
        def updatedData = projData + [description: 'test proj updated description']


        def result, projectId
        when:
        Project.withNewTransaction {
            result = service.create(projData)
            projectId = result.projectId
        }
        then: "ensure the response contains the id of the new project"
        result.status == 'ok'
        projectId != null

        when: "select the new project back from the database"
        def savedProj = Project.findByProjectId(projectId)


        then: "ensure the properties are the same as the original"
        savedProj.name == projData.name
        savedProj.description == projData.description
        savedProj.dataProviderId == dataProviderId
        savedProj.dataResourceId == dataResourceId
        //savedProj['dynamicProperty'] == projData.dynamicProperty  The dbo property on the domain object appears to be missing during unit tests which prevents dynamic properties from being retreived.

        when:
        Project.withNewTransaction {
            result = service.update(updatedData, projectId)
        }
        then: "ensure the response status is ok and the project was updated"
        result.status == 'ok'


        when: "select the updated project back from the database"
        savedProj = Project.findByProjectId(projectId)


        then: "ensure the unchanged properties are the same as the original"
        savedProj.name == projData.name
        //savedProj['dynamicProperty'] == projData.dynamicProperty  The dbo property on the domain object appears to be missing during unit tests which prevents dynamic properties from being retreived.

        then: "ensure the updated properties are the same as the change"
        savedProj.description == updatedData.description

    }

    def "test project validation"() {
        given:
        def projData = [description       : 'test proj description', dynamicProperty: 'dynamicProperty',
                        isExternal        : true,
                        name              : 'test2 name',
                        task              : 'test task',
                        aim               : 'test aim',
                        scienceType       : 'test scienceType',
                        difficulty        : 'Easy',
                        termsOfUseAccepted: true,
                        plannedStartDate  : new Date(),
                        organisationId    : 1,
                        projectSiteId     : 1]
        service.collectoryService.createDataProviderAndResource(_, _) >> [:]

        when:
        def result = service.create(projData)

        then:
        result.status == 'error'
        result.error != null

    }

    def "test create project validation"() {
        given:
        def projData = [description       : 'test proj description', dynamicProperty: 'dynamicProperty',
                        isExternal        : true,
                        name              : 'test3 name',
                        task              : 'test task',
                        aim               : 'test aim',
                        scienceType       : 'test scienceType',
                        difficulty        : 'Easy',
                        termsOfUseAccepted: true,
                        plannedStartDate  : new Date(),
                        organisationId    : 1,
                        projectSiteId     : 1]

        service.collectoryService.createDataProviderAndResource(_, _) >> [:]

        when: "valid project"
        def error = service.validate(projData)

        then: "no error"
        error == null

        when: "invalid project missing name"
        error = service.validate(projData.findAll { it.key != "name" })

        then: "error project missing name"
        error != null

        when: "invalid project missing description"
        error = service.validate(projData.findAll { it.key != "description" })

        then: "error project missing description"
        error != null

        when: "invalid project missing isExternal"
        error = service.validate(projData.findAll { it.key != "isExternal" })

        then: "error project missing isExternal"
        error != null

        when: "invalid project missing task"
        error = service.validate(projData.findAll { it.key != "task" })

        then: "error project missing task"
        error != null

        when: "invalid project missing aim"
        error = service.validate(projData.findAll { it.key != "aim" })

        then: "error project missing aim"
        error != null

        when: "invalid project missing scienceType"
        error = service.validate(projData.findAll { it.key != "scienceType" })

        then: "error project missing scienceType"
        error != null

        when: "invalid project missing difficulty"
        error = service.validate(projData.findAll { it.key != "difficulty" })

        then: "error project missing difficulty"
        error != null

        when: "invalid project missing termsOfUseAccepted"
        error = service.validate(projData.findAll { it.key != "termsOfUseAccepted" })

        then: "error project missing termsOfUseAccepted"
        error != null

        when: "invalid project missing plannedStartDate"
        error = service.validate(projData.findAll { it.key != "plannedStartDate" })

        then: "error project missing plannedStartDate"
        error != null

        when: "invalid project missing organisationId"
        error = service.validate(projData.findAll { it.key != "organisationId" })

        then: "error project missing organisationId"
        error != null

        when: "invalid project missing projectSiteId"
        error = service.validate(projData.findAll { it.key != "projectSiteId" })

        then: "error project missing projectSiteId"
        error != null

        when: "project invalid difficulty"
        def invalidData = projData.findAll { it.key != "difficulty" }
        invalidData.put("difficulty", "invalid")
        error = service.validate(invalidData)

        then: "no error project invalid difficulty"
        error != null


        when: "invalid project invalid projectSiteId"
        invalidData = projData.findAll { it.key != "projectSiteId" }
        invalidData.put("projectSiteId", 2)
        error = service.validate(invalidData)

        then: "error project invalid projectSiteId"
        error != null

        when: "invalid project invalid organisationId"
        invalidData = projData.findAll { it.key != "organisationId" }
        invalidData.put("organisationId", 2)
        error = service.validate(invalidData)

        then: "error project invalid organisationId"
        error != null
        invalidData.organisationName == 'organisation'
    }

    def "test update project validation"() {
        given:
        def projData = [description       : 'test proj description', dynamicProperty: 'dynamicProperty',
                        isExternal        : true,
                        name              : 'test4 name',
                        task              : 'test task',
                        aim               : 'test aim',
                        scienceType       : 'test scienceType',
                        difficulty        : 'Easy',
                        termsOfUseAccepted: true,
                        plannedStartDate  : new Date(),
                        organisationId    : 1,
                        projectSiteId     : 1]

        service.collectoryService.createDataProviderAndResource(_, _) >> [:]

        def result
        def projectId
        Project.withNewTransaction {
            result = service.create(projData)
            projectId = result.projectId
        }
        projData.remove("name")

        when: "valid project"
        def error = service.validate(projData, projectId)

        then: "no error"
        error == null

        when: "project missing name"
        error = service.validate(projData.findAll { it.key != "name" }, projectId)

        then: "no error project missing name"
        error == null

        when: "project missing description"
        error = service.validate(projData.findAll { it.key != "description" }, projectId)

        then: "no error project missing description"
        error == null

        when: "project missing isExternal"
        error = service.validate(projData.findAll { it.key != "isExternal" }, projectId)

        then: "no error project missing isExternal"
        error == null

        when: "project missing task"
        error = service.validate(projData.findAll { it.key != "task" }, projectId)

        then: "no error project missing task"
        error == null

        when: "project missing aim"
        error = service.validate(projData.findAll { it.key != "aim" }, projectId)

        then: "no error project missing aim"
        error == null

        when: "project missing scienceType"
        error = service.validate(projData.findAll { it.key != "scienceType" }, projectId)

        then: "no error project missing scienceType"
        error == null

        when: "project missing difficulty"
        error = service.validate(projData.findAll { it.key != "difficulty" }, projectId)

        then: "no error project missing difficulty"
        error == null

        when: "project missing termsOfUseAccepted"
        error = service.validate(projData.findAll { it.key != "termsOfUseAccepted" }, projectId)

        then: "no error project missing termsOfUseAccepted"
        error == null

        when: "project missing plannedStartDate"
        error = service.validate(projData.findAll { it.key != "plannedStartDate" }, projectId)

        then: "no error project missing plannedStartDate"
        error == null

        when: "project missing organisationId"
        error = service.validate(projData.findAll { it.key != "organisationId" }, projectId)

        then: "no error project missing organisationId"
        error == null

        when: "project missing projectSiteId"
        error = service.validate(projData.findAll { it.key != "projectSiteId" }, projectId)

        then: "no error project missing projectSiteId"
        error == null

        when: "project invalid difficulty"
        def invalidData = projData.findAll { it.key != "difficulty" }
        invalidData.put("difficulty", "invalid")
        error = service.validate(invalidData, projectId)

        then: "no error project invalid difficulty"
        error != null

        when: "invalid project invalid projectSiteId"
        invalidData = projData.findAll { it.key != "projectSiteId" }
        invalidData.put("projectSiteId", 2)
        error = service.validate(invalidData, projectId)

        then: "error project invalid projectSiteId"
        error != null

        when: "invalid project invalid organisationId"
        invalidData = projData.findAll { it.key != "organisationId" }
        invalidData.put("organisationId", 2)
        error = service.validate(invalidData, projectId)

        then: "error project invalid organisationId"
        error != null
        invalidData.organisationName == 'organisation'
    }
}
