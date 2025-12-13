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
}

module.exports = { publishQualityComment };
