package edu.bookpict.domain.book;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "edition", uniqueConstraints = {
        @UniqueConstraint(name = "uk_isbn_edition_print", columnNames = { "isbn", "edition_number", "print_number" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Edition {

    @Id
    @Column(name = "edition_id", length = 50)
    private String editionId; // isbn + seq

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "isbn", nullable = false)
    private Isbn isbn;

    @Column(name = "edition_number", nullable = false)
    private Integer editionNumber; // 1판, 2판

    @Column(name = "print_number", nullable = false)
    private Integer printNumber; // 1쇄, 2쇄

    @Column(name = "is_latest")
    @Builder.Default
    private Boolean isLatest = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
