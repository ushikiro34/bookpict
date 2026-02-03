package edu.bookpict.domain.book;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "isbn")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Isbn {

    @Id
    @Column(name = "isbn", length = 20)
    private String isbn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Column(name = "cover_type", length = 20)
    private String coverType; // paperback / hardcover

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "isbn", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Edition> editions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
