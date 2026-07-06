// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::monetization-sql-001[]
create TABLE RECEIPTS
(
	TRANSACTION_ID VARCHAR(128) not null,
	USERNAME VARCHAR(64) not null,
	SKU VARCHAR(128) not null,
	ORDER_DATA VARCHAR(32000),
	PURCHASE_DATE BIGINT,
	EXPIRY_DATE BIGINT,
	CANCELLATION_DATE BIGINT,
	LAST_VALIDATED BIGINT,
	STORE_CODE VARCHAR(20) default '' not null,
	primary key (TRANSACTION_ID, STORE_CODE)
)
// end::monetization-sql-001[]
