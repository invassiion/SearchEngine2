package searchengine.model;
import lombok.*;


import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "page",indexes = {@Index(name = "path_index",columnList = "path")})
@Data
@NoArgsConstructor
public class PageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column( nullable = false)
    private int id;

    @Column(columnDefinition = "TEXT NOT NULL")
    private String path;

    @Column( nullable = false)
    private int code;

    @Column( columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "site_id", nullable = false)
    private int siteId;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false, insertable = false,updatable = false)
    private SiteEntity siteEntity;
}
