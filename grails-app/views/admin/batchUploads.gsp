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

<h2>
    Batch processing
</h2>

<p>
    Batch processing of AVRO files. This page can be used to monitor AVRO batch processing.
</p>

<div class="btn-toolbar">
    <div class="btn-group mr-2 pull-right" role="group" aria-label="First group">
        <g:link controller="admin" action="clearUploads" class="btn-default btn ${batchServiceProcessingEnabled ? 'disabled': ''}">
            <span class="glyphicon glyphicon-remove" aria-hidden="true"></span>
            Delete all
        </g:link>
        <g:link controller="admin" action="clearFileQueue" class="btn-default btn ${batchServiceProcessingEnabled ? 'disabled': ''}">
            <span class="glyphicon glyphicon-remove" aria-hidden="true"></span>
            Clear file queue
        </g:link>
    <g:if test="${batchServiceProcessingEnabled}">
        <g:link controller="admin" action="disableBatchProcessing" class="btn-primary btn">
            <span class="glyphicon glyphicon-stop" aria-hidden="true"></span>
            Disable batch processing
        </g:link>
    </g:if>
    <g:else>
        <g:link controller="admin" action="enableBatchProcessing" class="btn-info btn">
            <span class="glyphicon glyphicon-play" aria-hidden="true"></span>
            Enable batch processing
        </g:link>
    </g:else>
    </div>
</div>

<h3>File queue</h3>
<g:if test="${files}">
    <table class="table table-condensed table-striped table-bordered ">
    <thead class="thead-dark">
    <th>batchID</th>
    <th>fileID</th>
    <th>dataResourceUid</th>
    <th>invalidRecords</th>
    <th>recordCount</th>
    <th>processedCount</th>
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
            <td>${batchFile.processedCount}</td>
            <td>${batchFile.newImages}</td>
            <td>${batchFile.metadataUpdates}</td>
            <td>${batchFile.dateCreated}</td>
            <td>${batchFile.lastUpdated}</td>
            <td>${batchFile.dateCompleted}</td>
            <td>${batchFile.status}</td>
            <td>
                <div class="btn-group-vertical">
                    <g:link action="batchReloadFile" params="${[fileId: batchFile.id]}" class="btn btn-default btn-sm btn-block">
                        <span class="glyphicon glyphicon-refresh" aria-hidden="true"></span>
                        Reload</g:link>
                    <g:link action="batchFileDeleteFromQueue" params="${[fileId: batchFile.id]}" class="btn btn-danger btn-sm btn-block">
                        <span class="glyphicon glyphicon-remove" aria-hidden="true"></span>
                        Delete
                    </g:link>
                </div>
            </td>
        </tr>
    </g:each>
    </tbody>
</table>
</g:if>
<g:else>
    Batch file processing will appear here when available.
</g:else>

<h3>Uploads</h3>
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
