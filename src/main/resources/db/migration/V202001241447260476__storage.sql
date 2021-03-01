create table storage_location
(
    id bigserial primary key,
    version bigint not null,
    date_created timestamp not null default current_timestamp,
    last_updated timestamp not null default current_timestamp,
    class text not null,
    public_read boolean,
    region text,
    prefix text,
    secret_key text,
    bucket text,
    access_key text,
    hostname text,
    path_style_access boolean,
    base_path text
);

create index storage_location_class on storage_location (class);

alter table image
    add storage_location_id bigint;

insert into storage_location (version, class, base_path, date_created, last_updated)
values (0, 'au.org.ala.images.FileSystemStorageLocation', '${imageRoot}', default, default);

update image set storage_location_id = (select id from storage_location limit 1);

alter table image
    alter column storage_location_id set not null,
    add constraint image_storage_location_fk
        foreign key (storage_location_id) references storage_location(id) on delete cascade;
