package edu.bookpict.domain.ranking;

import edu.bookpict.domain.book.Isbn;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "popular_rank", uniqueConstraints = @UniqueConstraint(columnNames = {"year_month", "isbn"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PopularRank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rank_id")
    private Long id;

    @Column(name = "school_level", length = 20)
    private String schoolLevel;

    @Column(length = 20)
    private String subject;

    @Column(name = "year_month", length = 6, nullable = false)
    private String yearMonth; // YYYYMM

    @Column(nullable = false)
    private Integer rank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "isbn", nullable = false)
    private Isbn isbn;

    @Column(name = "bookpict_index")
    private Double bookPictIndex;
}
