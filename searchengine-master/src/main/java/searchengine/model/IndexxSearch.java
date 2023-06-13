package searchengine.model;

import lombok.*;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@Table(name = "indexx")
@Getter
@Setter
public class IndexxSearch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @Column(name = "page_id",nullable = false)
    private int pageId;

    @Column(name = "lemma_id",nullable = false)
    private int lemmaId;

    @Column(nullable = false)
    private float lemmaCount;

    @ManyToOne
    @JoinColumn(name = "page_id",insertable = false,updatable = false,nullable = false)
    private PageEntity page;

    @ManyToOne
    @JoinColumn(name = "lemma_id",insertable = false,updatable = false,nullable = false)
    private LemmaEntity lemma;
    }
