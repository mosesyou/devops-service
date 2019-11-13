package io.choerodon.devops.infra.dto;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Creator: Runge
 * Date: 2018/6/19
 * Time: 11:10
 * Description:
 */
@Table(name = "devops_app_service_version_readme")
public class AppServiceVersionReadmeDTO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String readme;

    public AppServiceVersionReadmeDTO() {
    }


    /**
     * constructor
     *
     * @param readme README.md 内容
     */
    public AppServiceVersionReadmeDTO(String readme) {

        this.readme = readme;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReadme() {
        return readme;
    }

    public void setReadme(String readme) {
        this.readme = readme;
    }
}
