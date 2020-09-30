package au.org.ala.images

import com.opencsv.CSVReader
import org.apache.log4j.Logger

class IndexImageBackgroundTask extends BackgroundTask {

    private long _imageId
    private ElasticSearchService _elasticSearchService
    private Logger log = Logger.getLogger(IndexImageBackgroundTask.class)

    IndexImageBackgroundTask(long imageId, ElasticSearchService elasticSearchService) {
        _imageId = imageId
        _elasticSearchService = elasticSearchService
    }

    @Override
    void execute() {
        def imageInstance = Image.get(_imageId)
        if (imageInstance) {
            _elasticSearchService.indexImage(imageInstance)
        }
    }

    @Override
    String toString() {
        return "IndexImageBackgroundTask," + _imageId
    }
}

class ScheduleReindexAllImagesTask extends BackgroundTask {

    private ImageService _imageService
    private ElasticSearchService _elasticSearchService
    private Logger log = Logger.getLogger(ScheduleReindexAllImagesTask.class)

    ScheduleReindexAllImagesTask(ImageService imageService, ElasticSearchService elasticSearchService) {
        _imageService = imageService
        _elasticSearchService = elasticSearchService
    }

    @Override
    void execute() {
        _imageService.deleteIndex()

        def file = _imageService.exportIndexToFile()
        def csvReader = new CSVReader(new InputStreamReader(new FileInputStream(file)), '$'.toCharArray()[0])
        def headers = csvReader.readNext()
        def line = csvReader.readNext()
        def i = 1
        def start = System.currentTimeMillis()
        def batch = []
        while (line){
            def record = [:]
            if(line.length == headers.length) {
                line.eachWithIndex { field, idx ->
                    record[headers[idx]] = field
                }
                batch << record
                if (i % 1000 == 0){
                    def lastBatch  = System.currentTimeMillis() - start
                    _elasticSearchService.bulkIndexImageInES(batch)
                    batch.clear()
                    log.info("Indexing images: " + i + " time:" + lastBatch)
                    start = System.currentTimeMillis()
                }
            } else {
                log.error("Problem with line:" + i)

            }
            i += 1
            line = csvReader.readNext()
        }

        def lastBatch  = System.currentTimeMillis() - start
        _elasticSearchService.bulkIndexImageInES(batch)
        batch.clear()
        log.info("Indexing images: " + i + " time:" + lastBatch)
    }
}
