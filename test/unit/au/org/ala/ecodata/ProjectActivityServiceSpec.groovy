package au.org.ala.ecodata

import com.github.fakemongo.Fongo
import grails.test.mixin.TestMixin
import grails.test.mixin.mongodb.MongoDbTestMixin
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */

@TestMixin(MongoDbTestMixin)
class ProjectActivityServiceSpec extends Specification {

    ProjectActivityService service = new ProjectActivityService()
    def stubbedCollectoryService = Stub(CollectoryService)
    def stubbedUserService = Stub(UserService)
    def mockedPermissionService = Mock(PermissionService)
    MetadataService metadataService

    def setup() {
        Fongo fongo = new Fongo("ecodata-test")
        mongoDomain(fongo.mongo, [ProjectActivity, Project, Site])

        def result
        Project.withNewTransaction {
            result = new Project(projectId: "1", name: "test").save(flush: true)
        }

        Site.withNewTransaction {
            result = new Site(siteId: "1").save(flush: true)
        }

        service.commonService = new CommonService()
        service.commonService.grailsApplication = grailsApplication
        service.permissionService = mockedPermissionService
        metadataService = Mock(MetadataService)
        service.metadataService = metadataService

    }

    def "test create project activity validation"() {
        given:
        def actData = [name: 'test act1', description: 'test act description', dynamicProperty: 'dynamicProperty',
                        attribution: "",
                        startDate: new Date(),
                        projectId: 1,
                        status: "active"]

        def error

        when: "valid project activity"
        error = service.validate(actData)
        then: "no error"
        error == null

        when: "missing project activity name"
        error = service.validate(actData.findAll { it.key != "name" })
        then: "error, no name"
        error != null

        when: "missing project activity description"
        error = service.validate(actData.findAll { it.key != "description" })
        then: "error, no description"
        error != null

        when: "missing project activity startDate"
        error = service.validate(actData.findAll { it.key != "startDate" })
        then: "error, no startDate"
        error != null

        when: "missing project activity attribution"
        error = service.validate(actData.findAll { it.key != "attribution" })
        then: "error, no attribution"
        error != null

        when: "missing project activity projectId"
        error = service.validate(actData.findAll { it.key != "projectId" })
        then: "error, no projectId"
        error != null

        when: "valid project activity sites"
        def tmpData = actData.findAll { true }
        tmpData.put("sites", ["1"])
        error = service.validate(tmpData)
        then: "no error"
        error == null

        when: "invalid project activity sites"
        tmpData = actData.findAll { true }
        tmpData.put("sites", ["0"])
        error = service.validate(tmpData)
        then: "error, invalid siteId"
        error != null

        when: "valid project activity species ALL_SPECIES"
        tmpData = actData.findAll { true }
        tmpData.put("species", [type: "ALL_SPECIES"])
        error = service.validate(tmpData)
        then: "no error"
        error == null

        when: "valid project activity species SINGLE_SPECIES"
        tmpData = actData.findAll { true }
        tmpData.put("species", [type: "SINGLE_SPECIES", singleSpecies: [guid: "1", name: "0"]])
        error = service.validate(tmpData)
        then: "no error"
        error == null

        when: "valid project activity species GROUP_OF_SPECIES"
        tmpData = actData.findAll { true }
        tmpData.put("species", [type: "GROUP_OF_SPECIES", speciesLists: [[dataResourceUid: "1", listName: "0"]]])
        error = service.validate(tmpData)
        then: "no error"
        error == null

        when: "invalid project activity species"
        tmpData = actData.findAll { true }
        tmpData.put("species", "0")
        error = service.validate(tmpData)
        then: "error, invalid species"
        error != null

        when: "invalid project activity species type"
        tmpData = actData.findAll { true }
        tmpData.put("species", [type: "0"])
        error = service.validate(tmpData)
        then: "error, invalid species type"
        error != null
    }

    def "test update project activity validation"() {
        given:
        def actData = [name: 'test act1', description: 'test act description', dynamicProperty: 'dynamicProperty',
                       attribution: "",
                       startDate: new Date(),
                       projectId: 1,
                       status: "active"]

        def projectActivityId
        ProjectActivity.withNewTransaction {
            def result = service.create(actData)
            projectActivityId = result.projectActivityId
        }

        def error

        when: "valid project activity"
        error = service.validate(actData, projectActivityId)
        then: "no error"
        error == null

        when: "missing project activity name"
        error = service.validate(actData.findAll { it.key != "name" }, projectActivityId)
        then: "no error"
        error == null

        when: "missing project activity description"
        error = service.validate(actData.findAll { it.key != "description" }, projectActivityId)
        then: "no error"
        error == null

        when: "missing project activity startDate"
        error = service.validate(actData.findAll { it.key != "startDate" }, projectActivityId)
        then: "no error"
        error == null

        when: "missing project activity attribution"
        error = service.validate(actData.findAll { it.key != "attribution" }, projectActivityId)
        then: "no error"
        error == null

        when: "missing project activity projectId"
        error = service.validate(actData.findAll { it.key != "projectId" }, projectActivityId)
        then: "no error"
        error == null

        when: "valid project activity sites"
        def tmpData = actData.findAll { true }
        tmpData.put("sites", ["1"])
        error = service.validate(tmpData, projectActivityId)
        then: "no error"
        error == null

        when: "invalid project activity sites"
        tmpData = actData.findAll { true }
        tmpData.put("sites", ["0"])
        error = service.validate(tmpData, projectActivityId)
        then: "error, invalid siteId"
        error != null

        when: "valid project activity species ALL_SPECIES"
        tmpData = actData.findAll { true }
        tmpData.put("species", [type: "ALL_SPECIES"])
        error = service.validate(tmpData, projectActivityId)
        then: "no error"
        error == null

        when: "valid project activity species SINGLE_SPECIES"
        tmpData = actData.findAll { true }
        tmpData.put("species", [type: "SINGLE_SPECIES", singleSpecies: [guid: "1", name: "0"]])
        error = service.validate(tmpData, projectActivityId)
        then: "no error"
        error == null

        when: "valid project activity species GROUP_OF_SPECIES"
        tmpData = actData.findAll { true }
        tmpData.put("species", [type: "GROUP_OF_SPECIES", speciesLists: [[dataResourceUid: "1", listName: "0"]]])
        error = service.validate(tmpData, projectActivityId)
        then: "no error"
        error == null

        when: "invalid project activity species"
        tmpData = actData.findAll { true }
        tmpData.put("species", "0")
        error = service.validate(tmpData, projectActivityId)
        then: "error, invalid species"
        error != null

        when: "invalid project activity species type"
        tmpData = actData.findAll { true }
        tmpData.put("species", [type: "0"])
        error = service.validate(tmpData, projectActivityId)
        then: "error, invalid species type"
        error != null
    }


}
