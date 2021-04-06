package au.org.ala.images;

class ImageStoreResult {

    Image image = null
    boolean alreadyStored = false
    boolean isDuplicate = false

    ImageStoreResult(Image image, boolean alreadyStored, boolean isDuplicate){
        this.alreadyStored = alreadyStored
        this.image = image
        this.isDuplicate = isDuplicate;
    }
}