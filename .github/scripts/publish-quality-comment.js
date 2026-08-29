const fs = require('fs');

/**
 * Publish or update a pull request comment containing a quality report.
 *
 * @param {{github: import('@actions/github').GitHub, context: any, core: any, marker?: string, reportPath?: string}} options
 */
async function publishQualityComment({ github, context, core, marker, reportPath }) {
  const effectiveMarker = marker || '<!-- quality-report -->';
  const report = reportPath || 'quality-report.md';

  if (!fs.existsSync(report)) {
    core.warning(`${report} was not generated.`);
    return;
  }

  const body = `${effectiveMarker}\n${fs.readFileSync(report, 'utf8')}`;
  const { owner, repo } = context.repo;
  const issue_number = context.issue.number;

  // WARN, never throw. Publishing is not the gate: the workflow enforces the
  // analysis with generate-quality-report.py in an earlier step and this one
  // runs under `if: always()` purely so a human can read the result. Letting
  // it throw meant a transient GitHub API error failed a build whose gate had
  // already passed -- run 33263893857 died on a 504 from PATCH
  // /issues/comments while posting a report whose own first line was
  // "Continuous Quality Report" with 7416 tests and 0 failures. A red build
  // that says nothing is wrong is worse than a missing comment, and it
  // teaches people to re-run reds without reading them.
  try {
    const { data: comments } = await github.rest.issues.listComments({
      owner,
      repo,
      issue_number,
      per_page: 100,
    });

    const existing = comments.find(
      (comment) => comment.user?.type === 'Bot' && comment.body?.includes(effectiveMarker),
    );

    if (existing) {
      await github.rest.issues.updateComment({
        owner,
        repo,
        comment_id: existing.id,
        body,
      });
    } else {
      await github.rest.issues.createComment({
        owner,
        repo,
        issue_number,
        body,
      });
    }
  } catch (error) {
    // Surfaced as an annotation on the run, so a comment that stopped being
    // published is visible rather than silently absent.
    core.warning(`Could not publish the quality report comment: ${error.message}`);
  }
}

module.exports = { publishQualityComment };
