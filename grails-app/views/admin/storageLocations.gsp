<!doctype html>
<html>
    <head>
        <meta name="layout" content="adminLayout"/>
        <meta name="section" content="home"/>
        <title>ALA Images - Admin - Storage Locations</title>
    </head>
    <body>

        <content tag="pageTitle">Storage Locations</content>
        <content tag="adminButtonBar" />

        <div class="row">
            <div class="col-md-12">
                <button class="btn btn-success" id="btn-add-storage-location"><i class="glyphicon glyphicon-plus "> </i>&nbsp;Add</button>
            </div>
        </div>

        <div class="row" style="margin-top:10px;">
            <div class="col-md-12">
                <div id="storage-location-container" class="well well-small">
                    <img:spinner />
                </div>
            </div>
        </div>

        <div id="storage-location-modal" class="modal fade" role="dialog">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal">&times;</button>
                        <h4 class="modal-title">Storage Location</h4>
                    </div>
                    <div class="modal-body">
                        <form id="storage-location-form">
                            <div class="form-group">
                                <label class="radio-inline">
                                    <input type="radio" name="type" id="fs-type" value="fs" checked>
                                    File system
                                </label>
                                <label class="radio-inline">
                                    <input type="radio" name="type" id="s3-type" value="s3">
                                    AWS S3 Bucket
                                </label>
                                <label class="radio-inline">
                                    <input type="radio" name="type" id="swift-type" value="swift" disabled>
                                    Swift
                                </label>
                            </div>
                            <div id="fs-form" class="type-form">
                                <div class="form-group">
                                    <label for="basePath">Base Path</label>
                                    <input type="text" class="form-control" id="basePath" name="basePath" placeholder="/data/images/storage">
                                </div>
                            </div>
                            <div id="s3-form" class="type-form hidden">
                                <div class="form-group">
                                    <label for="region">Region</label>
                                    <input type="text" class="form-control" id="region" name="region" placeholder="ap-southeast-2">
                                </div>
                                <div class="form-group">
                                    <label for="bucket">Bucket name</label>
                                    <input type="text" class="form-control" id="bucket" name="bucket" placeholder="ala-image-service">
                                </div>
                                <div class="form-group">
                                    <label for="prefix">Object Prefix</label>
                                    <input type="text" class="form-control" id="prefix" name="prefix" placeholder="/images/prefix">
                                </div>
                                <div class="form-group">
                                    <label for="accessKey">Access Key</label>
                                    <input type="text" class="form-control" id="accessKey" name="accessKey" placeholder="asdfasdfasdf">
                                </div>
                                <div class="form-group">
                                    <label for="secretKey">Secret Key</label>
                                    <input type="text" class="form-control" id="secretKey" name="secretKey" placeholder="asdfasdfasdf">
                                </div>
                                <div class="checkbox">
                                    <label>
                                        <input type="checkbox" id="publicRead" name="publicRead"> Public read
                                    </label>
                                </div>
                            </div>
                            <button type="button" id="btn-save-storage-location" class="btn btn-default">Add</button>
                        </form>
                    </div>
                    <div class="modal-footer">
                    </div>
                </div>
            </div>
        </div>

    <asset:script type="text/javascript" asset-defer="">
        $(document).ready(function() {

            function loadLocations() {
                $('#storage-location-container').load('${createLink(controller: 'storageLocation', action: 'listFragment')}');
            }

            $('#btn-add-storage-location').on('click', function(e) {
                $('#storage-location-modal').modal('show');
            });
            $('#btn-save-storage-location').on('click', function(e) {
                 $.ajax({type: 'POST',
                         url: '${createLink(controller: 'storageLocation', action: 'create')}',
                         data: JSON.stringify($('#storage-location-form').serializeArray().reduce(function(a, x) { a[x.name] = x.value; return a; }, {})),
                         dataType: 'json',
                         contentType: 'application/json'
                }).done(function(data, status, jqXHR) {
                    $('#storage-location-modal').modal('hide');
                    loadLocations();
                }).fail(function(jqXHR, textStatus, errorThrown) {
                    $('#storage-location-modal').modal('hide');
                    console.log(errorThrown);
                    alert("Couldn't save storage location");
                });
            });

            $('input[name="type"]').on('change', function(e) {
                let type = $("input[name='type']:checked").val();
                $('.type-form').addClass('hidden');
                switch (type) {
                    case 'fs':
                         $('#fs-form').removeClass('hidden');
                        break;
                    case 's3':
                         $('#s3-form').removeClass('hidden');
                        break;
                }
            });

            $('#storage-location-container').on('click', '.btn-migrate', function(e) {
                var $this = $(this);
                var source = $this.data('source');
                $('#storage-location-migrate-modal').modal('show').on('click', '#btn-migrate-storage', function(e) {
                    $.ajax({type: 'POST',
                        url: "${createLink(controller: 'storageLocation', action: 'migrate')}",
                        data: { src: source, dst: $('#destination').val() }
                    }).done(function(data, status, jqXHR) {
                        $('#storage-location-migrate-modal').modal('hide');
                    }).fail(function(jqXHR, textStatus, errorThrown) {
                        $('#storage-location-migrate-modal').modal('hide');
                        console.log(errorThrown);
                        alert("Couldn't migrate storage location");
                    });

                });

            });

            $('#storage-location-container').on('change', 'input[type=radio][name=default]', function(e) {
                $.ajax({type: 'POST',
                        url: "${createLink(controller: 'storageLocation', action: 'setDefault')}",
                        data: { id: this.value }
                }).done(function(data, status, jqXHR) {
                    loadLocations();
                }).fail(function(jqXHR, textStatus, errorThrown) {
                    console.log(errorThrown);
                    alert("Couldn't set default");
                    loadLocations();
                });
            });

            loadLocations();
        });
    </asset:script>

    </body>
</html>



