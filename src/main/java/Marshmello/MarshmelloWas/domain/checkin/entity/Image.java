package Marshmello.MarshmelloWas.domain.checkin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "images")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id", nullable = false)
    private Long imageId;

    @Column(name = "image_data", nullable = false, columnDefinition = "BYTEA")
    private byte[] imageData;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checkin_id", nullable = false, unique = true)
    private CheckIn checkIn;

    protected Image() {
    }

    public Image(byte[] imageData, CheckIn checkIn) {
        this.imageData = imageData;
        this.checkIn = checkIn;
    }

    public Long getImageId() {
        return imageId;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public CheckIn getCheckIn() {
        return checkIn;
    }
}
