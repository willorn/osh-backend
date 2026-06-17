package com.backstage.system.domain.openproject.dto;

import com.backstage.common.annotation.OshResourceId;

import java.util.List;

public class OpenProjectEditDTO {

    @OshResourceId
    private Long id;
    private String projectName;
    private String projectDesc;
    private String authorName;
    private String projectCover;
    private List<Long> tagIds;
    private List<String> customTags;
    private List<OpenProjectResourceDTO> resources;
    private List<OpenProjectContributorDTO> contributors;
    private List<OpenProjectModuleDTO> modules;
    private List<OpenProjectTechComponentDTO> techComponents;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getProjectDesc() { return projectDesc; }
    public void setProjectDesc(String projectDesc) { this.projectDesc = projectDesc; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getProjectCover() { return projectCover; }
    public void setProjectCover(String projectCover) { this.projectCover = projectCover; }

    public List<Long> getTagIds() { return tagIds; }
    public void setTagIds(List<Long> tagIds) { this.tagIds = tagIds; }

    public List<String> getCustomTags() { return customTags; }
    public void setCustomTags(List<String> customTags) { this.customTags = customTags; }

    public List<OpenProjectResourceDTO> getResources() { return resources; }
    public void setResources(List<OpenProjectResourceDTO> resources) { this.resources = resources; }

    public List<OpenProjectContributorDTO> getContributors() { return contributors; }
    public void setContributors(List<OpenProjectContributorDTO> contributors) { this.contributors = contributors; }

    public List<OpenProjectModuleDTO> getModules() { return modules; }
    public void setModules(List<OpenProjectModuleDTO> modules) { this.modules = modules; }

    public List<OpenProjectTechComponentDTO> getTechComponents() { return techComponents; }
    public void setTechComponents(List<OpenProjectTechComponentDTO> techComponents) { this.techComponents = techComponents; }
}
