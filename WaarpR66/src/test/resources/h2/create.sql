
--
-- Name: multiplemonitor; Type: TABLE; Schema: public;
--

CREATE TABLE multiplemonitor (
    countconfig integer NOT NULL,
    counthost integer NOT NULL,
    countrule integer NOT NULL,
    hostid VARCHAR(8096) PRIMARY KEY
);

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
    hostid VARCHAR(8096) PRIMARY KEY
);

--
-- Name: hostconfig; Type: TABLE; Schema: public;
--

CREATE TABLE hostconfig (
    business longvarchar NOT NULL,
    roles longvarchar NOT NULL,
    aliases longvarchar NOT NULL,
    others longvarchar NOT NULL,
    updatedinfo integer NOT NULL,
    hostid VARCHAR(8096) PRIMARY KEY
);

--
-- Name: hosts; Type: TABLE; Schema: public;
--

CREATE TABLE hosts (
    address VARCHAR(8096) NOT NULL,
    port integer NOT NULL,
    isssl boolean NOT NULL,
    hostkey binary NOT NULL,
    adminrole boolean NOT NULL,
    isclient boolean NOT NULL,
    isactive boolean NOT NULL,
    isproxified boolean NOT NULL,
    updatedinfo integer NOT NULL,
    hostid VARCHAR(8096) PRIMARY KEY
);

--
-- Name: rules; Type: TABLE; Schema: public;
--

CREATE TABLE rules (
    hostids LONGVARCHAR,
    modetrans integer,
    recvpath VARCHAR(8096),
    sendpath VARCHAR(8096),
    archivepath VARCHAR(8096),
    workpath VARCHAR(8096),
    rpretasks LONGVARCHAR,
    rposttasks LONGVARCHAR,
    rerrortasks LONGVARCHAR,
    spretasks LONGVARCHAR,
    sposttasks LONGVARCHAR,
    serrortasks LONGVARCHAR,
    updatedinfo integer,
    idrule VARCHAR(8096) PRIMARY KEY
);

--
-- Name: runner; Type: TABLE; Schema: public;
--

CREATE TABLE runner (
    globalstep integer NOT NULL,
    globallaststep integer NOT NULL,
    step integer NOT NULL,
    rank integer NOT NULL,
    stepstatus char(3) NOT NULL,
    retrievemode boolean NOT NULL,
    filename VARCHAR(8096) NOT NULL,
    ismoved boolean NOT NULL,
    idrule VARCHAR(8096) NOT NULL,
    blocksz integer NOT NULL,
    originalname VARCHAR(8096) NOT NULL,
    fileinfo text NOT NULL,
    transferinfo text NOT NULL,
    modetrans integer NOT NULL,
    starttrans timestamp  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    stoptrans timestamp  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    infostatus char(3) NOT NULL,
    updatedinfo integer NOT NULL,
    ownerreq VARCHAR(8096) NOT NULL,
    requester VARCHAR(8096) NOT NULL,
    requested VARCHAR(8096) NOT NULL,
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


