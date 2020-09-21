<!doctype html>
<html>
<head>
    <meta name="layout" content="adminLayout"/>
    <title>ALA Images - Admin - Tools</title>
    <style type="text/css" media="screen">
    </style>
</head>
<body>
<content tag="pageTitle">Batch Uploads</content>
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
<table class="table table-condensed table-striped">
    <thead>
    <th>batchID</th>
    <th>dataResourceUid</th>
    <th>filePath</th>
    <th>invalidRecords</th>
    <th>recordCount</th>
    <th>newImages</th>
    <th>metadataUpdates</th>
    <th>dateCreated</th>
    <th>status</th>
    <th>status <updated></updated></th>
    <th>reload</th>
    </thead>
    <tbody>
    <g:each in="${files}" var="batchFile">
        <tr>
            <td>${batchFile.batchFileUpload.id}</td>
            <td>${batchFile.batchFileUpload.dataResourceUid}</td>
            <td>${batchFile.filePath}</td>
            <td>${batchFile.invalidRecords}</td>
            <td>${batchFile.recordCount}</td>
            <td>${batchFile.newImages}</td>
            <td>${batchFile.metadataUpdates}</td>
            <td>${batchFile.dateCreated}</td>
            <td>${batchFile.status}</td>
            <td>${batchFile.lastUpdated}</td>
            <td><g:link action="batchReloadFile" params="${[fileId: batchFile.id]}" class="btn-primary btn-sm">Reload</g:link></td>
        </tr>
    </g:each>
    </tbody>
</table>

<h3>
    Batch zip uploads
</h3>
<table class="table table-condensed table-striped">
    <thead>
        <th>batchID</th>
        <th>dataResourceUid</th>
        <th>filePath</th>
        <th>dateCreated</th>
        <th>status</th>
        <th>files</th>
    </thead>
    <tbody>
    <g:each in="${results}" var="batchFileUpload">
        <tr>
            <td>${batchFileUpload.id}</td>
            <td>${batchFileUpload.dataResourceUid}</td>
            <td>${batchFileUpload.filePath}</td>
            <td>${batchFileUpload.dateCreated}</td>
            <td>${batchFileUpload.status}</td>
            <td>${batchFileUpload.batchFiles.size()}</td>
        </tr>
    </g:each>
    </tbody>
</table>
</body>
</html>
