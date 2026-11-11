package org.example.authentication_cookiesession.repository;

import org.example.authentication_cookiesession.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ImageRepository extends JpaRepository<Image, UUID> {

    @Query("select image.imageUrl from Image image order by image.createdAt desc, image.id desc")
    List<String> findImageUrlsOrderByCreatedAtDesc();
}
