--
-- Name: configuration; Type: TABLE; Schema: public;
--

CREATE TABLE configuration (
    readgloballimit NUMBER NOT NULL,
    writegloballimit NUMBER NOT NULL,
    readsessionlimit NUMBER NOT NULL,
    writesessionlimit NUMBER NOT NULL,
    delaylimit NUMBER NOT NULL,
    updatedinfo NUMBER NOT NULL,
    hostid VARCHAR2(250) NOT NULL
);

--
-- Name: hostconfig; Type: TABLE; Schema: public;
--

CREATE TABLE hostconfig (
    business VARCHAR2(4000) NOT NULL,
    roles VARCHAR2(4000) NOT NULL,
    aliases VARCHAR2(4000) NOT NULL,
    others VARCHAR2(4000) NOT NULL,
    updatedinfo NUMBER NOT NULL,
    hostid VARCHAR2(250) NOT NULL
);

--
-- Name: hosts; Type: TABLE; Schema: public;
--

CREATE TABLE hosts (
    address VARCHAR2(250) NOT NULL,
    port NUMBER NOT NULL,
    isssl NUMBER(1) NOT NULL,
    hostkey RAW(1024) NOT NULL,
    adminrole NUMBER(1) NOT NULL,
    isclient NUMBER(1) NOT NULL,
    isactive NUMBER(1) NOT NULL,
    isproxified NUMBER(1) NOT NULL,
    updatedinfo NUMBER NOT NULL,
    hostid VARCHAR2(250) NOT NULL
);

--
-- Name: multiplemonitor; Type: TABLE; Schema: public;
--

CREATE TABLE multiplemonitor (
    countconfig NUMBER NOT NULL,
    counthost NUMBER NOT NULL,
    countrule NUMBER NOT NULL,
    hostid VARCHAR2(250) NOT NULL
);

--
-- Name: rules; Type: TABLE; Schema: public;
--

CREATE TABLE rules (
    hostids VARCHAR2(4000),
    modetrans NUMBER(1),
    recvpath VARCHAR2(4000),
    sendpath VARCHAR2(4000),
    archivepath VARCHAR2(4000),
    workpath VARCHAR2(4000),
    rpretasks VARCHAR2(4000),
    rposttasks VARCHAR2(4000),
    rerrortasks VARCHAR2(4000),
    spretasks VARCHAR2(4000),
    sposttasks VARCHAR2(4000),
    serrortasks VARCHAR2(4000),
    updatedinfo NUMBER,
    idrule VARCHAR2(250) NOT NULL
);

--
-- Name: runner; Type: TABLE; Schema: public;
--

CREATE TABLE runner (
    globalstep NUMBER NOT NULL,
    globallaststep NUMBER NOT NULL,
    step NUMBER NOT NULL,
    rank NUMBER NOT NULL,
    stepstatus CHAR(3) NOT NULL,
    retrievemode NUMBER(1) NOT NULL,
    filename VARCHAR2(4000) NOT NULL,
    ismoved NUMBER(1) NOT NULL,
    idrule VARCHAR2(250) NOT NULL,
    blocksz NUMBER NOT NULL,
    originalname VARCHAR2(4000) NOT NULL,
    fileinfo VARCHAR2(4000) NOT NULL,
    transferinfo VARCHAR2(4000) NOT NULL,
    modetrans NUMBER NOT NULL,
    starttrans TIMESTAMP NOT NULL,
    stoptrans TIMESTAMP NOT NULL,
    infostatus CHAR(3) NOT NULL,
    updatedinfo NUMBER NOT NULL,
    ownerreq VARCHAR2(250) NOT NULL,
    requester VARCHAR2(250) NOT NULL,
    requested VARCHAR2(250) NOT NULL,
    specialid NUMBER NOT NULL
);

--
-- Name: runseq; Type: SEQUENCE; Schema: public;
--

CREATE SEQUENCE runseq
  START WITH -9223372036854775807
  INCREMENT BY 1
  MINVALUE -9223372036854775807
  NOMAXVALUE
  NOCACHE;

--
-- Name: runseq; Type: SEQUENCE SET; Schema: public;
--

-- SELECT pg_catalog.setval('runseq', -9223372036854775805, true);

--
-- Name: configuration configuration_pkey; Type: CONSTRAINT; Schema: public;
--

ALTER TABLE configuration
    ADD CONSTRAINT configuration_pkey PRIMARY KEY (hostid);

--
-- Name: hostconfig hostconfig_pkey; Type: CONSTRAINT; Schema: public;
--

ALTER TABLE hostconfig
    ADD CONSTRAINT hostconfig_pkey PRIMARY KEY (hostid);

--
-- Name: hosts hosts_pkey; Type: CONSTRAINT; Schema: public;
--

ALTER TABLE hosts
    ADD CONSTRAINT hosts_pkey PRIMARY KEY (hostid);

--
-- Name: multiplemonitor multiplemonitor_pkey; Type: CONSTRAINT; Schema: public;
--

ALTER TABLE multiplemonitor
    ADD CONSTRAINT multiplemonitor_pkey PRIMARY KEY (hostid);

--
-- Name: rules rules_pkey; Type: CONSTRAINT; Schema: public;
--

ALTER TABLE rules
    ADD CONSTRAINT rules_pkey PRIMARY KEY (idrule);

--
-- Name: runner runner_pk; Type: CONSTRAINT; Schema: public;
--

ALTER TABLE runner
    ADD CONSTRAINT runner_pk PRIMARY KEY (ownerreq, requester, requested, specialid);



CREATE INDEX idx_config ON configuration (hostid, updatedinfo);
CREATE INDEX idx_hostconf ON hostconfig (hostid, updatedinfo);
CREATE INDEX idx_host ON hosts (updatedinfo);
CREATE INDEX idx_rule ON rules (updatedinfo);
CREATE INDEX idx_run_filter ON runner (ownerreq, starttrans, updatedinfo, stepstatus, infostatus, globallaststep, globalstep, requested, stoptrans);
