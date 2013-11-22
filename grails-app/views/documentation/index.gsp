<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="api"/>
    <title>MERIT draft API</title>
</head>

<body>

<div class="container-fluid api">

    <div class="row-fluid">
        <div class="bs-callout bs-callout-warning">
            <h4>This API documentation is in draft format</h4>
            <p>This API documentation is intended as a starting point for discussion and is subject to change.<p>
            <p>
             This API was designed with the following assumptions:
             <ul>
                <li>
                    The primary use case is the bulk upload of project activity information to satisfy the requirements of a MERIT interim project report.
                    The granularity of the API has been set accordingly.
                </li>
                <li>
                    At least in the short term, projects will be created by an import process from DoE.  This API assumes that all projects already exist in the MERIT system.
                </li>
             </ul>
            </p>
        </div>
    </div>

    <div class="row-fluid">
        <h3>Operations</h3>

        <h4>Project activities</h4>
        <p>
            The collection of Activities to be undertaken by a project.  Each activity is identified by a type, date range and description and is expected to produce a collection of outputs.
            These outputs collated by the MERIT system for monitoring and reporting purposes.
        </p>

        <table class="table table-striped">
            <thead>
                <tr>
                    <th class="api-method">Method / URL</th><th class="api-description">Description</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>
                        GET /external/draft/projectActivities
                    </td>
                    <td>
                        Returns the collection of activities to be undertaken by a project.
                    </td>
                </tr>
                <tr>
                    <td>
                        <g:link action="postProjectActivities">POST /external/draft/projectActivities</g:link>
                    </td>
                    <td>
                        Updates the output targets and activities that will be undertaken by a project.
                    </td>
                </tr>

            </tbody>
        </table>


        <h4>Project sites</h4>
        <p>
            The collection of sites on which project activities will be undertaken.  A site is defined by a name, description and extent.
        </p>

        <table class="table table-striped">
            <thead>
            <tr>
                <th class="api-method">Method / URL</th><th class="api-description">Description</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td>
                    <g:link action="getProjectSites">GET /external/draft/projectSites</g:link>
                </td>
                <td>
                    Returns the collection of sites on which project activities will be undertaken.  In the short term, it is expected that the
                    project sites will be defined in the MERIT system and referenced by uuid when using the <g:createLink action="postProjectActivities">POST /external/draft/projectActivities</g:createLink> API.
                </td>

            </tr>

            </tbody>
        </table>

    </div>

    <div class="row-fluid">
        <h3>Activities and Output Descriptions</h3>
        <p>
            In the MERIT system, each project has a collection of Activities.  Each activity in turn has a collection of Outputs.
        </p>
        <h4>Activities</h4>
        <table class="table table-striped">
            <thead>
            <tr>
                <th class="api-method">Name</th><th class="api-description">Description</th>
            </tr>
            </thead>
            <tbody>
                <g:each in="${activitiesModel.activities}" var="activity">
                    <tr>
                        <td>
                            <g:link action="activity" id="${activity.name+'.html'}">${activity.name}</g:link>
                        </td>
                        <td>
                            <g:message code="${'api.'+activity.name+'.description'}" default="${g.message([code:'api.description.missing'])}"/>
                        </td>
                    </tr>
                </g:each>

            </tbody>
        </table>
        <h4>Outputs</h4>
        <table class="table table-striped">
            <thead>
            <tr>
                <th class="api-method">Name</th><th class="api-description">Description</th>
            </tr>
            </thead>
            <tbody>
            <g:each in="${activitiesModel.outputs}" var="output">
                <tr>
                    <td>
                        <g:link action="output" id="${output.name+'.html'}">${output.name}</g:link>
                    </td>
                    <td>
                        <g:message code="${'api.'+output.name+'.description'}" default="${g.message([code:'api.description.missing'])}"/>
                    </td>
                </tr>
            </g:each>

            </tbody>
        </table>
    </div>
</div>

</body>
</html>