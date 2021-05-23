--
-- Data for Name: configuration; Type: TABLE DATA; Schema: public; Owner: waarp
--

INSERT INTO configuration VALUES (1, 2, 3, 4, 5, 1, 'server1');
INSERT INTO configuration VALUES (2, 3, 2, 2, 2, 0, 'server2');
INSERT INTO configuration VALUES (5, 6, 3, 4, 3, 0, 'server3');


--
-- Data for Name: hostconfig; Type: TABLE DATA; Schema: public; Owner: waarp
--

INSERT INTO hostconfig VALUES ('joyaux', 'marchand', 'le borgne', 'misc', 1, 'server1');
INSERT INTO hostconfig VALUES ('ba', '', '', '<root><version>3.0.9</version></root>', 0, 'server2');
INSERT INTO hostconfig VALUES ('ba', '', '', '<root><version>3.0.9</version></root>', 0, 'server3');
INSERT INTO hostconfig VALUES ('', '', '', '<root><version>3.0.9</version></root>', 0, 'server4');
INSERT INTO hostconfig VALUES ('', '', '', '<root><version>3.0.9</version></root>', 0, 'server5');


--
-- Data for Name: hosts; Type: TABLE DATA; Schema: public; Owner: waarp
--

INSERT INTO hosts VALUES ('127.0.0.1', 6666, true,
'303465626439323336346235616136306332396630346461353132616361346265303639646336633661383432653235', false, true,
false, true, 3, 'server1');
INSERT INTO hosts VALUES ('127.0.0.1', 6667, true,
'303465626439323336346235616136306332396630346461353132616361346265303639646336633661383432653235', false, false,
true, false, 0, 'server1-ssl');
INSERT INTO hosts VALUES ('127.0.0.3', 6676, false,
'303465626439323336346235616136306332396630346461353132616361346265303639646336633661383432653235', false, false,
true, false, 0, 'server2');


--
-- Data for Name: multiplemonitor; Type: TABLE DATA; Schema: public; Owner: waarp
--

INSERT INTO multiplemonitor VALUES (11, 18, 29, 'server1');
INSERT INTO multiplemonitor VALUES (1, 2, 3, 'server2');
INSERT INTO multiplemonitor VALUES (0, 0, 0, 'server3');
INSERT INTO multiplemonitor VALUES (0, 0, 0, 'server4');


--
-- Data for Name: rules; Type: TABLE DATA; Schema: public; Owner: waarp
--

INSERT INTO rules VALUES ('<hostids></hostids>', 1, '/in', '/out', '/arch', '/work', '<tasks></tasks>', '<tasks></tasks>', '<tasks></tasks>', '<tasks></tasks>', '<tasks></tasks>', '<tasks></tasks>', 0, 'default');
INSERT INTO rules VALUES ('<hostids><hostid>blabla</hostid><hostid>blabla2</hostid><hostid>blabla3</hostid></hostids>', 1, '/in', '/out', '/arch', '/work', '<tasks></tasks>', '<tasks><task><type>test</type><path>aa</path><delay>1</delay></task></tasks>', '<tasks><task><type>test</type><path>aa</path><delay>1</delay></task><task><type>test</type><path>aa</path><delay>1</delay></task></tasks>', '<tasks><task><type>test</type><path>aa</path><delay>1</delay></task><task><type>test</type><path>aa</path><delay>1</delay></task><task><type>test</type><path>aa</path><delay>1</delay></task></tasks>', '<tasks></tasks>', '<tasks></tasks>', 42, 'dummy');
INSERT INTO rules VALUES ('<hostids></hostids>', 3, '/in', '/out', '/arch', '/work', '<tasks></tasks>', '<tasks></tasks>', '<tasks></tasks>', '<tasks></tasks>', '<tasks></tasks>', '<tasks></tasks>', 0, 'dummy2');

--
-- Data for Name: runner; Type: TABLE DATA; Schema: public; Owner: waarp
--

INSERT INTO runner VALUES (5, 0, 0, 0, 'C  ', true, 'data/server1/log/client.log', false, 'default', 65536, 'data/server1/log/client.log', 'noinfo', '{"ORIGINALSIZE":18391}', 1, '2018-06-27 14:31:37.738', '2018-06-27 14:31:58.042', 'C  ', 5, 'server1', 'server1', 'server2', -9223372036854775807);
INSERT INTO runner VALUES (5, 0, 0, 0, 'C  ', true, 'data/server1/log/client.log', false, 'default', 65536, 'data/server1/log/client.log', 'noinfo', '{"ORIGINALSIZE":52587}', 1, '2018-06-20 14:36:00.116', '2018-06-20 14:36:20.374', 'C  ', 4, 'server1', 'server1', 'server2', -9223372036854775806);
INSERT INTO runner VALUES (5, 0, 0, 0, 'C  ', true, 'tintin', false, 'tintin', 65536, 'tintin', 'noinfo', '{"ORIGINALSIZE":-1, "follow":123456789}', 1, '2018-06-22 14:39:01.28', '2018-06-22 14:39:21.518', 'C  ', 4, 'server1', 'server1', 'server2', -9223372036854775805);
INSERT INTO runner VALUES (5, 0, 0, 0, 'C  ', true, 'tintin', false, 'default', 65536, 'tintin', 'noinfo', '{"ORIGINALSIZE":-1, "follow":1234567890}', 1, '2018-06-24 14:39:01.28', '2018-06-24 14:39:21.518', 'C  ', 4, 'server1', 'server1', 'server2', 0);

