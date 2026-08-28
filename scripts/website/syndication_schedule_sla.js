function expectedRunWindow(nowValue, scheduleHourUtc, scheduleMinuteUtc, graceHours) {
  const now = new Date(nowValue);
  const values = [
    now.getTime(),
    scheduleHourUtc,
    scheduleMinuteUtc,
    graceHours,
  ];
  if (!values.every(Number.isFinite)) {
    throw new Error('Syndication schedule SLA values must be finite numbers.');
  }

  const expectedStart = new Date(now);
  expectedStart.setUTCHours(scheduleHourUtc, scheduleMinuteUtc, 0, 0);
  const deadline = new Date(expectedStart.getTime() + graceHours * 3600000);
  if (now < deadline) {
    expectedStart.setUTCDate(expectedStart.getUTCDate() - 1);
    deadline.setUTCDate(deadline.getUTCDate() - 1);
  }

  return { expectedStart, deadline };
}

module.exports = { expectedRunWindow };
