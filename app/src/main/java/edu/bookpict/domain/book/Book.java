package edu.bookpict.domain.book;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "book")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @Column(name = "book_id", length = 50)
    private String bookId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 100)
    private String publisher;

    @Column(nullable = false, length = 20)
    private String subject; // 국어/영어/수학/역사 등

    @Column(name = "school_level", nullable = false, length = 20)
    private String schoolLevel; // 초등 / 중등 등

    @Column(name = "grade", length = 10)
    private String grade; // 1학년 ~ 6학년 등

    @Column(name = "semester", length = 10)
    private String semester; // 1학기, 2학기, 전체

    @Column(length = 20)
    private String curriculum; // 2015 / 2022 개정

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String toc;

    @Column(name = "category_name", length = 500)
    private String categoryName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "full_description", columnDefinition = "TEXT")
    private String fullDescription;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Isbn> isbns = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
