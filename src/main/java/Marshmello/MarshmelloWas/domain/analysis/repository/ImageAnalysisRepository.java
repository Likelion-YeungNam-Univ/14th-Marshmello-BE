package Marshmello.MarshmelloWas.domain.analysis.repository;

import Marshmello.MarshmelloWas.domain.analysis.entity.ImageAnalysis;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageAnalysisRepository extends JpaRepository<ImageAnalysis, Long> {

    List<ImageAnalysis> findByImageIdIn(Collection<Long> imageIds);
}
