package au.org.ala.images

import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.file.DataFileWriter
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.avro.io.DatumWriter

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AvroUtils {

    static File generateTestArchive(){
        Schema multimediaSchema = SchemaBuilder
                .builder()
                .record("multimedia")
                .fields()
                .requiredString("identifier")
                .endRecord();

        // generate a test archive
        Schema multimediaRecordSchema = SchemaBuilder
                .builder()
                .record("multimediaRecord")
                .fields()
                .requiredString("id")
                .name("multimediaItems").type().nullable().array().items(multimediaSchema)
                .noDefault().endRecord()

        org.apache.avro.generic.GenericRecord multimedia = new GenericRecordBuilder(multimediaSchema)
                .set("identifier", "https://www.ala.org.au/app/uploads/2019/05/palm-cockatoo-by-Alan-Pettigrew-1920-1200-CCBY-28072018-640x480.jpg")
                .build();

        org.apache.avro.generic.GenericRecord record = new GenericRecordBuilder(multimediaRecordSchema)
                .set("id", "1")
                .set("multimediaItems", [multimedia])
                .build();


        File newArchiveDir = new File("/tmp/image-service-avro-test")
        newArchiveDir.mkdir()
        File newArchive = new File(newArchiveDir, "data.avro.zip");
        FileOutputStream fos = new FileOutputStream(newArchive);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        ZipEntry zipEntry = new ZipEntry("data.avro");
        zipOut.putNextEntry(zipEntry);

        DatumWriter<GenericRecord> writer = new GenericDatumWriter<GenericRecord>(multimediaRecordSchema);
        DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<GenericRecord>(writer);
        dataFileWriter.create(multimediaRecordSchema, zipOut);
        dataFileWriter.append(record);
        dataFileWriter.close();

        zipOut.close();
        fos.close();

        newArchive
    }
}
