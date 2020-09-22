<!doctype html>
<html>
<head>
    <meta name="layout" content="adminLayout"/>
    <title>ALA Images - Admin - Tools</title>
    <style type="text/css" media="screen">
    </style>
</head>
<body>
<content tag="pageTitle">Batch uploads</content>
<content tag="adminButtonBar" />
<g:if test="${flash.message}">
    <div class="alert alert-success" style="display: block">${flash.message}</div>
</g:if>
<g:if test="${flash.errorMessage}">
    <div class="alert alert-danger" style="display: block">${flash.errorMessage}</div>
</g:if>
<h3>
    Batch file processing
</h3>
<g:if test="${files}">
<table class="table table-condensed table-striped table-bordered ">
    <thead class="thead-dark">
    <th>batchID</th>
    <th>fileID</th>
    <th>dataResourceUid</th>
    <th>invalidRecords</th>
    <th>recordCount</th>
    <th>newImages</th>
    <th>metadataUpdates</th>
    <th>dateCreated</th>
    <th>lastUpdated</th>
    <th>dateCompleted</th>
    <th>status</th>
    <th>reload</th>
    </thead>
    <tbody>
    <g:each in="${files}" var="batchFile">
        <tr>
            <td>${batchFile.batchFileUpload.id}</td>
            <td>${batchFile.id}</td>
            <td>${batchFile.batchFileUpload.dataResourceUid}</td>
            <td>${batchFile.invalidRecords}</td>
            <td>${batchFile.recordCount}</td>
            <td>${batchFile.newImages}</td>
            <td>${batchFile.metadataUpdates}</td>
            <td>${batchFile.dateCreated}</td>
            <td>${batchFile.lastUpdated}</td>
            <td>${batchFile.dateCompleted}</td>
            <td>${batchFile.status}</td>
            <td><g:link action="batchReloadFile" params="${[fileId: batchFile.id]}" class="btn-primary btn-sm">Reload</g:link></td>
        </tr>
    </g:each>
    </tbody>
</table>
</g:if>
<g:else>
    Batch file processing will appear here when available.
</g:else>

<h3>
    Batch zip uploads
</h3>
<g:if test="${results}">
<table class="table table-condensed table-striped table-bordered ">
    <thead class="thead-dark">
        <th>batchID</th>
        <th>files</th>
        <th>dataResourceUid</th>
        <th>dateCreated</th>
        <th>dateCompleted</th>
        <th>status</th>
    </thead>
    <tbody>
    <g:each in="${results}" var="batchFileUpload">
        <tr>
            <td>${batchFileUpload.id}</td>
            <td>${batchFileUpload.batchFiles.size()}</td>
            <td>${batchFileUpload.dataResourceUid}</td>
            <td>${batchFileUpload.dateCreated}</td>
            <td>${batchFileUpload.dateCompleted }</td>
            <td>${batchFileUpload.status}</td>
        </tr>
    </g:each>
    </tbody>
</table>
</g:if>
<g:else>
    Batch details will appear here when available.
</g:else>
</body>
</html>
