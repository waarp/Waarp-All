--
-- Name: configuration; Type: TABLE; Schema: public;
--

CREATE TABLE public.configuration (
    readgloballimit bigint NOT NULL,
    writegloballimit bigint NOT NULL,
    readsessionlimit bigint NOT NULL,
    writesessionlimit bigint NOT NULL,
    delaylimit bigint NOT NULL,
    updatedinfo integer NOT NULL,
    hostid character varying(250) NOT NULL
);

--
-- Name: hostconfig; Type: TABLE; Schema: public;
--

CREATE TABLE public.hostconfig (
    business VARCHAR(16000) NOT NULL,
    roles VARCHAR(16000) NOT NULL,
    aliases VARCHAR(16000) NOT NULL,
    others VARCHAR(16000) NOT NULL,
    updatedinfo integer NOT NULL,
    hostid character varying(250) NOT NULL
);

--
-- Name: hosts; Type: TABLE; Schema: public;
--

CREATE TABLE public.hosts (
    address character varying(250) NOT NULL,
    port integer NOT NULL,
    isssl boolean NOT NULL,
    hostkey bytea NOT NULL,
    adminrole boolean NOT NULL,
    isclient boolean NOT NULL,
    isactive boolean NOT NULL,
    isproxified boolean NOT NULL,
    updatedinfo integer NOT NULL,
    hostid character varying(250) NOT NULL
);

--
-- Name: multiplemonitor; Type: TABLE; Schema: public;
--

CREATE TABLE public.multiplemonitor (
    countconfig integer NOT NULL,
    counthost integer NOT NULL,
    countrule integer NOT NULL,
    hostid character varying(250) NOT NULL
);

--
-- Name: rules; Type: TABLE; Schema: public;
--

CREATE TABLE public.rules (
    hostids VARCHAR(16000),
    modetrans integer,
    recvpath character varying(8096),
    sendpath character varying(8096),
    archivepath character varying(8096),
    workpath character varying(8096),
    rpretasks VARCHAR(16000),
    rposttasks VARCHAR(16000),
    rerrortasks VARCHAR(16000),
    spretasks VARCHAR(16000),
    sposttasks VARCHAR(16000),
    serrortasks VARCHAR(16000),
    updatedinfo integer,
    idrule character varying(250) NOT NULL
);

--
-- Name: runner; Type: TABLE; Schema: public;
--

CREATE TABLE public.runner (
    specialid bigint NOT NULL,
    globalstep integer NOT NULL,
    globallaststep integer NOT NULL,
    step integer NOT NULL,
    rank integer NOT NULL,
    blocksz integer NOT NULL,
    modetrans integer NOT NULL,
    updatedinfo integer NOT NULL,
    stepstatus character(3) NOT NULL,
    infostatus character(3) NOT NULL,
    retrievemode boolean NOT NULL,
    ismoved boolean NOT NULL,
    starttrans timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    stoptrans timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    ownerreq character varying(250) NOT NULL,
    requester character varying(250) NOT NULL,
    requested character varying(250) NOT NULL,
    idrule character varying(250) NOT NULL,
    filename character varying(8096) NOT NULL,
    originalname character varying(8096) NOT NULL,
    fileinfo VARCHAR(8096) NOT NULL,
    transferinfo VARCHAR(8096) NOT NULL
);

--
-- Name: runseq; Type: SEQUENCE; Schema: public;
--

CREATE SEQUENCE public.runseq
    START WITH -9223372036854775807
    INCREMENT BY 1
    MINVALUE -9223372036854775807
    NO MAXVALUE
    CACHE 1;

--
-- Name: runseq; Type: SEQUENCE SET; Schema: public;
--

SELECT pg_catalog.setval('public.runseq', -9223372036854775805, true);

--
-- Name: configuration configuration_pkey; Type: CONSTRAINT; Schema: public;
--

ALTER TABLE ONLY public.configuration
    ADD CONSTRAINT configuration_pkey PRIMARY KEY (hostid);

--
-- Name: hostconfig hostconfig_pkey; Type: CONSTRAINT; Schema: public;
--

ALTER TABLE ONLY public.hostconfig
    ADD CONSTRAINT hostconfig_pkey PRIMARY KEY (hostid);

--
-- Name: hosts hosts_pkey; Type: CONSTRAINT; Schema: public;
--

ALTER TABLE ONLY public.hosts
    ADD CONSTRAINT hosts_pkey PRIMARY KEY (hostid);

--
-- Name: multiplemonitor multiplemonitor_pkey; Type: CONSTRAINT; Schema: public;
--

ALTER TABLE ONLY public.multiplemonitor
    ADD CONSTRAINT multiplemonitor_pkey PRIMARY KEY (hostid);

--
-- Name: rules rules_pkey; Type: CONSTRAINT; Schema: public;
--

ALTER TABLE ONLY public.rules
    ADD CONSTRAINT rules_pkey PRIMARY KEY (idrule);

--
-- Name: runner runner_pk; Type: CONSTRAINT; Schema: public;
--

ALTER TABLE ONLY public.runner
    ADD CONSTRAINT runner_pk PRIMARY KEY (specialid, ownerreq, requester, requested);

--
-- Type: INDEX; Schema: public;
--

CREATE INDEX IF NOT EXISTS idx_config ON public.configuration USING btree (hostid, updatedinfo);
CREATE INDEX IF NOT EXISTS idx_hostconf ON public.hostconfig USING btree (hostid, updatedinfo);
CREATE INDEX IF NOT EXISTS idx_host ON public.hosts USING btree (updatedinfo);
CREATE INDEX IF NOT EXISTS idx_rule ON public.rules USING btree (updatedinfo);
CREATE INDEX  IF NOT EXISTS idx_run_filter ON public.runner USING btree (ownerreq, starttrans, updatedinfo, stepstatus, infostatus, globallaststep, globalstep, requested, stoptrans);
