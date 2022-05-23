CREATE TABLE node_headers
(
    id                VARCHAR(64) PRIMARY KEY,
    parent_id         VARCHAR(64) NOT NULL,
    version           SMALLINT    NOT NULL,
    height            INTEGER     NOT NULL,
    n_bits            BIGINT      NOT NULL,
    difficulty        NUMERIC     NOT NULL,
    timestamp         BIGINT      NOT NULL,
    state_root        VARCHAR(66) NOT NULL,
    ad_proofs_root    VARCHAR(64) NOT NULL,
    transactions_root VARCHAR(64) NOT NULL,
    extension_hash    VARCHAR(64) NOT NULL,
    miner_pk          VARCHAR     NOT NULL,
    w                 VARCHAR     NOT NULL,
    n                 VARCHAR     NOT NULL,
    d                 VARCHAR     NOT NULL,
    votes             VARCHAR     NOT NULL,
    main_chain        BOOLEAN     NOT NULL
);

CREATE INDEX "node_headers__parent_id" ON node_headers (parent_id);
CREATE INDEX "node_headers__height" ON node_headers (height);
CREATE INDEX "node_headers__ts" ON node_headers (timestamp);
CREATE INDEX "node_headers__main_chain" ON node_headers (main_chain);
CREATE INDEX "node_headers__d" ON node_headers (d);

CREATE TABLE node_extensions
(
    header_id VARCHAR(64) PRIMARY KEY REFERENCES node_headers (id),
    digest    VARCHAR(64) NOT NULL,
    fields    JSON        NOT NULL
);

CREATE TABLE node_ad_proofs
(
    header_id   VARCHAR(64) PRIMARY KEY REFERENCES node_headers (id),
    proof_bytes VARCHAR NOT NULL,
    digest      VARCHAR NOT NULL
);

/* Block stats
 */
CREATE TABLE blocks_info
(
    header_id              VARCHAR(64) PRIMARY KEY REFERENCES node_headers (id),
    timestamp              BIGINT  NOT NULL,
    height                 INTEGER NOT NULL,
    difficulty             BIGINT  NOT NULL,
    block_size             INTEGER NOT NULL,
    block_coins            BIGINT  NOT NULL,
    block_mining_time      BIGINT,
    txs_count              INTEGER NOT NULL,
    txs_size               INTEGER NOT NULL,
    miner_address          VARCHAR NOT NULL,
    miner_reward           BIGINT  NOT NULL,
    miner_revenue          BIGINT  NOT NULL,
    block_fee              BIGINT  NOT NULL,
    block_chain_total_size BIGINT  NOT NULL,
    total_txs_count        BIGINT  NOT NULL,
    total_coins_issued     BIGINT  NOT NULL,
    total_mining_time      BIGINT  NOT NULL,
    total_fees             BIGINT  NOT NULL,
    total_miners_reward    BIGINT  NOT NULL,
    total_coins_in_txs     BIGINT  NOT NULL,
    max_tx_gix             BIGINT  NOT NULL,
    max_box_gix            BIGINT  NOT NULL,
    main_chain             BOOLEAN NOT NULL
);

/* Stats table indexes. By height and ts.
 */
CREATE INDEX "blocks_info__height" ON blocks_info (height);
CREATE INDEX "blocks_info__ts" ON blocks_info (timestamp);
CREATE INDEX "blocks_info__main_chain" ON blocks_info (main_chain);

CREATE TABLE node_transactions
(
    id               VARCHAR(64) NOT NULL,
    header_id        VARCHAR(64) REFERENCES node_headers (id),
    inclusion_height INTEGER     NOT NULL,
    coinbase         BOOLEAN     NOT NULL,
    timestamp        BIGINT      NOT NULL,
    size             INTEGER     NOT NULL,
    index            INTEGER     NOT NULL,
    global_index     BIGINT      NOT NULL,
    main_chain       BOOLEAN     NOT NULL,
    PRIMARY KEY (id, header_id)
);

CREATE INDEX "node_transactions__header_id" ON node_transactions (header_id);
CREATE INDEX "node_transactions__timestamp" ON node_transactions (timestamp);
CREATE INDEX "node_transactions__inclusion_height" ON node_transactions (inclusion_height);
CREATE INDEX "node_transactions__main_chain" ON node_transactions (main_chain);

/* Table that represents inputs in ergo transactions.
 * Has tx_id field that point to the tx where this input was spent.
 */
CREATE TABLE node_inputs
(
    box_id      VARCHAR(64) NOT NULL,
    tx_id       VARCHAR(64) NOT NULL,
    header_id   VARCHAR(64) NOT NULL,
    proof_bytes VARCHAR,
    extension   JSON        NOT NULL,
    index       INTEGER     NOT NULL,
    main_chain  BOOLEAN     NOT NULL,
    PRIMARY KEY (box_id, header_id)
);

CREATE INDEX "node_inputs__tx_id" ON node_inputs (tx_id);
CREATE INDEX "node_inputs__box_id" ON node_inputs (box_id);
CREATE INDEX "node_inputs__header_id" ON node_inputs (header_id);
CREATE INDEX "node_inputs__main_chain" ON node_inputs (main_chain);

CREATE TABLE node_data_inputs
(
    box_id      VARCHAR(64) NOT NULL,
    tx_id       VARCHAR(64) NOT NULL,
    header_id   VARCHAR(64) NOT NULL,
    index       INTEGER     NOT NULL,
    main_chain  BOOLEAN     NOT NULL,
    PRIMARY KEY (box_id, tx_id, header_id)
);

CREATE INDEX "node_data_inputs__tx_id" ON node_data_inputs (tx_id);
CREATE INDEX "node_data_inputs__box_id" ON node_data_inputs (box_id);
CREATE INDEX "node_data_inputs__header_id" ON node_data_inputs (header_id);
CREATE INDEX "node_data_inputs__main_chain" ON node_data_inputs (main_chain);

/* Table that represents outputs in ergo transactions.
 * Has tx_id field pointing to the tx which created this output.
 */
CREATE TABLE node_outputs
(
    box_id                  VARCHAR(64) NOT NULL,
    tx_id                   VARCHAR(64) NOT NULL,
    header_id               VARCHAR(64) NOT NULL,
    value                   BIGINT      NOT NULL,
    creation_height         INTEGER     NOT NULL,
    settlement_height       INTEGER     NOT NULL,
    index                   INTEGER     NOT NULL,
    global_index            BIGINT      NOT NULL,
    ergo_tree               VARCHAR     NOT NULL,
    ergo_tree_template_hash VARCHAR(64) NOT NULL,
    address                 VARCHAR     NOT NULL,
    additional_registers    JSON        NOT NULL,
    timestamp               BIGINT      NOT NULL,
    main_chain              BOOLEAN     NOT NULL,
    PRIMARY KEY (box_id, header_id)
);

CREATE INDEX "node_outputs__box_id" ON node_outputs (box_id);
CREATE INDEX "node_outputs__tx_id" ON node_outputs (tx_id);
CREATE INDEX "node_outputs__header_id" ON node_outputs (header_id);
CREATE INDEX "node_outputs__address" ON node_outputs using hash (address);
CREATE INDEX "node_outputs__ergo_tree" ON node_outputs using hash (ergo_tree);
CREATE INDEX "node_outputs__ergo_tree_template_hash" ON node_outputs (ergo_tree_template_hash);
CREATE INDEX "node_outputs__timestamp" ON node_outputs (timestamp);
CREATE INDEX "node_outputs__main_chain" ON node_outputs (main_chain);

CREATE TABLE node_assets
(
    token_id  VARCHAR(64) NOT NULL,
    box_id    VARCHAR(64) NOT NULL,
    header_id VARCHAR(64) NOT NULL,
    index     INTEGER     NOT NULL,
    value     BIGINT      NOT NULL,
    PRIMARY KEY (index, token_id, box_id, header_id)
);

CREATE INDEX "node_assets__box_id" ON node_assets (box_id);
CREATE INDEX "node_assets__token_id" ON node_assets (token_id);
CREATE INDEX "node_assets__header_id" ON node_assets (header_id);

CREATE TABLE box_registers
(
    id               VARCHAR(2)    NOT NULL,
    box_id           VARCHAR(64)   NOT NULL,
    value_type       VARCHAR(128)  NOT NULL,
    serialized_value VARCHAR       NOT NULL,
    rendered_value   VARCHAR       NOT NULL,
    PRIMARY KEY (id, box_id)
);

CREATE INDEX "box_registers__id" ON box_registers (id);
CREATE INDEX "box_registers__box_id" ON box_registers (box_id);

CREATE TABLE script_constants
(
    index            INTEGER       NOT NULL,
    box_id           VARCHAR(64)   NOT NULL,
    value_type       VARCHAR(128)  NOT NULL,
    serialized_value VARCHAR       NOT NULL,
    rendered_value   VARCHAR       NOT NULL,
    PRIMARY KEY (index, box_id)
);

CREATE INDEX "script_constants__box_id" ON script_constants (box_id);
CREATE INDEX "script_constants__rendered_value" ON script_constants using hash (rendered_value);

/* Unconfirmed transactions.
 */
CREATE TABLE node_u_transactions
(
    id                 VARCHAR(64) PRIMARY KEY,
    creation_timestamp BIGINT  NOT NULL,
    size               INTEGER NOT NULL
);

/* Inputs containing in unconfirmed transactions.
 */
CREATE TABLE node_u_inputs
(
    box_id      VARCHAR(64) NOT NULL,
    tx_id       VARCHAR(64) NOT NULL REFERENCES node_u_transactions (id) ON DELETE CASCADE,
    index       INTEGER     NOT NULL,
    proof_bytes VARCHAR,
    extension   JSON        NOT NULL,
    PRIMARY KEY (box_id, tx_id)
);

CREATE INDEX "node_u_inputs__tx_id" ON node_u_inputs (tx_id);
CREATE INDEX "node_u_inputs__box_id" ON node_u_inputs (box_id);

/* Data inputs containing in unconfirmed transactions.
 */
CREATE TABLE node_u_data_inputs
(
    box_id      VARCHAR(64) NOT NULL,
    tx_id       VARCHAR(64) NOT NULL REFERENCES node_u_transactions (id) ON DELETE CASCADE,
    index       INTEGER     NOT NULL,
    PRIMARY KEY (box_id, tx_id)
);

CREATE INDEX "node_u_data_inputs__tx_id" ON node_u_data_inputs (tx_id);
CREATE INDEX "node_u_data_inputs__box_id" ON node_u_data_inputs (box_id);

/* Outputs containing in unconfirmed transactions.
 */
CREATE TABLE node_u_outputs
(
    box_id                  VARCHAR(64) PRIMARY KEY,
    tx_id                   VARCHAR(64) NOT NULL REFERENCES node_u_transactions (id) ON DELETE CASCADE,
    value                   BIGINT      NOT NULL,
    creation_height         INTEGER     NOT NULL,
    index                   INTEGER     NOT NULL,
    ergo_tree               VARCHAR     NOT NULL,
    ergo_tree_template_hash VARCHAR(64) NOT NULL,
    address                 VARCHAR,
    additional_registers    JSON        NOT NULL
);

CREATE INDEX "node_u_outputs__box_id" ON node_u_outputs (box_id);
CREATE INDEX "node_u_outputs__tx_id" ON node_u_outputs (tx_id);
CREATE INDEX "node_u_outputs__address" ON node_u_outputs using hash (address);
CREATE INDEX "node_u_outputs__ergo_tree_template_hash" ON node_u_outputs (ergo_tree_template_hash);

/* Inputs containing in unconfirmed outputs.
 */
CREATE TABLE node_u_assets
(
    token_id VARCHAR(64) NOT NULL,
    box_id   VARCHAR(64) NOT NULL REFERENCES node_u_outputs (box_id) ON DELETE CASCADE,
    index    INTEGER     NOT NULL,
    value    BIGINT      NOT NULL,
    PRIMARY KEY (index, token_id, box_id)
);

CREATE INDEX "node_u_assets__box_id" ON node_u_assets (box_id);

/* Verbose names for known miners.
 */
CREATE TABLE known_miners
(
    miner_address VARCHAR PRIMARY KEY,
    miner_name    VARCHAR NOT NULL
);

CREATE TABLE tokens
(
    token_id        VARCHAR(64)   PRIMARY KEY,
    box_id          VARCHAR(64)   NOT NULL,
    emission_amount BIGINT        NOT NULL,
    name            VARCHAR,
    description     VARCHAR,
    type            VARCHAR,
    decimals        INTEGER
);

CREATE INDEX "tokens__box_id" ON tokens (box_id);

CREATE TABLE epochs_parameters
(
    id                 INTEGER  PRIMARY KEY,
    height             INTEGER  NOT NULL,
    storage_fee_factor INTEGER  NOT NULL,
    min_value_per_byte INTEGER  NOT NULL,
    max_block_size     INTEGER  NOT NULL,
    max_block_cost     INTEGER  NOT NULL,
    block_version      SMALLINT NOT NULL,
    token_access_cost  INTEGER  NOT NULL,
    input_cost         INTEGER  NOT NULL,
    data_input_cost    INTEGER  NOT NULL,
    output_cost        INTEGER  NOT NULL
);

CREATE INDEX "epochs_parameters_height" ON epochs_parameters (height);
