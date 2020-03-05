<%@ page import="au.org.ala.images.FileSystemStorageLocation" %>
<table class="table">
    <thead>
        <tr>
            <td>id</td>
            <th>Type</th>
            <th>Detail</th>
            <th>Default</th>
            <th>Image count</th>
            <th></th>
        </tr>
    </thead>
    <tbody>

    <g:each in="${storageLocationList}" var="sl">
        <tr>
            <td>
                ${sl.id}
            </td>
            <td>
                <g:if test="${sl instanceof FileSystemStorageLocation}">
                    FS
                </g:if>
                <g:else>
                    S3
                </g:else>
            </td>
            <td>
                <g:if test="${sl instanceof FileSystemStorageLocation}">
                    ${sl.basePath}
                </g:if>
                <g:else>
                    ${sl.region}:${sl.bucket}/${sl.prefix}
                </g:else>
            </td>
            <td>
                <g:radio class="radio-default" name="default" value="${sl.id}" checked="${sl.id == defaultId}"></g:radio>
            </td>
            <td>
                ${imageCounts[sl.id]}
            </td>
            <td>
%{--                <button class="btn btn-xs btn-danger btn-delete" data-id="${sl.id}"><i class="fa fa-trash"></i></button>--}%
                <button class="btn btn-xs btn-default btn-migrate" data-source="${sl.id}"><i class="fa fa-suitcase"></i></button>
            </td>
        </tr>
    </g:each>
    </tbody>
</table>

<div id="storage-location-migrate-modal" class="modal fade" role="dialog">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">Storage Location</h4>
            </div>
            <div class="modal-body">
                <form id="migrate-form">


                    <div class="form-group">
                        <label for="destination">Migrate to</label>
                        <select class="form-control" id="destination" name="destination">
                            <g:each in="${storageLocationList}" var="sl">
                                <option value="${sl.id}">
                                    <g:if test="${sl instanceof FileSystemStorageLocation}">
                                        FS:${sl.basePath}
                                    </g:if>
                                    <g:else>
                                        S3:${sl.region}:${sl.bucket}/${sl.prefix}
                                    </g:else>
                                </option>
                            </g:each>
                        </select>
                    </div>

                    <button type="button" id="btn-migrate-storage" class="btn btn-default">Do it</button>
                </form>
            </div>
            <div class="modal-footer">
            </div>
        </div>
    </div>
</div>