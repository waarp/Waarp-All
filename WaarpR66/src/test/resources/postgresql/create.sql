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
    hostid character varying(8096) NOT NULL
);

--
-- Name: hostconfig; Type: TABLE; Schema: public;
--

CREATE TABLE public.hostconfig (
    business text NOT NULL,
    roles text NOT NULL,
    aliases text NOT NULL,
    others text NOT NULL,
    updatedinfo integer NOT NULL,
    hostid character varying(8096) NOT NULL
);

--
-- Name: hosts; Type: TABLE; Schema: public;
--

CREATE TABLE public.hosts (
    address character varying(8096) NOT NULL,
    port integer NOT NULL,
    isssl boolean NOT NULL,
    hostkey bytea NOT NULL,
    adminrole boolean NOT NULL,
    isclient boolean NOT NULL,
    isactive boolean NOT NULL,
    isproxified boolean NOT NULL,
    updatedinfo integer NOT NULL,
    hostid character varying(8096) NOT NULL
);

--
-- Name: multiplemonitor; Type: TABLE; Schema: public;
--

CREATE TABLE public.multiplemonitor (
    countconfig integer NOT NULL,
    counthost integer NOT NULL,
    countrule integer NOT NULL,
    hostid character varying(8096) NOT NULL
);

--
-- Name: rules; Type: TABLE; Schema: public;
--

CREATE TABLE public.rules (
    hostids text,
    modetrans integer,
    recvpath character varying(8096),
    sendpath character varying(8096),
    archivepath character varying(8096),
    workpath character varying(8096),
    rpretasks text,
    rposttasks text,
    rerrortasks text,
    spretasks text,
    sposttasks text,
    serrortasks text,
    updatedinfo integer,
    idrule character varying(8096) NOT NULL
);

--
-- Name: runner; Type: TABLE; Schema: public;
--

CREATE TABLE public.runner (
    globalstep integer NOT NULL,
    globallaststep integer NOT NULL,
    step integer NOT NULL,
    rank integer NOT NULL,
    stepstatus character(3) NOT NULL,
    retrievemode boolean NOT NULL,
    filename character varying(8096) NOT NULL,
    ismoved boolean NOT NULL,
    idrule character varying(8096) NOT NULL,
    blocksz integer NOT NULL,
    originalname character varying(8096) NOT NULL,
    fileinfo text NOT NULL,
    transferinfo text NOT NULL,
    modetrans integer NOT NULL,
    starttrans timestamp without time zone NOT NULL,
    stoptrans timestamp without time zone NOT NULL,
    infostatus character(3) NOT NULL,
    updatedinfo integer NOT NULL,
    ownerreq character varying(8096) NOT NULL,
    requester character varying(8096) NOT NULL,
    requested character varying(8096) NOT NULL,
    specialid bigint NOT NULL
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
    ADD CONSTRAINT runner_pk PRIMARY KEY (ownerreq, requester, requested, specialid);

--
-- Name: idx_runner; Type: INDEX; Schema: public;
--

CREATE INDEX idx_runner ON public.runner USING btree (starttrans, ownerreq, stepstatus, updatedinfo, globalstep, infostatus, specialid);

