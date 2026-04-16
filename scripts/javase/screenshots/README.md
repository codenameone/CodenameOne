# JavaSE simulator screenshot baselines

This directory stores baseline PNG files for JavaSE simulator integration screenshot tests:

- `javase-single-window.png`
- `javase-multi-window.png`

The CI workflow compares generated simulator screenshots with these files.
If screenshots differ (or are missing), CI uploads artifacts and posts a PR comment with visual previews.
