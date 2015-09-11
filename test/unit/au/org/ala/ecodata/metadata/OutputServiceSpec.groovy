package au.org.ala.ecodata.metadata

import au.org.ala.ecodata.Activity
import au.org.ala.ecodata.CommonService
import au.org.ala.ecodata.MetadataService
import au.org.ala.ecodata.Output
import au.org.ala.ecodata.OutputService
import au.org.ala.ecodata.Record
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import net.sf.json.groovy.JsonSlurper
import spock.lang.Specification

@TestFor(OutputService)
@Mock([Activity, Output, Record])
class OutputServiceSpec extends Specification {

    OutputService service
    MetadataService mockMetadataService

    def setup() {
        service = new OutputService()
        mockMetadataService = Mock(MetadataService)
        service.metadataService = mockMetadataService
        service.grailsApplication = [mainContext: [commonService: Mock(CommonService)]]
    }

    def "create output should create a record if the output data model has record = true"() {
        setup:
        String activityId = 'activity1'
        Activity activity = new Activity(activityId: activityId, type: 'Test', description: 'A test activity')
        activity.save(flush: true, failOnError: true)
        mockMetadataService.getOutputDataModelByName(_) >> [modelName: "Single Sighting",
                                                            dataModel: [[record: "true", dataType: "singleSighting"]]]

        when:
        def request = """{
            "activityId": "activity1",
            "name": "Single Sighting",
            "data": {
                    "userId"          : "10598",
                    "individualCount" : "3",
                    "decimalLatitude" : "-31.203404950917385",
                    "decimalLongitude": "146.95312499999997"
            }
        }"""

        Map response = service.create(new JsonSlurper().parseText(request))

        then:
        println response
        response.status != "error"
        Output.count() == 1
        Record.count() == 1
    }

    def "create output should create records for each list item if the output data model has record = true and data type = list"() {
        setup:
        String activityId = 'activity1'
        Activity activity = new Activity(activityId: activityId, type: 'Test', description: 'A test activity')
        activity.save(flush: true, failOnError: true)
        mockMetadataService.getOutputDataModelByName(_) >> [modelName: "Model 1",
                                                            dataModel: [
                                                                    [
                                                                            record: "true",
                                                                            name: "actions",
                                                                            dataType: "list",
                                                                    columns: [
                                                                            [
                                                                                    name: "col1"
                                                                            ]
                                                                    ]]]]

        when:
        def request = """{
            "activityId": "activity1",
            "name": "Something",
            "data": {
                    "actions": [
                        {"col1": "action1"},
                        {"col1": "action2"}
                    ]
            }
        }"""

        Map response = service.create(new JsonSlurper().parseText(request))

        then:
        println response
        response.status != "error"
        Output.count() == 1
        Record.count() == 2
    }
}
