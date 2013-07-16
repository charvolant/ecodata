<%--
  Created by IntelliJ IDEA.
  User: dos009
  Date: 5/07/13
  Time: 12:32 PM
  To change this template use File | Settings | File Templates.
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.layout.skin?:'main'}"/>
    <title>Search Results</title>
</head>
<body>
<div id="wrapper" class="container-fluid">
    <div class="row-fluid">
        <div class="span12" id="header">
            <h1 class="pull-left">Search Results</h1>
            <g:form controller="search" method="GET" class="hide form-horizontal pull-right" style="padding-top:5px;">
                <div class="input-append">
                    <g:textField class="input-large" name="query" value="${params.query}"/>
                    <button class="btn" type="submit">Search</button>
                </div>
            </g:form>
        </div>

        <g:if test="${flash.error}">
            <div class="row-fluid">
                <div class="alert alert-error">
                    <button type="button" class="close" data-dismiss="alert">&times;</button>
                    <span>${flash.error}</span>
                </div>
            </div>
        </g:if>
    </div>

    <div class="row-fluid ">
        <div class="span9">
            <p class="lead">
                <fc:searchResultsCounts max="${results.max}" offset="${results.offset}" total="${results.totalRecords}"/>
                <strong>${params.query}</strong>
            </p>
        </div>
        <div class="span3 pull-right" style="text-align: right;">
            <g:form class="max" controller="search" method="GET">
                <g:hiddenField name="query" value="${params.query}"/>
                <g:hiddenField name="offset" value="${params.offset}"/>
                %{--<g:hiddenField name="sort" value="${params.sort}"/>--}%
                %{--<g:hiddenField name="order" value="${params.order}"/>--}%
                Items per page:
                <g:select name="max" from="${[10,20,50,100]}" value="${params.max}" class="btn btn-small" style="width:auto;"
                          onChange="console.log('change');jQuery('form.max').submit();"/>
            </g:form>
        </div>
    </div>
    <div id="content" class="row-fluid ">
        <div class="span12">
            <g:if test="${results.totalRecords > 0}">
                <table class="table table-bordered table-condensed table-striped">
                    <thead>
                    <tr><th>Type</th><th>Details</th><th>Date created</th></tr>
                    </thead>
                    <tbody>
                    <g:each var="r" in="${results.results}">
                        <tr>
                            <td><g:message code="label.${r.class}" default="${r.class}"/></td>
                            <td>
                                <g:if test="${r.class=~/Project/}">
                                    <g:link controller="project" id="${r.projectId}">${r.name}</g:link>
                                </g:if>
                                <g:if test="${r.class=~/Site/}">
                                    <g:link controller="site" id="${r.siteId}">${r.name}</g:link>
                                </g:if>
                                <g:elseif test="${r.class=~/Activity/}">
                                    <g:link controller="activity" id="${r.activityId}">${r.name?:r.type}</g:link>
                                </g:elseif>
                                <g:else>
                                    ${r.type}
                                </g:else>
                                <g:if test="${r.description}">
                                    &mdash; ${r.description}
                                </g:if>
                            </td>
                            <td><fc:formatDateString date="${r.dateCreated}"/></td>
                        </tr>
                    </g:each>
                    </tbody>
                </table>
                <div class="pagination">
                    <g:paginate controller="search" total="${results.totalRecords}" params="${params}" />
                </div>
            </g:if>
        </div>
    </div>
</div>
</body>
</html>