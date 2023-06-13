package searchengine.model;
import lombok.*;
import javax.persistence.*;

@Entity
@Table(name = "lemma",uniqueConstraints = @UniqueConstraint(columnNames = "lemma"))
@NoArgsConstructor
@Data
public class LemmaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @Column(name = "site_id", nullable = false)
    private int siteId;

    @ManyToOne
    @JoinColumn(name = "site_id",insertable = false,updatable = false,nullable = false)
    private SiteEntity siteEntity;

}
