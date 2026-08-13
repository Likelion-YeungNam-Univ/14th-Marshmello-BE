package Marshmello.MarshmelloWas.domain.analysis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "images_analysis")
public class ImageAnalysis {

    @Id
    @Column(name = "image_id", nullable = false)
    private Long imageId;

    @Column(name = "score", nullable = false)
    private short score;

    protected ImageAnalysis() {
    }

    public ImageAnalysis(Long imageId, short score) {
        this.imageId = imageId;
        this.score = score;
    }

    public Long getImageId() {
        return imageId;
    }

    public short getScore() {
        return score;
    }
}
