package com.jeep.repository.service;

import com.jeep.identity.domain.User;
import com.jeep.identity.exception.IdentityException;
import com.jeep.identity.repository.UserRepository;
import com.jeep.organization.repository.OrganizationMemberRepository;
import com.jeep.project.domain.Project;
import com.jeep.project.repository.ProjectRepository;
import com.jeep.repository.domain.Repository;
import com.jeep.repository.domain.RepositoryProvider;
import com.jeep.repository.dto.CreateRepositoryRequest;
import com.jeep.repository.dto.RepositoryResponse;
import com.jeep.repository.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepositoryService {

    private final RepositoryRepository repositoryRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    @Transactional
    public RepositoryResponse createRepository(String userEmail, CreateRepositoryRequest request) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));

        Project project = projectRepository.findById(request.projectId())
            .orElseThrow(() -> new IdentityException("Project not found"));

        organizationMemberRepository.findByOrganizationAndUser(project.getOrganization(), user)
            .orElseThrow(() -> new IdentityException("User is not a member of this organization"));

        RepositoryProvider provider = switch (request.provider().toLowerCase()) {
            case "github" -> RepositoryProvider.GITHUB;
            case "gitlab" -> RepositoryProvider.GITLAB;
            case "bitbucket" -> RepositoryProvider.BITBUCKET;
            case "azure" -> RepositoryProvider.AZURE_DEVOPS;
            case "azure-devops" -> RepositoryProvider.AZURE_DEVOPS;
            default -> throw new IdentityException("Unsupported repository provider");
        };

        Repository repository = Repository.builder()
            .project(project)
            .provider(provider)
            .remoteUrl(request.remoteUrl())
            .defaultBranch(request.defaultBranch())
            .build();

        Repository saved = repositoryRepository.save(repository);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RepositoryResponse> getRepositoriesForProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new IdentityException("Project not found"));

        return repositoryRepository.findByProject(project).stream()
            .map(this::toResponse)
            .toList();
    }

    private RepositoryResponse toResponse(Repository repository) {
        return new RepositoryResponse(
            repository.getId(),
            repository.getProject().getId(),
            repository.getProvider(),
            repository.getRemoteUrl(),
            repository.getDefaultBranch(),
            repository.getCreatedAt(),
            repository.getUpdatedAt()
        );
    }
}
