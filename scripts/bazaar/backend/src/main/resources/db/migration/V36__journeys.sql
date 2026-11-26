-- Wave A: journeys.

-- A customer journey: a flowchart customers walk through, a step at a time.
CREATE TABLE journey (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    graph_json TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX journey_store_id ON journey (store_id);
CREATE INDEX journey_status ON journey (status);

-- A customer inside a journey: where they are and when they move on.
CREATE TABLE journey_enrollment (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    journey_id VARCHAR(32) NOT NULL,
    customer_id VARCHAR(32) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    resume_at BIGINT NOT NULL,
    entered_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX journey_enrollment_status_resume_at ON journey_enrollment (status, resume_at);
CREATE INDEX journey_enrollment_journey_id ON journey_enrollment (journey_id);
CREATE UNIQUE INDEX journey_enrollment_u_journey_id_customer_id ON journey_enrollment (journey_id, customer_id);

-- How many customers have passed a journey node.
CREATE TABLE journey_node_stat (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    journey_id VARCHAR(32) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    passed BIGINT NOT NULL
);
CREATE UNIQUE INDEX journey_node_stat_u_journey_id_node_id ON journey_node_stat (journey_id, node_id);

