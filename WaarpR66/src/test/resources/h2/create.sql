--
-- Name: configuration; Type: TABLE; Schema: public;
--

CREATE TABLE configuration (
    readgloballimit bigint NOT NULL,
    writegloballimit bigint NOT NULL,
    readsessionlimit bigint NOT NULL,
    writesessionlimit bigint NOT NULL,
    delaylimit bigint NOT NULL,
    updatedinfo integer NOT NULL,
    hostid character varying(8096) NOT NULL,
    PRIMARY KEY (hostid)
);

--
-- Name: hostconfig; Type: TABLE; Schema: public;
--

CREATE TABLE hostconfig (
    business text NOT NULL,
    roles text NOT NULL,
    aliases text NOT NULL,
    others text NOT NULL,
    updatedinfo integer NOT NULL,
    hostid character varying(8096) NOT NULL,
    PRIMARY KEY (hostid)
);

--
-- Name: hosts; Type: TABLE; Schema: public;
--

CREATE TABLE hosts (
    address character varying(8096) NOT NULL,
    port integer NOT NULL,
    isssl boolean NOT NULL,
    hostkey bytea(255) NOT NULL,
    adminrole boolean NOT NULL,
    isclient boolean NOT NULL,
    isactive boolean NOT NULL,
    isproxified boolean NOT NULL,
    updatedinfo integer NOT NULL,
    hostid character varying(8096) NOT NULL,
    PRIMARY KEY (hostid)
);

--
-- Name: multiplemonitor; Type: TABLE; Schema: public;
--

CREATE TABLE multiplemonitor (
    countconfig integer NOT NULL,
    counthost integer NOT NULL,
    countrule integer NOT NULL,
    hostid character varying(8096) NOT NULL,
    PRIMARY KEY (hostid)
);

--
-- Name: rules; Type: TABLE; Schema: public;
--

CREATE TABLE rules (
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
    idrule character varying(8096) NOT NULL,
    PRIMARY KEY (idrule)
);

--
-- Name: runner; Type: TABLE; Schema: public;
--

CREATE TABLE runner (
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
    specialid bigint NOT NULL,
    PRIMARY KEY (ownerreq, requester, requested, specialid)
);

--
-- Name: runseq; Type: SEQUENCE; Schema: public;
--

CREATE SEQUENCE runseq
    START WITH -9223372036854775807
    INCREMENT BY 1
    MINVALUE -9223372036854775807
    NO MAXVALUE
    NOCACHE;
--
-- Name: idx_runner; Type: INDEX; Schema: public;
--

CREATE INDEX idx_runner ON runner (
  starttrans, ownerreq, stepstatus, updatedinfo, globalstep, infostatus,
  specialid);


