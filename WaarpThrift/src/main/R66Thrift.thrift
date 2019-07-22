namespace java org.waarp.thrift.r66
namespace cpp org.waarp.thrift.r66
namespace php OrgWaarpThriftR66
namespace perl OrgWaarpThriftR66
namespace py OrgWaarpThriftR66
namespace py.twisted OrgWaarpThriftR66
namespace rb OrgWaarpThrift.R66
namespace csharp OrgWaarpThrift.R66
namespace js OrgWaarpThriftR66
namespace st OrgWaarpThriftR66
namespace delphi OrgWaarpThrift.R66
namespace cocoa OrgWaarpThriftR66
namespace go OrgWaarpThriftR66
namespace * org.waarp.thrift.r66

// Error Codes
enum ErrorCode {
	/**
	 * Code stands for initialization ok (internal connection, authentication)
	 */
	InitOk,
	/**
	 * Code stands for pre processing ok
	 */
	PreProcessingOk,
	/**
	 * Code stands for transfer OK
	 */
	TransferOk,
	/**
	 * Code stands for post processing ok
	 */
	PostProcessingOk,
	/**
	 * Code stands for All action are completed ok
	 */
	CompleteOk,
	/**
	 * Code stands for connection is impossible (remote or local reason)
	 */
	ConnectionImpossible,
	/**
	 * Code stands for connection is impossible now due to limits(remote or local reason)
	 */
	ServerOverloaded,
	/**
	 * Code stands for bad authentication (remote or local)
	 */
	BadAuthent,
	/**
	 * Code stands for External operation in error (pre, post or error processing)
	 */
	ExternalOp,
	/**
	 * Code stands for Transfer is in error
	 */
	TransferError,
	/**
	 * Code stands for Transfer in error due to MD5
	 */
	MD5Error,
	/**
	 * Code stands for Network disconnection
	 */
	Disconnection,
	/**
	 * Code stands for Remote Shutdown
	 */
	RemoteShutdown,
	/**
	 * Code stands for final action (like moving file) is in error
	 */
	FinalOp,
	/**
	 * Code stands for unimplemented feature
	 */
	Unimplemented,
	/**
	 * Code stands for shutdown is in progress
	 */
	Shutdown,
	/**
	 * Code stands for a remote error is received
	 */
	RemoteError,
	/**
	 * Code stands for an internal error
	 */
	Internal,
	/**
	 * Code stands for a request of stopping transfer
	 */
	StoppedTransfer,
	/**
	 * Code stands for a request of canceling transfer
	 */
	CanceledTransfer,
	/**
	 * Warning in execution
	 */
	Warning,
	/**
	 * Code stands for unknown type of error
	 */
	Unknown,
	/**
	 * Code stands for a request that is already remotely finished
	 */
	QueryAlreadyFinished,
	/**
	 * Code stands for request that is still running
	 */
	QueryStillRunning,
	/**
	 * Code stands for not known host
	 */
	NotKnownHost,
	/**
	 * Code stands for self requested host starting request is invalid
	 */
	LoopSelfRequestedHost,
	/**
	 * Code stands for request should exist but is not found on remote host
	 */
	QueryRemotelyUnknown,
	/**
	 * Code stands for File not found error
	 */
	FileNotFound,
	/**
	 * Code stands for Command not found error
	 */
	CommandNotFound,
	/**
	 * Code stands for a request in PassThroughMode and required action is incompatible with this
	 * mode
	 */
	PassThroughMode,
	/**
	 * Code stands for running step
	 */
	Running
}


enum RequestMode {
/*
	Needs at least 3 or 4 arguments:
		the XML client configuration file,

		'-to' the remoteHost Id,
		'-file' the file to transfer,
		'-rule' the rule

	Other options:
		'-info' information to send,
		'-md5' to force MD5 by packet control,
		'-block' size of packet > 1K (prefered is 64K),
		'-nolog' to not log locally this action
		'-start' \"time start\" as yyyyMMddHHmmss (override previous -delay options)
		'-delay' \"+delay in ms\" as delay in ms from current time(override previous -start options)
		'-delay' \"delay in ms\" as time in ms (override previous -start options)
*/
	SYNCTRANSFER = 1,
	ASYNCTRANSFER = 2,
/*
	Needs at least 5 arguments:
		the XML client configuration file,
		'-id' the transfer Id,
		'-to' the requested host Id or '-from' the requester host Id (localhost will be the opposite),

	Other options (only one):
		'-cancel' to cancel completely the transfer,
		'-stop' to stop the transfer (maybe restarted),
		'-restart' to restart if possible a transfer
*/
	INFOREQUEST = 3,
/*
	Needs at least 3 arguments:
		the XML client configuration file,
		'-to' the remoteHost Id,
		'-rule' the rule

	Other options:
		'-file' the optional file for which to get info,
		'-exist' to test the existence
		'-detail' to get the detail on file
		'-list' to get the list of files
		'-mlsx' to get the list and details of files
*/
	INFOFILE = 4
}

enum Action {
	Exist = 1,
	Cancel = 2,
	Stop = 3,
	Restart = 4,
	Detail = 10,
	List = 11,
	Mlsx = 12
}

struct R66Result {
	1: required RequestMode mode;
	// TRANSFER MODE
	2: optional string fromuid;
	3: optional string destuid;
	4: optional string file;
	5: optional string rule;
	// INFO MODE
	20: optional i64 tid;
	21: optional Action action;
	
	// Specific part for result
	30: required ErrorCode code;
	31: required string resultinfo;
/*
    <globalstep>2</globalstep>
    <globallaststep>2</globallaststep>
    <step>1</step>
    <rank>237</rank>
    <retrievemode>false</retrievemode>
    <ismoved>false</ismoved>
    <originalname>Documents2.rar</originalname>
    <blocksize>65536</blocksize>
    <mode>1</mode>
    <start>2009-08-13 12:26:43.209</start>
    <stop>2009-08-13 12:26:46.079</stop>
*/
	32: optional i32 globalstep;
    33: optional i32 globallaststep;
    34: optional i32 step;
    35: optional i32 rank;
    36: optional bool retrievemode;
    37: optional bool ismoved;
    38: optional string originalfilename;
    39: optional i32 blocksize;
	40: optional i32 modetransfer;
	41: optional string start;
	42: optional string stop;
}

struct R66Request {
	1: required RequestMode mode;
	// TRANSFER MODE
	2: optional string fromuid;
	3: optional string destuid;
	4: optional string file;
	5: optional string rule;
	
	// optional information
	10: optional string info;
	11: optional bool md5 = false;
	12: optional i32 blocksize;
	13: optional string start;
	14: optional string delay;
	15: optional bool notrace = false; 
	
	// INFO MODE
	20: optional i64 tid;
	21: optional Action action;
}

service R66Service {
	// Async operations (if any ?)
	// Sync operations
	// Transfer: SYNCTRANSFER or ASYNCTRANSFER
	R66Result transferRequestQuery(1:R66Request request);
	// Request on Transfer: INFOREQUEST
	R66Result infoTransferQuery(1:R66Request request);
	bool isStillRunning(1:string fromuid, 2:string touid, 3:i64 tid);
	// Request on Files: INFOFILE
	list<string> infoListQuery(1:R66Request request);
}
