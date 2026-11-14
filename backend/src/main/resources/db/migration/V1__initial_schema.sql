-- JEEP initial schema (V1).
-- Columns match the JPA entities exactly: UUID primary keys, VARCHAR for
-- enums (stored as strings), TIMESTAMP WITH TIME ZONE for java.time.Instant.
-- Applied by Flyway on startup (spring.flyway.enabled=true) while
-- spring.jpa.hibernate.ddl-auto=validate checks the schema against the entities.
-- All identifiers are double-quoted so the names match Hibernate exactly:
-- the app sets hibernate.globally_quoted_identifiers=true, so Hibernate
-- expects lowercase quoted names. On PostgreSQL quoted and unquoted
-- lowercase names are the same; quoting also keeps H2 verification working.

CREATE TABLE "users" (
    "id" UUID NOT NULL,
    "first_name" VARCHAR(100) NOT NULL,
    "last_name" VARCHAR(100) NOT NULL,
    "email" VARCHAR(255) NOT NULL,
    "password_hash" VARCHAR(255) NOT NULL,
    "status" VARCHAR(30) NOT NULL,
    "role" VARCHAR(30) NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "last_login_at" TIMESTAMP WITH TIME ZONE,
    CONSTRAINT "pk_users" PRIMARY KEY ("id"),
    CONSTRAINT "uk_users_email" UNIQUE ("email")
);

CREATE TABLE "organizations" (
    "id" UUID NOT NULL,
    "name" VARCHAR(150) NOT NULL,
    "description" VARCHAR(500),
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "is_active" BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT "pk_organizations" PRIMARY KEY ("id")
);

CREATE TABLE "organization_members" (
    "id" UUID NOT NULL,
    "organization_id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "role" VARCHAR(30) NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "pk_organization_members" PRIMARY KEY ("id"),
    CONSTRAINT "fk_organization_members_organization" FOREIGN KEY ("organization_id") REFERENCES "organizations" ("id"),
    CONSTRAINT "fk_organization_members_user" FOREIGN KEY ("user_id") REFERENCES "users" ("id"),
    CONSTRAINT "uk_organization_members_org_user" UNIQUE ("organization_id", "user_id")
);
CREATE INDEX "idx_organization_members_organization_id" ON "organization_members" ("organization_id");
CREATE INDEX "idx_organization_members_user_id" ON "organization_members" ("user_id");

CREATE TABLE "teams" (
    "id" UUID NOT NULL,
    "name" VARCHAR(100) NOT NULL,
    "organization_id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "pk_teams" PRIMARY KEY ("id"),
    CONSTRAINT "fk_teams_organization" FOREIGN KEY ("organization_id") REFERENCES "organizations" ("id")
);
CREATE INDEX "idx_teams_organization_id" ON "teams" ("organization_id");

CREATE TABLE "team_members" (
    "id" UUID NOT NULL,
    "team_id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "role" VARCHAR(30) NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "pk_team_members" PRIMARY KEY ("id"),
    CONSTRAINT "fk_team_members_team" FOREIGN KEY ("team_id") REFERENCES "teams" ("id"),
    CONSTRAINT "fk_team_members_user" FOREIGN KEY ("user_id") REFERENCES "users" ("id"),
    CONSTRAINT "uk_team_members_team_user" UNIQUE ("team_id", "user_id")
);
CREATE INDEX "idx_team_members_team_id" ON "team_members" ("team_id");
CREATE INDEX "idx_team_members_user_id" ON "team_members" ("user_id");

CREATE TABLE "projects" (
    "id" UUID NOT NULL,
    "organization_id" UUID NOT NULL,
    "name" VARCHAR(150) NOT NULL,
    "description" VARCHAR(1000),
    "status" VARCHAR(30) NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "pk_projects" PRIMARY KEY ("id"),
    CONSTRAINT "fk_projects_organization" FOREIGN KEY ("organization_id") REFERENCES "organizations" ("id")
);
CREATE INDEX "idx_projects_organization_id" ON "projects" ("organization_id");

CREATE TABLE "tasks" (
    "id" UUID NOT NULL,
    "project_id" UUID NOT NULL,
    "assignee_id" UUID,
    "title" VARCHAR(200) NOT NULL,
    "description" VARCHAR(2000),
    "priority" VARCHAR(20) NOT NULL,
    "status" VARCHAR(20) NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "pk_tasks" PRIMARY KEY ("id"),
    CONSTRAINT "fk_tasks_project" FOREIGN KEY ("project_id") REFERENCES "projects" ("id"),
    CONSTRAINT "fk_tasks_assignee" FOREIGN KEY ("assignee_id") REFERENCES "users" ("id")
);
CREATE INDEX "idx_tasks_project_id" ON "tasks" ("project_id");
CREATE INDEX "idx_tasks_assignee_id" ON "tasks" ("assignee_id");

CREATE TABLE "repositories" (
    "id" UUID NOT NULL,
    "project_id" UUID NOT NULL,
    "provider" VARCHAR(30) NOT NULL,
    "remote_url" VARCHAR(500) NOT NULL,
    "default_branch" VARCHAR(100) NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "pk_repositories" PRIMARY KEY ("id"),
    CONSTRAINT "fk_repositories_project" FOREIGN KEY ("project_id") REFERENCES "projects" ("id")
);
CREATE INDEX "idx_repositories_project_id" ON "repositories" ("project_id");

CREATE TABLE "notifications" (
    "id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "type" VARCHAR(30) NOT NULL,
    "channel" VARCHAR(20) NOT NULL,
    "title" VARCHAR(200) NOT NULL,
    "message" VARCHAR(1000),
    "is_read" BOOLEAN NOT NULL DEFAULT FALSE,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "pk_notifications" PRIMARY KEY ("id"),
    CONSTRAINT "fk_notifications_user" FOREIGN KEY ("user_id") REFERENCES "users" ("id")
);
CREATE INDEX "idx_notifications_user_id" ON "notifications" ("user_id");
