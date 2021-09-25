use test;

--
-- Name: configuration; Type: TABLE;
--

CREATE TABLE configuration (
    readgloballimit bigint NOT NULL,
    writegloballimit bigint NOT NULL,
    readsessionlimit bigint NOT NULL,
    writesessionlimit bigint NOT NULL,
    delaylimit bigint NOT NULL,
    updatedinfo integer NOT NULL,
    hostid character varying(250) NOT NULL,
    PRIMARY KEY (hostid)
);

--
-- Name: hostconfig; Type: TABLE;
--

CREATE TABLE hostconfig (
    business TEXT NOT NULL,
    roles TEXT NOT NULL,
    aliases TEXT NOT NULL,
    others TEXT NOT NULL,
    updatedinfo integer NOT NULL,
    hostid character varying(250) NOT NULL,
    PRIMARY KEY (hostid)
);

--
-- Name: hosts; Type: TABLE;
--

CREATE TABLE hosts (
    address character varying(250) NOT NULL,
    port integer NOT NULL,
    isssl boolean NOT NULL,
    hostkey VARBINARY(1024) NOT NULL,
    adminrole boolean NOT NULL,
    isclient boolean NOT NULL,
    isactive boolean NOT NULL,
    isproxified boolean NOT NULL,
    updatedinfo integer NOT NULL,
    hostid character varying(250) NOT NULL,
    PRIMARY KEY (hostid)
);

--
-- Name: multiplemonitor; Type: TABLE;
--

CREATE TABLE multiplemonitor (
    countconfig integer NOT NULL,
    counthost integer NOT NULL,
    countrule integer NOT NULL,
    hostid character varying(250) NOT NULL,
    PRIMARY KEY (hostid)
);

--
-- Name: rules; Type: TABLE;
--

CREATE TABLE rules (
    hostids TEXT,
    modetrans integer,
    recvpath character varying(8096),
    sendpath character varying(8096),
    archivepath character varying(8096),
    workpath character varying(8096),
    rpretasks TEXT,
    rposttasks TEXT,
    rerrortasks TEXT,
    spretasks TEXT,
    sposttasks TEXT,
    serrortasks TEXT,
    updatedinfo integer,
    idrule character varying(250) NOT NULL,
    PRIMARY KEY (idrule)
);

--
-- Name: runner; Type: TABLE;
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
    idrule character varying(250) NOT NULL,
    blocksz integer NOT NULL,
    originalname character varying(8096) NOT NULL,
    fileinfo VARCHAR(8096) NOT NULL,
    transferinfo VARCHAR(8096) NOT NULL,
    modetrans integer NOT NULL,
    starttrans timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    stoptrans timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    infostatus character(3) NOT NULL,
    updatedinfo integer NOT NULL,
    ownerreq character varying(250) NOT NULL,
    requester character varying(250) NOT NULL,
    requested character varying(250) NOT NULL,
    specialid bigint NOT NULL,
    PRIMARY KEY (ownerreq, requester, requested, specialid)
);

CREATE SEQUENCE IF NOT EXISTS RUNSEQ MINVALUE -9223372036854775807 START WITH 1;

CREATE INDEX IF NOT EXISTS idx_config USING btree ON configuration (hostid, updatedinfo);
CREATE INDEX IF NOT EXISTS idx_hostconf USING btree ON hostconfig (hostid, updatedinfo);
CREATE INDEX IF NOT EXISTS idx_host USING btree ON hosts (updatedinfo);
CREATE INDEX IF NOT EXISTS idx_rule USING btree ON rules (updatedinfo);
CREATE INDEX IF NOT EXISTS idx_run_filter USING btree ON runner (ownerreq, starttrans, updatedinfo, stepstatus, infostatus, globallaststep, globalstep, requested, stoptrans);