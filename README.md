\# Jira GitHub Integration Demo



This repository demonstrates DevOps workflow integration using Jira, GitHub, Jenkins, Docker, and Slack.



\## SCRUM-8: Jira and GitHub Integration



This change is linked to Jira issue SCRUM-8.



GitHub commits and pull requests are tracked in Jira to provide development traceability.



\## SCRUM-10: GitHub and Slack Integration



This change is linked to Jira issue SCRUM-10 for GitHub and Slack activity tracking.



Slack notification testing was completed for SCRUM-10 to validate GitHub activity notifications in Slack.



\## SCRUM-11: GitHub and Jira Integration Verification



This change verifies the Jira and GitHub integration workflow.



\- \*\*Jira issue:\*\* SCRUM-11

\- \*\*GitHub repository:\*\* `santoshibai/jenkins-cicd-demo`

\- \*\*Feature branch:\*\* `feature/SCRUM-11-github-jira-integration`

\- \*\*Target branch:\*\* `main`



\### Verification Objectives



1\. Verify that commits referencing SCRUM-11 are associated with the correct Jira issue.

2\. Verify that the SCRUM-11 feature branch is available in GitHub.

3\. Create a pull request from the SCRUM-11 feature branch into `main`.

4\. Verify that the associated GitHub branch, commits, and pull request appear in the Jira Development panel.



\### Expected Result



Jira displays the relevant GitHub development activity associated with SCRUM-11, providing traceability between Jira issues and GitHub development work.



\## DevOps CI/CD Workflow



1\. Track development tasks in Jira.

2\. Manage source code and feature branches in GitHub.

3\. Trigger Jenkins builds through GitHub integration.

4\. Build and verify the Java application using Maven.

5\. Build and run the application using Docker.

6\. Send workflow notifications to Slack.

7\. Track development activity and CI/CD outcomes.



\## Application Health Check



The application exposes a health-check endpoint:



`/health`



Expected response:



`Application is healthy`



\## Integration Summary



| Integration | Purpose |

|---|---|

| Jira | Issue tracking and development traceability |

| GitHub | Source control, branches, commits, and pull requests |

| Jenkins | CI/CD pipeline automation |

| Maven | Java build and verification |

| Docker | Application containerization |

| Slack | Team notifications |



\## Important Notes



\- Include Jira issue keys in relevant commit messages and pull request titles or descriptions.

\- Use feature branches for integration testing and submit pull requests into `main`.

\- Verify changes before merging a pull request.

\- Keep credentials and sensitive configuration out of source control.

