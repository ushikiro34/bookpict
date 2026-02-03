package edu.bookpict.domain.ranking;

import edu.bookpict.domain.book.Book;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Book과 ISBN13을 연결하는 공통 매칭 테이블
 * - 하나의 Book이 여러 ISBN을 가질 수 있음
 * - ISBN13을 통해 SalesRanking, AladinSnapshot과 연결
 */
@Entity
@Table(name = "book_isbn_link",
       uniqueConstraints = @UniqueConstraint(columnNames = {"isbn13"}),
       indexes = {
           @Index(name = "idx_link_book_id", columnList = "book_id"),
           @Index(name = "idx_link_isbn13", columnList = "isbn13")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookIsbnLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "isbn13", nullable = false, length = 13)
    private String isbn13;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "bookIsbnLink", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SalesRanking> salesRankings = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
