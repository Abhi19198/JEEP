package com.jeep.project.service;

import com.jeep.identity.domain.User;
import com.jeep.identity.exception.IdentityException;
import com.jeep.identity.repository.UserRepository;
import com.jeep.organization.domain.Organization;
import com.jeep.organization.domain.OrganizationMember;
import com.jeep.organization.repository.OrganizationMemberRepository;
import com.jeep.organization.repository.OrganizationRepository;
import com.jeep.project.domain.Project;
import com.jeep.project.domain.ProjectStatus;
import com.jeep.project.dto.CreateProjectRequest;
import com.jeep.project.dto.ProjectResponse;
import com.jeep.project.dto.UpdateProjectRequest;
import com.jeep.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProjectResponse createProject(String userEmail, CreateProjectRequest request) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));

        Organization organization = organizationRepository.findById(UUID.fromString(request.organizationId()))
            .orElseThrow(() -> new IdentityException("Organization not found"));

        organizationMemberRepository.findByOrganizationAndUser(organization, user)
            .orElseThrow(() -> new IdentityException("User is not a member of this organization"));

        Project project = Project.builder()
            .organization(organization)
            .name(request.name())
            .description(request.description())
            .status(ProjectStatus.ACTIVE)
            .build();

        Project saved = projectRepository.save(project);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsForOrganization(String userEmail, UUID organizationId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new IdentityException("Organization not found"));
        organizationMemberRepository.findByOrganizationAndUser(organization, user)
            .orElseThrow(() -> new IdentityException("User is not a member of this organization"));

        return projectRepository.findByOrganization(organization).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(String userEmail, UUID projectId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new IdentityException("Project not found"));
        organizationMemberRepository.findByOrganizationAndUser(project.getOrganization(), user)
            .orElseThrow(() -> new IdentityException("User is not a member of this organization"));
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(String userEmail, UUID projectId, UpdateProjectRequest request) {
        Project project = findMemberProject(userEmail, projectId);
        project.update(request.name(), request.description(), request.status());
        return toResponse(project);
    }

    @Transactional
    public void archiveProject(String userEmail, UUID projectId) {
        Project project = findMemberProject(userEmail, projectId);
        project.update(project.getName(), project.getDescription(), ProjectStatus.ARCHIVED);
    }

    private Project findMemberProject(String userEmail, UUID projectId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new IdentityException("Project not found"));
        organizationMemberRepository.findByOrganizationAndUser(project.getOrganization(), user)
            .orElseThrow(() -> new IdentityException("User is not a member of this organization"));
        return project;
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
            project.getId(),
            project.getOrganization().getId(),
            project.getName(),
            project.getDescription(),
            project.getStatus(),
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }
}
