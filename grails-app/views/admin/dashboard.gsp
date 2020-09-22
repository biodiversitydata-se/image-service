<!doctype html>
<html>
    <head>
        <meta name="layout" content="adminLayout"/>
        <title>Admin</title>
        <meta name="breadcrumbs" content="${g.createLink( controller: 'image', action: 'list')}, Images"/>
    </head>

    <body>
        <content tag="pageTitle">Dashboard</content>
        <content tag="adminButtonBar" />

        <g:if test="${flash.message}">
            <div class="alert alert-success" style="display: block">${flash.message}</div>
        </g:if>
        <g:if test="${flash.errorMessage}">
            <div class="alert alert-danger" style="display: block">${flash.errorMessage}</div>
        </g:if>

        <g:if test="${grailsApplication.config.security.cas.disableCAS.toBoolean()}">
            <div class="alert alert-warning" style="display: block">WARNING: CAS authentication disabled - this means admin functions are exposed!</div>
        </g:if>

        <div class="well well-small">
            <h4>Database statistics</h4>
            <table class="table table-striped">
                <tr>
                    <td class="col-md-6">Image count </td>
                    <td class="col-md-6"><span id="statImageCount"><asset:image src="spinner.gif" /></span></td>
                </tr>
                <tr>
                    <td class="col-md-6">Deleted image count</td>
                    <td class="col-md-6"><span id="statDeletedImageCount"><asset:image src="spinner.gif" /></span></td>
                </tr>
                <tr>
                    <td class="col-md-6">Licences count</td>
                    <td class="col-md-6"><span id="statLicenceCount"><asset:image src="spinner.gif" /></span></td>
                </tr>
                <tr>
                    <td class="col-md-6">Licence mapping count</td>
                    <td class="col-md-6"><span id="statLicenceMappingCount"><asset:image src="spinner.gif" /></span></td>
                </tr>
            </table>
            <p>Note: these counts are taken from the database, not the search index.</p>
            <h4 style="margin-top:40px;">Background processing</h4>
            <table class="table">
                <tr>
                    <td class="col-md-6">
                        Batch (AVRO) uploads
                    </td>
                    <td class="col-md-6">
                        <span id="batchUploads"><asset:image src="spinner.gif" /></span>
                    </td>
                </tr>
                <tr>
                    <td class="col-md-6">
                        Import/Thumbnail/Delete queue size
                    </td>
                    <td class="col-md-6">
                        <span id="statQueueSize"><asset:image src="spinner.gif" /></span>
                    </td>
                </tr>
                <tr>
                    <td class="col-md-6">
                        Tiling queue size
                    </td>
                    <td class="col-md-6">
                        <span id="tilingQueueSize"><asset:image src="spinner.gif" /></span>
                    </td>
                </tr>

            </table>
        </div>
        <script>

            $(document).ready(function() {

                updateQueueLength();
                updateRepoStatistics();

                setInterval(updateQueueLength, 5000);
                setInterval(updateRepoStatistics, 5000);
            });

            function updateRepoStatistics() {
                $.ajax("${createLink(controller:'webService', action:'getRepositoryStatistics')}").done(function(data) {
                    $("#statImageCount").html(data.imageCount);
                    $("#statDeletedImageCount").html(data.deletedImageCount);
                    $("#statLicenceCount").html(data.licenceCount);
                    $("#statLicenceMappingCount").html(data.licenceMappingCount);
                });
            }

            function updateQueueLength() {
                $.ajax("${createLink(controller:'webService', action:'getBackgroundQueueStats')}").done(function(data) {
                    $("#statQueueSize").html(data.queueLength);
                    $("#tilingQueueSize").html(data.tilingQueueLength);
                    $("#batchUploads").html(data.batchUploads);
                });
            }
        </script>
    </body>
</html>
