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
<div class="btn-toolbar">
    <div class="btn-group mr-2 pull-right" role="group" aria-label="First group">
        <button type="button" class="btn btn-default" data-toggle="modal" data-target="#helpModal">
            <span class="glyphicon glyphicon-info-sign" aria-hidden="true"></span>
            Help
        </button>
        <g:link controller="admin" action="clearUploads"
                class="btn-default btn ${batchServiceProcessingEnabled ? 'disabled': ''}">
            <span class="glyphicon glyphicon-remove" aria-hidden="true"></span>
            Clear all
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

<div id="helpModal" class="modal fade" role="dialog">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h3 class="modal-title">Processing status Information</h3>
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
            <div class="modal-body">
                <p>The following are a set of processing statuses visible during loading of files:
                </p>
                <ul>
                    <li><b>UNPACKING</b> - An upload zip file has been received </li>
                    <li><b>UNZIPPED</b> - An uploaded zip file has been successfully unzipped, and loading job has been logged in the database</li>
                    <li><b>QUEUED</b> - A file queue, awaiting loading. Loading will commence when batch processing is enabled
                    </li>
                    <li><b>LOADING</b> - A file is being read and new images are being added metadata updated.</li>
                    <li><b>STOPPED</b> - A file load has been stopped by batch processing being disabled, or a system restart</li>
                    <li><b>COMPLETE</b> - A file has been successfully loaded.</li>
                </ul>
                <p>
                    File queue table headings:
                    <ul>
                     <li><b>batchID</b> - the batch ID which can be used to retrieve the file upload details from the database</li>
                     <li><b>recordCount</b> - number of records counted when the file was initially read in the UNPACKING stage
                     Note: the recordCount doesnt not take into account duplicates.
                         Hence newImages and metadataUpdates counts may not equal recordCount.
                     </li>
                     <li><b>invalidRecords</b> - these are entries in the uploaded avro files with an empty or missing `identifier` value</li>
                     <li><b>processedCount</b> - number of entries in the file successfully read during loading</li>
                     <li><b>newImages</b> - these are entries in the file resulting in new images being added</li>
                     <li><b>metadataUpdates</b> - these are entries in the file resulting in metadata updates to existing images</li>
                     <li><b>created</b> - when the batch file was registered in the system for loading</li>
                     <li><b>lastUpdated</b> -  date the batch file status was last updated</li>
                     <li><b>timeTaken</b> - time taken in minutes to complete or the current time elapsed if in 'LOADING' state</li>
                     <li><b>dateCompleted</b> - date the batch file load was successfully completed</li>
                     <li><b>status</b> - the current status for this file</li>
                    </ul>
                </p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
</div>

<h3>File queue</h3>
<g:if test="${files}">
    <table class="table table-condensed table-bordered ">
    <thead class="thead-dark">
    <th>batchID</th>
    <th>fileID</th>
    <th>dataResourceUid</th>
    <th>recordCount</th>
    <th>processedCount</th>
    <th>newImages</th>
    <th>metadataUpdates</th>
    <th>created</th>
    <th>lastUpdated</th>
    <th>timeTaken</th>
    <th>dateCompleted</th>
    <th>status</th>
    <th>reload</th>
    </thead>
    <tbody>
    <g:each in="${files}" var="batchFile">
        <tr class="${batchFile.status == 'LOADING' ? 'active' : ''} ${batchFile.status == 'COMPLETE' ? 'success' : ''} ${batchFile.status == 'QUEUED' ? 'warning' : ''} ${batchFile.status == 'STOPPED' ? 'danger' : ''}">
            <td>${batchFile.batchFileUpload.id}</td>
            <td>${batchFile.id}</td>
            <td>${batchFile.batchFileUpload.dataResourceUid}</td>
            <td>${batchFile.recordCount}</td>
            <td>${batchFile.processedCount}</td>
            <td>${batchFile.newImages}</td>
            <td>${batchFile.metadataUpdates}</td>
            <td><prettytime:display date="${batchFile.dateCreated}" /></td>
            <td><prettytime:display date="${batchFile.lastUpdated}" /></td>
            <td>
                <g:if test="${batchFile.timeTakenToLoad}">
                    <g:set var="hours" value="${((batchFile.timeTakenToLoad.toInteger() / 60).toInteger() /60).toInteger() }"/>
                    <g:set var="minutes" value="${(batchFile.timeTakenToLoad.toInteger() / 60).toInteger() % 60}"/>
                    <g:set var="seconds" value="${batchFile.timeTakenToLoad.toInteger()  % 60}"/>
                    <g:if test="${hours}">${hours} hr</g:if>
                    <g:if test="${minutes}">${minutes} min</g:if>
                    <g:if test="${seconds}">${seconds} secs</g:if>
                </g:if>
                <g:else>
                    <g:message code="batch.processing.not.loaded" />
                </g:else>
            <td><prettytime:display date="${batchFile.dateCompleted}" /></td>
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
    <table class="table table-condensed table-bordered ">
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
        <tr class="${batchFileUpload.status == 'COMPLETE' ? 'success' : 'warning'}">
            <td>${batchFileUpload.id}</td>
            <td>${batchFileUpload.batchFiles.size()}</td>
            <td>${batchFileUpload.dataResourceUid}</td>
            <td><prettytime:display date="${batchFileUpload.dateCreated}" /></td>
            <td><prettytime:display date="${batchFileUpload.dateCompleted}" /></td>
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
