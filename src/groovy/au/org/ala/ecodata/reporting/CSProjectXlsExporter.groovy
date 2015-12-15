package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.ActivityService
import au.org.ala.ecodata.ProjectActivityService
import au.org.ala.ecodata.ProjectService
import au.org.ala.ecodata.RecordService
import au.org.ala.ecodata.SiteService
import au.org.ala.ecodata.UserService
import au.org.ala.ecodata.metadata.ConstantGetter
import au.org.ala.ecodata.metadata.OutputMetadata
import au.org.ala.ecodata.metadata.OutputModelProcessor
import au.org.ala.web.AuthService
import com.mongodb.BasicDBObject
import grails.util.Holders
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import pl.touk.excel.export.multisheet.AdditionalSheet

/**
 * Export Citizen Science style projects to a zip file containing:
 *
 * <ol>
 *     <li>An Excel spreadsheet with:
 *          <ol>
 *              <li>1 tab listing all projects</li>
 *              <li>1 tab listing all sites</li>
 *              <li>1 tab for each Project Activity in the set of projects, with all fields from all Outputs as columns</li>
 *          </ol>
 *     </li>
 *     <li>A directory containing shape files (as .zip files) for each site</li>
 *     <li>A directory containing all images, with subdirectories for ActivityIds and RecordsIds</li>
 * </ol>
 */
class CSProjectXlsExporter extends ProjectExporter {
    static Log log = LogFactory.getLog(ProjectXlsExporter.class)

    List<String> projectHeaders = ['Project ID', 'Grant ID', 'External ID', 'Organisation', 'Name', 'Description', 'Program', 'Sub-program', 'Start Date', 'End Date', 'Funding']
    List<String> projectProperties = ['projectId', 'grantId', 'externalId', 'organisationName', 'name', 'description', 'associatedProgram', 'associatedSubProgram', 'plannedStartDate', 'plannedEndDate', 'funding']

    List<String> siteHeaders = ['Site ID', 'Name', 'Description', 'lat', 'lon']
    List<String> siteProperties = ['siteId', 'name', 'description', 'lat', 'lon']
    List<String> surveyHeaders = ['Project ID', 'Project Activity ID', 'Activity ID', 'Site IDs', 'Start date', 'End date', 'Description', 'Status']

    List<String> recordHeaders = ["GUID", "Scientific Name", "Rights Holder", "Institution ID", "Access Rights", "Basis Of Record", "Data Set ID", "Data Set Name", "Location ID", "Location Name", "Locality", "Latitude", "Longitude"]
    List<String> recordProperties = ["guid", "scientificName", "rightsHolder", "institutionID", "accessRights", "basisOfRecord", "datasetID", "datasetName", "locationID", "locationName", "locality"]

    ProjectActivityService projectActivityService = Holders.grailsApplication.mainContext.getBean("projectActivityService")
    ProjectService projectService = Holders.grailsApplication.mainContext.getBean("projectService")
    SiteService siteService = Holders.grailsApplication.mainContext.getBean("siteService")
    ActivityService activityService = Holders.grailsApplication.mainContext.getBean("activityService")
    RecordService recordService = Holders.grailsApplication.mainContext.getBean("recordService")
    UserService userService = Holders.grailsApplication.mainContext.getBean("userService")

    XlsExporter exporter

    AdditionalSheet projectSheet
    AdditionalSheet sitesSheet
    AdditionalSheet recordSheet

    Map<String, AdditionalSheet> surveySheets = [:]

    public CSProjectXlsExporter(XlsExporter exporter) {
        this.exporter = exporter
    }

    @Override
    void export(Map project) {
        projectSheet()
        sitesSheet()
        recordSheet()

        addSites(project)

        addProjectActivities(project)

        addRecords(project)

        int row = projectSheet.getSheet().lastRowNum
        projectSheet.add([project], projectProperties, row + 1)
    }

    @Override
    void exportAll(List<Map> projects) {
        projects.each { export(it) }
    }

    private void addSites(Map project) {
        if (project.sites) {
            List sites = project.sites.collect {
                def centre = it.extent?.geometry?.centre
                [siteId: it.siteId, name: it.name, description: it.description, lat: centre ? centre[1] : "", lon: centre ? centre[0] : ""]
            }
            int row = sitesSheet.getSheet().lastRowNum
            sitesSheet.add(sites, siteProperties, row + 1)
        }
    }

    private void addProjectActivities(Map project) {
        List<Map> projectActivities = projectActivityService.getAllByProject(project.projectId, ProjectActivityService.ALL)

        List<String> restrictedSurveys = projectActivityService.listRestrictedProjectActivityIds(userService.currentUserDetails?.userId, project.projectId)

        projectActivities.each { survey ->
            if (!restrictedSurveys.contains(survey.projectActivityId)) {
                AdditionalSheet sheet = surveySheets[survey.name]
                if (!sheet) {
                    sheet = createSurveySheet(survey)
                    surveySheets.put(survey.name, sheet)
                }
            }
        }
    }

    private AdditionalSheet createSurveySheet(Map projectActivity) {
        AdditionalSheet sheet = null

        List<Map> activities = activityService.findAllForProjectActivityId(projectActivity.projectActivityId)

        if (activities) {
            List<String> headers = []
            headers.addAll(surveyHeaders)

            sheet = exporter.sheet(exporter.sheetName(projectActivity.name))

            OutputModelProcessor processor = new OutputModelProcessor()

            Set<String> uniqueOutputs = [] as HashSet<String>
            activities.each { activity ->
                List rows = [[:]]

                List properties = [
                        new ConstantGetter("projectId", projectActivity.projectId),
                        new ConstantGetter("projectActivityId", projectActivity.projectActivityId),
                        new ConstantGetter("activityId", activity.activityId),
                        new ConstantGetter("sites", projectActivity.sites.collect { it.siteId }.join(", ")),
                        new ConstantGetter("startDate", projectActivity.startDate),
                        new ConstantGetter("endDate", projectActivity.endDate),
                        new ConstantGetter("description", projectActivity.description),
                        new ConstantGetter("status", projectActivity.status)
                ]

                activity?.outputs?.each { output ->
                    Map outputConfig = outputProperties(output.name)
                    if (!uniqueOutputs.contains(output.name)) {
                        headers.addAll(outputConfig.headers)
                        uniqueOutputs << output.name
                    }

                    properties.addAll(outputConfig.propertyGetters)

                    OutputMetadata outputModel = new OutputMetadata(metadataService.getOutputDataModelByName(output.name))

                    List rowSets = processor.flatten(output, outputModel)

                    // some outputs (e.g. with list datatypes) result in multiple rows in the spreadsheet, so make sure that the existing rows are duplicated
                    while (rows.size() < rowSets.size()) {
                        rows << rows[0].clone()
                        // shallow clone is ok here, we just need to ensure we have a different map instance
                    }

                    if (rowSets.size() == 1 && rows.size() > 1) {
                        rows.each {
                            if (rowSets[0] instanceof BasicDBObject) {
                                it.putAll(rowSets[0].toMap())
                            }
                        }
                    } else {
                        rowSets.eachWithIndex { outputFields, index ->
                            if (outputFields instanceof BasicDBObject) {
                                rows[index].putAll(outputFields.toMap())
                            }
                        }
                    }
                }

                if (!rows[0].isEmpty()) {
                    sheet.add(rows, properties, sheet.sheet.lastRowNum + 1)
                }
            }

            sheet.fillHeader(headers)
            exporter.styleRow(sheet, 0, exporter.headerStyle(exporter.getWorkbook()))
        }

        sheet
    }

    private addRecords(Map project) {
        List properties = []
        properties.addAll(recordProperties)
        properties << null
        properties << null

        List<String> restrictedSurveys = projectActivityService.listRestrictedProjectActivityIds(userService.currentUserDetails?.userId, project.projectId)

        recordService.getAllByProject(project.projectId).each {
            println "restricted: " + restrictedSurveys
            println "pa id : " + it.projectActivityId
            println "remove : " + restrictedSurveys.contains(it.projectActivityId)
            if (!restrictedSurveys.contains(it.projectActivityId)) {
                println "adding "
                if (it.verbatimCoordinates) {
                    properties[-2] = new ConstantGetter("Verbatim Latitude", it.verbatimCoordinates[1])
                    properties[-1] = new ConstantGetter("Verbatim Longitude", it.verbatimCoordinates[0])
                } else if (it.decimalLatitude || it.decimalLongitude) {
                    properties[-2] = new ConstantGetter("Verbatim Latitude", it.decimalLatitude)
                    properties[-1] = new ConstantGetter("Verbatim Longitude", it.decimalLongitude)
                }

                recordSheet.add([it], properties, recordSheet.sheet.lastRowNum + 1)
            }
        }
    }

    private AdditionalSheet projectSheet() {
        if (!projectSheet) {
            projectSheet = exporter.addSheet('Project', projectHeaders)
        }
        projectSheet
    }

    private AdditionalSheet sitesSheet() {
        if (!sitesSheet) {
            sitesSheet = exporter.addSheet('Sites', siteHeaders)
        }
        sitesSheet
    }

    private AdditionalSheet recordSheet() {
        if (!recordSheet) {
            recordSheet = exporter.addSheet('DwC Records', recordHeaders)
        }
        recordSheet
    }

}
